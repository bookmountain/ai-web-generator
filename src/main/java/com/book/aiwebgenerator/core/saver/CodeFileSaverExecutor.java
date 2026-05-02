package com.book.aiwebgenerator.core.saver;

import com.book.aiwebgenerator.ai.model.HtmlCodeResult;
import com.book.aiwebgenerator.ai.model.MultiFileCodeResult;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;

import java.io.File;


public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaverTemplate htmlCodeFileSaver = new HtmlCodeFileSaverTemplate();

    private static final MultiFileCodeFileSaverTemplate multiFileCodeFileSaver = new MultiFileCodeFileSaverTemplate();

    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenType, Long appId) throws BusinessException {
        return switch (codeGenType) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId);
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Unsupported code generation type: " + codeGenType);
        };
    }
}
