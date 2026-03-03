package com.openmind.hagap.application.agent;

import com.openmind.hagap.infrastructure.cli.CliOutput;
import com.openmind.hagap.infrastructure.cli.OpenCodeCliExecutor;
import com.openmind.hagap.infrastructure.concurrency.WorkspaceLockManager;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OpenCodeToolAgent implements AgentTool<String, CliOutput> {

    private final OpenCodeCliExecutor cliExecutor;
    private final WorkspaceLockManager lockManager;
    private final CircuitBreaker cliBreaker;
    private final Timer cliTimer;

    public OpenCodeToolAgent(OpenCodeCliExecutor cliExecutor,
                              WorkspaceLockManager lockManager,
                              @Qualifier("cliCircuitBreaker") CircuitBreaker cliBreaker,
                              @Qualifier("cliExecutionTimer") Timer cliTimer) {
        this.cliExecutor = cliExecutor;
        this.lockManager = lockManager;
        this.cliBreaker = cliBreaker;
        this.cliTimer = cliTimer;
    }

    @Override
    public String name() {
        return "opencode.run";
    }

    @Override
    public CliOutput execute(String prompt, AgentContext context) {
        if (!lockManager.tryAcquire(context.getWorkspaceId())) {
            throw new com.openmind.hagap.domain.exception.CliExecutionException(
                    "Workspace busy — too many concurrent CLI executions");
        }

        try {
            return cliTimer.record(() ->
                    cliBreaker.executeSupplier(() ->
                            cliExecutor.execute(prompt, context.getWorkspacePath(), context.getKnowledgeFiles(), context.getCliSessionId())
                    )
            );
        } finally {
            lockManager.release(context.getWorkspaceId());
        }
    }
}
