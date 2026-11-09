# Day 15 – RBAC, permission, tenant scoping

## Mục tiêu
Mô hình hóa Role/Permission/Group theo tenant và theo client, đưa `roles`/`permissions` vào token, quản lý qua API.

## Chuẩn bị
- Day 14 xong. Đọc `plan.html` mục 9 (tầng 1).
- Tham khảo skill `postgresql-table-design` cho index các bảng nối.

## Steps

### Step 1 – Mô hình
- [ ] Bảng (đã tạo Day 02, bổ sung): `role(id, tenant_id, client_id nullable, name, description, composite)`, `permission(id, tenant_id, code, description)`, `role_permission`, `user_role`, `group`, `group_role`, `user_group`.
- [ ] `role.client_id` null = realm role (toàn tenant); có giá trị = client role (chỉ app đó).
- [ ] Composite role (role chứa role) – giới hạn độ sâu (≤3) và chống vòng lặp khi lưu.
- [ ] Quy ước `permission.code`: `resource:action` (vd. `invoice:read`, `invoice:approve`).

### Step 2 – Service
- [ ] `AccessService.effectivePermissions(userId, clientId)`: hợp (user_role + group_role + composite) → set permission; cache Redis (key theo user+client, invalidate khi đổi role).
- [ ] Event `RoleAssignmentChanged` → xoá cache + (tuỳ chọn) thu hồi token.

### Step 3 – Đưa vào token
- [ ] `OAuth2TokenCustomizer`: claim `roles` (tên role áp dụng cho client đó), `scope` giữ nguyên; permission **không** nhét hết vào JWT nếu > ~50 mục (dùng Check API Day 16).
- [ ] Cấu hình mỗi client: `include_roles`, `include_permissions` (`NONE|ROLES|PERMISSIONS`).
- [ ] Namespaced claim cho dễ đọc: `{"roles":["editor"],"tenant":"acme"}`.

### Step 4 – API quản lý (tạm, UI ở Day 18)
- [ ] `POST/GET/PUT/DELETE /admin/t/{tenant}/roles`, `/permissions`, `/users/{id}/roles`, `/groups`.
- [ ] Bảo vệ bằng chính IDP: cần role `idp-admin` của tenant; super-admin cho toàn hệ thống.
- [ ] Mọi thay đổi ghi audit (actor, target, before/after).

### Step 5 – Mapping từ federation
- [ ] Nối với `ClaimMapper` Day 09: claim upstream → role nội bộ (default role cho user mới, mapping theo group LDAP/Keycloak).

### Step 6 – Test & commit
- [ ] Test composite role, vòng lặp bị chặn, cache invalidate đúng.
- [ ] Test cách ly tenant: admin tenant A không gán role cho user tenant B.
- [ ] `feat(day15): rbac with composite roles and token claims`.

## Definition of Done
- Gán role `editor` cho user → token mới có `roles:["editor"]`; gỡ role → token sau không còn.
- Không thể gán role/permission chéo tenant.

## Lỗi hay gặp
- Role explosion → dùng group + composite, đặt tên theo chức năng.
- Cache không invalidate → quyền "ma" tồn tại.
- Cho client tự khai role trong request → luôn tính ở server.

## Tham khảo
- Keycloak roles & groups, NIST RBAC model.
