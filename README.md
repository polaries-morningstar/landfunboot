# 📘 Nova 1.0 企业级架构设计与开发规范白皮书

> **版本:** 1.0.0-FINAL
> **适用范围:** 企业级后台管理系统、复杂业务中台
> **核心哲学:** 显式编程 · 领域模块化 · 混合 RPC · 类型安全 · AI 友好

---

## 一、 核心设计理念 (Core Philosophy)

Nova 架构旨在构建一个面向 2026 年的高效、稳健且 AI 友好的后端体系。我们彻底摒弃为了分层而分层的传统教条（如 Controller/Service/Dao/Mapper 的机械划分），拥抱“业务为王”的开发模式。

1. **显式大于隐式 (Explicit > Implicit):**
* **拒绝魔法：** 所有的 SQL 执行、关联数据抓取（Fetch）、事务边界，必须在业务代码中显式声明。严禁依赖隐式的延迟加载（Lazy Loading）导致不可预测的 N+1 性能问题。
* **所见即所得：** 消除不必要的中间层，业务逻辑在哪里，数据操作就在哪里。


2. **读写分离的交互契约 (CQRS Interaction):**
* 利用 HTTP 语义优化读取性能（缓存友好），利用 RPC 语义强化写入意图（AI 友好）。
* 严格区分 **“查询（Query）”** 与 **“命令（Command）”**。


3. **领域模块化 (Modular Monolith):**
* 代码组织以 **“业务领域”** 为根节点，而非技术层级。确保一个业务功能的所有相关代码（实体、接口、逻辑、DTO）在物理上聚合，实现高内聚低耦合，天然支持未来的微服务拆分。


4. **去繁从简 (Zero Boilerplate):**
* **无 Repository 层：** 消除无效的转发代码，Service 直连数据源。
* **无 XML：** 全面拥抱 Java Config 与注解，实现 0 配置维护。
* **无手动 DTO 转换：** 依托 Jimmer 及其代码生成器实现端到端类型同步。



---

## 二、 技术选型标准 (Technology Stack)

我们坚持“买新不买旧，买强不买多”的原则，构建高内聚的技术底座。

| 领域 | 核心组件 | 决策理由与标准 |
| --- | --- | --- |
| **基础环境** | **Java 21 (LTS)** | 强制启用 **Virtual Threads** 处理高并发 I/O；全线使用 **Record** 作为 DTO；利用 Switch 模式匹配简化逻辑。 |
| **Web 框架** | **Spring Boot 4.0+** | 基于 Spring 7，支持 AOT 编译，移除旧版历史包袱，全面拥抱 Jakarta EE。 |
| **持久层** | **Jimmer** | **架构核心**。替代 MyBatis/JPA。提供声明式对象图抓取、强类型 SQL DSL。**直接在 Service 中使用**。 |
| **数据库** | **MySQL 8.0+** | 推荐使用 JSON 列存储非结构化扩展数据。**禁止**使用数据库级外键约束。 |
| **权限安全** | **Sa-Token** | 轻量级 RBAC 框架。支持多端登录、踢人下线、无状态 JWT 及细粒度路由拦截。 |
| **日志系统** | **SLF4J + Logback** | 必须配置 AsyncAppender 或基于虚拟线程的异步写入，防止日志 IO 阻塞业务线程。 |
| **序列化** | **Jackson** | 必须引入 `jackson-module-parameter-names` 模块以原生支持 Java Record。**严禁**使用 Fastjson/Fastjson2。 |
| **API 文档** | **SpringDoc** | 基于 OpenAPI 3 标准，用于生成机器可读文档，驱动前端 SDK 自动生成。 |
| **版本管理** | **Flyway** | 数据库版本控制工具。**严禁**手动在生产环境执行 DDL/DML，所有变更必须版本化。 |
| **构建容器** | **Maven + Jib** | 使用 Jib 进行无 Dockerfile 的分层镜像构建，加速 CI/CD 流水线。 |
| **代码规范** | **Spotless** | 强制 Google Java Style 格式化，构建环节检查，不达标即失败。 |

---

## 三、 工程组织与目录结构 (Engineering Organization)

采用 **垂直切片 (Vertical Slices)** 结构。
**注意：** 工程中**不存在** `repository`、`dao` 或 `mapper` 包。

### 1. 顶层结构概览

```text
com.company.nova
├── NovaApplication.java          // [启动类]
│
├── infrastructure                // [基础设施层] 纯技术底座，严禁依赖业务代码
│   ├── config                    // 全局配置 (Jackson, Jimmer, SaToken)
│   ├── consts                    // 全局常量/枚举
│   ├── exception                 // 全局异常体系 (GlobalExceptionHandler)
│   ├── filter                    // Jimmer 全局过滤器 (数据权限核心)
│   ├── log                       // 日志切面与异步处理
│   ├── web                       // Web 交互基类 (R, PageResult)
│   └── utils                     // 通用工具 (仅限纯技术工具)
│
└── modules                       // [业务模块层] 垂直业务切片
    ├── auth                      // [认证模块] 独立领域
    ├── system                    // [系统模块]
    │   ├── dept                  // [部门聚合根]
    │   └── user                  // [用户聚合根] 
    │       ├── User.java           // Entity: 数据的唯一真理
    │       ├── UserController.java // Controller: API 接入
    │       ├── UserService.java    // Service: 业务逻辑 + 数据访问
    │       ├── UserInterceptor.java// Interceptor: 保存前校验
    │       └── dto                 // DTO: 输入(Cmd)与输出(Vo) Record
    │
    └── [business]                // [核心业务] 如 order, product

```

### 2. 包依赖与引用规范

