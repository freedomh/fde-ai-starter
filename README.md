# FDE AI Starter

侯旭的 AI 项目落地课程骨架。克隆后，通过 Docker 一次启动 Web 前端、Web 管理端、Java 后端、MySQL 和 Redis；再把 [课程提示词](docs/课程提示词.md) 逐轮发给 AI，完成自己的项目。

**main 分支只有技术骨架，不含业务。** 完成版“课程反馈台”在 `demo/class-feedback` 分支，适合讲师现场演示和学员课后对照。

## 学员第一次运行（Windows / PowerShell）

课前安装并启动 Docker Desktop（Linux 容器模式），准备 Git 和能读写工程、执行命令的 AI 编码工具。全容器路线不要求在电脑上另装 Node、Java、Maven、MySQL、Redis。

```powershell
git clone https://github.com/freedomh/fde-ai-starter.git my-ai-project
cd my-ai-project
powershell -NoProfile -ExecutionPolicy Bypass -File .\Check-Environment.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\Start-Local.ps1
```

首次构建需要联网下载镜像和依赖；请课前完成。Start-Local 会生成本机随机配置、构建镜像、启动五个服务，并等待后端/数据库/缓存可用，最后打印访问地址。

| 入口 | main 默认地址 |
|---|---|
| Web 前端 | http://127.0.0.1:18481 |
| Web 管理端 | http://127.0.0.1:18482 |
| 后端检查 | http://127.0.0.1:18480/api/health |

## 讲师克隆演示成品

推荐另用一个目录，避免切分支时混用本机环境文件与数据卷：

```powershell
git clone --branch demo/class-feedback https://github.com/freedomh/fde-ai-starter.git class-feedback-demo
cd class-feedback-demo
powershell -NoProfile -ExecutionPolicy Bypass -File .\Start-Local.ps1
```

演示分支默认使用另一组端口 `18580/18581/18582` 和独立 Compose 名称、数据库及数据卷，不与空骨架混用。

## 技术与目录

沿用本地空架子的 Vue 3 + TypeScript + Vite、Java 17 / Spring Boot 3.5.7、Spring JDBC / Flyway、MySQL 8.0.42；补齐 Redis 7.2.8 的默认启动与连接检查。前端依赖以各自 package-lock.json 为准。

```text
frontend/   Web 前端
admin/      Web 管理端
backend/    Java API 与数据库迁移
infra/      Dockerfile、Nginx、Compose
docs/       安装、提示词、演示需求与验收
```

两个 Vue 工程独立构建，共用同一 Java API。Nginx 把 /api 代理到 backend:8080；Java 在容器里通过 mysql / redis 服务名连接依赖。数据库和缓存不发布宿主机端口。

## 日常命令

```powershell
docker compose --env-file infra/.env -f infra/compose.yaml ps
docker compose --env-file infra/.env -f infra/compose.yaml logs --tail 80 backend
powershell -NoProfile -ExecutionPolicy Bypass -File .\Stop-Local.ps1
```

停止保留 MySQL 卷；重新运行 Start-Local.ps1 即可。修改源码后，启动脚本会重新构建。不要用 `down -v` 作为常规排错方法。

infra/.env 在本机首次运行时随机生成，已被 Git 忽略。不要上传它或把真实用户数据带入课堂。已有 MySQL 卷不会因为改 .env 就自动更改账号密码。

端口冲突时，只修改自己副本 infra/.env 的 BACKEND_PORT/WEB_PORT/ADMIN_PORT；不要结束别人的服务。脚本最终打印的地址为准。完整系统准备见 [空电脑准备](docs/空电脑准备.md)。

这是本地教学骨架，未提供用户登录或权限隔离，不直接作为公网生产应用。
