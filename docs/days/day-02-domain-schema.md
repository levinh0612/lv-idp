# Day 02 – Domain model & DB schema (Flyway)

## Mục tiêu
Thiết lập schema cốt lõi (tenant, user, credential, federated identity, role) với tenant isolation ngay từ đầu, kèm repository và test.

## Chuẩn bị
- Day 01 xong. Đọc `plan.html` mục 8 (ER diagram).
- Tham khảo skill `postgresql-table-design` cho kiểu dữ liệu & index.

## Steps

### Step 1 – Quyết định quy ước
- [ ] ID: `uuid` (UUIDv7 nếu có thư viện, nếu không thì `gen_random_uuid()`).
- [ ] Thời gian: `timestamptz`, mọi bảng có `created_at`, `updated_at`.
- [ ] Mọi bảng nghiệp vụ có `tenant_id` (trừ bảng toàn cục).
- [ ] Email lưu lowercase; unique `(tenant_id, lower(email))` và `(tenant_id, lower(username))`.

### Step 2 – Migration V1 (`idp-persistence/src/main/resources/db/migration`)
- [ ] `V1__tenant.sql`: `tenant(id, slug unique, display_name, status, created_at)`; seed tenant `default`.
- [ ] `V2__user.sql`: `app_user(id, tenant_id fk, username, email, email_verified, status, attributes jsonb, ...)`.
- [ ] `V3__credential.sql`: `credential(id, user_id fk, type, secret_hash, data jsonb, created_at, last_used_at)`; `type` ∈ `PASSWORD|TOTP|WEBAUTHN|RECOVERY`.
- [ ] `V4__federation.sql`: `idp_connection`, `federated_identity (unique connection_id + external_subject)`.
- [ ] `V5__role.sql`: `role`, `permission`, `user_role`, `role_permission` (để Day 15 dùng, tạo sẵn cũng được).
- [ ] Index: FK, `(tenant_id, status)`, GIN cho `attributes` nếu cần query.

### Step 3 – Entity & Repository (JPA)
- [ ] Module `idp-core`: record/value object thuần (không annotation JPA) cho `User`, `Tenant`.
- [ ] Module `idp-persistence`: entity `UserEntity`... + mapper (MapStruct hoặc tay).
- [ ] `TenantContext` (ThreadLocal hoặc `ScopedValue`) giữ tenant hiện tại.
- [ ] Repository luôn nhận `tenantId`: `findByTenantIdAndEmail(...)`. **Không** có method `findByEmail` trần.

### Step 4 – Test
- [ ] Testcontainers: migration chạy sạch từ đầu.
- [ ] Test: tạo 2 tenant cùng email → ok; cùng tenant trùng email (khác hoa/thường) → vi phạm unique.
- [ ] Test: query tenant A không thấy user tenant B.

### Step 5 – Commit
- [ ] `feat(day02): domain model and schema`.

## Definition of Done
- `mvn verify` xanh, Flyway `validate` pass.
- Không có repository method nào thiếu `tenantId` (review thủ công / ArchUnit rule).

## Lỗi hay gặp
- Quên `lower()` index → login phân biệt hoa thường.
- Dùng `ddl-auto=update` → **đừng**; đặt `validate`, schema chỉ do Flyway quản lý.
- Entity lộ ra tầng core → giữ domain thuần để dễ test và đổi storage.

## Tham khảo
- Flyway migrations, Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` cho jsonb.
