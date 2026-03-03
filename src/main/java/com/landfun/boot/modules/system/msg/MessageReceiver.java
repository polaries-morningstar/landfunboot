package com.landfun.boot.modules.system.msg;

import java.time.LocalDateTime;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.user.User;

@Entity
@Table(name = "sys_message_receiver")
public interface MessageReceiver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @ManyToOne
    @JoinColumn(name = "message_id")
    Message message();

    @ManyToOne
    @JoinColumn(name = "user_id")
    User user();

    @Nullable
    @Column(name = "read_at")
    LocalDateTime readAt();
}
