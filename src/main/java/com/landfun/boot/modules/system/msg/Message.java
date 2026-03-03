package com.landfun.boot.modules.system.msg;

import java.time.LocalDateTime;
import java.util.List;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.user.User;

@Entity
@Table(name = "sys_message")
public interface Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @JoinColumn(name = "sender_id")
    User sender();

    String title();

    @Nullable
    String content();

    /** Message target type when sent: USER, DEPT, DEPT_WITH_CHILDREN, ALL, ROLE, USER_IDS */
    @Nullable
    @Column(name = "target_type")
    MessageTargetType targetType();

    LocalDateTime createdTime();

    @OneToMany(mappedBy = "message")
    List<MessageReceiver> receivers();
}
