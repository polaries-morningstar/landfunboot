# 📋 Nova 1.0 系统核心功能清单

## 1. 认证与授权中心 (Auth Module)

**模块代码:** `modules.auth`
**交互:** RPC Post

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **账号登录** | 账号密码登录，返回 JWT。 | **POST** `/auth/login`<br>

<br>技术: Sa-Token。 |
| **验证码** | 获取图形验证码。 | **GET** `/auth/captcha` |
| **用户信息** | 获取当前用户基础信息、权限码、角色码。 | **GET** `/auth/user-info` |
| **元数据同步** | **(新增替代字典)** 前端获取所有枚举定义（如性别、状态），用于渲染下拉框。 | **GET** `/auth/meta/enums`<br>

<br>技术: 自动扫描包下所有实现了 `BaseEnum` 的枚举，返回 Map 结构：<br>

<br>`{ "UserStatus": [{"code": "ACTIVE", "desc": "正常"}...] }` |
| **退出/踢人** | 注销或强制下线。 | **POST** `/auth/logout`, `/auth/kick-out` |

---

## 2. 部门管理 (Dept Module)

**模块代码:** `modules.system.dept`
**核心痛点:** 树形结构

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **部门树查询** | 获取部门树结构。 | **GET** `/dept/tree`<br>

<br>技术: Jimmer Recursive Fetcher。 |
| **创建部门** | 新增部门。 | **POST** `/dept/create`<br>

<br>字段: `name` (String), `parentId` (Long)。 |
| **编辑/删除** | 修改或逻辑删除部门。 | **POST** `/dept/update`, `/dept/delete` |

---

## 3. 菜单与权限管理 (Menu Module)

**模块代码:** `modules.system.menu`
**核心痛点:** 字符串枚举类型

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **菜单树查询** | 获取路由菜单树。 | **GET** `/menu/tree` |
| **新增/修改** | 配置菜单详情。 | **POST** `/menu/create`<br>

<br>**枚举优化:** 菜单类型字段 `type` 存字符串：<br>

<br>`DIRECTORY` (目录), `MENU` (菜单), `BUTTON` (按钮)。 |
| **删除菜单** | 逻辑删除。 | **POST** `/menu/delete` |

---

## 4. 角色管理 (Role Module)

**模块代码:** `modules.system.role`
**核心痛点:** 字符串数据权限策略

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **角色 CRUD** | 角色管理。 | **POST** `/role/create`, `/role/update`<br>

<br>字段: `code` (String, 如 "HR_MANAGER")。 |
| **分配功能权限** | 绑定 `Role-Menu`。 | **POST** `/role/assign-menu` |
| **分配数据权限** | 配置角色数据范围。 | **POST** `/role/assign-data-scope`<br>

<br>**枚举优化:** `dataScope` 字段存字符串：<br>

<br>`ALL` (全部), `DEPT_SAME` (本部门), `DEPT_RECURSIVE` (本部门及以下), `SELF` (仅本人)。 |

---

## 5. 用户管理 (User Module)

**模块代码:** `modules.system.user`
**核心痛点:** 字符串状态

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **用户搜索** | 多条件分页搜索。 | **POST** `/user/search`<br>

<br>DSL 条件: `where(table.status().eq(UserStatus.ACTIVE))` (编译期类型安全)。 |
| **新建用户** | 录入账号信息。 | **POST** `/user/create`<br>

<br>**枚举优化:**<br>

<br>性别 `gender`: `"MALE"`, `"FEMALE"`<br>

<br>状态 `status`: `"ACTIVE"`, `"LOCKED"` |
| **编辑用户** | 修改基本信息。 | **POST** `/user/update` |
| **改密/状态** | 重置密码或冻结用户。 | **POST** `/user/reset-password`<br>

<br>**POST** `/user/change-status` (入参传 `"LOCKED"` 或 `"ACTIVE"` 字符串)。 |
| **删除用户** | 逻辑删除。 | **POST** `/user/delete`<br>

<br>逻辑: 删除后 `deleted_millis` 设为时间戳，状态逻辑上视为失效。 |

---

## 6. 系统监控 (Monitor Module)

**模块代码:** `modules.monitor`

| 功能点 | 详细描述 | 架构与技术实现备注 |
| --- | --- | --- |
| **操作日志** | 审计写操作。 | **POST** `/log/operation/search`<br>

<br>**枚举优化:** 操作类型 `actionType`: `"CREATE"`, `"UPDATE"`, `"DELETE"`, `"GRANT"`。 |
| **登录日志** | 登录历史。 | **POST** `/log/login/search`<br>

<br>**枚举优化:** 结果 `result`: `"SUCCESS"`, `"FAIL"`。 |
| **在线用户** | 在线会话管理。 | **GET** `/monitor/online/list` |

---