# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Java-based real-time messaging backend (harbor-im) featuring:

- **Multi-module Maven project** with clear separation of concerns
- **Spring Boot 3.3.1** REST API platform (port 8100) for HTTP operations
- **Netty-based WebSocket server** (port 8101) for real-time messaging
- **MySQL database** with MyBatis Plus ORM for data persistence
- **Redis caching** with Redisson for session management and distributed locks
- **RocketMQ** for asynchronous message processing
- **MinIO** for file storage and management
- **JWT-based authentication** with access and refresh tokens

## Development Commands

### Maven Commands

```bash
# Build all modules
mvn clean install

# Build specific module with dependencies
mvn clean package -pl harbor-platform -am

# Run tests
mvn test

# Build without tests
mvn clean package -DskipTests

# Run Spring Boot application (from module directory)
mvn spring-boot:run
```

### Docker Commands

```bash
# Build platform module Docker image
docker build -f harbor-platform/Dockerfile -t harbor-platform .

# Build server module Docker image
docker build -f harbor-server/Dockerfile -t harbor-server .
```

### Module-Specific Commands

```bash
# Run platform module (REST API)
cd harbor-platform && mvn spring-boot:run

# Run server module (WebSocket)
cd harbor-server && mvn spring-boot:run
```

## Environment Configuration

### Port Configuration

- **Platform Module**: Port 8100 (REST API at `/api`)
- **Server Module**: Port 8101 (WebSocket at `/im`)
- **TCP Socket**: Port 8102 (disabled by default)

### Environment Profiles

Configuration files in `src/main/resources/`:

- `application.yaml` - Base configuration
- `application-dev.yaml` - Development profile
- `application-prod.yaml` - Production profile

Active profile set via `spring.profiles.active: dev`

### Key Environment Variables

Configured in `application-*.yaml` files:

- Database connection (MySQL)
- Redis connection
- RocketMQ configuration
- MinIO credentials
- JWT secrets (must match between platform and server modules)

## Architecture & Key Patterns

### Module Structure

```
harbor-im/
├── harbor-common/     # Shared models, enums, constants
├── harbor-client/     # IM client library for platform→server communication
├── harbor-platform/   # Spring Boot REST API (business logic, database)
└── harbor-server/     # Netty WebSocket server (real-time messaging)
```

### Key Architectural Patterns

1. **Microservices-style Separation**:
    - Platform handles HTTP requests, business logic, database operations
    - Server manages WebSocket connections, message routing, real-time communication
    - Client module provides API for platform to send messages to connected clients

2. **Netty Event-driven Architecture**:
    - WebSocket server uses Netty for high-performance connection handling
    - Message processors (`ProcessorFactory`) handle different message types
    - Custom handlers for login, heartbeat, and message processing

3. **Message Queue Integration**:
    - RocketMQ for async message processing and persistence
    - Consumers handle message storage and delivery guarantees

4. **Redis-based Session Management**:
    - User online status tracking
    - Distributed locks for concurrent operations
    - Repeat submission prevention

5. **Database Layer**:
    - MyBatis Plus with enhanced CRUD operations
    - Automatic camelCase to snake_case mapping
    - Connection pooling via Druid

### Important Files to Understand

#### Platform Module (harbor-platform/)

- `src/main/java/com/jianhui/project/harbor/platform/HarborPlatformApp.java` - Main application
- `src/main/resources/application.yaml` - Configuration
- `controller/` - REST controllers (FriendController, GroupController, UserController, etc.)
- `service/` - Business logic services
- `mapper/` - MyBatis Plus mappers

#### Server Module (harbor-server/)

- `src/main/java/com/jianhui/project/harbor/server/netty/wbsocket/WebSocketServer.java` - WebSocket server entry point
- `src/main/java/com/jianhui/project/harbor/server/netty/processor/` - Message processors
- `src/main/resources/application.yml` - Server configuration

#### Common Module (harbor-common/)

- `src/main/java/com/jianhui/project/harbor/common/model/` - Message models (BaseMessage, ChatMessage, etc.)
- `src/main/java/com/jianhui/project/harbor/common/enums/` - Enums (MessageType, ResponseCode, etc.)

### Message Flow

1. **Client Connection**: WebSocket connection to `ws://host:8101/im`
2. **Authentication**: JWT token validation
3. **Message Processing**:
    - Netty handlers process login, heartbeat, messages
    - ProcessorFactory routes to appropriate message processor
4. **Business Logic**: Platform module handles friend requests, group management, etc.
5. **Real-time Messaging**: Server module routes messages between connected clients
6. **Async Processing**: RocketMQ consumers handle message persistence

## Development Environment

### Technology Stack

- **Java 17** - Runtime
- **Spring Boot 3.3.1** - Main framework
- **Netty 4.1.42** - WebSocket/TCP server
- **MyBatis Plus 3.5.7** - ORM
- **Redis** with Redisson 3.21.3 - Caching and distributed locks
- **RocketMQ** - Message queue
- **MinIO 8.5.1** - Object storage
- **MySQL** - Database
- **JWT** (java-jwt 3.11.0) - Authentication

### Key Libraries

- **Hutool 5.8.28** - Chinese utility library
- **FastJSON 1.2.83** - JSON processing
- **Knife4j 4.5.0** - API documentation (Swagger UI)
- **Druid 1.1.22** - Database connection pool
- **Lombok** - Code generation
- **Thumbnailator 0.4.8** - Image processing

### IDE Support

- IntelliJ IDEA project files present (`.idea/`)
- Standard Maven project structure

## Database Schema

Key entities managed via MyBatis Plus mappers:

- Users, Friends, Groups, Group Members
- Private Messages, Group Messages
- Friend Requests, Group Join Requests

## Key Features

- Real-time chat (private and group)
- Friend management with request/approval flow
- Group chat with member management and admin roles
- File upload and storage via MinIO
- WebRTC support (ICE server configuration)
- Online status tracking
- Message recall (300-second window)
- Heartbeat mechanism (60-second timeout)
- Distributed locks for concurrent operations
- Repeat submission prevention

## Deployment

### Building for Production

```bash
# Build with production profile
mvn clean package -Pprod -DskipTests

# Or build specific module
mvn clean package -pl harbor-platform -am -Pprod -DskipTests
```

### Docker Deployment

- Each module has its own Dockerfile
- Use `docker-settings.xml` for Maven Docker builds
- Environment-specific configurations in `application-prod.yaml`

### Port Requirements

- Platform: 8100 (HTTP)
- Server: 8101 (WebSocket)
- MySQL: 3306
- Redis: 6379
- RocketMQ: 9876, 10909, 10911
- MinIO: 9000