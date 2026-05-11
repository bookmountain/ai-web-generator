package com.book.aiwebgenerator.core.handler;

import com.book.aiwebgenerator.model.entity.User;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import com.book.aiwebgenerator.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Stream handler executor
 * Creates the appropriate stream handler based on the code generation type:
 * 1. Traditional Flux<String> stream (HTML, MULTI_FILE) -> SimpleTextStreamHandler
 * 2. Complex TokenStream format stream (VUE_PROJECT) -> JsonMessageStreamHandler
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * Create a stream handler and process chat history
     *
     * @param originFlux         original stream
     * @param chatHistoryService chat history service
     * @param appId              application ID
     * @param loginUser          logged-in user
     * @param codeGenType        code generation type
     * @return processed stream
     */
    public Flux<String> doExecute(Flux<String> originFlux,
                                  ChatHistoryService chatHistoryService,
                                  long appId, User loginUser, CodeGenTypeEnum codeGenType) {
        return switch (codeGenType) {
            case VUE_PROJECT -> // use the injected component instance
                    jsonMessageStreamHandler.handle(originFlux, chatHistoryService, appId, loginUser);
            case HTML, MULTI_FILE -> // simple text handler does not need dependency injection
                    new SimpleTextStreamHandler().handle(originFlux, chatHistoryService, appId, loginUser);
        };
    }
}