# 脚本说明

## manual-migrate-add-target-type.sql

当访问「全部消息」接口出现 **Unknown column 'tb_1_.target_type' in 'field list'** 时，说明数据库表 `sys_message` 尚未执行增加 `target_type` 的迁移（例如 Docker 镜像在加入该迁移前已构建）。

**解决方式二选一：**

1. **重新构建并启动（推荐）**  
   在项目根目录执行：  
   `docker compose build app --no-cache && docker compose up -d`  
   启动时 Flyway 会自动执行 `V1.0.2__add_message_target_type.sql`。

2. **仅手动执行迁移（不重建镜像）**  
   在项目根目录执行（将 `YOUR_ROOT_PASSWORD` 换成 MySQL root 密码）：  
   `docker exec -i landfun-mysql mysql -uroot -pYOUR_ROOT_PASSWORD landfunboot < scripts/manual-migrate-add-target-type.sql`  
   若提示列已存在可忽略。
