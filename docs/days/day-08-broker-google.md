# Day 08 – Identity Broker framework + Google

## Mục tiêu
Xây khung `FederationConnector` (kiểu Dex) và provider đầu tiên: Google (OIDC). Nút "Đăng nhập với Google" trên trang login; user mới được tạo JIT.

## Chuẩn bị
- Day 07 xong. Tạo OAuth client ở Google Cloud Console (type Web), redirect URI: `http://localhost:8080/t/default/broker/google/callback`.
- Đọc `plan.html` mục 6.1 và mục 7 (SPI).

## Steps

### Step 1 – SPI trong `idp-core`
- [ ] `FederationConnector` (type, buildAuthorizationRequest, handleCallback) – xem plan mục 7.
- [ ] `ExternalIdentity(subject, email, emailVerified, name, picture, rawClaims)`.
- [ ] `ConnectorRegistry` (Spring bean tìm connector theo `type`).

### Step 2 – Cấu hình provider theo tenant
- [ ] Bảng `idp_connection(id, tenant_id, alias, type, enabled, config_encrypted, claim_mappings)` (đã tạo Day 02).
- [ ] `config_encrypted` chứa `client_id`, `client_secret` (mã hóa bằng `KeyCipher` Day 05).
- [ ] Seed 1 connection `google` cho tenant `default` qua biến môi trường (chưa cần UI).

### Step 3 – Broker endpoints (`idp-federation`)
- [ ] `GET /t/{tenant}/broker/{alias}/login`: tạo `state` + `nonce` + PKCE, lưu vào Redis (TTL 10 phút) gắn với `authRequestId`/session, redirect sang upstream.
- [ ] `GET /t/{tenant}/broker/{alias}/callback`: validate `state`, đổi code lấy token, **verify id_token** (chữ ký qua JWKS Google, `iss`, `aud`, `exp`, `nonce`).
- [ ] Sau khi có `ExternalIdentity` → `FederatedLoginService`.

### Step 4 – Google connector
- [ ] Dùng discovery `https://accounts.google.com/.well-known/openid-configuration`; cache có TTL.
- [ ] Scope `openid email profile`; lấy `sub`, `email`, `email_verified`.
- [ ] SSRF guard cho mọi HTTP gọi ra ngoài (chặn private IP, giới hạn timeout, size).

### Step 5 – FederatedLoginService (phiên bản đầu)
- [ ] Tìm `federated_identity(connection_id, external_subject)` → có thì đăng nhập user đó.
- [ ] Chưa có → JIT: tạo user mới (`email_verified` theo upstream), tạo `federated_identity`. (Linking với user hiện có để Day 09.)
- [ ] Tạo Spring Security `Authentication` → quay lại luồng `/oauth2/authorize` ban đầu.

### Step 6 – UI & test
- [ ] Trang login: render nút theo danh sách connection enabled.
- [ ] Test với WireMock giả lập Google (discovery, token, jwks) – không gọi Google thật trong CI.
- [ ] `feat(day08): identity broker + google`.

## Definition of Done
- Bấm "Google" → đăng nhập → quay lại client với token; DB có `app_user` + `federated_identity`.
- `state` sai/hết hạn → từ chối; `nonce` sai → từ chối.

## Lỗi hay gặp
- Không lưu được state vì cookie SameSite=Strict chặn callback cross-site → dùng `Lax`.
- Dùng email làm khoá định danh → dùng `sub` của upstream.
- Quên kiểm tra `aud` id_token → chấp nhận token của app khác.

## Tham khảo
- Google OIDC docs, Dex connector interface, OIDC Core §3.1.3.7 (validation).
