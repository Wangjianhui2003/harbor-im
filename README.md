# harbor-im

Backend services for Harbor IM. This repository contains the HTTP platform service and the Netty-based real-time messaging service.

Frontend repository: <https://github.com/Wangjianhui2003/harbor-frontend>

## Overview

`harbor-im` is a multi-module Maven project split into two runtime applications:

- `platform`: Spring Boot HTTP API on port `8100` with base path `/api`
- `server`: Netty WebSocket service on port `8101` with connection path `/im`
- `common`: shared models, constants, MQ helpers, utilities, and Protobuf definitions

The backend supports authentication, user profiles, friend and group management, private and group messaging, file upload, and WebRTC signaling.

## Repository Layout

```text
harbor-im/
├── common/                        # shared code and protobuf
├── platform/                      # Spring Boot HTTP API
│   └── src/main/resources/mapper/ # MyBatis XML mappers
├── server/                        # Netty IM service
├── sql/harbor/                    # schema and seed SQL
├── pom.xml                        # Maven aggregator
└── docker-settings.xml            # Docker build Maven settings
```

## Tech Stack

- Java `25`
- Maven `3.9+`
- Spring Boot `4`
- Netty
- MyBatis-Plus
- MySQL
- Redis
- RocketMQ
- MinIO
- Protobuf

## Local Development

### Prerequisites

Start the following dependencies before launching the services:

- MySQL
- Redis
- RocketMQ
- MinIO

Import the required SQL scripts from `sql/harbor/`.

### Build

```sh
mvn clean package
```

Useful checks:

```sh
mvn test
mvn -q -DskipTests compile
```

### Run Locally

Recommended startup order:

1. Start MySQL, Redis, RocketMQ, and MinIO.
2. Start `server`.
3. Start `platform`.

Run the packaged JARs:

```sh
java -jar server/target/server.jar --spring.profiles.active=dev
java -jar platform/target/platform.jar --spring.profiles.active=dev
```

## Service Endpoints

### HTTP API

- Base URL: `http://localhost:8100/api`
- API docs: `http://localhost:8100/api/doc.html`

Main route groups include:

- `/captcha`, `/login`, `/register`, `/refreshToken`, `/modifyPwd`
- `/user/*`
- `/friend/*`
- `/add/*`
- `/group/*`
- `/message/private/*`
- `/message/group/*`
- `/image/upload`, `/file/upload`, `/video/upload`
- `/webrtc/config`, `/webrtc/private/*`

### WebSocket

- Endpoint: `ws://localhost:8101/im`
- Heartbeat idle timeout: `60s`

## Docker Build

Build from the repository root:

```sh
docker network create harbor
docker build -f platform/Dockerfile -t harbor-platform .
docker build -f server/Dockerfile -t harbor-server .
docker run -d --name harbor-platform -p 8100:8100 --network harbor harbor-platform
docker run -d --name harbor-server -p 8101:8101 --network harbor harbor-server
```

## Development Notes

- `common/src/main/proto/im_ws.proto` defines the IM protocol; rebuild after changing it.
- `platform/src/main/resources/application.yaml` defines the HTTP port and `/api` servlet path.
- `server/src/main/resources/application.yml` defines the WebSocket and TCP socket ports.
- Keep secrets and machine-specific settings out of versioned config when possible.
