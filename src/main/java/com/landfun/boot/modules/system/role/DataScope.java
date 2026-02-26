package com.landfun.boot.modules.system.role;

import org.babyfish.jimmer.sql.EnumItem;

public enum DataScope {
    @EnumItem(name = "ALL")
    ALL,

    @EnumItem(name = "DEPT_SAME")
    DEPT_SAME,

    @EnumItem(name = "DEPT_RECURSIVE")
    DEPT_RECURSIVE,

    @EnumItem(name = "DEPT_CUSTOM")
    DEPT_CUSTOM,

    @EnumItem(name = "SELF")
    SELF
}
