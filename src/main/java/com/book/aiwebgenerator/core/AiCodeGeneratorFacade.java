package com.book.aiwebgenerator.core;

import com.book.aiwebgenerator.ai.AiCodeGeneratorService;
import com.book.aiwebgenerator.ai.model.HtmlCodeResult;
import com.book.aiwebgenerator.ai.model.MultiFileCodeResult;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

@Slf4j
@Service
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "The code generation type cannot be empty");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCode(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCode(userMessage);
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.PARAMS_ERROR, errorMessage);
            }
        };
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenTypeEnum) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "The code generation type cannot be empty");
        }
        return switch (codeGenTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String errorMessage = "Unsupported code generation type: " + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        // Save code after stream generation is complete
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(chunk -> {
                    // Collect code chunks in real-time
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // Save code after stream completion
                    try {
                        String completeHtmlCode = codeBuilder.toString();
                        HtmlCodeResult htmlCodeResult = CodeParser.parseHtmlCode(completeHtmlCode);
                        // Save code to file
                        File savedDir = CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
                        log.info("Save successful, path: " + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Save failed: {}", e.getMessage());
                    }
                });
    }

    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        // Save code after stream generation is complete
        StringBuilder codeBuilder = new StringBuilder();
        return result
                .doOnNext(chunk -> {
                    // Collect code chunks in real-time
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // Save code after stream completion
                    try {
                        String completeMultiFileCode = codeBuilder.toString();
                        MultiFileCodeResult multiFileResult = CodeParser.parseMultiFileCode(completeMultiFileCode);
                        // Save code to file
                        File savedDir = CodeFileSaver.saveMultiFileCodeResult(multiFileResult);
                        log.info("Save successful, path: " + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("Save failed: {}", e.getMessage());
                    }
                });
    }


    private File generateAndSaveHtmlCode(String userMessage) {
        HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
        return CodeFileSaver.saveHtmlCodeResult(htmlCodeResult);
    }

    private File generateAndSaveMultiFileCode(String userMessage) {
        MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
        return CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult);
    }
}
