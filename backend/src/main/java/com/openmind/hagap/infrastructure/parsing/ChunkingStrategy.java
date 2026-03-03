package com.openmind.hagap.infrastructure.parsing;

import java.util.List;

public interface ChunkingStrategy {

    List<String> chunk(String text);
}
