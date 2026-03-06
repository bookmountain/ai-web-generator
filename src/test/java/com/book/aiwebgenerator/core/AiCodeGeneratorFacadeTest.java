package com.book.aiwebgenerator.core;

import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.List;


@SpringBootTest
class AiCodeGeneratorFacadeTest {
    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Test
    void generateAndSaveCode() {
        File file = aiCodeGeneratorFacade.generateAndSaveCode("Generate a login page, no longer than 20 lines of code", CodeGenTypeEnum.MULTI_FILE);
        Assertions.assertNotNull(file);
    }

    @Test
    void generateAndSaveCodeStream() {
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream("Record task progress website, no longer than 20 lines of code", CodeGenTypeEnum.MULTI_FILE);
        // Block and collect the stream results for testing purposes
        List<String> result = codeStream.collectList().block();
        // Validate the result
        Assertions.assertNotNull(result);
        String completeContent = String.join("", result);
        Assertions.assertNotNull(completeContent);
    }

}