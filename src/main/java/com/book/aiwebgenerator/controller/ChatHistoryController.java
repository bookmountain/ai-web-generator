package com.book.aiwebgenerator.controller;

import com.book.aiwebgenerator.annotation.AuthCheck;
import com.book.aiwebgenerator.common.BaseResponse;
import com.book.aiwebgenerator.common.ResultUtils;
import com.book.aiwebgenerator.constant.UserConstant;
import com.book.aiwebgenerator.exception.ErrorCode;
import com.book.aiwebgenerator.exception.ThrowUtils;
import com.book.aiwebgenerator.model.dto.chathistory.ChatHistoryQueryRequest;
import com.book.aiwebgenerator.model.entity.ChatHistory;
import com.book.aiwebgenerator.model.entity.User;
import com.book.aiwebgenerator.service.ChatHistoryService;
import com.book.aiwebgenerator.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/chatHistory")
public class ChatHistoryController {
    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @GetMapping("/app/{appId}")
    public BaseResponse<Page<ChatHistory>> listAppChatHistory(@PathVariable Long appId,
                                                              @RequestParam(defaultValue = "10") int pageSize,
                                                              @RequestParam(required = false) LocalDateTime lastCreateTime,
                                                              HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<ChatHistory> result = chatHistoryService.listAppChatHistoryByPage(appId, pageSize, lastCreateTime, loginUser);
        return ResultUtils.success(result);
    }

    @PostMapping("/admin/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<ChatHistory>> listAllChatHistoryByPageForAdmin(@RequestBody ChatHistoryQueryRequest chatHistoryQueryRequest) {
        ThrowUtils.throwIf(chatHistoryQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = chatHistoryQueryRequest.getPageNum();
        long pageSize = chatHistoryQueryRequest.getPageSize();
        // 查询数据
        QueryWrapper queryWrapper = chatHistoryService.getQueryWrapper(chatHistoryQueryRequest);
        Page<ChatHistory> result = chatHistoryService.page(Page.of(pageNum, pageSize), queryWrapper);

        return ResultUtils.success(result);
    }


}
