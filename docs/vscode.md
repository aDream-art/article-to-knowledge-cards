# 在 VS Code 里运行

> 目标：打开文件夹 → 按 F5 起后端 → 一个任务起前端，全程不离开编辑器。
> 你的环境（已确认）：JDK 17、Maven 3.9.16、Node 22、MySQL 8（服务）、便携版 Redis。

---

## 0. 一次性准备

### 0.1 装扩展（3 个必装）

在 VS Code 里 `Ctrl+Shift+X` 搜索安装，或者用命令行：

```powershell
code --install-extension vscjava.vscode-java-pack
code --install-extension Vue.volar
code --install-extension vmware.vscode-spring-boot
```

| 扩展 | 作用 | 必须 |
|---|---|---|
| **Extension Pack for Java** | Java 语言支持 + Maven + 调试器 | ✅ |
| **Vue - Official**（原 Volar） | `.vue` 语法/TS 提示 | ✅ |
| Spring Boot Extension Pack | `application.yml` 补全、运行面板 | 可选 |

> 不要同时装 Vetur（旧版 Vue2 插件），会和 Volar 冲突。

### 0.2 确认 JDK 是 17

项目需要 JDK 17。VS Code 的 Java 扩展会自动探测本机 JDK；如果本机装了多个版本，请在 VS Code 的 Java 配置中选择 17。仓库不写死本机 JDK / Maven 路径。

---

## 1. 打开**项目文件夹**（最容易错的一步）

```powershell
# 换成你本机 clone 下来的路径
code "<你的项目路径>"
```

**为什么必须打开这一层**：后端启动时工作目录 = VS Code 打开的文件夹，而 `.env`（数据库密码、API Key）就在这层。如果你的工作目录是上一级，`application.yml` 里这句会读不到配置：

```yaml
spring.config.import: optional:file:.env[.properties]
```

结果就是连不上数据库、或者悄悄退回 mock 模式。

> 如果你习惯打开上一级 `default-workspace`，把 `.vscode/launch.json` 里的 `"cwd"` 改成 `"${workspaceFolder}/ai-chat-assistant"` 即可。

---

## 2. 启动顺序（四件套）

### 2.1 MySQL

是 Windows 服务，开机自启。检查：

```powershell
Get-Service MySQL80
```

### 2.2 Redis

`Ctrl+Shift+P` → **Tasks: Run Task** → 选 **`redis: 启动（幂等）`**

（等价命令：`.\scripts\start-redis.bat`。已经在跑的话它会直接退出，不会起第二个。）

### 2.3 后端

按 **F5**，或左侧调试图标选 **`后端：ChatApplication（读 .env）`**。

- 启动成功的标志：终端出现 `Started ChatApplication in ... seconds`
- 默认端口 8080；如果被占用，加参数 `--server.port=8081`（改 `launch.json` 的 `args`）

### 2.4 前端

`Ctrl+Shift+P` → **Tasks: Run Task** → **`web: 启动前端（5173）`**

或者自己开一个终端：

```powershell
cd web
npm install   # 第一次才需要
npm run dev
```

---

## 3. 验证

| 检查 | 地址 | 期望 |
|---|---|---|
| 后端存活 | <http://127.0.0.1:8080/actuator/health> | `{"status":"UP"}` |
| 接口文档 | <http://127.0.0.1:8080/swagger-ui.html> | Swagger 页面 |
| 前端 | <http://127.0.0.1:5173> | 登录页；仓库不预置账号，首次使用请注册 |

---

## 4. 调试技巧

### 4.1 断点打在哪

| 想看什么 | 文件 | 方法 |
|---|---|---|
| 流式链路 | `ChatService` | `doStream` 开头、`invoke` 内部 |
| 上下文拼装 | `ContextService` | `build` |
| RAG 召回 | `RagServiceImpl` | `retrieve`（看返回的片段数） |
| SQL | 不用断点 | 默认 `INFO` 不打印完整 SQL 参数；需要时临时把 `com.luojiajun.chat` 调到 `debug`，调试后恢复，避免日志保留聊天内容 |

### 4.2 断点别停太久

SSE 生成跑在 `chat-stream-N` 线程上，**断点停住时心跳仍在每 15s 发一次**；但如果停超过几十秒，浏览器或 Nginx 可能判超时断开，恢复后你会看到 `ABORTED`。

调试长流程时，建议临时把 `ChatService.HEARTBEAT_SECONDS` 调大（比如 60），或者把断点放在请求前半段（参数校验、检索）而不是逐字输出处。

### 4.3 改完代码怎么生效

- **后端**：F5 重启即可（Java 扩展会先编译）；或者用任务 `backend: 打包（跳过测试）`
- **前端**：`npm run dev` 有 HMR，保存即生效（`.vue`、`.ts` 都支持）

---

## 5. 常见问题

| 现象 | 原因 | 解决 |
|---|---|---|
| Java 文件满屏红波浪线 | 扩展还在建索引 / Lombok 未生效 | 等右下角进度结束；`Ctrl+Shift+P` → **Java: Clean Java Language Server Workspace** 后重启 |
| `Cannot resolve symbol 'lombok'` | 同上 | 确认装了 Extension Pack for Java（新版已内置 Lombok 支持） |
| 启动报 `Access denied for user 'chat_app'` | 打开的是上级目录，`.env` 没读到 | 按第 1 节重新打开文件夹 |
| 启动报 `Port 8080 was already in use` | 之前 `java -jar` 的进程还在 | `Get-NetTCPConnection -LocalPort 8080`，kill 掉对应 PID |
| Redis 连不上 | 没启动 | 跑任务 `redis: 启动（幂等）`，或看 `.tools/redis/redis.log` |
| 前端接口 404 | 后端没起 / 端口不是 8080 | 检查 `web/vite.config.ts` 的 proxy 目标 |
| `npm run dev` 报端口占用 | 5173 被上一轮占着 | kill 掉旧 node 进程 |
| 终端中文乱码 | 代码页不是 UTF-8 | `chcp 65001`，或改用 PowerShell 7 |

---

## 6. 任务一览（`Ctrl+Shift+P` → Tasks: Run Task）

| 任务 | 等价命令 |
|---|---|
| `redis: 启动（幂等）` | `.\scripts\start-redis.bat` |
| `backend: 打包（跳过测试）` | `mvn -DskipTests package` |
| `backend: 跑测试` | `mvn test`（31 个单测，不依赖 MySQL/Redis） |
| `web: 启动前端（5173）` | `cd web && npm run dev` |
| `web: 构建（tsc + vite）` | `cd web && npm run typecheck && npm run build` |

---

## 7. `.vscode` 要不要提交？

现在 `.gitignore` 里是忽略的：

```
.vscode/
```

意味着**别人 clone 下来没有这套配置**。想让它跟着仓库走（方便自己换电脑、也显得专业），把 `.gitignore` 里那行改成：

```
.vscode/*
!.vscode/settings.json
!.vscode/launch.json
!.vscode/tasks.json
```

> 注意：`settings.json` 里写了你本机的 JDK 和 Maven 绝对路径，提交前最好改成注释说明，或者干脆只提交 `launch.json` + `tasks.json`。
