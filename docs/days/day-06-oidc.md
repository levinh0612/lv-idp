# Day 06 – OIDC: discovery, UserInfo, consent, logout

## Mục tiêu
Trở thành OIDC Provider đầy đủ cho MVP: id_token chuẩn, discovery, userinfo, consent screen, SSO session và logout (RP-initiated + back-channel). Có SPA mẫu đăng nhập thành công.

## Chuẩn bị
- Day 05 xong. Đọc OIDC Core (id_token claims), RP-Initiated Logout 1.0, Back-Channel Logout 1.0.

## Steps

### Step 1 – ID Token & claims
- [ ] Bật OIDC (`.oidc(Customizer.withDefaults())`).
- [ ] `OAuth2TokenCustomizer` cho id_token: `sub` (ổn định, không dùng email), `email`, `email_verified`, `name`, `preferred_username`, `auth_time`, `amr`, `acr`, `nonce`.
- [ ] Scope → claim map: `profile`, `email`, `phone`, `offline_access` (cấp refresh token).
- [ ] Bảng/class `ScopeClaimRegistry` để mở rộng sau (ClaimMapper SPI).

### Step 2 – Discovery & UserInfo
- [ ] Kiểm tra `/.well-known/openid-configuration` có đủ: `issuer`, `authorization_endpoint`, `token_endpoint`, `jwks_uri`, `userinfo_endpoint`, `end_session_endpoint`, `code_challenge_methods_supported=[S256]`, `scopes_supported`.
- [ ] `/userinfo` trả claim theo scope của access token.

### Step 3 – Consent
- [ ] Trang consent tuỳ biến (Thymeleaf): liệt kê scope, tên client, nút Allow/Deny.
- [ ] Client first-party có thể `requireAuthorizationConsent=false`.
- [ ] Lưu consent, cho phép user thu hồi (sẽ dùng ở Admin/Account Day 18).

### Step 4 – SSO session & Logout
- [ ] Bảng `sso_session` (hoặc dựa Spring Session) + liên kết với authorization (để logout biết token nào cần hủy).
- [ ] `end_session_endpoint`: nhận `id_token_hint`, `post_logout_redirect_uri` (đã đăng ký), `state`. Xác nhận trang "Bạn muốn đăng xuất?" nếu không có hint.
- [ ] Back-channel logout: client đăng ký `backchannel_logout_uri` → IDP POST `logout_token` (JWT) khi session kết thúc. Thực hiện bất đồng bộ + retry.

### Step 5 – SPA mẫu
- [ ] Thư mục `samples/spa` (Vite + React) dùng `oidc-client-ts`: login, hiển thị claim, gọi API, logout.
- [ ] Chạy ở `http://localhost:5173`, đăng ký làm client `demo-spa`.
- [ ] Cấu hình CORS cho `/oauth2/token`, `/userinfo`, `/.well-known/**` theo allow-list client.

### Step 6 – Test & commit
- [ ] Test id_token: verify chữ ký bằng JWKS, kiểm tra `iss/aud/nonce`.
- [ ] `feat(day06): oidc discovery, userinfo, consent, logout`.

## Definition of Done
- SPA: bấm Login → IDP → quay lại SPA với user info; Logout đăng xuất cả IDP.
- Có thể dùng thư viện OIDC bất kỳ chỉ với `issuer` (discovery).

## Lỗi hay gặp
- `sub` = email → đổi email là mất danh tính; dùng UUID.
- Thiếu CORS cho token endpoint → SPA lỗi mơ hồ.
- Logout chỉ xóa session IDP mà không hủy refresh token → "zombie session".

## Tham khảo
- OIDC Core 1.0, RP-Initiated Logout 1.0, Back-Channel Logout 1.0, `oidc-client-ts`.
