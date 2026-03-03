package com.landfun.boot.modules.system.msg;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.landfun.boot.infrastructure.annotation.HasPermission;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.infrastructure.web.R;
import com.landfun.boot.modules.system.msg.dto.MessageListView;
import com.landfun.boot.modules.system.msg.dto.MessageView;
import com.landfun.boot.modules.system.msg.dto.MyMessageRow;
import com.landfun.boot.modules.system.msg.dto.SendMessageReq;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Message", description = "Message APIs")
@RestController
@RequestMapping("/sys/msg")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Send message")
    @PostMapping("/send")
    @HasPermission("msg:send")
    public R<MessageView> send(@RequestBody @Valid SendMessageReq req) {
        return R.ok(messageService.send(req));
    }

    @Operation(summary = "List all messages (data-scope filtered)")
    @GetMapping("/list")
    @HasPermission("msg:list")
    public R<PageResult<MessageListView>> list(@PageableDefault Pageable pageable) {
        return R.ok(messageService.listAll(pageable));
    }

    @Operation(summary = "My messages (inbox)")
    @GetMapping("/my")
    @HasPermission("msg:self")
    public R<PageResult<MyMessageRow>> myList(@PageableDefault Pageable pageable) {
        return R.ok(messageService.myList(pageable));
    }

    @Operation(summary = "Get message detail (by message id, permission check)")
    @GetMapping("/my/{messageId}")
    @HasPermission("msg:self")
    public R<MessageView> getMessageDetail(@PathVariable long messageId) {
        return R.ok(messageService.getMessageDetail(messageId));
    }

    @Operation(summary = "Mark message as read")
    @PatchMapping("/my/{messageId}/read")
    @HasPermission("msg:self")
    public R<Void> markRead(@PathVariable long messageId) {
        messageService.markReadByMessageId(messageId);
        return R.ok(null);
    }
}
