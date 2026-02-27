package com.landfun.boot.modules.system.role;

import java.time.LocalDateTime;
import java.util.List;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import com.landfun.boot.modules.system.dept.Dept;
import com.landfun.boot.modules.system.menu.Menu;
import com.landfun.boot.modules.system.user.User;

@Entity
@Table(name = "sys_role")
public interface Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id();

    @Key
    String code();

    String name();

    String description();

    @Column(name = "data_scope")
    DataScope dataScope();

    @OneToMany(mappedBy = "role")
    List<User> users();

    @ManyToMany
    @JoinTable(name = "sys_role_menu_mapping", joinColumnName = "role_id", inverseJoinColumnName = "menu_id")
    List<Menu> menus();

    @ManyToMany
    @JoinTable(name = "sys_role_dept_mapping", joinColumnName = "role_id", inverseJoinColumnName = "dept_id")
    List<Dept> depts();

    @LogicalDeleted("now")
    @Nullable
    @Column(name = "delete_time")
    LocalDateTime deleteTime();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();
}
