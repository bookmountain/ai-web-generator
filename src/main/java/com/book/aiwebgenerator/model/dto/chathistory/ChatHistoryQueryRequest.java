package com.book.aiwebgenerator.model.dto.chathistory;

import com.book.aiwebgenerator.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class ChatHistoryQueryRequest extends PageRequest implements Serializable {
    private Long id;

    private String message;

    private String messageType;

    private Long appId;

    private Long userId;

    /**
     * Cursor query - creation time of the last record
     * Used for pagination queries to fetch records earlier than this time
     */
    private LocalDateTime lastCreateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}

