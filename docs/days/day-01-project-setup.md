# Day 01 – Skeleton: multi-module, Docker Compose, CI

## Mục tiêu
Có skeleton chạy được: Maven multi-module, Postgres + Redis + Mailpit qua Docker Compose, app khởi động có health endpoint, CI build xanh.

## Chuẩn bị
JDK 21+, Maven 3.9+, Docker Desktop, git.

---

## 1. Kiến thức cần nắm

### 1.1 Multi-module Maven
Một repo, nhiều module, mỗi module build ra một jar. **Parent pom** ở gốc gom chúng lại, nên một lệnh `mvn verify` build tất cả.

```
lv-idp/
├─ pom.xml            parent: version chung + danh sách module
├─ idp-core/          domain + SPI interface, KHÔNG phụ thuộc Spring Web
├─ idp-persistence/   JPA entity + Flyway migration
├─ idp-server/        app chạy được (hàm main)
├─ infra/             docker-compose.yml
└─ docs/
```

### 1.2 Phụ thuộc một chiều
```
idp-server ──► idp-persistence ──► idp-core
```
Module ngoài biết module trong, không có chiều ngược lại. Nếu `idp-core` import `idp-persistence`, Maven báo vòng phụ thuộc và từ chối build. Đó là chủ ý: domain không bị kéo vào chi tiết hạ tầng. Domain cần dữ liệu thì khai báo *interface* trong `idp-core`, `idp-persistence` cài đặt nó.

### 1.3 Chuỗi kế thừa pom
```
idp-server ──► lv-idp (pom gốc) ──► spring-boot-starter-parent:4.1.2
```
- Dùng `spring-boot-starter-parent` (không chỉ import BOM) vì BOM chỉ quản lý version *dependency*, còn parent quản lý cả version *plugin*.
- `<dependencyManagement>` ở pom gốc khai version module nội bộ một lần. Module con chỉ ghi `groupId` + `artifactId`.

### 1.4 Hai cách ghi version Boot
| Dùng ở đâu | Cách ghi |
|---|---|
| Tham số `bootVersion` của Spring Initializr | `4.1.2.RELEASE` |
| `pom.xml` (Maven Central) | `4.1.2` |

### 1.5 Docker Compose: cổng và file env
- `ports: "5434:5432"`: vế trái là cổng **máy host**, vế phải là cổng **trong container**. Đổi vế trái để tránh đụng cổng.
- Compose chỉ tự đọc `.env` nằm cạnh file compose (`infra/`). `.env` của ta ở thư mục gốc, nên luôn thêm `--env-file .env`.
- `healthcheck` giúp biết service đã sẵn sàng (`healthy`), không chỉ là "đang chạy".

---

## 2. Việc cần làm

> Trình xem trước Markdown của IntelliJ không cho bấm checkbox. Mở file ở chế độ Editor, đổi `- [ ]` thành `- [x]`.

### Đã xong
- [x] **Dọn git:** commit `d6a4d17 chore: reset repo for lv-idp`.
- [x] **Sinh `idp-server` từ Initializr** (Boot 4.1.2, Java 21).
- [x] **Parent pom + `idp-core` + `idp-persistence`:** `mvn -B verify -DskipTests` → 4 module `SUCCESS`.
- [x] **`.gitignore`:** `target/`, `.idea/`, `*.iml`, `.env`, `.DS_Store`.
- [x] **Docker Compose:** 3 service `healthy` (Postgres cổng 5434).

### Còn lại
- [ ] **Dọn thư mục thừa:** `claims-service/target/` còn sót (đã ignore, an toàn để xóa): `rm -rf claims-service`.
- [x] **`application.yml`:** xem mục 2.1 bên dưới.
- [x] **Chạy app, kiểm tra health:** `/actuator/health` → `{"status":"UP"}` (cổng 8099).
- [x] **Test context load** bằng Testcontainers (`IdpServerApplicationTests`) → `mvn -B verify` xanh (image ghim `postgres:16`, `redis:7`).
- [x] **CI:** `.github/workflows/ci.yml` (setup-java 21, `mvn -B verify`). Chỉ chạy được sau khi đẩy repo lên GitHub.
- [x] **Quy ước:** `docs/journal.md` và `docs/adr/0001-modular-monolith.md`.
- [ ] **Commit:** `feat(day01): project skeleton`.

### 2.1 `application.yml` (`idp-server/src/main/resources/application.yml`)

File cấu hình của Spring Boot: nói cho app biết kết nối DB, Redis nào, mở cổng nào, lộ endpoint nào. Đã **xóa** `application.properties` do Initializr sinh, để không có hai file cấu hình trùng nhau.

