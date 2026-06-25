# Minecloud

Minecloud 是一个基于微服务架构的云端文件管理系统，采用前后端分离设计，支持文件上传、下载、预览、分享、回收站等完整功能，并包含用户认证、权限管理、管理后台等企业级特性。

## 功能特性

### 核心功能

- **文件管理** - 上传（普通、分片、秒传）、下载、预览、重命名、移动、复制、删除
- **文件预览** - 支持图片、PDF、视频、音频、代码高亮预览
- **回收站** - 文件恢复、彻底删除、自动清理
- **文件分享** - 创建分享链接、设置密码/过期时间/下载次数限制
- **文件搜索** - 关键词搜索、类型过滤、排序

### 用户系统

- **认证** - 注册、登录、邮箱验证、忘记密码/重置密码
- **权限** - RBAC（用户-角色-权限）体系
- **管理后台** - 用户管理、角色管理、权限管理、仪表盘统计

### 技术特性

- **微服务架构** - Gateway + Auth + Storage + Share + Search
- **存储抽象** - 支持本地、S3、挂载存储等多种后端
- **JWT 认证** - Access Token + Refresh Token 双令牌机制
- **分片上传** - 大文件分片上传、断点续传
- **秒传** - 文件哈希校验，相同文件秒传

## 技术栈

### 前端

| 技术 | 版本 | 说明 |
|------|------|------|
| React | 19.2.6 | UI 框架 |
| Vite | 8.0.12 | 构建工具 |
| TypeScript | 6.0.2 | 类型系统 |
| react-router-dom | 7.18.0 | 路由管理 |
| highlight.js | 11.11.1 | 代码预览高亮 |
| spark-md5 | 3.0.2 | 文件哈希计算 |

### 后端

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.14 | 应用框架 |
| Spring Cloud | 2025.0.0 | 微服务框架 |
| Spring Cloud Alibaba | 2025.0.0.0 | 微服务组件 |
| Java | 21 | 运行环境 |
| MyBatis Plus | 最新 | ORM 框架 |

### 数据库 & 基础设施

| 技术 | 版本 | 说明 |
|------|------|------|
| PostgreSQL | 16 | 主数据库 |
| Mailpit | 最新 | 邮件测试工具 |

### 移动端

| 技术 | 版本 | 说明 |
|------|------|------|
| Expo | 56 | 移动端框架 |
| React Native | 0.85.3 | 移动端 UI |

## 项目结构

```
minecloud/
├── apps/
│   ├── web/                 # React 前端 (Vite)
│   ├── server/              # Spring Boot 后端 (Maven 多模块)
│   │   ├── api/             # 共享 DTOs
│   │   ├── common/          # 公共组件 (JWT, R<> wrapper, 异常处理)
│   │   ├── gateway/         # API 网关 (port 8080)
│   │   └── services/
│   │       ├── auth/        # 认证服务 (port 8081)
│   │       ├── storage/     # 存储服务 (port 8082)
│   │       ├── share/       # 分享服务 (port 8083)
│   │       └── search/      # 搜索服务 (port 8084)
│   ├── mobile/              # Expo/React Native 移动端
│   └── cli/                 # CLI 工具 (开发中)
├── packages/
│   ├── types/               # 共享类型定义 (开发中)
│   └── ui/                  # 共享 UI 组件 (开发中)
├── infra/
│   ├── docker/              # Docker Compose 配置
│   └── sql/                 # 数据库初始化脚本
└── data/                    # 本地存储目录
```

## 快速开始

### 环境要求

- Java 21+
- Node.js 18+
- pnpm 11+
- PostgreSQL 16+
- Maven 3.9+

### 1. 克隆项目

```bash
git clone https://github.com/Heartforhero/minecloud.git
cd minecloud
```

### 2. 启动数据库

```bash
cd infra/docker
docker-compose up -d
```

这将启动 PostgreSQL 和 Mailpit（邮件测试工具）。

### 3. 初始化数据库

```bash
psql -h localhost -U postgres -d minecloud -f infra/sql/init.sql
```

### 4. 启动后端

在 IDEA 中启动以下服务（按顺序）：

1. **Gateway** (8080) - API 网关
2. **Auth** (8081) - 认证服务
3. **Storage** (8082) - 存储服务
4. **Share** (8083) - 分享服务
5. **Search** (8084) - 搜索服务

或使用命令行：

```bash
cd apps/server
mvn spring-boot:run -pl gateway
mvn spring-boot:run -pl services/auth
mvn spring-boot:run -pl services/storage
mvn spring-boot:run -pl services/share
mvn spring-boot:run -pl services/search
```

### 5. 启动前端

```bash
cd apps/web
pnpm install
pnpm dev
```

访问 http://localhost:5173

### 6. 创建管理员账户

1. 访问 http://localhost:5173/register 注册账户
2. 在数据库中分配管理员角色：

