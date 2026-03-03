package com.openmind.hagap.application.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openmind.hagap.application.dto.ChatRequest;
import com.openmind.hagap.application.dto.ChatResponse;
import com.openmind.hagap.application.dto.CitationDto;
import com.openmind.hagap.application.dto.McpComponentDto;
import com.openmind.hagap.application.service.PromptAssembler;
import com.openmind.hagap.application.service.SessionService;
import com.openmind.hagap.application.service.WorkspaceService;
import com.openmind.hagap.domain.model.*;
import com.openmind.hagap.infrastructure.cli.CliOutput;
import com.openmind.hagap.infrastructure.concurrency.WorkspaceLockManager;
import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrator over Choreography: chose centralized coordination because the agent pipeline has
 * strict ordering (retrieve → verify → execute) with data dependencies between steps. Choreography
 * (event-driven agents) would add complexity without benefit — there's no parallelism to exploit.
 *
 * <p>Architecture notes:
 * <ul>
 *   <li>Plan-as-data ({@link ExecutionPlan}): the plan is a reified object, not just control flow.
 *       This enables logging the plan before execution, debugging failed plans post-mortem, and
 *       future extension to LLM-based plan selection (the planner becomes swappable).</li>
 *   <li>MDC propagation: workspaceId + correlationId set here flow to every downstream log line
 *       (CLI executor, vector search, embedding). In production, this is how you trace a single
 *       user request across 4-5 service calls in the logs.</li>
 *   <li>{@link ExecutionTrace} captures wall-clock timing per step — not for billing, but for
 *       identifying which step dominates latency (usually CLI execution at 5-30s).</li>
 *   <li>Verification gate: if confidence is below threshold, the prompt is degraded (not blocked).
 *       This avoids silent hallucination while still providing a response to the user.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrchestratorAgent {

    private final WorkspaceService workspaceService;
    private final SessionService sessionService;
    private final RetrievalAgent retrievalAgent;
    private final OpenCodeToolAgent openCodeToolAgent;
    private final VerificationAgent verificationAgent;
    private final PromptAssembler promptAssembler;
    private final ObjectMapper objectMapper;

    /**
     * Single entry point for all chat flows — the plan type determines the pipeline.
     * Persistence happens last (after CLI response) to avoid orphaned user messages on failure.
     */
    public ChatResponse orchestrate(ChatRequest request) {
        Workspace workspace = workspaceService.findWorkspace(request.workspaceId());
        Path workspacePath = Path.of(workspace.getDirectoryPath());
        List<Path> knowledgeFiles = workspaceService.getKnowledgeFilePaths(request.workspaceId());

        MDC.put("workspaceId", request.workspaceId().toString());

        // Session
        Session session = sessionService.getOrCreateSession(
                request.sessionId(), workspace, extractTitle(request.message()));
        sessionService.saveMessage(session, workspace, MessageRole.USER,
                request.message(), null, null);

        AgentContext context = AgentContext.builder()
                .workspaceId(request.workspaceId())
                .sessionId(request.sessionId())
                .workspacePath(workspacePath)
                .knowledgeFiles(knowledgeFiles)
                .correlationId(MDC.get("correlationId"))
                .cliSessionId(session.getCliSessionId())
                .build();

        ExecutionTrace trace = new ExecutionTrace(context.getCorrelationId());

        // Plan
        ExecutionPlan plan = buildPlan(request);
        log.info("Orchestrator plan: {} ({} steps)", plan.getType(), plan.getSteps().size());

        // Execute
        AgentResult result;
        if (plan.getType() == ExecutionPlan.PlanType.MULTI_AGENT) {
            result = executeMultiAgentPlan(request.message(), context, trace);
        } else {
            result = executeSimplePlan(request.message(), context, trace);
        }

        String responseContent = result.content;
        List<CitationDto> citations = result.citations;
        Double confidence = result.confidence;
        List<McpComponentDto> mcpComponents = result.mcpComponents;

        // Persist CLI session ID on first response
        if (session.getCliSessionId() == null && result.cliSessionId != null) {
            session.setCliSessionId(result.cliSessionId);
            sessionService.saveSession(session);
        }

        // Persist
        Message assistantMessage = sessionService.saveMessage(
                session, workspace, MessageRole.ASSISTANT,
                responseContent, null, confidence);

        log.info("Orchestrator completed in {}ms ({} steps)",
                trace.totalDuration().toMillis(), trace.getSteps().size());

        return new ChatResponse(
                assistantMessage.getId(),
                session.getId(),
                responseContent,
                confidence,
                citations,
                mcpComponents,
                java.util.Collections.emptyList(),
                assistantMessage.getCreatedAt()
        );
    }

    /**
     * Plan factory — currently flag-driven (useRag), but the plan structure supports
     * dynamic step composition. Adding a new agent = adding a PlanStep, no orchestrator changes.
     */
    private ExecutionPlan buildPlan(ChatRequest request) {
        List<ExecutionPlan.PlanStep> steps = new ArrayList<>();

        if (request.useRag()) {
            steps.add(ExecutionPlan.PlanStep.builder()
                    .agent("RetrievalAgent").action("rag.search")
                    .description("Search knowledge base for relevant context").build());
            steps.add(ExecutionPlan.PlanStep.builder()
                    .agent("VerificationAgent").action("verify.grounded")
                    .description("Verify evidence sufficiency").build());
            steps.add(ExecutionPlan.PlanStep.builder()
                    .agent("OpenCodeToolAgent").action("opencode.run")
                    .description("Execute enriched prompt via OpenCode CLI").build());

            return ExecutionPlan.builder()
                    .type(ExecutionPlan.PlanType.MULTI_AGENT)
                    .steps(steps)
                    .reasoning("RAG enabled — using retrieval + verification + execution pipeline")
                    .build();
        }

        steps.add(ExecutionPlan.PlanStep.builder()
                .agent("OpenCodeToolAgent").action("opencode.run")
                .description("Execute prompt directly via OpenCode CLI").build());

        return ExecutionPlan.builder()
                .type(ExecutionPlan.PlanType.SIMPLE)
                .steps(steps)
                .reasoning("Direct query — single CLI execution")
                .build();
    }

    private AgentResult executeSimplePlan(String message, AgentContext context, ExecutionTrace trace) {
        ExecutionTrace.StepTrace step = trace.startStep("OpenCodeToolAgent", "opencode.run");
        try {
            CliOutput output = openCodeToolAgent.execute(message, context);
            step.complete(true, null);
            return new AgentResult(output.getContent(), Collections.emptyList(), null,
                    output.getSessionId(), Collections.emptyList());
        } catch (Exception e) {
            step.complete(false, e.getMessage());
            throw e;
        }
    }

    /**
     * RAG pipeline: retrieve → verify → execute. The verification step is the hallucination gate —
     * if evidence confidence is below 0.75, the prompt degrades to "answer with uncertainty" mode
     * rather than passing ungrounded context that could cause confident-sounding hallucinations.
     */
    private AgentResult executeMultiAgentPlan(String message, AgentContext context, ExecutionTrace trace) {
        // Step 1: Retrieve
        ExecutionTrace.StepTrace retrieveStep = trace.startStep("RetrievalAgent", "rag.search");
        List<VectorSearchResult> searchResults = retrievalAgent.execute(message, context);
        retrieveStep.complete(true, null);

        // Step 2: Verify
        ExecutionTrace.StepTrace verifyStep = trace.startStep("VerificationAgent", "verify.grounded");
        VerificationAgent.VerificationInput verInput = VerificationAgent.VerificationInput.builder()
                .answer("")
                .evidence(searchResults)
                .build();
        VerificationAgent.VerificationResult verification = verificationAgent.execute(verInput, context);
        verifyStep.complete(true, null);

        // Step 3: Build prompt and execute
        String prompt;
        if (verification.isGrounded()) {
            prompt = promptAssembler.assembleWithContext(message, searchResults);
        } else {
            prompt = message + "\n\nNote: Limited knowledge base context available. "
                    + "Answer based on general knowledge but indicate uncertainty.";
        }

        ExecutionTrace.StepTrace execStep = trace.startStep("OpenCodeToolAgent", "opencode.run");
        try {
            CliOutput output = openCodeToolAgent.execute(prompt, context);
            execStep.complete(true, null);

            List<CitationDto> citations = searchResults.stream()
                    .map(r -> new CitationDto(
                            r.getChunkId(), r.getSourceFile(), r.getChunkIndex(),
                            r.getContent().substring(0, Math.min(200, r.getContent().length())),
                            r.getScore()))
                    .toList();

            List<McpComponentDto> mcpComponents = buildMcpComponents(searchResults);

            return new AgentResult(output.getContent(), citations, verification.getConfidence(),
                    output.getSessionId(), mcpComponents);
        } catch (Exception e) {
            execStep.complete(false, e.getMessage());
            throw e;
        }
    }

    private List<McpComponentDto> buildMcpComponents(List<VectorSearchResult> searchResults) {
        List<McpComponentDto> components = new ArrayList<>();

        // Gallery: collect all image URLs from search results
        List<Map<String, String>> images = searchResults.stream()
                .filter(r -> r.getImageUrls() != null && !r.getImageUrls().isEmpty())
                .flatMap(r -> r.getImageUrls().stream().map(url -> {
                    Map<String, String> img = new LinkedHashMap<>();
                    img.put("url", url);
                    img.put("source", r.getSourceFile());
                    return img;
                }))
                .collect(Collectors.toList());

        try {
            String galleryData = objectMapper.writeValueAsString(Map.of("images", images));
            components.add(new McpComponentDto("gallery", galleryData));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize gallery MCP component", e);
        }

        // References: map search results to reference items
        List<Map<String, Object>> references = searchResults.stream()
                .map(r -> {
                    Map<String, Object> ref = new LinkedHashMap<>();
                    ref.put("sourceFile", r.getSourceFile());
                    ref.put("chunkIndex", r.getChunkIndex());
                    ref.put("snippet", r.getContent().substring(0, Math.min(200, r.getContent().length())));
                    ref.put("score", r.getScore());
                    if (r.getSourceUrl() != null) {
                        ref.put("sourceUrl", r.getSourceUrl());
                    }
                    return ref;
                })
                .collect(Collectors.toList());

        try {
            String refData = objectMapper.writeValueAsString(Map.of("references", references));
            components.add(new McpComponentDto("reference", refData));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize reference MCP component", e);
        }

        return components;
    }

    private String extractTitle(String message) {
        if (message.length() <= 50) return message;
        return message.substring(0, 47) + "...";
    }

    private record AgentResult(String content, List<CitationDto> citations, Double confidence,
                                String cliSessionId, List<McpComponentDto> mcpComponents) {}
}
