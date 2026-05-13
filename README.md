# library-cloud — 图书馆微服务

Spring Boot 3.2、Spring Cloud 2023：用户、图书、借阅、Eureka、Gateway、Config Server，Docker Compose + Nginx。

---

## 已实现

- 多模块 Maven、`library-common`；Eureka 注册发现；Gateway 路由 + JWT + 白名单。
- `borrow-service` → `BOOK-SERVICE`：OpenFeign、LoadBalancer、Resilience4j、`BookFeignFallbackFactory`。
- 内部库存接口 `PUT /api/books/{id}/stock` 校验 `X-Internal-Token`；借阅 Feign 自动带令牌与 `X-Trace-Id`。
- 统一错误（RFC 7807 / `ProblemDetail`）；网关 401 为 `application/problem+json`。
- **PPT 对齐**：Nginx（默认 `18888`）→ Gateway；**Config Server** + `config-repo`（镜像内本地 Git，`file:///git-config`）。
- Docker：独立 compose 项目名、网络、MySQL 命名卷；`env.example` → `.env` 配端口与密钥；MySQL 默认映射 **13306**。
- 本地默认 H2；`docker` profile + MySQL 三库。

详见 **[docs/阶段实施计划与验收.md](docs/阶段实施计划与验收.md)**（端口、验收步骤、与 PDF 条目对照）。

---

## 未实现

- Kubernetes、Jenkins/GitOps、Prometheus/OpenTelemetry 等未接。
- 网关未聚合各服务 Swagger。
- Config 未接远端 Git 仓库（可自行改 `uri`）。
- Compose 仍暴露业务直连端口（9001–9003），非「仅网关出口」。

---

## 技术栈（简表）

| 类别 | 选型 |
|------|------|
| 服务治理 | Eureka |
| 调用 | OpenFeign + LoadBalancer |
| 容错 | Resilience4j |
| 网关 | Spring Cloud Gateway |
| 配置 | Spring Cloud Config |
| 数据 | JPA；H2 / MySQL 8 |

---

## Docker 启动

```bash
cd library-cloud
cp env.example .env   # 可选
docker compose up -d --build
```

| 用途 | 默认 |
|------|------|
| Nginx 入口 | `http://localhost:18888` |
| 直连网关 | `http://localhost:8080` |
| Eureka | `http://localhost:8761` |
| Config | `http://localhost:8888` |
| MySQL | `localhost:13306` |

---

## 克隆本仓库（GitHub 仓库名为 `-` 时）

```bash
git clone git@github.com:2023113088/-.git library-cloud && cd library-cloud
```
