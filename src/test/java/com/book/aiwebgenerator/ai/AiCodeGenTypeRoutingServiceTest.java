package com.book.aiwebgenerator.ai;

import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
public class AiCodeGenTypeRoutingServiceTest {

    @Resource
    private AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService;

    @Test
    public void testRouteCodeGenType() {
        String userPrompt = "Create a simple personal introduction page";

        CodeGenTypeEnum result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("User requirement: {} -> {}", userPrompt, result.getValue());

        userPrompt = "Create a company official website with three pages: Home, About Us, and Contact Us";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("User requirement: {} -> {}", userPrompt, result.getValue());

        userPrompt = "Create an e-commerce management system with user management, product management, and order management, plus routing and state management";
        result = aiCodeGenTypeRoutingService.routeCodeGenType(userPrompt);
        log.info("User requirement: {} -> {}", userPrompt, result.getValue());
    }
}