# Day 18 – Admin API + Admin UI

## Mục tiêu
Quản trị tập trung: tenant, client, user, connection (social/OIDC/SAML), storage, role/policy, session – qua REST API có phân quyền và UI React.

## Chuẩn bị
- Day 17 xong. Skill `frontend-design` có thể dùng để dựng giao diện admin chuyên nghiệp.

## Steps

### Step 1 – Admin API (`idp-admin-api`)
- [ ] OpenAPI (springdoc) cho toàn bộ endpoint; version `/admin/v1`.
- [ ] Resource: `tenants`, `clients` (+ rotate secret), `users` (search, disable, reset credential, force logout), `connections`, `storages`, `roles/permissions/policies`, `sessions`, `keys`, `audit`.
- [ ] Phân quyền chi tiết: `idp:tenant:admin`, `idp:user:manage`, `idp:client:manage`, `idp:audit:read`… (dùng chính Check API).
- [ ] Secret chỉ hiển thị **một lần** khi tạo/rotate; GET trả `****`.
- [ ] Validate cấu hình connection/storage trước khi lưu (nút "Test connection": thử discovery, thử bind LDAP, thử SELECT 1).
- [ ] Phân trang, lọc, sắp xếp chuẩn; idempotency key cho POST quan trọng.

### Step 2 – Admin UI (`idp-admin-ui`)
- [ ] React + Vite + Tailwind; đăng nhập bằng chính IDP (OIDC, client `admin-ui`, PKCE; hoặc BFF).
- [ ] Màn hình tối thiểu: Dashboard (số user, login/ngày, lỗi), Users, Clients, Connections (form theo từng loại), Roles, Sessions, Audit log.
- [ ] Form cấu hình connection sinh từ JSON schema của từng connector (`connector.configSchema()`).
- [ ] Thao tác nguy hiểm cần xác nhận + nhập lại tên đối tượng.
- [ ] Accessibility cơ bản, responsive, dark mode.

### Step 3 – Account Console cho người dùng
- [ ] `/account`: hồ sơ, đổi mật khẩu, MFA/passkey, liên kết tài khoản, phiên đang đăng nhập (đăng xuất từ xa), ứng dụng đã cấp quyền (revoke consent).

### Step 4 – Bảo mật admin
- [ ] Admin bắt buộc MFA (flow riêng cho client `admin-ui`, `acr` cao).
- [ ] CSP chặt, không nhúng iframe; rate-limit API admin.
- [ ] Audit mọi thay đổi (ai, lúc nào, trước/sau, IP).

### Step 5 – Test & commit
- [ ] Test API: phân quyền (user thường 403), cách ly tenant, ẩn secret.
- [ ] E2E (Playwright): tạo client từ UI → dùng client đó login SPA mẫu.
- [ ] `feat(day18): admin api, admin ui, account console`.

## Definition of Done
- Thêm Google/Keycloak connection và tạo client **hoàn toàn từ UI**, không sửa DB tay.
- User thường không truy cập được Admin API.

## Lỗi hay gặp
- Admin UI gọi API bằng token có quyền quá rộng → dùng scope riêng `admin`.
- Trả `client_secret` trong response list → rò rỉ.
- Quên kiểm tra tenant ở endpoint admin (IDOR) → luôn so `tenantId` trong path với quyền của admin.

## Tham khảo
- Keycloak Admin REST API, springdoc-openapi, Playwright.
