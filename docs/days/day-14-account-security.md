# Day 14 – Reset password, verify email, brute-force, risk, step-up

## Mục tiêu
Hoàn thiện vòng đời tài khoản và lớp phòng thủ tấn công: verify email, quên mật khẩu, rate-limit, lockout, breached-password check, step-up.

## Chuẩn bị
- Day 13 xong. Mailpit đang chạy (Day 01) để xem email dev.

## Steps

### Step 1 – Email service
- [ ] `NotificationService` + template email (Thymeleaf) đa ngôn ngữ (vi/en); gửi bất đồng bộ qua queue (Spring `@Async` hoặc outbox table).
- [ ] SMTP cấu hình theo tenant (override mặc định).

### Step 2 – Verify email
- [ ] Token ngẫu nhiên 32 byte, lưu **hash**, TTL 24h, dùng một lần; link `/verify-email?token=...`.
- [ ] Đổi email → gửi xác nhận vào email mới trước khi áp dụng.
- [ ] Chưa verify → hạn chế (tuỳ cấu hình: không cho link social, không nhận một số scope).

### Step 3 – Quên / đổi mật khẩu
- [ ] `POST /forgot-password`: luôn trả thông báo chung (không lộ email tồn tại); rate limit theo IP + email.
- [ ] Token reset TTL 15–30 phút, một lần, hash trong DB; sau khi đổi: **huỷ mọi session & refresh token** của user, gửi email thông báo.
- [ ] Đổi mật khẩu trong tài khoản: yêu cầu mật khẩu cũ; kiểm tra lịch sử N mật khẩu gần nhất (tuỳ chọn).

### Step 4 – Chống brute-force / credential stuffing
- [ ] Rate limit (Bucket4j + Redis): theo IP, theo username, theo (IP, username).
- [ ] Lockout mềm: sau N lần sai → trì hoãn tăng dần (exponential backoff) hoặc yêu cầu CAPTCHA; mở lại tự động; thông báo user qua email.
- [ ] Breached-password: gọi HIBP Pwned Passwords (k-anonymity: gửi 5 ký tự đầu SHA-1) khi đặt mật khẩu.
- [ ] Phát hiện bất thường đơn giản: IP/quốc gia/thiết bị mới → cộng điểm rủi ro.

### Step 5 – Risk-based & step-up
- [ ] `RiskEvaluator` → `LOW|MEDIUM|HIGH` từ tín hiệu (IP mới, nhiều lần sai gần đây, user-agent lạ).
- [ ] Flow Day 12: `MEDIUM+` ⇒ bắt buộc MFA kể cả khi user chưa bật; không có factor ⇒ gửi OTP email.
- [ ] Step-up: client gửi `acr_values`/`max_age` ⇒ IDP buộc xác thực lại (MFA) trước khi cấp token; token thao tác nhạy cảm ghi `acr` cao.

### Step 6 – Thông báo & test
- [ ] Email "đăng nhập từ thiết bị mới", "mật khẩu đã đổi".
- [ ] Test: vượt rate limit → 429 + `Retry-After`; token reset dùng 2 lần → lần 2 fail; reset xong refresh token cũ không dùng được.
- [ ] `feat(day14): account lifecycle, brute-force protection, step-up`.

## Definition of Done
- 10 lần sai liên tiếp → bị chặn tạm; user nhận email.
- Reset password xong, mọi phiên cũ bị đăng xuất.

## Lỗi hay gặp
- Lockout cứng theo username → kẻ tấn công khoá tài khoản nạn nhân (DoS). Dùng backoff/CAPTCHA, không khoá cứng.
- Token reset lưu plaintext hoặc không hết hạn.
- Thông báo lỗi khác nhau giữa "sai user" và "sai mật khẩu".

## Tham khảo
- OWASP Authentication & Forgot Password Cheat Sheets, HIBP Pwned Passwords API, Bucket4j.
