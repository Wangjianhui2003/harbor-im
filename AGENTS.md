# Repository Guidelines

## Project Structure & Module Organization
This repository is a multi-module Maven backend. The root `pom.xml` aggregates `common`, `platform`, and `server`. Put shared constants, protocol models, MQ helpers, and Protobuf definitions in `common` (`src/main/java`, `src/main/proto`). Keep HTTP APIs, services, MyBatis mappers, and mapper XML in `platform`; XML files live in `platform/src/main/resources/mapper`. Keep Netty/WebSocket runtime code in `server`. Database schema and seed data belong in `sql/harbor`.

## Build, Test, and Development Commands
Use JDK `25` and Maven `3.9+`.

- `mvn clean package`: build all modules and create `platform/target/platform.jar` and `server/target/server.jar`.
- `mvn test`: run the current test suite for all modules.
- `mvn -q -DskipTests compile`: fast compile check from the repo root.
- `java -jar platform/target/platform.jar --spring.profiles.active=dev`: start the HTTP API on `8100` with `/api`.
- `java -jar server/target/server.jar --spring.profiles.active=dev`: start the IM server on `8101`.

Local development also requires MySQL, Redis, RocketMQ, and MinIO.

## Coding Style & Naming Conventions
Follow the existing Java style: 4-space indentation, one public class per file, and grouped imports. Use `PascalCase` for classes and `camelCase` for methods and fields. Match established suffixes such as `*ReqDTO`, `*RespDTO`, `*Mapper`, `*Service`, `*ServiceImpl`, and `*DO`. Prefer constructor injection with Lombok annotations already used in the codebase, such as `@RequiredArgsConstructor`.

## Testing Guidelines
Tests are currently light, so add focused coverage with every behavior change. Place tests under the matching module path, for example `platform/src/test/java/...`, and use `*Test` class names. Favor service, mapper, and protocol serialization tests. If you change `common/src/main/proto/im_ws.proto`, run a full Maven build so generated classes stay in sync.

## Commit & Pull Request Guidelines
Recent commits follow short conventional prefixes such as `feat:refactor` and `fix:group message`. Keep subjects lowercase, imperative, and concise. PRs should name affected modules, list config or SQL changes, and include verification steps. For API or protocol changes, add example payloads or describe the contract delta clearly.

## Security & Configuration Tips
Runtime config is profile-based in `application.yaml` plus `application-dev.yaml` or `application-prod.yaml`. Put machine-specific overrides in ignored `application-local*.yml` files, and never commit new secrets, passwords, or environment-specific endpoints.
