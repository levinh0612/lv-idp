# Day 11 – User Storage SPI: External JDBC, LDAP, lazy migration

## Mục tiêu
Đăng nhập bằng user lưu ở DB khác (Postgres/MySQL/Oracle/SQL Server) hoặc LDAP, với hash legacy; sau lần login đầu tự import & rehash sang Argon2id.

## Chuẩn bị
- Day 10 xong. Dựng DB "legacy" mẫu bằng Docker (Postgres/MySQL) với bảng `users(id, login, mail, pwd_hash, full_name, active)`; hash bcrypt, md5+salt, pbkdf2 để thử.
- Đọc `plan.html` mục 6.2.

## Steps

### Step 1 – SPI & chain
- [ ] `UserStorageProvider` (xem plan mục 7): `findByUsername`, `findByExternalId`, `verifyCredential`, `readOnly`.
- [ ] `UserStorageChain`: duyệt các storage của tenant theo `priority` (Local luôn đầu tiên).
- [ ] Bảng `user_storage_config(id, tenant_id, type, enabled, priority, config_encrypted)`.
- [ ] `app_user.storage_id`, `app_user.external_id` để biết user đến từ đâu.

### Step 2 – ExternalJdbcStorageProvider
- [ ] Config: JDBC URL, user/pass (mã hóa), driver (allow-list), pool nhỏ (HikariCP).
- [ ] Query cấu hình bằng named parameter **chỉ SELECT**: `SELECT id, login, mail, pwd_hash, full_name, active FROM users WHERE login = :username`.
- [ ] Mapping cột → thuộc tính user (JSON config).
- [ ] Kết nối tài khoản DB **chỉ quyền read** trên bảng cần thiết.
- [ ] Chặn SQL injection: chỉ cho tham số hoá; validate query khi lưu (chỉ SELECT, một statement).

### Step 3 – Legacy hash verifiers
- [ ] Interface `LegacyHashVerifier` + implementation: `bcrypt`, `pbkdf2-sha256`, `sha256+salt`, `md5+salt` (chỉ để verify, ghi cảnh báo).
- [ ] Config chọn `hash_algorithm` + vị trí salt.
- [ ] So sánh constant-time (`MessageDigest.isEqual`).

### Step 4 – Lazy migration
- [ ] Verify thành công → tạo `app_user` local + `credential PASSWORD` băm Argon2id → ghi `external_id`.
- [ ] Từ đó đăng nhập trực tiếp bằng Local; storage ngoài chỉ còn là fallback (có thể tắt khi migrate xong).
- [ ] Chế độ `READ_ONLY_FEDERATION` (không import, luôn hỏi DB ngoài) cho hệ thống chưa cho phép sao chép.
- [ ] Job báo cáo tiến độ migrate (đã import / còn lại).

### Step 5 – LDAP provider
- [ ] Spring LDAP / UnboundID: bind account tìm DN theo `uid`/`sAMAccountName`, rồi bind bằng DN + password của user để xác thực.
- [ ] Config: URL (`ldaps://`), base DN, filter, mapping thuộc tính, group → role.
- [ ] Test bằng container `osixia/openldap` (hoặc `lldap`).

### Step 6 – Test & commit
- [ ] Test từng hash algorithm với dữ liệu mẫu; test user inactive bị từ chối.
- [ ] Test migrate: lần 2 không còn gọi DB ngoài (verify bằng mock count).
- [ ] `feat(day11): user storage spi, external jdbc, ldap, lazy migration`.

## Definition of Done
- User ở DB legacy đăng nhập được qua IDP; sau đó hash trong DB local là `{argon2}`.
- Query không phải SELECT bị từ chối khi cấu hình.

## Lỗi hay gặp
- Mở pool tới DB ngoài không giới hạn → nghẽn khi có tấn công login. Pool nhỏ + timeout ngắn + circuit breaker.
- Lưu credential DB ngoài plaintext trong config. Phải mã hóa.
- LDAP injection khi ghép filter bằng string → escape giá trị.

## Tham khảo
- Keycloak User Storage SPI, OWASP LDAP Injection Prevention.
