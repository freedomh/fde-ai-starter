# AI 开发约定

先读 README.md 与 docs/课程提示词.md。main 是无业务的技术骨架；本轮只做用户给出的需求。前端、管理端、Java 后端与 infra 同级，不复制其他产品数据、业务源码或凭据。

沿用 Vue 3 + TypeScript、Java 17 + Spring Boot、MySQL、Redis 和 Docker Compose。所有请求走 /api，相同字段以前后端共同的接口文档为准；数据库变化新增 Flyway 迁移，保留旧数据。

运行时密码仅由 Init-Local.ps1 在本机生成，infra/.env 不提交、不打印。仅绑定本机端口；不部署服务器，不关闭防火墙，不删除数据卷，不影响其他项目。

实际创建文件和执行命令。构建走 Docker，前端 npm ci/build、后端 mvn verify；启动后检查 /api/health 及涉及的业务流程。交付说明已修改、已验证、未完成，不把“写完代码”当成“跑通”。
