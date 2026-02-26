package com.landfun.boot.modules.system.menu;

import com.landfun.boot.modules.system.role.Role;
import org.babyfish.jimmer.sql.*;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "sys_menu")
public interface Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    @Column(name = "parent_id")
    long parentId();

    String name();

    @Nullable // Added @Nullable
    String path();

    @Key
    @Nullable
    String permission();

    @Nullable
    String component();

    @Nullable
    String icon();

    @Column(name = "sort_order")
    int sortOrder();

    Type type();

    @ManyToMany(mappedBy = "menus")
    List<Role> roles();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();

    enum Type {
        DIR,
        MENU,
        BUTTON
    }
}
