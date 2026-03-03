package com.openmind.hagap.infrastructure.embedding;

import java.util.List;

public interface EmbeddingService {

    List<Float> generateEmbedding(String text);

    List<List<Float>> generateEmbeddings(List<String> texts);
}
