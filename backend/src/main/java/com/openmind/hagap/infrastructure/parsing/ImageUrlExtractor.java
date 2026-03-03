package com.openmind.hagap.infrastructure.parsing;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ImageUrlExtractor {

    private static final Pattern IMAGE_PATTERN = Pattern.compile("!\\[([^\\]]*)\\]\\(([^)]+)\\)");
    private static final Pattern SOURCE_URL_PATTERN = Pattern.compile("(?m)^source_url:\\s*[\"']?([^\"'\\s]+)[\"']?");

    public record ImageInfo(String url, String alt) {}

    public List<ImageInfo> extractImages(String content) {
        if (content == null || content.isBlank()) {
            return Collections.emptyList();
        }

        List<ImageInfo> images = new ArrayList<>();
        Matcher matcher = IMAGE_PATTERN.matcher(content);

        while (matcher.find()) {
            String alt = matcher.group(1);
            String url = matcher.group(2);
            images.add(new ImageInfo(url, alt));
        }

        return images;
    }

    public String extractSourceUrl(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }

        Matcher matcher = SOURCE_URL_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
