package com.landfun.boot.modules.system.user;

import com.landfun.boot.modules.system.role.Role;
import com.landfun.boot.modules.system.dept.Dept;
import org.babyfish.jimmer.sql.*;
import org.springframework.lang.Nullable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sys_user")
public interface User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String username();

    @Key
    String email();

    String password();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();

    @Default("true")
    @Column(name = "is_active")
    boolean isActive();

    @LogicalDeleted("now")
    @Nullable
    @Column(name = "delete_time")
    LocalDateTime deleteTime();

    @ManyToMany
    @JoinTable(name = "sys_user_role_mapping", joinColumnName = "user_id", inverseJoinColumnName = "role_id")
    List<Role> roles();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "dept_id")
    Dept dept();
}
