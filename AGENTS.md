提示词（写进CLAUDE.md或AGENT.md的）：
你正在参与开发一个企业级私有云平台项目，请在后续设计、编码和架构决策中始终遵循以下背景和约束。

项目概述

本项目是一个基于 React + Spring Boot 的现代化私有云平台，采用 pnpm + Turborepo 管理的 Monorepo 架构。

项目目标不仅是实现传统网盘功能，还将逐步扩展为集：

- 文件存储与管理
- 文件共享
- 本地挂载
- 在线预览与编辑
- OCR
- RAG 知识库
- AI Agent
- 自动化工作流

于一体的智能私有云平台。

后续可能支持桌面客户端、CLI、插件系统、多节点部署等能力。

Monorepo 结构

apps/

- web：React 主前端
- admin：管理后台
- server：Spring Boot 核心业务服务
- gateway：统一网关
- agent-service：Agent 服务
- rag-service：RAG 服务
- ocr-service：OCR 服务
- preview-service：文件预览服务

packages/

- sdk：前后端共享接口定义
- types：公共类型
- utils：公共工具
- protocol：事件与通信协议

当前开发重点

当前优先开发 Spring Boot 核心业务服务（server）。

目标是先完成：

- 用户系统
- 认证授权
- 文件管理
- 对象存储抽象
- 文件上传下载
- 分享功能
- 回收站
- 基础搜索

后续再逐步接入 OCR、RAG、Agent 等 AI 能力。

后端架构原则

遵循：

- DDD 思想（适度，不追求过度设计）
- 模块化单体优先
- 高内聚低耦合
- 面向接口编程
- 领域能力可独立拆分为微服务

禁止直接将所有业务堆积到 Controller 或 Service 中。

技术栈

Java 21

Spring Boot 3.x

Spring Security

JWT

MyBatis Plus

PostgreSQL

Redis

MinIO

Docker

Maven

核心模块

auth

负责：

- 登录
- 注册
- 邮箱验证
- JWT签发
- Refresh Token
- RBAC权限控制

典型表：

- sys_user
- sys_role
- sys_permission
- sys_user_role
- sys_role_permission
- sys_refresh_token

storage

负责：

- 文件上传
- 文件下载
- 文件删除
- 文件移动
- 文件复制
- 文件预览
- 文件元数据管理

该模块是整个系统核心。

必须实现存储抽象层。

禁止业务代码直接操作本地文件系统或 MinIO SDK。

统一定义：

StorageProvider

接口。

后续支持：

- Local Storage
- MinIO
- S3
- WebDAV
- SMB

等存储后端。

file

负责文件元数据管理。

数据库仅保存元数据：

- 文件名
- 文件大小
- MIME
- Hash
- Storage Key
- 创建时间
- 修改时间

实际文件由 StorageProvider 管理。

share

负责：

- 文件分享
- 提取码
- 分享链接
- 访问控制

search

负责：

- 文件名搜索
- 标签搜索
- 内容搜索（后续）

recycle

负责：

- 软删除
- 回收站恢复
- 定时清理

文件系统设计原则

数据库保存元数据。

对象存储保存实际文件。

业务层不得直接依赖具体存储实现。

统一通过：

StorageProvider

接口访问存储。

AI 能力设计原则

AI 能力不是核心业务的一部分。

Agent、OCR、RAG 必须独立服务。

Spring Boot 仅负责：

- 用户
- 权限
- 文件
- 任务调度
- API

AI 服务通过 HTTP、MQ 或 RPC 集成。

禁止将 OCR、Embedding、LLM 调用逻辑直接写入核心业务模块。

编码要求

生成代码时优先考虑：

- 可维护性
- 可扩展性
- 清晰的模块边界
- 接口抽象
- 单元测试友好

避免：

- 上帝类
- 巨型 Service
- 工具类泛滥
- 魔法字符串
- 硬编码

新增功能时优先思考：

1. 属于哪个模块？
2. 是否应该定义接口？
3. 是否会影响未来扩展？
4. 是否符合存储抽象设计？
5. 是否符合 RBAC 权限体系？

请始终以企业级长期演进项目的标准进行设计与实现。