# Library Management System

图书馆管理系统微服务实践项目。项目基于 JDK 17、Spring Boot 3.2、Spring Cloud 2023，包含 Eureka 注册中心、Config Server、Gateway 网关、用户服务、图书服务、借阅服务和静态前端页面。

## 演示启动方式

### 1. 启动后端微服务

在 IntelliJ IDEA 中按顺序启动以下 Spring Boot 启动类：

```text
1. EurekaServerApplication
2. UserServiceApplication
3. BookServiceApplication
4. BorrowServiceApplication
5. GatewayApplication
```

启动完成后访问 Eureka：

```text
http://localhost:8761
```

应看到以下服务注册成功：

```text
USER-SERVICE
BOOK-SERVICE
BORROW-SERVICE
GATEWAY-SERVICE
```

### 2. 启动前端

打开 PowerShell，执行：

```powershell
cd "D:\OneDrive\文档\GitHub\Library-Management-System\frontend"
node server.mjs
```

浏览器访问前端页面：

```text
http://localhost:5173
```

前端默认调用后端 Gateway：

```text
http://localhost:18080
```

### 3. 常用访问地址

| 用途 | 地址 |
|---|---|
| 前端页面 | `http://localhost:5173` |
| Gateway 接口入口 | `http://localhost:18080` |
| Eureka 注册中心 | `http://localhost:8761` |
| Config Server | `http://localhost:8888` |
| user-service | `http://localhost:9001` |
| book-service | `http://localhost:9002` |
| borrow-service | `http://localhost:9003` |

### 4. Gateway 接口示例

查询图书：

```http
GET http://localhost:18080/api/books
```

用户登录：

```http
POST http://localhost:18080/api/auth/login
Content-Type: application/json

{
  "username": "reader1",
  "password": "123456"
}
```

查询借阅记录：

```http
GET http://localhost:18080/api/borrows
Authorization: Bearer <TOKEN>
```

## 已实现功能

- 多模块 Maven 工程。
- Eureka 服务注册与发现。
- Gateway 统一入口、路由转发、JWT 认证、角色权限控制、访问日志、限流。
- `borrow-service` 通过 OpenFeign 调用 `user-service` 和 `book-service`。
- 基于 Eureka + OpenFeign 的服务发现和负载均衡。
- 基于 Resilience4j 的 Feign 熔断/降级。
- 用户注册、登录、角色区分。
- 图书查询、搜索、新增、修改、删除。
- 借书、还书、重复借阅拦截。
- 每个业务微服务独立数据库及表：`userdb`、`bookdb`、`borrowdb`。
- 本地默认使用 H2，Docker 环境使用 MySQL 8。
- 静态前端页面，用于演示系统主要功能。
- 自动化端到端测试脚本。

## 自动化测试

运行全自动端到端测试：

```powershell
cd "D:\OneDrive\文档\GitHub\Library-Management-System"
powershell -ExecutionPolicy Bypass -File .\scripts\run-e2e-tests.ps1
```

如果本机 `JAVA_HOME` 配置不正确，可以先在当前 PowerShell 临时指定 JDK 17：

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.7.7-hotspot"
```

测试脚本会自动验证：

- Gateway 路由转发
- Eureka 注册发现
- Feign 负载均衡
- Gateway 限流
- Feign 熔断/降级
- 用户注册和登录
- JWT 认证
- 管理员权限控制
- 图书新增
- 借书、重复借阅拦截、还书

## Docker 启动

也可以使用 Docker Compose 启动：

```bash
docker compose up -d --build
```

Docker 默认入口：

| 用途 | 地址 |
|---|---|
| Nginx 入口 | `http://localhost:18888` |
| Gateway 直连 | `http://localhost:18080` |
| Eureka | `http://localhost:8761` |
| Config Server | `http://localhost:8888` |
| MySQL | `localhost:13306` |

## 可继续完善

- Kubernetes、Jenkins/GitOps、Prometheus/OpenTelemetry 尚未接入。
- Gateway 尚未聚合各服务 Swagger。
- Config Server 当前 Docker 环境使用镜像内本地 Git 仓库，尚未接远程 Git 仓库。
- Docker Compose 仍暴露业务服务端口，生产环境可进一步收敛为只暴露 Nginx 或 Gateway。
