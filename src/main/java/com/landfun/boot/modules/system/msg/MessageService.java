package com.landfun.boot.modules.system.msg;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.babyfish.jimmer.Page;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.landfun.boot.infrastructure.exception.BizException;
import com.landfun.boot.infrastructure.service.DataScopeService;
import com.landfun.boot.infrastructure.web.AuthContext;
import com.landfun.boot.infrastructure.web.PageResult;
import com.landfun.boot.modules.system.dept.DeptFilter;
import com.landfun.boot.modules.system.msg.dto.MessageListView;
import com.landfun.boot.modules.system.msg.dto.MessageView;
import com.landfun.boot.modules.system.msg.dto.MyMessageRow;
import com.landfun.boot.modules.system.msg.dto.SendMessageReq;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.user.UserFilter;
import com.landfun.boot.modules.system.user.UserTable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final JSqlClient sqlClient;
    private final DataScopeService dataScopeService;

    @Transactional
    public MessageView send(SendMessageReq req) {
        User currentUser = AuthContext.getUser();
        long senderId = currentUser.id();

        Set<Long> candidateIds = resolveRecipientIds(req);
        if (candidateIds.isEmpty()) {
            throw new BizException(400, "未解析到任何接收人，请检查目标类型与目标ID");
        }

        Set<Long> visibleUserIds = dataScopeService.getVisibleUserIds(currentUser);
        Set<Long> recipientIds;
        if (visibleUserIds == null) {
            recipientIds = candidateIds;
        } else {
            recipientIds = candidateIds.stream().filter(visibleUserIds::contains).collect(Collectors.toSet());
        }
        if (recipientIds.isEmpty()) {
            throw new BizException(400, "所选接收人不在您的数据权限范围内");
        }

        Message message = sqlClient.getEntities().save(
                MessageDraft.$.produce(d -> {
                    d.setSenderId(senderId);
                    d.setTitle(req.getTitle());
                    d.setContent(req.getContent() != null ? req.getContent() : "");
                    d.setTargetType(req.getTargetType());
                }),
                org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY
        ).getModifiedEntity();

        for (Long userId : recipientIds) {
            sqlClient.getEntities().save(
                    MessageReceiverDraft.$.produce(d -> {
                        d.setMessageId(message.id());
                        d.setUserId(userId);
                    }),
                    org.babyfish.jimmer.sql.ast.mutation.SaveMode.INSERT_ONLY
            );
        }
        return sqlClient.findById(MessageView.class, message.id());
    }

    private Set<Long> resolveRecipientIds(SendMessageReq req) {
        Set<Long> ids = new HashSet<>();
        JSqlClient noFilter = sqlClient.filters(cfg -> cfg.disableByTypes(UserFilter.class, DeptFilter.class));
        switch (req.getTargetType()) {
            case USER -> {
                if (req.getTargetId() != null) ids.add(req.getTargetId());
            }
            case DEPT -> {
                if (req.getTargetId() != null) {
                    List<Long> userIds = noFilter.createQuery(UserTable.$)
                            .where(UserTable.$.deptId().eq(req.getTargetId()), UserTable.$.deleteTime().isNull())
                            .select(UserTable.$.id())
                            .execute();
                    ids.addAll(userIds);
                }
            }
            case DEPT_WITH_CHILDREN -> {
                if (req.getTargetId() != null) {
                    List<Long> deptIds = dataScopeService.getSubDeptIds(req.getTargetId());
                    if (!deptIds.isEmpty()) {
                        List<Long> userIds = noFilter.createQuery(UserTable.$)
                                .where(UserTable.$.deptId().in(deptIds), UserTable.$.deleteTime().isNull())
                                .select(UserTable.$.id())
                                .execute();
                        ids.addAll(userIds);
                    }
                }
            }
            case ALL -> {
                List<Long> userIds = noFilter.createQuery(UserTable.$)
                        .where(UserTable.$.deleteTime().isNull())
                        .select(UserTable.$.id())
                        .execute();
                ids.addAll(userIds);
            }
            case ROLE -> {
                if (req.getTargetId() != null) {
                    List<Long> userIds = noFilter.createQuery(UserTable.$)
                            .where(UserTable.$.roleId().eq(req.getTargetId()), UserTable.$.deleteTime().isNull())
                            .select(UserTable.$.id())
                            .execute();
                    ids.addAll(userIds);
                }
            }
            case USER_IDS -> {
                if (req.getTargetIds() != null) ids.addAll(req.getTargetIds());
            }
        }
        return ids;
    }

    public PageResult<MessageListView> listAll(Pageable pageable) {
        User currentUser = AuthContext.getUser();
        if (currentUser == null) throw new BizException(401, "未登录");
        Set<Long> visibleUserIds = dataScopeService.getVisibleUserIds(currentUser);
        // 前端传 page=1 表示第一页，转为 0-based
        int pageIndex = pageable.getPageNumber() > 0 ? pageable.getPageNumber() - 1 : 0;
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        Page<MessageListView> page;
        if (visibleUserIds == null) {
            page = sqlClient.createQuery(MessageTable.$)
                    .orderBy(MessageTable.$.createdTime().desc())
                    .select(MessageTable.$.fetch(MessageListView.class))
                    .fetchPage(pageIndex, pageSize);
        } else {
            if (visibleUserIds.isEmpty()) {
                return PageResult.of(0, List.of());
            }
            page = sqlClient.createQuery(MessageTable.$)
                    .where(MessageTable.$.senderId().in(visibleUserIds))
                    .orderBy(MessageTable.$.createdTime().desc())
                    .select(MessageTable.$.fetch(MessageListView.class))
                    .fetchPage(pageIndex, pageSize);
        }
        return PageResult.of(page);
    }

    public PageResult<MyMessageRow> myList(Pageable pageable) {
        Long userId = AuthContext.getUserId();
        int pageIndex = pageable.getPageNumber() > 0 ? pageable.getPageNumber() - 1 : 0;
        int pageSize = pageable.getPageSize() > 0 ? pageable.getPageSize() : 10;
        Page<MyMessageRow> page = sqlClient.createQuery(MessageReceiverTable.$)
                .where(MessageReceiverTable.$.userId().eq(userId))
                .orderBy(MessageReceiverTable.$.message().createdTime().desc())
                .select(MessageReceiverTable.$.fetch(MyMessageRow.class))
                .fetchPage(pageIndex, pageSize);
        return PageResult.of(page);
    }

    public MyMessageRow getMyMessage(long id) {
        Long userId = AuthContext.getUserId();
        MyMessageRow row = sqlClient.createQuery(MessageReceiverTable.$)
                .where(
                        MessageReceiverTable.$.id().eq(id),
                        MessageReceiverTable.$.userId().eq(userId))
                .select(MessageReceiverTable.$.fetch(MyMessageRow.class))
                .fetchOneOrNull();
        if (row == null) {
            throw new BizException(404, "消息不存在或无权查看");
        }
        return row;
    }

    public void markRead(long receiverId) {
        Long userId = AuthContext.getUserId();
        MessageReceiver receiver = sqlClient.createQuery(MessageReceiverTable.$)
                .where(
                        MessageReceiverTable.$.id().eq(receiverId),
                        MessageReceiverTable.$.userId().eq(userId))
                .select(MessageReceiverTable.$)
                .fetchOneOrNull();
        if (receiver == null) throw new BizException(404, "消息不存在或无权操作");
        if (receiver.readAt() != null) return;
        sqlClient.createUpdate(MessageReceiverTable.$)
                .set(MessageReceiverTable.$.readAt(), java.time.LocalDateTime.now())
                .where(MessageReceiverTable.$.id().eq(receiverId))
                .execute();
    }

    /** Mark as read by message id (finds receiver row for current user). */
    public void markReadByMessageId(long messageId) {
        Long userId = AuthContext.getUserId();
        MessageReceiver receiver = sqlClient.createQuery(MessageReceiverTable.$)
                .where(
                        MessageReceiverTable.$.messageId().eq(messageId),
                        MessageReceiverTable.$.userId().eq(userId))
                .select(MessageReceiverTable.$)
                .fetchOneOrNull();
        if (receiver == null) return;
        if (receiver.readAt() != null) return;
        sqlClient.createUpdate(MessageReceiverTable.$)
                .set(MessageReceiverTable.$.readAt(), java.time.LocalDateTime.now())
                .where(MessageReceiverTable.$.id().eq(receiver.id()))
                .execute();
    }

    public MessageView getMessageDetail(long messageId) {
        Long userId = AuthContext.getUserId();
        Long senderId = sqlClient.createQuery(MessageTable.$)
                .where(MessageTable.$.id().eq(messageId))
                .select(MessageTable.$.senderId())
                .fetchOneOrNull();
        if (senderId == null) throw new BizException(404, "消息不存在");
        if (senderId.equals(userId)) {
            return sqlClient.findById(MessageView.class, messageId);
        }
        Long receiverRowId = sqlClient.createQuery(MessageReceiverTable.$)
                .where(
                        MessageReceiverTable.$.messageId().eq(messageId),
                        MessageReceiverTable.$.userId().eq(userId))
                .select(MessageReceiverTable.$.id())
                .fetchOneOrNull();
        if (receiverRowId != null) {
            return sqlClient.findById(MessageView.class, messageId);
        }
        User currentUser = AuthContext.getUser();
        Set<Long> visibleUserIds = dataScopeService.getVisibleUserIds(currentUser);
        if (visibleUserIds != null && !visibleUserIds.contains(senderId)) {
            throw new BizException(403, "无权查看该消息");
        }
        return sqlClient.findById(MessageView.class, messageId);
    }
}
