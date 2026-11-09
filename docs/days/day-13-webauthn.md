# Day 13 – WebAuthn / Passkeys

## Mục tiêu
Cho phép đăng ký passkey/security key và đăng nhập không mật khẩu (usernameless + MFA factor).

## Chuẩn bị
- Day 12 xong. Thêm `com.webauthn4j:webauthn4j-core` (kiểm tra bản mới nhất). **Không tự viết** phần verify attestation/assertion.
- Đọc W3C WebAuthn **Level 3** (đã là Recommendation từ 25/08/2026; Level 2 từ 04/2021). Khái niệm: RP ID, challenge, resident key, user verification. Level 3 có *Related Origin Requests* (một passkey dùng cho vài domain liên quan) – liên quan trực tiếp đến quyết định `rpId` bên dưới.
- Tham khảo cách Keycloak 26.4 làm passkeys (conditional UI + modal UI) để so sánh UX.
- Spring Security 7 cũng có WebAuthn/passkey và API MFA mới; cân nhắc so với `webauthn4j` trước khi chọn.

## Steps

### Step 1 – Cấu hình RP
- [ ] `rpId` = domain (vd. `idp.example.com`; dev: `localhost`), `rpName`, `origins` cho phép.
- [ ] Lưu ý multi-tenant: dùng chung `rpId` toàn IDP hoặc theo domain tenant (custom domain) – chốt và ghi ADR (đổi `rpId` sau này sẽ làm mất toàn bộ passkey).

### Step 2 – Lưu credential
- [ ] `credential(type=WEBAUTHN)`: `credential_id`, `public_key (COSE)`, `sign_count`, `aaguid`, `transports`, `backup_eligible`, `backup_state`, `name`, `last_used_at`.
- [ ] Index unique trên `credential_id`.

### Step 3 – Registration ceremony
- [ ] `POST /webauthn/register/options`: sinh `challenge` (≥16 byte ngẫu nhiên, lưu Redis TTL 5 phút), `user.id` (handle ngẫu nhiên, không phải email), `excludeCredentials`, `authenticatorSelection` (`residentKey=preferred`, `userVerification=preferred`).
- [ ] `POST /webauthn/register/verify`: dùng webauthn4j kiểm `challenge`, `origin`, `rpIdHash`, flags UP/UV, lưu credential.
- [ ] Frontend: `navigator.credentials.create()` (helper chuyển base64url ↔ ArrayBuffer).

### Step 4 – Authentication ceremony
- [ ] Options: `allowCredentials` (nếu đã biết user) hoặc rỗng (usernameless/discoverable).
- [ ] Verify assertion: chữ ký, `challenge`, `origin`, tăng `sign_count` (nếu > 0 mà không tăng → nghi clone, ghi cảnh báo).
- [ ] Conditional UI (`mediation: "conditional"`) để trình duyệt gợi ý passkey ở ô username.
- [ ] Làm thành `PasskeyStage` trong flow engine: coi như đã thoả cả "mật khẩu" lẫn MFA khi `UV=true` (`amr=["hwk","user"]`).

### Step 5 – Quản lý
- [ ] Trang `/account/security`: liệt kê passkey, đổi tên, xoá (giữ ít nhất 1 phương thức đăng nhập).
- [ ] Gợi ý tạo passkey sau khi đăng nhập thành công bằng password.

### Step 6 – Test & commit
- [ ] Test với authenticator ảo: Chrome DevTools → WebAuthn virtual authenticator, hoặc test dữ liệu cố định của webauthn4j.
- [ ] `feat(day13): webauthn passkeys`.

## Definition of Done
- Đăng ký passkey, đăng xuất, đăng nhập lại chỉ bằng passkey (không nhập mật khẩu).
- Challenge dùng lại / origin sai bị từ chối.

## Lỗi hay gặp
- `rpId` không khớp domain thực → lỗi `SecurityError`; dev phải dùng `localhost` hoặc HTTPS.
- Dùng email làm `user.id` → lộ thông tin & không ổn định; dùng handle ngẫu nhiên.
- Không xử lý `transports`/ `backup flags` → UX tệ khi đồng bộ passkey.

## Tham khảo
- webauthn4j docs, passkeys.dev, W3C WebAuthn spec.
