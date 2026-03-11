# Repository Guidelines

## Project Structure & Module Organization
`harbor-im` is a multi-module Maven backend. The root [`pom.xml`](/home/jianhui/project/harbor/harbor-im/pom.xml) aggregates:

- `harbor-common`: shared IM models, constants, MQ helpers, and utilities under `src/main/java`; protobuf sources live in `src/main/proto`.
- `harbor-platform`: Spring Boot HTTP API on port `8100`, with Java code in `src/main/java`, MyBatis XML in `src/main/resources/mapper`, and tests in `src/test/java`.
- `harbor-server`: Netty/WebSocket service on port `8101`, with runtime config in `src/main/resources`.
- `sql/harbor`: schema and seed SQL scripts.

## Build, Test, and Development Commands
- `mvn clean package`: build all modules and produce runnable jars in each module’s `target/`.
- `mvn test`: run the current test suite.
- `java -jar harbor-server/target/harbor-server.jar --spring.profiles.active=dev`: start the IM server locally.
- `java -jar harbor-platform/target/harbor-platform.jar --spring.profiles.active=dev`: start the HTTP API locally.
- `docker build -f harbor-platform/Dockerfile -t harbor-platform .`: build the platform image from the repo root. Use the same pattern for `harbor-server`.

Use JDK `25` and Maven `3.9+`. Local startup also requires MySQL, Redis, RocketMQ, and MinIO.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, one public class per file, and organized imports. Use `PascalCase` for classes, `camelCase` for methods and fields, and keep existing suffixes such as `*ReqDTO`, `*RespDTO`, `*Mapper`, and `*ServiceImpl`. Put Spring MVC controllers in `controller`, persistence entities in `dao/entity`, and MyBatis interfaces in `dao/mapper` with matching XML names.

## Testing Guidelines
There is no enforced coverage gate today, and test coverage is light. Add focused JUnit-style tests under the matching module and package path, using `*Test` names. Prefer service, mapper, and serialization tests for behavior changes; pair SQL changes with a reproducible test or verification note.

## Commit & Pull Request Guidelines
Recent history uses short conventional prefixes such as `feat:local cache` and `fix:private message`. Keep commit subjects lowercase, imperative, and scoped. PRs should describe behavior changes, affected modules, required config or SQL updates, and manual verification steps. For API changes, include example requests or response shape changes.

## Security & Configuration Tips
Configuration is profile-based (`application.yaml`, `application-dev*.yml`). Do not introduce new hardcoded secrets; prefer environment-specific overrides. Treat `sql/harbor` and profile configs as deployable artifacts and review them as carefully as Java code.
