package com.book.aiwebgenerator.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgUtilsTest {

    @Test
    void extractsAndAcceptsSafeSvg() {
        String response = """
                Here is the logo:
                ```svg
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 1024">
                    <defs>
                        <linearGradient id="brandGradient">
                            <stop offset="0%" stop-color="#2563eb"/>
                            <stop offset="100%" stop-color="#7c3aed"/>
                        </linearGradient>
                    </defs>
                    <circle cx="512" cy="512" r="300" fill="url(#brandGradient)"/>
                </svg>
                ```
                """;

        String svg = SvgUtils.extractAndSanitizeSvg(response);

        assertTrue(svg.startsWith("<svg"));
        assertTrue(svg.contains("viewBox=\"0 0 1024 1024\""));
    }

    @Test
    void rejectsScripts() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
                    <script>alert('unsafe')</script>
                </svg>
                """;

        assertThrows(IllegalArgumentException.class, () -> SvgUtils.extractAndSanitizeSvg(svg));
    }

    @Test
    void rejectsExternalUrls() {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10">
                    <circle cx="5" cy="5" r="5" fill="url(https://example.com/color)"/>
                </svg>
                """;

        assertThrows(IllegalArgumentException.class, () -> SvgUtils.extractAndSanitizeSvg(svg));
    }

    @Test
    void rejectsDoctypeDeclarations() {
        String svg = """
                <!DOCTYPE svg [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 10 10"><desc>&xxe;</desc></svg>
                """;

        assertThrows(IllegalArgumentException.class, () -> SvgUtils.extractAndSanitizeSvg(svg));
    }
}
