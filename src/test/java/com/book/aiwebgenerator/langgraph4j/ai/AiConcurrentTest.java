package com.book.aiwebgenerator.langgraph4j.ai;

import com.book.aiwebgenerator.ai.AiCodeGenTypeRoutingService;
import com.book.aiwebgenerator.ai.AiCodeGenTypeRoutingServiceFactory;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@Slf4j
@SpringBootTest
public class AiConcurrentTest {

    @Resource
    private AiCodeGenTypeRoutingServiceFactory routingServiceFactory;

    @Test
    public void testConcurrentRoutingCalls() throws InterruptedException {
        String[] prompts = {
                "Make a simple HTML page",
                "Make a multi-page website project",
                "Make a Vue management system"
        };

        ExecutorService executor = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("routing-test-", 1).factory()
        );

        try {
            List<Future<CodeGenTypeEnum>> futures = new ArrayList<>();
            for (String prompt : prompts) {
                futures.add(executor.submit(() -> {
                    AiCodeGenTypeRoutingService service = routingServiceFactory.createAiCodeGenTypeRoutingService();
                    return service.routeCodeGenType(prompt);
                }));
            }

            for (int i = 0; i < futures.size(); i++) {
                String prompt = prompts[i];
                try {
                    CodeGenTypeEnum result = futures.get(i).get(60, TimeUnit.SECONDS);
                    assertNotNull(result, "Routing result must not be null for prompt: " + prompt);
                    log.info("Request {}: {} -> {}", i + 1, prompt, result.getValue());
                } catch (ExecutionException e) {
                    throw new AssertionError("Concurrent routing failed for prompt: " + prompt, e.getCause());
                } catch (TimeoutException e) {
                    throw new AssertionError("Concurrent routing timed out for prompt: " + prompt, e);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
