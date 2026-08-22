package com.book.aiwebgenerator.langgraph4j.ai;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ImageCollectionServiceTest {

    @Resource
    private ImageCollectionService imageCollectionService;

    @Test
    void testTechWebsiteImageCollection() {
        // Note: collectImages returns List<ImageResource>, but if your test treats it as String or needs adjustment based on your return type, change accordingly.
        String result = imageCollectionService.collectImages("Build a technical blog website that needs to display programming tutorials and system architecture");
        Assertions.assertNotNull(result);
        System.out.println("Images collected for the tech website: " + result);
    }

    @Test
    void testEcommerceWebsiteImageCollection() {
        String result = imageCollectionService.collectImages("Build an e-commerce shopping website that needs to display products and brand image");
        Assertions.assertNotNull(result);
        System.out.println("Images collected for the e-commerce website: " + result);
    }
}