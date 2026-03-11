# harbor-im
这是后端代码仓库,前端：https://github.com/Wangjianhui2003/harbor-frontend
Harbor 的后端服务，采用多模块 Maven 结构，拆分为 HTTP 平台服务和 Netty 实时通信服务。

## 模块说明

```text
harbor-im/
├── harbor-platform   # Spring Boot HTTP API，端口 8100，统一前缀 /api
├── harbor-server     # Netty WebSocket 服务，端口 8101，连接路径 /im
├── sql               # 数据库脚本目录
└── pom.xml           # 多模块聚合工程
```

`harbor-platform` 负责登录注册、用户资料、好友关系、群组管理、消息历史、文件上传和 WebRTC 协调。

`harbor-server` 负责 WebSocket 长连接、心跳检测、在线状态、实时消息投递和 RocketMQ 消费。

## 运行环境

- JDK `25`
- Maven `3.9+`
- MySQL
- Redis
- RocketMQ
- MinIO

## 默认端口

| 服务 | 端口 | 说明 |
| --- | --- | --- |
| `harbor-platform` | `8100` | HTTP API，基础路径 `/api` |
| `harbor-server` | `8101` | WebSocket 服务，连接路径 `/im` |

## 本地开发

推荐启动顺序：

1. 启动 MySQL、Redis、RocketMQ、MinIO。
2. 启动 `harbor-server`。
3. 启动 `harbor-platform`。


### 使用 Jar 启动

```sh
cd harbor-im
java -jar harbor-server/target/harbor-server.jar --spring.profiles.active=dev
```

```sh
cd harbor-im
java -jar harbor-platform/target/harbor-platform.jar --spring.profiles.active=dev
```

## 接口入口

### HTTP API

- 基础地址：`http://localhost:8100/api`
- OpenAPI / Knife4j：`http://localhost:8100/api/doc.html`

主要接口分组：

- 认证：`/captcha`、`/login`、`/register`、`/refreshToken`、`/modifyPwd`
- 用户：`/user/*`
- 好友：`/friend/*`
- 加好友 / 加群申请：`/add/*`
- 群组：`/group/*`
- 私聊消息：`/message/private/*`
- 群消息：`/message/group/*`
- 文件上传：`/image/upload`、`/file/upload`、`/video/upload`
- WebRTC：`/webrtc/config`、`/webrtc/private/*`

### WebSocket

- 连接地址：`ws://localhost:8101/im`
- 心跳空闲超时：`60s`

## Docker 构建

两个模块都提供了 Dockerfile，构建上下文必须是 `harbor-im` 根目录。

```sh
docker network create harbor

cd harbor-im


docker build -f harbor-platform/Dockerfile -t harbor-platform .
docker build -f harbor-server/Dockerfile -t harbor-server .

docker run -d --name harbor-platform -p 8100:8100 --network harbor harbor-platform
docker run -d --name harbor-server -p 8101:8101 --network harbor harbor-server
```


## 功能概览

- 用户注册、登录、刷新 Token、修改密码
- 用户资料查询与更新
- 好友申请、好友列表、好友备注、删除好友
- 群组创建、搜索、邀请、退群、踢人、管理员设置
- 私聊 / 群聊消息发送、撤回、离线拉取、已读同步、历史查询
- 图片、文件、视频上传
- WebRTC 单聊呼叫、接听、挂断、候选同步、心跳保活

## 说明

- `IMSender` 已并入 `harbor-platform`，平台服务内部直接负责向 IM 服务投递消息。
- `sql/` 目录当前为空，首次部署需要你自行准备 `harbor` 库表结构和初始化数据。
- 如果前端需要联调，HTTP 入口是 `8100`，实时消息入口是 `8101`，两者都需要可用。
