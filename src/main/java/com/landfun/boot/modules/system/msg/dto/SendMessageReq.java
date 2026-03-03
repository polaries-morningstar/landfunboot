package com.landfun.boot.modules.system.msg.dto;

import java.util.List;

import com.landfun.boot.modules.system.msg.MessageTargetType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageReq {

    @NotBlank(message = "标题不能为空")
    private String title;

    private String content;

    @NotNull(message = "目标类型不能为空")
    private MessageTargetType targetType;

    /** For USER, DEPT, DEPT_WITH_CHILDREN, ROLE: single target id */
    private Long targetId;

    /** For USER_IDS: list of user ids */
    private List<Long> targetIds;
}
