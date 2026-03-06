package com.book.aiwebgenerator.core;

import com.book.aiwebgenerator.ai.model.HtmlCodeResult;
import com.book.aiwebgenerator.ai.model.MultiFileCodeResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CodeParserTest {
    @Test
    void parseHtmlCode() {
        String codeContent = """
                Random description:
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Test Page</title>
                </head>
                <body>
                    <h1>Hello World!</h1>
                </body>
                </html>
                ```
                
                Another random description
                """;
        HtmlCodeResult result = CodeParser.parseHtmlCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
    }

    @Test
    void parseMultiFileCode() {
        String codeContent = """
                Create a complete webpage:
                ```html
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Multi-file Example</title>
                    <link rel="stylesheet" href="style.css">
                </head>
                <body>
                    <h1>Welcome</h1>
                    <script src="script.js"></script>
                </body>
                </html>
                ```
                
                ```css
                h1 {
                    color: blue;
                    text-align: center;
                }
                ```
                
                ```js
                console.log('Page loaded');
                ```
                
                Files created successfully!
                """;
        MultiFileCodeResult result = CodeParser.parseMultiFileCode(codeContent);
        assertNotNull(result);
        assertNotNull(result.getHtmlCode());
        assertNotNull(result.getCssCode());
        assertNotNull(result.getJsCode());
    }
}
