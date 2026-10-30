# 课程反馈台 · 演示成品

这是 `freedomh/fde-ai-starter` 的 `demo/class-feedback` 演示分支。教师录入课堂表现→本地模板整理草稿→教师编辑确认→学员端查看。用于演示 AI 项目开发后的实际结果；草稿不是外部模型生成，不需要 API Key。

从空架子学习请克隆 `main`，再逐轮发送 [课程提示词](docs/课程提示词.md)。本分支已经完成业务，适合讲师展示、学员对照及运行验收。所有姓名与内容为虚构数据，无账号/生产鉴权，不直接对公网运行。

## 学员第一次运行（Windows / PowerShell）

课前安装并启动 Docker Desktop（Linux 容器模式），准备 Git 和能读写工程、执行命令的 AI 编码工具。全容器路线不要求在电脑上另装 Node、Java、Maven、MySQL、Redis。

```powershell
git clone --branch demo/class-feedback https://github.com/freedomh/fde-ai-starter.git class-feedback-demo
cd class-feedback-demo
powershell -NoProfile -ExecutionPolicy Bypass -File .\Check-Environment.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\Start-Local.ps1
```

首次构建需要联网下载镜像和依赖；请课前完成。Start-Local 会生成本机随机配置、构建镜像、启动五个服务，并等待后端/数据库/缓存可用，最后打印访问地址。

| 入口 | 演示分支默认地址 |
|---|---|
| 学员端 | http://127.0.0.1:18581 |
| 教师管理端 | http://127.0.0.1:18582 |
| 后端检查 | http://127.0.0.1:18580/api/health |

## 一次完整演示

1. 两个浏览器页分别打开教师端与学员端，选择“小林（演示）”。
2. 教师录入主题、课堂表现和课后任务，保存记录。
3. 点击“生成反馈草稿（本地模板）”，学员端刷新仍不显示。
4. 教师改写反馈内容，点击“保存并确认”；学员端刷新看到相同内容。
5. 展示统计变化、确认后只读、重启后数据仍在。细节见 [演示项目](docs/演示项目.md)。

可重复验收（会增加虚构练习数据，保留已有记录）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Verify-Demo.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Verify-Demo.ps1 -CheckRestart
```

演示分支默认使用另一组端口 `18580/18581/18582` 和独立 Compose 名称、数据库及数据卷，不与空骨架混用。

## 技术与目录

沿用本地空架子的 Vue 3 + TypeScript + Vite、Java 17 / Spring Boot 3.5.7、Spring JDBC / Flyway、MySQL 8.0.42、Redis 7.2.8。前端依赖以各自 package-lock.json 为准。完整契约见 [需求与接口](docs/需求与接口.md)。

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

infra/.env 首次运行时随机生成，已被 Git 忽略。Compose 名称带当前目录短哈希，不同克隆副本的数据卷隔离，端口仍需避开占用。不要上传该文件或真实用户数据。已有 MySQL 卷不会因为改 .env 就自动更改账号密码。

端口冲突时，只修改自己副本 infra/.env 的 BACKEND_PORT/WEB_PORT/ADMIN_PORT；不要结束别人的服务。脚本最终打印的地址为准。完整系统准备见 [空电脑准备](docs/空电脑准备.md)。

这是本地教学骨架，未提供用户登录或权限隔离，不直接作为公网生产应用。
