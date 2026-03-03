package com.openmind.hagap.application.service;

import com.openmind.hagap.infrastructure.vector.VectorSearchResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptAssembler {

    public String assembleWithContext(String userQuery, List<VectorSearchResult> retrievedChunks) {
        if (retrievedChunks.isEmpty()) {
            return userQuery;
        }

        String context = retrievedChunks.stream()
                .map(chunk -> String.format(
                        "[Source: %s, Chunk %d, Score: %.2f]\n%s",
                        chunk.getSourceFile(),
                        chunk.getChunkIndex(),
                        chunk.getScore(),
                        chunk.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));

        return String.format("""
                Use the following context to answer the question. \
                Only use information from the provided context. \
                If the context doesn't contain enough information, say so clearly.

                ## Context
                %s

                ## Question
                %s
                """, context, userQuery);
    }
}
