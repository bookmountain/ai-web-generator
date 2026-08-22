package com.book.aiwebgenerator.langgraph4j.node;

import com.book.aiwebgenerator.core.builder.VueProjectBuilder;
import com.book.aiwebgenerator.exception.BusinessException;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.langgraph4j.state.WorkflowContext;
import com.book.aiwebgenerator.model.enums.CodeGenTypeEnum;
import com.book.aiwebgenerator.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Slf4j
public class ProjectBuilderNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("Executing node: Project Building");

            // Get necessary parameters
            String generatedCodeDir = context.getGeneratedCodeDir();
            CodeGenTypeEnum generationType = context.getGenerationType();
            String buildResultDir;
            // Vue project type: use VueProjectBuilder for building
            try {
                VueProjectBuilder vueBuilder = SpringContextUtil.getBean(VueProjectBuilder.class);
                // Execute Vue project build (npm install + npm run build)
                boolean buildSuccess = vueBuilder.buildProject(generatedCodeDir);
                if (buildSuccess) {
                    // Build successful, return the dist directory path
                    buildResultDir = generatedCodeDir + File.separator + "dist";
                    log.info("Vue project built successfully, dist directory: {}", buildResultDir);
                } else {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Vue project build failed");
                }
            } catch (Exception e) {
                log.error("Vue project build exception: {}", e.getMessage(), e);
                buildResultDir = generatedCodeDir; // Return original path on exception
            }

            // Update state
            context.setCurrentStep("Project Building");
            context.setBuildResultDir(buildResultDir);
            log.info("Project building node completed, final directory: {}", buildResultDir);
            return WorkflowContext.saveContext(context);
        });
    }
}