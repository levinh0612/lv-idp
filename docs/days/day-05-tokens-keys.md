# Day 05 – Token, JWKS, key rotation, refresh rotation, revoke/introspect

## Mục tiêu
Quản lý khóa ký trong DB (mã hóa), JWKS nhiều key, xoay khóa không làm hỏng token cũ; refresh token rotation có phát hiện tái sử dụng; revoke & introspect.

## Chuẩn bị
- Day 04 xong. Đọc RFC 9068 (JWT access token profile), RFC 7009, RFC 7662.

## Steps

### Step 1 – Signing key store
- [ ] Bảng `signing_key(id, tenant_id, kid, alg, status[ACTIVE|PASSIVE|RETIRED], public_jwk, private_key_encrypted, created_at, not_after)`.
- [ ] Mã hóa private key at-rest: AES-GCM với master key từ env/KMS (envelope encryption). Class `KeyCipher`.
- [ ] `KeyService.generate(tenant, alg=ES256|RS256)`; tạo key đầu tiên khi tenant mới.
- [ ] Bean `JWKSource<SecurityContext>` đọc key ACTIVE + PASSIVE theo tenant; JWKS public chỉ trả public key.

### Step 2 – Rotation
- [ ] Job `@Scheduled` (ShedLock nếu chạy nhiều pod): tạo key mới → ACTIVE, key cũ → PASSIVE (vẫn publish trong JWKS), RETIRED sau `max(token_ttl)+buffer`.
- [ ] Endpoint admin tạm thời `POST /admin/keys/rotate` để test.
- [ ] Test: token ký bằng key cũ vẫn verify được sau rotate; token mới có `kid` mới.

### Step 3 – Access token (JWT)
- [ ] Customizer `OAuth2TokenCustomizer<JwtEncodingContext>`: thêm `tenant`, `scope`, `client_id`, `jti`, header `typ=at+jwt` (RFC 9068).
- [ ] TTL access 10 phút (cấu hình theo client).

### Step 4 – Refresh token rotation + reuse detection
- [ ] Refresh token opaque, lưu **hash** (SHA-256) trong DB cùng `family_id`.
- [ ] Mỗi lần refresh: đánh dấu token cũ `used_at`, cấp token mới cùng family.
- [ ] Nếu nhận token đã `used` → revoke cả family + SSO session, log sự kiện `TOKEN_REUSE_DETECTED`.
- [ ] Absolute lifetime (vd. 30 ngày) + idle timeout (vd. 7 ngày).

### Step 5 – Revoke & Introspect
- [ ] `/oauth2/revoke` (RFC 7009) cho access & refresh.
- [ ] `/oauth2/introspect` (RFC 7662) – yêu cầu client authentication; trả `active`, `scope`, `exp`, `sub`...
- [ ] Denylist `jti` trong Redis cho access token bị revoke (TTL = thời gian còn lại).

### Step 6 – Test & commit
- [ ] Test reuse detection (dùng refresh cũ lần 2 → cả family bị hủy).
- [ ] `feat(day05): key management, refresh rotation, revoke/introspect`.

## Definition of Done
- Rotate khóa → JWKS có 2 key; token cũ vẫn hợp lệ, token mới dùng `kid` mới.
- Reuse refresh token → `invalid_grant` và family bị revoke.

## Lỗi hay gặp
- Xóa key cũ quá sớm → mọi token cũ fail. Chỉ RETIRED sau khi hết TTL.
- Lưu refresh token plaintext → lộ DB = lộ phiên. Chỉ lưu hash.
- Nhiều pod rotate cùng lúc → dùng lock (ShedLock/advisory lock Postgres).

## Tham khảo
- RFC 9068, 7009, 7662; Nimbus JOSE+JWT docs.
