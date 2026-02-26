package com.landfun.boot.modules.system.dept;

import java.time.LocalDateTime;
import java.util.List;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.user.User;

@Entity
@Table(name = "sys_dept")
public interface Dept {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String name();

    @Nullable
    @ManyToOne
    @JoinColumn(name = "parent_id")
    Dept parent();

    @OneToMany(mappedBy = "parent")
    List<Dept> children();

    @OneToMany(mappedBy = "dept")
    List<User> users();

    @LogicalDeleted("now")
    @Nullable
    @Column(name = "delete_time")
    LocalDateTime deleteTime();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();
}
