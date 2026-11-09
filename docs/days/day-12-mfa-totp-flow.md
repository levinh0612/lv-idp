# Day 12 – Authentication flow engine, TOTP, recovery codes

## Mục tiêu
Chuyển đăng nhập sang pipeline các stage (kiểu Authentik), thêm MFA TOTP + recovery codes, ghi `amr/acr` vào token.

## Chuẩn bị
- Day 11 xong. Đọc RFC 6238 (TOTP). Đọc `plan.html` mục 6.4.

## Steps

### Step 1 – Flow engine tối thiểu
- [ ] `Authenticator` SPI: `id()`, `authenticate(FlowContext) → CONTINUE | CHALLENGE(view) | FAIL`.
- [ ] `FlowDefinition` theo tenant/client: danh sách stage có điều kiện, ví dụ `[identification, password, mfa(if: user.hasMfa || risk>=MEDIUM), consent]`.
- [ ] `FlowContext` lưu tiến độ (stage hiện tại, user tạm, các factor đã đạt) trong Redis, TTL ngắn, gắn với session pre-auth.
- [ ] Refactor login Day 03 + broker Day 08–10 thành các stage: `IdentificationStage`, `PasswordStage`, `FederatedStage`, `MfaStage`.
- [ ] Sau khi hoàn tất flow mới tạo `Authentication` đầy đủ; trước đó chỉ có "pre-authenticated".

### Step 2 – TOTP
- [ ] Sinh secret 160-bit ngẫu nhiên (SecureRandom), base32; lưu **mã hóa** (AES-GCM) trong `credential(type=TOTP)`.
- [ ] Enrollment: trang hiển thị QR (`otpauth://totp/Issuer:email?secret=...&issuer=...`) + nhập code xác nhận trước khi bật.
- [ ] Verify: SHA-1/6 digit/30s (tương thích Google Authenticator), cho phép lệch ±1 bước.
- [ ] Chống replay: lưu bước thời gian đã dùng cuối cùng (không cho dùng lại code trong cùng window).
- [ ] Rate-limit nhập code (5 lần/5 phút).

### Step 3 – Recovery codes
- [ ] Sinh 10 mã (vd. 10 ký tự, nhóm 5-5), hiển thị **một lần**, lưu hash.
- [ ] Dùng xong đánh dấu `used`; cho phép tạo lại bộ mới (vô hiệu bộ cũ).

### Step 4 – amr/acr
- [ ] Ghi `amr` (`pwd`, `otp`, `fed`, `hwk`...) vào session → id_token.
- [ ] `acr` (vd. `urn:lv:acr:mfa`) khi đạt MFA; client có thể yêu cầu qua `acr_values`.

### Step 5 – Account UI
- [ ] Trang `/account/security`: bật/tắt TOTP (cần xác nhận lại mật khẩu), xem recovery codes, danh sách factor.

### Step 6 – Test & commit
- [ ] Unit test TOTP với vector RFC 6238.
- [ ] Integration: login password → bị chặn ở MFA → nhập TOTP đúng → vào được; recovery code dùng một lần.
- [ ] `feat(day12): flow engine, totp, recovery codes`.

## Definition of Done
- User bật MFA: login yêu cầu thêm TOTP; token có `amr=["pwd","otp"]`.
- Dùng lại cùng code TOTP trong cùng window bị từ chối.

## Lỗi hay gặp
- Server lệch giờ → TOTP fail; bật NTP.
- Lưu secret TOTP plaintext → lộ DB = mất MFA.
- Cho tắt MFA mà không xác thực lại → kẻ chiếm session tắt được MFA.

## Tham khảo
- RFC 6238, RFC 4226, OWASP MFA Cheat Sheet.
