package com.landfun.boot.modules.system.user;

import java.time.LocalDateTime;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.dept.Dept;
import com.landfun.boot.modules.system.role.Role;

@Entity
@Table(name = "sys_user")
public interface User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    String username();

    @Key
    String email();

    String password();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();

    @Default("true")
    @Column(name = "is_active")
    boolean isActive();

    @Default("false")
    @Column(name = "is_superuser")
    boolean isSuperuser();

    @LogicalDeleted("now")
    @Nullable
    @Column(name = "delete_time")
    LocalDateTime deleteTime();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "role_id")
    Role role();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "dept_id")
    Dept dept();
}
