# Day 07 – Client credentials, Device flow, Token exchange, Multi-tenant issuer

## Mục tiêu
Hoàn thiện các grant còn lại và tách tenant bằng issuer riêng (`/t/{slug}`), mỗi tenant có key & client riêng.

## Chuẩn bị
- Day 06 xong. Đọc RFC 8628 (Device), RFC 8693 (Token Exchange).

## Steps

### Step 1 – Multi-tenant issuer
- [ ] Resolve tenant từ path: `https://idp.example.com/t/{slug}/...` (hoặc subdomain). Filter đặt tenant vào `TenantContext` trước mọi chain.
- [ ] `AuthorizationServerSettings` động: issuer = `{base}/t/{slug}`; discovery & JWKS theo tenant.
- [ ] Mọi repository (client, key, user, authorization) lọc theo tenant. Thêm ArchUnit test: không repository nào thiếu tenant.
- [ ] Test: token của tenant A không verify được bằng JWKS tenant B; client tenant A không dùng được ở tenant B.

### Step 2 – Client Credentials (M2M)
- [ ] Cho phép `client_credentials` cho confidential client; client auth: `client_secret_basic` & `private_key_jwt` (tùy chọn).
- [ ] Token không có `sub` user; `sub = client_id`; scope giới hạn theo cấu hình client.
- [ ] Test với `demo-backend`.

### Step 3 – Device Authorization (RFC 8628)
- [ ] Endpoint `/oauth2/device_authorization`; trang `/activate` nhập `user_code`.
- [ ] `user_code` ngắn, dễ đọc (8 ký tự, bỏ ký tự dễ nhầm), TTL ~10 phút, rate-limit nhập.
- [ ] Polling `interval`, trả `authorization_pending`, `slow_down`.
- [ ] Demo CLI nhỏ (bash/Java) đăng nhập bằng device flow.

### Step 4 – Token Exchange (RFC 8693) + DPoP
- [ ] Spring Security 7 đã hỗ trợ sẵn token exchange và DPoP (RFC 9449) trong module authorization server → bật và cấu hình, không tự viết lại; kiểm tra docs "What's New" của bản bạn dùng.
- [ ] DPoP: cho phép bật theo client (`dpop_bound_access_tokens`); resource server phải validate DPoP proof (thử với sample ở Day 17).
- [ ] Grant `urn:ietf:params:oauth:grant-type:token-exchange`.
- [ ] Use case: service A đổi access token của user lấy token cho service B (`audience` khác, scope thu hẹp) – "on-behalf-of".
- [ ] Policy: chỉ client được phép + kiểm tra `audience` hợp lệ; ghi audit.

### Step 5 – Commit
- [ ] `feat(day07): grants and multi-tenant issuer`.

## Definition of Done
- 2 tenant `acme`, `globex` có issuer/JWKS/clients độc lập.
- Device flow chạy từ CLI; client credentials cấp được token; token exchange thu hẹp scope.

## Lỗi hay gặp
- `iss` không khớp giữa discovery và token → client OIDC từ chối. Dùng cùng một nguồn issuer.
- Token exchange không giới hạn → leo thang đặc quyền. Chỉ cho phép thu hẹp scope/audience.
- Cache JWKS toàn cục không theo tenant → trả nhầm key.

## Tham khảo
- RFC 8628, RFC 8693, Spring Authorization Server – multi-tenancy guide.
