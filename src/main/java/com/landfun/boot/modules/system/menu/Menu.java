package com.landfun.boot.modules.system.menu;

import java.time.LocalDateTime;
import java.util.List;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.role.Role;

@Entity
@Table(name = "sys_menu")
public interface Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @Nullable
    @ManyToOne
    @JoinColumn(name = "parent_id")
    Menu parent();

    @OneToMany(mappedBy = "parent")
    List<Menu> children();

    String name();

    @Nullable
    String icon();

    @Nullable
    String path();

    @Key
    @Nullable
    String permission();

    MenuType type();

    @ManyToMany(mappedBy = "menus")
    List<Role> roles();

    @LogicalDeleted("now")
    @Nullable
    @Column(name = "delete_time")
    LocalDateTime deleteTime();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();
}
