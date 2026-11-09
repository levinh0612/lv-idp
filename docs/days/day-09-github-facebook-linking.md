# Day 09 – GitHub, Facebook, account linking, claim mapping

## Mục tiêu
Thêm 2 provider OAuth2 thuần (không có id_token), xử lý account linking an toàn và cơ chế claim/role mapping cấu hình được.

## Chuẩn bị
- Day 08 xong. Tạo OAuth App ở GitHub (Developer settings) và App ở Meta for Developers.

## Steps

### Step 1 – Abstract OAuth2 (non-OIDC) connector
- [ ] `AbstractOAuth2Connector`: authorize URL, token exchange, gọi `userinfo` endpoint để lấy profile.
- [ ] GitHub: scope `read:user user:email`; gọi `/user` và `/user/emails` (lấy email `primary && verified`).
- [ ] Facebook: scope `email public_profile`; gọi Graph API `/me?fields=id,name,email`. Lưu ý Facebook **không bảo đảm** email đã verified → coi `emailVerified=false`.
- [ ] Mỗi connector khai báo `emailTrustLevel`: `VERIFIED` (Google, GitHub primary verified) / `UNVERIFIED` (Facebook).

### Step 2 – Chính sách account linking
Quy tắc (viết thành `LinkingPolicy` + unit test đầy đủ):
- [ ] Đã có `federated_identity` → login thẳng.
- [ ] Chưa có, email upstream **verified** và trùng user local **verified** → tự động link (tuỳ cấu hình tenant: `AUTO | CONFIRM | NEVER`).
- [ ] Email trùng nhưng một bên chưa verified → **không** auto-link; yêu cầu user đăng nhập tài khoản cũ để xác nhận (chống account takeover).
- [ ] Không trùng → JIT tạo user mới.
- [ ] Trang "Liên kết tài khoản": xác nhận bằng password/MFA của tài khoản hiện có.
- [ ] Màn hình quản lý: user xem/gỡ liên kết (không cho gỡ cái cuối nếu chưa có credential khác).

### Step 3 – Claim / Role mapping
- [ ] `ClaimMapper` SPI: nhận `ExternalIdentity` + cấu hình mapping → thuộc tính user nội bộ.
- [ ] Mapping dạng JSON theo connection, ví dụ: `{"attributes.avatar":"picture","attributes.company":"company"}`.
- [ ] Mapping role: nếu upstream claim `groups` chứa `admins` → gán role `admin` (mở rộng Day 15).
- [ ] Chế độ đồng bộ: `IMPORT_ONCE | FORCE_EVERY_LOGIN`.

### Step 4 – Test & commit
- [ ] WireMock cho GitHub/Facebook.
- [ ] Test ma trận linking: (verified/unverified) × (trùng/không trùng) × (policy).
- [ ] `feat(day09): github, facebook, linking policy, claim mapping`.

## Definition of Done
- 1 user đăng nhập được bằng password, Google và GitHub (cùng 1 `app_user`).
- Email Facebook chưa verified không bao giờ tự link vào tài khoản có sẵn.

## Lỗi hay gặp
- GitHub trả `email=null` nếu user ẩn email → phải gọi `/user/emails`.
- Auto-link theo email không kiểm tra verified → lỗ hổng chiếm tài khoản kinh điển.
- Race condition: 2 callback đồng thời tạo 2 user → unique constraint + xử lý retry.

## Tham khảo
- GitHub OAuth docs, Facebook Login docs, bài viết "pre-hijacking attacks" về account linking.