| Khối | Ý nghĩa | Vì sao |
|---|---|---|
| `server.port: ${IDP_SERVER_PORT:8099}` | Cổng app, mặc định 8099 | Đổi được qua env khi cổng bị chiếm |
| `spring.datasource.*` | URL, user, password của Postgres | Lấy từ env, **password không có mặc định** để không lộ bí mật trong code |
| `spring.jpa.open-in-view: false` | Không giữ connection DB suốt request | Tránh nghẽn connection pool |
| `spring.jpa.hibernate.ddl-auto: none` | Hibernate không tự sửa schema | Schema do Flyway quản lý, có phiên bản, kiểm soát được |
| `spring.flyway.enabled: true` | Chạy migration `db/migration` khi khởi động | Day 02 sẽ thêm file migration đầu tiên |
| `spring.data.redis.host/port` | Kết nối Redis | Lấy từ env, mặc định `localhost:6379` |
| `management.endpoints...include: health,info` | Chỉ lộ 2 endpoint actuator | Lộ ít nhất có thể. `prometheus` thêm ở Day 19 khi có dependency |

**Cú pháp `${BIẾN:mặc-định}`:** dùng giá trị của biến môi trường `BIẾN`, nếu chưa đặt thì dùng giá trị sau dấu `:`.

**Khi chạy lần đầu sẽ thấy cảnh báo** `No migrations found`. Đây là bình thường, vì chưa có file migration nào.

---

## 3. Lệnh hay dùng

```bash
# Hạ tầng
docker compose -f infra/docker-compose.yml --env-file .env up -d
docker compose -f infra/docker-compose.yml --env-file .env ps
docker compose -f infra/docker-compose.yml --env-file .env exec postgres psql -U idp -d idp -c "select 1"
docker compose -f infra/docker-compose.yml --env-file .env exec redis redis-cli ping
docker compose -f infra/docker-compose.yml --env-file .env down      # tắt (giữ dữ liệu)

# Build
mvn -B -q validate                 # kiểm tra cú pháp pom
mvn -B verify -DskipTests          # build, bỏ test
mvn -B verify                      # build + test (cần Docker chạy)

# Chạy app
set -a; source .env; set +a        # nạp biến từ .env vào shell
mvn -B -q -DskipTests install      # cài core + persistence (chỉ cần khi chúng thay đổi)
mvn -pl idp-server spring-boot:run # KHÔNG dùng -am (xem bảng lỗi)
curl -s localhost:8099/actuator/health        # kỳ vọng {"status":"UP"}

# Đổi cổng: IDP_SERVER_PORT=9000 mvn -pl idp-server spring-boot:run
```

Mailpit UI: http://localhost:8025

---

## 4. Definition of Done
- 3 container `healthy`.
- `mvn -B verify` xanh.
- `curl localhost:8099/actuator/health` → `{"status":"UP"}`.

## 5. Lỗi đã gặp và cách xử lý

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| `mvn`: `there is no POM in this directory` | Chưa có `pom.xml` gốc, hoặc đứng sai thư mục | Chạy `mvn` ở thư mục gốc `lv-idp/` |
| `Non-resolvable parent POM` | Sai `groupId`/`artifactId`/`version` ở `<parent>`, hoặc Boot ghi `4.1.2.RELEASE` | Parent của `idp-server`: `dev.lv` / `lv-idp` / `0.0.1-SNAPSHOT`. Boot ghi `4.1.2` |
| `required variable IDP_DB_PASSWORD is missing` | Thiếu `.env`, hoặc quên `--env-file .env` | Tạo `.env` từ `.env.example`, thêm `--env-file .env` |
| `Bind for 0.0.0.0:5433 failed: port is already allocated` | Cổng đã có container/tiến trình khác | Xem ai giữ: `docker ps` và `lsof -iTCP:<port> -sTCP:LISTEN`. Đổi `IDP_DB_PORT` và `IDP_DB_URL` trong `.env` |
| `spring-boot:run`: `Unable to find a suitable main class` | Dùng `-am`, Maven chạy `spring-boot:run` cả trên module gốc `lv-idp` (không có `main`) | Bỏ `-am`. Cài trước core + persistence bằng `mvn -DskipTests install` |
| `Web server failed to start. Port 8099 was already in use` | Cổng đang có tiến trình khác | Xem ai giữ: `lsof -iTCP:8099 -sTCP:LISTEN`. Chạy với `IDP_SERVER_PORT=<cổng khác>` |
| Testcontainers không tìm thấy Docker | Docker Desktop chưa chạy | Bật Docker Desktop, kiểm tra `DOCKER_HOST` |
| Flyway thiếu driver Postgres | Thiếu module riêng (Flyway 10+) | Thêm `flyway-database-postgresql` (đã có sẵn trong `idp-server`) |

Cổng trên máy này: 5432 (Postgres local), 5433 (container `lpf-postgres`), nên dự án dùng **5434**. Cổng 8080 đang bị một container khác giữ, nên app dùng **8099**.

## Tham khảo
Spring Initializr, Testcontainers Postgres module, Flyway docs.
