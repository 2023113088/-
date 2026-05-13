# library-cloud — 图书馆微服务（云原生实验）

基于 **Spring Boot 3.2**、**Spring Cloud 2023** 的多模块 Maven 工程：用户、图书、借阅、注册发现、网关、配置中心，配套 **Docker Compose** 与 **Nginx** 入口，与课程实验 PDF / PPT 架构对齐。

更细的分阶段验收与协作说明见：**[docs/阶段实施计划与验收.md](docs/阶段实施计划与验收.md)**。

---

## 技术栈

| 类别 | 选型 |
|------|------|
| 运行时 | Java 17+、Spring Boot 3.2 |
| 服务治理 | Netflix Eureka |
| 远程调用 | OpenFeign + Spring Cloud LoadBalancer |
| 容错 | Resilience4j（熔断 / 降级，`fallbackFactory`） |
| 网关 | Spring Cloud Gateway（路由、JWT、`X-Trace-Id`） |
| 配置 | Spring Cloud Config（镜像内本地 Git，模拟「从 Git 拉配置」） |
| 数据 | JPA；本地 **H2**；Docker 下 **MySQL 8** 三库（userdb / bookdb / borrowdb） |
| 容器 | Docker Compose；独立项目名、桥接网络、命名卷；宿主机端口可配 |

---

## 模块说明

| 模块 | 说明 |
|------|------|
| `library-common` | 公共 Web 配置、RFC 7807 错误体、`X-Trace-Id` MDC、内部接口头常量 |
| `eureka-server` | 注册中心 |
| `config-server` | 配置中心，读取构建时写入镜像的 `config-repo`（`file:///git-config`） |
| `gateway-service` | API 网关：JWT 鉴权、白名单、路由到各 `lb://` 服务 |
| `user-service` | 注册 / 登录、JWT 签发 |
| `book-service` | 图书 CRUD / 搜索；库存变更接口受 **内部令牌** 保护 |
| `borrow-service` | 借阅；**Feign** 调图书服务；出站透传 Trace + 内部令牌；降级工厂 |

---

## 已实现的「补充 / 工程化」能力（README 汇总）

以下内容是在满足实验大纲 **(1) 微服务框架 (2) Feign+LB+熔断 (3) Eureka (4) 网关认证与路由** 基础上，额外做到或可写进报告的点：

1. **与课程 PPT 拓扑一致**  
   - **Nginx**（默认宿主机 `18888`）→ **Gateway**（容器内 `8080`）；对外主入口与 PPT「前端经 Nginx 进网关」一致。  
   - **Spring Cloud Config**：各业务与网关在 `docker` profile 下通过 **`spring.config.import: optional:configserver:http://config-server:8888/`** 拉取合并配置；`config-repo/` 在 **config-server 镜像构建** 内 `git init` 并提交，Config 使用 **`file:///git-config`**，语义上对齐「Config + Git」（远端 Git 可将 `uri` 改为 `https://...` 自行扩展）。

2. **Docker 与环境隔离**  
   - Compose **项目名** `library-cloud`、**独立网络** `library-cloud-backend`、MySQL **命名卷**，减少与其它 Compose 栈冲突。  
   - **`env.example`** 列出全部可覆盖变量；复制为 **`.env`** 后修改端口与密钥；**`.gitignore` 忽略 `.env`**，避免密钥进仓库。  
   - MySQL 宿主机端口默认 **13306**（`MYSQL_HOST_PORT`），减轻与本机 **3306** 占用冲突。

3. **服务间调用与安全**  
   - 借阅 → 图书：**OpenFeign** + **LoadBalancer** + **Resilience4j** + **`BookFeignFallbackFactory`**（图书不可用时明确降级与日志）。  
   - Feign 出站 **`BorrowOutboundFeignConfig`**：透传 **`X-Trace-Id`**，并带 **`X-Internal-Token`**（与 `INTERNAL_SERVICE_TOKEN` / `app.internal.api-token` 一致）。  
   - **`PUT /api/books/{id}/stock`** 由 **`InternalStockProtectionFilter`** 校验内部令牌，无令牌 **403**。

4. **网关与统一错误**  
   - JWT 全局过滤器、登录/注册等白名单；**401** 使用 **`application/problem+json`**（RFC 7807）。  
   - **`TraceIdGatewayFilter`**：入口生成 / 透传 **`X-Trace-Id`**，便于全链路排查。

5. **可演示的运维向能力**  
   - 各服务 **Actuator health**（含探针相关配置预留）。  
   - **`docker compose up -d --scale book-service=2`** 可演示 **BOOK-SERVICE** 多实例与 Feign 负载均衡。

---

## 快速开始（Docker 全栈）

```bash
cd library-cloud
cp env.example .env   # 可选：改端口与密钥
docker compose up -d --build
```

| 用途 | 默认地址（可在 `.env` 中改） |
|------|------------------------------|
| **对外主入口（PPT / Nginx）** | `http://localhost:18888`（`NGINX_HOST_PORT`） |
| 直连网关（调试 / Postman） | `http://localhost:8080`（`GATEWAY_HOST_PORT`） |
| Eureka | `http://localhost:8761`（`EUREKA_HOST_PORT`） |
| Config Server | `http://localhost:8888`（`CONFIG_SERVER_HOST_PORT`） |
| MySQL | `localhost:13306`（`MYSQL_HOST_PORT`） |
| 各业务直连（Swagger 等） | `9001`～`9003`（见 `env.example`） |

**注意**：网关依赖 Eureka 注册，冷启动后若偶发 **503**，稍等数秒待实例注册完成再试。  
**不用 Docker**：可用 `mvn` 各模块启动，默认 **H2**；详见 `docs/阶段实施计划与验收.md` 中「本地运行顺序」。

---

## 与本 GitHub 仓库

远程仓库名为 `-` 时，克隆示例：

```bash
git clone git@github.com:2023113088/-.git
cd -
```

若 shell 将单字符目录名解析异常，可使用：

```bash
git clone git@github.com:2023113088/-.git library-cloud && cd library-cloud
```

---

## 许可证

教学实验用途；按需自行补充 LICENSE。
