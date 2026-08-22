package com.book.aiwebgenerator.utils;

import java.util.Objects;

public final class UrlUtils {

    private UrlUtils() {
    }

    public static String joinUrl(String baseUrl, String... pathSegments) {
        Objects.requireNonNull(baseUrl, "Base URL cannot be null");

        String joinedUrl = trimTrailingSlashes(baseUrl.trim());
        if (joinedUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL cannot be blank");
        }
        if (pathSegments == null) {
            return joinedUrl;
        }

        for (String pathSegment : pathSegments) {
            if (pathSegment == null) {
                continue;
            }
            String normalizedSegment = trimSlashes(pathSegment.trim());
            if (!normalizedSegment.isBlank()) {
                joinedUrl += "/" + normalizedSegment;
            }
        }
        return joinedUrl;
    }

    private static String trimSlashes(String value) {
        return trimTrailingSlashes(value.replaceFirst("^/+", ""));
    }

    private static String trimTrailingSlashes(String value) {
        return value.replaceFirst("/+$", "");
    }
}
