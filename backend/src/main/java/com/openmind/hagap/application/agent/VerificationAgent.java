package com.openmind.hagap.application.agent;

import com.openmind.hagap.application.service.HallucinationControlService;
import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import io.micrometer.core.instrument.Counter;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class VerificationAgent implements AgentTool<VerificationAgent.VerificationInput, VerificationAgent.VerificationResult> {

    private final HallucinationControlService hallucinationControl;
    private final Counter rejectionCounter;

    public VerificationAgent(HallucinationControlService hallucinationControl,
                              @Qualifier("hallucinationRejectionCounter") Counter rejectionCounter) {
        this.hallucinationControl = hallucinationControl;
        this.rejectionCounter = rejectionCounter;
    }

    @Override
    public String name() {
        return "verify.grounded";
    }

    @Override
    public VerificationResult execute(VerificationInput input, AgentContext context) {
        double confidence = hallucinationControl.calculateConfidence(input.getEvidence());
        boolean grounded = hallucinationControl.meetsThreshold(confidence);

        if (!grounded) {
            rejectionCounter.increment();
            log.warn("Verification rejected: confidence {} below threshold {} for workspace {}",
                    confidence, hallucinationControl.getThreshold(), context.getWorkspaceId());
        }

        return VerificationResult.builder()
                .grounded(grounded)
                .confidence(confidence)
                .threshold(hallucinationControl.getThreshold())
                .citationCount(input.getEvidence().size())
                .build();
    }

    @Getter
    @Builder
    public static class VerificationInput {
        private final String answer;
        private final List<VectorSearchResult> evidence;
    }

    @Getter
    @Builder
    public static class VerificationResult {
        private final boolean grounded;
        private final double confidence;
        private final double threshold;
        private final int citationCount;
    }
}