```sql
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);
```

## 配置说明

### JWT 配置

Gateway 和 Auth 服务需要配置相同的 JWT Secret：

```yaml
jwt:
  secret: your-secret-key-here
  access-token-expire: 3600000  # 1小时
  refresh-token-expire: 604800000  # 7天
```

### 邮件配置

Auth 服务需要配置 SMTP 服务器：

```yaml
mail:
  host: smtp.example.com
  port: 587
  username: your-email@example.com
  password: your-password
  from: noreply@minecloud
```

开发环境可使用 Mailpit（默认已启动）：

```yaml
mail:
  host: localhost
  port: 1025
  from: noreply@minecloud
```

### 存储配置

Storage 服务支持多种存储后端：

```yaml
storage:
  local:
    base-path: ./data  # 本地存储路径
  recycle-bin:
    retention-days: 30  # 回收站保留天数
```

## API 文档

### 认证 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/auth/register | 用户注册 |
| POST | /api/v1/auth/verify-email | 验证邮箱 |
| POST | /api/v1/auth/login | 用户登录 |
| POST | /api/v1/auth/forgot-password | 忘记密码 |
| GET | /api/v1/auth/validate-reset-token | 验证重置令牌 |
| POST | /api/v1/auth/reset-password | 重置密码 |
| POST | /api/v1/auth/refresh | 刷新令牌 |
| POST | /api/v1/auth/logout | 登出 |

### 文件 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/files/list | 文件列表 |
| POST | /api/v1/files/upload | 上传文件 |
| POST | /api/v1/files/chunk | 分片上传 |
| POST | /api/v1/files/merge | 合并分片 |
| GET | /api/v1/files/check-hash | 检查哈希（秒传） |
| POST | /api/v1/files/quick-upload | 秒传 |
| GET | /api/v1/files/{id}/download | 下载文件 |
| GET | /api/v1/files/{id}/preview | 预览文件 |
| PUT | /api/v1/files/{id}/rename | 重命名 |
| POST | /api/v1/files/{id}/move | 移动 |
| POST | /api/v1/files/{id}/copy | 复制 |
| DELETE | /api/v1/files/{id} | 删除 |

### 分享 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/v1/share/create | 创建分享 |
| GET | /api/v1/share/list | 分享列表 |
| PUT | /api/v1/share/{id} | 更新分享 |
| DELETE | /api/v1/share/{id} | 删除分享 |
| GET | /api/v1/share/public/{token} | 公开分享信息 |
| POST | /api/v1/share/public/{token}/verify | 验证分享密码 |

### 管理后台 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/v1/admin/dashboard | 仪表盘统计 |
| GET | /api/v1/admin/users | 用户列表 |
| PUT | /api/v1/admin/users/{id} | 更新用户 |
| PUT | /api/v1/admin/users/{id}/roles | 分配角色 |
| GET | /api/v1/admin/roles | 角色列表 |
| POST | /api/v1/admin/roles | 创建角色 |
| PUT | /api/v1/admin/roles/{id} | 更新角色 |
| DELETE | /api/v1/admin/roles/{id} | 删除角色 |

## 开发指南

### 前端开发

```bash
cd apps/web
pnpm install    # 安装依赖
pnpm dev        # 启动开发服务器
pnpm build      # 构建生产版本
```

### 后端开发

```bash
cd apps/server
mvn compile     # 编译
mvn test        # 测试
mvn package     # 打包
```

### 代码规范

- 前端使用 TypeScript，遵循 ESLint 规则
- 后端使用 Java 21，遵循 Spring 最佳实践
- 所有 API 使用 `R<T>` 统一响应格式

### 存储抽象

所有文件操作必须通过 `StorageFacade`：

```java
// 正确方式
storageFacade.upload(file, userId);

// 禁止方式
Files.write(path, bytes);  // ❌ 禁止直接调用 java.nio.file
```

## 路线图

### 已完成 ✅

- [x] 用户认证（注册、登录、邮箱验证、密码重置）
- [x] 文件管理（上传、下载、预览、重命名、移动、复制）
- [x] 分片上传、秒传
- [x] 回收站（恢复、彻底删除、自动清理）
- [x] 文件分享（密码、过期时间、下载次数）
- [x] 文件搜索
- [x] 管理后台（用户、角色、权限管理）
- [x] RBAC 权限体系

### 进行中 🚧

- [ ] S3 存储后端实现
- [ ] 单元测试和集成测试
- [ ] CI/CD 流程
- [ ] 移动端应用完善

### 计划中 📋

- [ ] Redis 缓存
- [ ] Office 文档预览
- [ ] 文件版本管理
- [ ] 团队协作功能
- [ ] OCR 文字识别
- [ ] RAG 智能检索
- [ ] Agent 自动化处理

## 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件。

## 联系方式

- 作者：Heartforhero
- GitHub：https://github.com/Heartforhero/minecloud
