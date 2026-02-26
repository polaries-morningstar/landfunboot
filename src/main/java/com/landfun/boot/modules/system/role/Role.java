package com.landfun.boot.modules.system.role;

import com.landfun.boot.modules.system.menu.Menu;
import com.landfun.boot.modules.system.user.User;
import com.landfun.boot.modules.system.dept.Dept;
import org.babyfish.jimmer.sql.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @ManyToMany(mappedBy = "roles")
    List<User> users();

    @ManyToMany
    @JoinTable(name = "sys_role_menu_mapping", joinColumnName = "role_id", inverseJoinColumnName = "menu_id")
    List<Menu> menus();

    @ManyToMany
    @JoinTable(name = "sys_role_dept_mapping", joinColumnName = "role_id", inverseJoinColumnName = "dept_id")
    List<Dept> depts();

    LocalDateTime createdTime();

    LocalDateTime updatedTime();
}