1. **单向依赖:** `modules` 可依赖 `infrastructure`，反之禁止。
2. **模块间通信:**
* **强关联:** Service 之间可直接注入（如 `OrderService` 注入 `UserService`）。
* **弱关联:** 推荐使用 Spring Event 进行解耦（如用户注册后触发发券）。


3. **循环依赖:** 零容忍。出现循环依赖必须重构或提取公共模块。

---

## 四、 开发法则 (The Code Laws)

### 法则一：API 交互法则 (Interaction Law)

**目的：** 明确“读”与“写”的界限，兼顾浏览器缓存特性与 AI 理解力。

1. **读操作 (Query) -> GET:**
* **定义：** 不改变服务器状态的操作（列表、详情、字典、配置）。
* **规范：** HTTP Method 为 **`GET`**。
* **参数：** 必须通过 QueryString (URL 参数) 传递。
* **命名：** 名词或“动词+名词”，如 `/user/list`, `/dept/tree`。


2. **写操作 (Command) -> POST:**
* **定义：** 改变服务器状态的操作（增、删、改）以及 **参数复杂的搜索**。
* **规范：** HTTP Method 为 **`POST`**。
* **参数：** 必须通过 **`@RequestBody Record`** 传递单一对象。
* **命名：** 必须采用 **RPC 动词风格**，如 `/user/create`, `/order/ship`, `/report/export`。


3. **禁止:**
* 严禁使用 RESTful 风格的 `PUT`, `DELETE`, `PATCH`。
* 严禁在 `GET` 请求中使用 Body 传参。



### 法则二：去 Repository 法则 (No-Repo Law)

**目的：** 让业务逻辑与数据访问原子化，消除转发式代码。

1. **Service 全权负责:**
* 所有数据库操作（CRUD、DSL 查询）直接在 Service 类中注入 `JSqlClient` 完成。
* Service 类直接定义为 **Class**，**严禁定义 Interface**（除非涉及多态策略模式）。


2. **复用即方法:**
* 如果某段 SQL 逻辑需要在多个 Service 复用，**请在所属 Service 中定义 public 方法**，其他 Service 注入调用。
* **禁止** 创建 `repository` 或 `dao` 包。



### 法则三：显式数据法则 (Data Law)

**目的：** 根除 N+1 问题，确保类型安全与前后端契约稳定。

1. **显式抓取 (Explicit Fetching):**
* Service 层查询必须使用 Jimmer 的 Fetcher 显式声明需要加载的关联字段（如 `dept`, `roles`）。


2. **数据防腐 (Anti-Corruption):**
* **入参:** Controller 接收的必须是 **Command Record**。
* **出参:** Controller 返回的必须是 **View Object Record (VO)**。
* Service 层负责 Entity 到 VO 的转换，Controller 不直接暴露 Entity 给前端（管理后台通用 CRUD 场景除外）。


3. **校验下沉:**
* 业务逻辑校验（如“用户名唯一”）推荐下沉到 Jimmer 的 **`DraftInterceptor`** 或 Service 层入口，确保逻辑闭环。



---

## 五、 关键业务实现标准 (Domain Standards)

### 1. 逻辑删除标准 (Logical Deletion)

**目的：** 实现数据保留，同时解决唯一索引冲突问题。

* **数据库规范:**
* 字段名：**`deleted_millis`** (类型 `bigint`)。
* 默认值：`0` (表示有效)。
* 删除行为：填入当前时间戳。
* **唯一索引:** 必须包含该字段，例如 `UNIQUE KEY (username, deleted_millis)`。


* **代码规范:**
* Entity 字段标注 `@LogicalDeleted(value = "0", useTimestamp = true)`。
* Service 层直接调用 `delete` 方法，框架自动处理 UPDATE 逻辑。
* **禁止:** 手动编写 SQL 更新删除状态。



### 2. 权限管理标准 (Access Control)

**目的：** 实现“控制层拦截操作，数据层隔离数据”的双重保障。

* **模型:** 采用标准 RBAC 五表模型 (User, Role, Menu, UserRole, RoleMenu)。
* **功能权限 (Functional):**
* 基于 **Sa-Token**。
* 在 Controller 方法上使用注解 `@SaCheckPermission("user:add")` 进行显式拦截。


* **数据权限 (Data Scope):**
* 基于 **Jimmer Global Filter**。
* **严禁** 在 Service 层手动拼接 `dept_id` 或 `creator_id` 条件。
* 必须实现 `Filter<DeptProps>` 接口，根据当前用户上下文自动注入 SQL `WHERE` 条件，实现无感知的行级隔离。



### 3. 日志与观测标准 (Observability)

**目的：** 构建结构化、可审计的日志体系。

* **工具:** SLF4J + Logback (AsyncAppender)。
* **业务审计:**
* 写操作必须记录审计日志。
* 使用自定义注解 `@BizLog(module="用户", action="修改")`。
* 实现机制：AOP 拦截 -> 发布 Spring Event -> 虚拟线程异步写入数据库。


* **规范:**
* 必须使用占位符 `{}`，禁止使用 `+` 拼接字符串。
* 异常堆栈必须完整记录：`log.error("Error: {}", id, e)`。
* **禁止** 使用 `System.out.println`。



---

## 六、 AI 协作指南 (AI Collaboration)

为了让 Cursor / Copilot 等 AI 工具生成符合本规范的代码，请在 Prompt 中包含以下上下文：

> **Context:** Nova Framework (Spring Boot 4 + Jimmer).
> **Rules:**
> 1. Use **Java Records** for all DTOs.
> 2. **No Repository interfaces**; use `JSqlClient` directly in Service classes.
> 3. Use **RPC-style naming** for POST endpoints (e.g., `/user/create`).
> 4. Explicitly fetch associations using Jimmer Fetchers.
> 5. Use `R<T>` for response wrapping.
> 
> 

---