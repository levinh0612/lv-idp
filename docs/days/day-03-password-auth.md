# Day 03 – User, password (Argon2id), session

## Mục tiêu
Đăng ký + đăng nhập bằng form (server-side session), mật khẩu băm Argon2id, chưa có OAuth2.

## Chuẩn bị
- Day 02 xong. Thêm dependency `spring-boot-starter-security`, `spring-boot-starter-thymeleaf`, `spring-session-data-redis`, `bcprov` (cho Argon2) hoặc dùng `Argon2PasswordEncoder` của Spring Security.

## Steps

### Step 1 – Password hashing
- [ ] Bean `PasswordEncoder` = `Argon2PasswordEncoder` (tham số khởi điểm: memory 19–64 MiB, iterations 2–3, parallelism 1 – tune theo máy; mục tiêu ≈ 100–300ms/hash).
- [ ] Dùng `DelegatingPasswordEncoder` để về sau hỗ trợ hash khác (`{bcrypt}`, `{argon2}`) – cần cho lazy migration Day 11.
- [ ] Policy: tối thiểu 10 ký tự, chặn danh sách mật khẩu phổ biến (file top 10k) – HIBP để Day 14.

### Step 2 – Use case
- [ ] `idp-core`: interface `UserService.register(tenantId, cmd)`, `AuthenticationService`.
- [ ] Đăng ký: validate → check trùng → lưu user (`status=PENDING_VERIFICATION` hoặc `ACTIVE`) + credential PASSWORD.
- [ ] Chống user enumeration: thông báo chung ở login; ở register có thể báo "email đã dùng" nhưng gắn rate limit.

### Step 3 – Web & Security config
- [ ] `SecurityFilterChain`: `/login`, `/register`, `/css/**` public; còn lại authenticated.
- [ ] `UserDetailsService` tuỳ biến đọc user theo `tenant` hiện tại (resolve tenant từ host/path – tạm dùng tenant `default`).
- [ ] Template Thymeleaf: `login.html`, `register.html` (có CSRF token).
- [ ] Session lưu Redis (Spring Session), cookie `HttpOnly; SameSite=Lax; Secure` (Secure bật ở prod).
- [ ] Đổi session id sau login (session fixation – mặc định Spring đã bật, kiểm tra lại).

### Step 4 – Test
- [ ] Unit: encoder verify đúng/sai; policy từ chối pwd yếu.
- [ ] Integration (MockMvc + Testcontainers): register → login → truy cập `/me` → logout.
- [ ] Test timing: login user không tồn tại vẫn chạy 1 lần hash giả (giảm timing attack).

### Step 5 – Commit
- [ ] `feat(day03): local registration and login`.

## Definition of Done
- Trình duyệt: đăng ký → đăng nhập → thấy trang `/me` hiển thị email → logout.
- DB không chứa plaintext; hash dạng `{argon2}...`.

## Lỗi hay gặp
- Tham số Argon2 quá nặng → app chậm, DoS qua login; đo và giới hạn.
- Quên CSRF cho form → 403 hoặc, tệ hơn, tắt CSRF luôn. Giữ bật.
- Tenant resolve sai → đọc nhầm user; giữ `TenantContext` rõ ràng, clear sau request.

## Tham khảo
- OWASP Password Storage Cheat Sheet, Spring Security `Argon2PasswordEncoder`.
