package com.openmind.hagap.infrastructure.parsing;

import java.nio.file.Path;

public interface DocumentParser {

    boolean supports(String filename);

    String parse(Path filePath);
}
