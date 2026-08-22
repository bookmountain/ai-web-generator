package com.book.aiwebgenerator.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlUtilsTest {

    @Test
    void joinsUrlAndNormalizesBoundarySlashes() {
        String url = UrlUtils.joinUrl(
                "https://cdn.example.com/",
                "/ai-web-generator/",
                "mermaid/diagram.svg"
        );

        assertEquals(
                "https://cdn.example.com/ai-web-generator/mermaid/diagram.svg",
                url
        );
    }

    @Test
    void ignoresBlankAndNullSegments() {
        String url = UrlUtils.joinUrl("https://cdn.example.com", "", null, "/image.svg");

        assertEquals("https://cdn.example.com/image.svg", url);
    }

    @Test
    void rejectsBlankBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> UrlUtils.joinUrl("  ", "image.svg"));
    }
}
