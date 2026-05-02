package com.book.aiwebgenerator.core.saver;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.book.aiwebgenerator.constant.AppConstant;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;

import java.io.File;
import java.nio.charset.StandardCharsets;

public abstract class CodeFileSaverTemplate<T> {
    private static final String FILE_SAVE_ROOT_DIR = AppConstant.CODE_OUTPUT_ROOT_DIR;
    
    public final File saveCode(T result, Long appId) {
        validateInput(result);
        String baseDirPath = buildUniqueDir(appId);
        saveFiles(result, baseDirPath);
        return new File(baseDirPath);
    }

    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "The code generation result cannot be null");
        }
    }

    protected final String buildUniqueDir(Long appId) {
        if (appId == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "appId cannot be null");
        }
        String codeType = getCodeType().getValue();
        String uniqueDirName = StrUtil.format("{}_{}", codeType, appId);
        String dirPath = FILE_SAVE_ROOT_DIR + File.separator + uniqueDirName;
        FileUtil.mkdir(dirPath);
        return dirPath;
    }


    protected final void writeToFile(String dirPath, String filename, String content) {
        if (StrUtil.isNotBlank(content)) {
            String filePath = dirPath + File.separator + filename;
            FileUtil.writeString(content, filePath, StandardCharsets.UTF_8);
        }
    }


    protected abstract CodeGenTypeEnum getCodeType();


    protected abstract void saveFiles(T result, String baseDirPath);
}
