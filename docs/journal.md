# Journal

## 2026-11-09 – Day 01
**Làm:** skeleton multi-module, Docker Compose, application.yml, test Testcontainers, CI.

**Gặp lỗi:**
- `no POM in this directory`: chưa có pom gốc.
- Cổng 5432/5433/8080 đều bị chiếm: dùng 5434 (Postgres) và 8099 (app).
- `spring-boot:run -am` chạy cả module gốc không có `main`: bỏ `-am`, cài module phụ thuộc bằng `mvn install`.
- Quên `--env-file .env` khi chạy docker compose.

**Học được:**
- Initializr ghi version `4.1.2.RELEASE`, còn Maven Central và `pom.xml` dùng `4.1.2`.
- Compose chỉ tự đọc `.env` nằm cạnh file compose.
- Ghim image Testcontainers (`postgres:16`, `redis:7`) cho khớp môi trường dev, tránh `latest`.
