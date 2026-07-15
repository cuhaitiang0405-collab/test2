# MDT 多学科会诊中心 — Windows 10 LTSC 本地部署指南

> 本指南面向**从未配置过网站开发环境的新手**，每步都有详细说明。预计总耗时 30–45 分钟（取决于下载速度）。

---

## 一、需要安装的软件（3 个）

### 1. JDK 21（Java 开发包）

本项目用 Java 20，装 JDK 21 LTS 完全兼容且更稳定。

1. 打开 https://adoptium.net/download/
2. 选 **Temurin 21 (LTS)** → **Windows** → **x64** → 下载 `.msi` 安装包
3. 双击安装，**全部默认选项**，一路「下一步」到底
4. 验证：打开 **命令提示符**（按 `Win+R`，输入 `cmd`，回车），输入：

```
java -version
```

看到 `openjdk version "21.x.x"` 即成功。

---

### 2. Node.js 22 LTS

1. 打开 https://nodejs.org/
2. 下载 **22.x.x LTS**（左边绿色按钮），选 `.msi`
3. 双击安装，**全部默认**，一路下一步
4. 验证：

```
node -v
npm -v
```

---

### 3. PostgreSQL 15（数据库）

> Windows 上直接装 PostgreSQL，比 Docker 简单。

1. 打开 https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
2. 选 **PostgreSQL 15.x** → **Windows x86-64**，下载 `.exe`
3. 双击安装：
   - 安装目录：默认（`C:\Program Files\PostgreSQL\15`）
   - **密码**：设一个你能记住的，比如 `mdt_pass`（**一定记住！**）
   - **端口**：保持 `5432`（默认）
   - Stack Builder：可以取消不装
4. 安装完成后，打开 **pgAdmin 4**（开始菜单可搜到）
5. 在 pgAdmin 里：
   - 展开左侧 Servers → PostgreSQL 15
   - 右键 「Databases」→ Create → Database
   - Database name：`mdt`
   - Owner：`postgres`
   - 点 Save
6. 验证：在 pgAdmin 里双击 `mdt` 数据库，能展开即成功。

> **账号密码汇总（记好）：**
> - 数据库用户名：`postgres`
> - 数据库密码：你设的那个（如 `mdt_pass`）

---

## 二、配置数据库连接

代码里数据库密码通过环境变量传入，不在配置文件里硬编码。

### Windows 设置环境变量

1. 按 `Win+R`，输入 `sysdm.cpl`，回车
2. 点「高级」→「环境变量」
3. 在「用户变量」区域，点「新建」，添加以下变量：

| 变量名 | 变量值（改成你的密码） |
|--------|----------------------|
| `POSTGRES_PASSWORD` | `你的数据库密码`（如 `mdt_pass`） |

4. 点确定保存。**重启命令提示符**后再往下操作。

---

## 三、启动后端服务

> 后端是 6 个 Java 微服务，但当前里程碑只需要启动 4 个：auth、integration、image、gateway。

打开**命令提示符**（`cmd`），进入项目目录：

```
cd 你放代码的目录\test2
```

### 3.1 检查 Maven（Java 构建工具）

项目自带 Maven Wrapper，无需单独安装：

```
cd backend
mvnw.cmd --version
```

> 第一次运行会自动下载 Maven，等 1-2 分钟。看到版本号即成功。

### 3.2 构建所有后端

```
mvnw.cmd clean package -DskipTests
```

> 首次构建会下载大量依赖，可能需要 **10–20 分钟**，取决于网速。喝杯咖啡等他跑完。  
> 看到 `BUILD SUCCESS` 就完成了。

### 3.3 按顺序启动服务

**开 4 个独立的命令提示符窗口**，每个窗口运行一个服务：

**窗口 1 — 权限服务（8081 / gRPC 50054）**
```
cd backend\mdt-auth
..\mvnw.cmd spring-boot:run
```

**窗口 2 — 集成服务（8082 / gRPC 50051）**
```
cd backend\mdt-integration
..\mvnw.cmd spring-boot:run
```

**窗口 3 — 影像服务（8083 / gRPC 50056）**
```
cd backend\mdt-image
..\mvnw.cmd spring-boot:run
```

**窗口 4 — API 网关（8080）**
```
cd backend\mdt-gateway
..\mvnw.cmd spring-boot:run
```

> 每个窗口出现 `Started XxxApplication in ...` 字样即启动成功。

### 3.4 验证后端

新开一个命令提示符，输入：

```
curl http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"doctor\",\"password\":\"doctor123\"}"
```

> 如果没装 curl，浏览器打开 http://localhost:8080 看是否返回数据也行。

---

## 四、启动前端服务

新开一个命令提示符：

```
cd you\test2\frontend
npm install
npm run dev
```

`npm install` 首次运行需下载依赖，约 2–5 分钟。

看到：
```
  ➜  Local:   http://localhost:5173/
```

即启动成功。

---

## 五、访问系统

浏览器打开 **http://localhost:5173**

默认演示账号：
- 用户名：`doctor`
- 密码：`doctor123`

---

## 六、常见问题

| 问题 | 解决 |
|------|------|
| `mvnw.cmd` 不是内部命令 | 确认在 `backend` 目录下执行，用 `dir mvnw*` 确认文件存在 |
| 构建时报 `JAVA_HOME` 错误 | 重启电脑，或手动设置环境变量 `JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.x.x` |
| 启动服务时报数据库连接错误 | 确认 PostgreSQL 已启动（pgAdmin 能连上），环境变量 `POSTGRES_PASSWORD` 已设置 |
| WebGL 阅片页面显示灰色 | 浏览器输入 `chrome://flags` 搜索 `WebGL`，确保没有禁用；更新显卡驱动 |
| 端口被占用 | 改成其他端口需同时改 `application.yml` 和网关路由配置，建议先排查占用（`netstat -ano | findstr :8080`） |

---

## 七、环境变量完整清单（记录用）

生产部署需要配置：

| 变量名 | 用途 | 示例值 |
|--------|------|--------|
| `POSTGRES_PASSWORD` | 数据库密码 | `mdt_pass` |
| `POSTGRES_USER` | 数据库用户（默认 postgres） | `postgres` |
| `POSTGRES_DB` | 数据库名 | `mdt` |
| `PG_HOST` | 数据库地址 | `localhost` |
| `PG_PORT` | 数据库端口 | `5432` |

---

> 遇到问题随时回来问，我帮你排查。
