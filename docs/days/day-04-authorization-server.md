# Day 04 – Authorization Server: client registry, Code + PKCE

## Mục tiêu
Cấu hình Spring Authorization Server, lưu client trong DB, hoàn thành luồng Authorization Code + PKCE (lấy `code`, đổi token).

## Chuẩn bị
- Day 03 xong. Đọc OAuth 2.0 Security BCP (RFC 9700) phần PKCE & redirect URI.
- Dependency: với Boot 4 / Security 7 dùng `spring-boot-starter-security-oauth2-authorization-server` (tên cũ `spring-boot-starter-oauth2-authorization-server` bị deprecated, vẫn publish); module lõi là `org.springframework.security:spring-security-oauth2-authorization-server` (version = Spring Security). Nếu ở lại Boot 3.5 thì dùng SAS 1.5.x đứng riêng. Đọc migration guide 6.x→7.x vì API có thay đổi.
- Từ Security 7 PKCE bật mặc định và password grant đã bị loại – khớp với chính sách của dự án.

## Steps

### Step 1 – Hai SecurityFilterChain
- [ ] Chain 1 (`@Order(1)`): endpoint của Authorization Server (`/oauth2/**`, `/.well-known/**`), `exceptionHandling` redirect về `/login`.
- [ ] Chain 2: login form + còn lại (từ Day 03).

### Step 2 – Client registry trong PostgreSQL
- [ ] Migration `oauth_client` (xem ER ở plan) hoặc dùng schema JDBC mặc định của SAS rồi bọc thêm `tenant_id`.
- [ ] Implement `RegisteredClientRepository` đọc DB, **lọc theo tenant**.
- [ ] Lưu `client_secret` dạng hash (`{bcrypt}`/`{argon2}`), không plaintext.
- [ ] Seed 2 client: `demo-spa` (public, PKCE bắt buộc), `demo-backend` (confidential).

### Step 3 – Ràng buộc bảo mật
- [ ] `requireProofKey(true)` cho public client; chỉ cho phép `S256`.
- [ ] Redirect URI so khớp chính xác (không wildcard, không `http` trừ `localhost`).
- [ ] Tắt Implicit & ROPC (SAS vốn không hỗ trợ – ghi nhận lại trong ADR).
- [ ] Authorization code TTL ≤ 60s, dùng 1 lần.

### Step 4 – Lưu authorization state
- [ ] `OAuth2AuthorizationService` dạng JDBC (bảng `oauth2_authorization`) – cân nhắc tách token ra bảng riêng để index/hash ở Day 05.
- [ ] `OAuth2AuthorizationConsentService` JDBC.

### Step 5 – Thử bằng tay
- [ ] Sinh `code_verifier`/`code_challenge` (S256) bằng script.
- [ ] Mở `/oauth2/authorize?response_type=code&client_id=demo-spa&redirect_uri=...&scope=openid&code_challenge=...&code_challenge_method=S256&state=xyz`.
- [ ] Đăng nhập → nhận `code` → `curl -X POST /oauth2/token` với `code_verifier` → có `access_token`.
- [ ] Viết integration test tự động hoá luồng này.

### Step 6 – Commit
- [ ] `feat(day04): authorization server with PKCE`.

## Definition of Done
- Đổi code không có/ sai `code_verifier` → `invalid_grant`.
- `redirect_uri` lạ → bị từ chối, **không** redirect.
- Test integration cho luồng hoàn chỉnh xanh.

## Lỗi hay gặp
- Quên `issuer` → token/ discovery sai URL. Set `AuthorizationServerSettings.builder().issuer(...)`.
- Cookie session của IDP và của client app trùng domain `localhost` → ghi đè nhau; dùng `127.0.0.1` vs `localhost` hoặc đặt tên cookie khác.
- Chain order sai → `/login` bị chain 1 chặn.

## Tham khảo
- Spring Authorization Server reference, RFC 7636 (PKCE), RFC 9700.
