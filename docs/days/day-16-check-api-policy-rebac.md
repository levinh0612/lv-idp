# Day 16 – Check API, ABAC policy, ReBAC

## Mục tiêu
Dịch vụ quyết định quyền `POST /authz/check`, thêm policy theo thuộc tính (ABAC) và quan hệ (ReBAC kiểu Zanzibar). Tách Authorization khỏi Authentication.

## Chuẩn bị
- Day 15 xong. Đọc `plan.html` mục 9 và bài báo Google Zanzibar (tóm tắt), tài liệu OpenFGA.

## Steps

### Step 1 – Check API
- [ ] `POST /authz/check` body: `{ "subject": "user:uuid", "action": "invoice:approve", "resource": "invoice:123", "context": {"ip":"...", "amount": 5000} }` → `{ "allowed": true, "reason": "..." }`.
- [ ] Batch: `POST /authz/batch-check`.
- [ ] Xác thực bằng client credentials (service gọi service) với scope `authz:check`.
- [ ] Cache kết quả ngắn (vd. 5–30s) key theo (subject, action, resource, hash(context)); metric hit/miss.

### Step 2 – Evaluator pipeline
- [ ] `PolicyEvaluator` chạy theo thứ tự: **explicit deny** → RBAC → ABAC → ReBAC; mặc định deny.
- [ ] Giao diện: `Decision evaluate(AuthzRequest)`; `Decision` có `allowed`, `reason`, `matchedPolicies` (hỗ trợ debug/audit).

### Step 3 – ABAC
- [ ] Policy lưu DB: `policy(id, tenant_id, effect[ALLOW|DENY], actions[], resources pattern, condition, priority)`.
- [ ] `condition` viết bằng biểu thức an toàn: **CEL** (cel-java) hoặc SpEL hạn chế (không cho gọi method tuỳ ý). Ví dụ: `subject.attributes.department == resource.owner_department && context.amount < 10000`.
- [ ] Sandbox: timeout, không truy cập I/O, kiểm tra cú pháp khi lưu.
- [ ] Test các policy theo giờ, IP range, giá trị.

### Step 4 – ReBAC
Chọn một trong hai (ghi ADR):
- **A. Tự làm tuple store (học khái niệm):**
  - [ ] Bảng `authz_tuple(tenant_id, object_type, object_id, relation, subject_type, subject_id, subject_relation)` index theo (object) và (subject).
  - [ ] Model: `type document { relations: owner, editor: owner or ..., viewer: editor or viewer from parent }` lưu JSON; resolver đệ quy có giới hạn độ sâu + chống vòng.
  - [ ] API: `write tuples`, `check`, `list-objects`.
- **B. Tích hợp OpenFGA (khuyến nghị cho production; CNCF Incubating từ 10/2025, thay thế khả dĩ: SpiceDB):**
  - [ ] Thêm container OpenFGA vào Compose, client Java; adapter `OpenFgaPolicyEvaluator`.
  - [ ] Map tenant → store; đồng bộ role → tuple khi cần.

### Step 5 – Đồng bộ với RBAC
- [ ] Role gán ở Day 15 phản chiếu thành tuple `tenant:acme#admin@user:...` để ReBAC dùng chung nguồn sự thật.

### Step 6 – Test & commit
- [ ] Bộ test bảng (table-driven) ≥ 20 case cho allow/deny/explicit deny/kế thừa folder.
- [ ] Load test nhẹ Check API (k6): mục tiêu p95 < 20ms có cache.
- [ ] `feat(day16): authz check api, abac, rebac`.

## Definition of Done
- `check` trả đúng cho: role, điều kiện thuộc tính, quan hệ kế thừa (owner của folder → xem file trong folder).
- Policy có điều kiện sai cú pháp bị từ chối khi lưu.

## Lỗi hay gặp
- Mặc định allow khi lỗi → luôn **fail closed**.
- Đệ quy quan hệ không giới hạn → tốn tài nguyên/vòng lặp.
- Cache không tính `context` → trả sai kết quả.

## Tham khảo
- Zanzibar paper, OpenFGA docs, CEL spec, OPA/Rego (so sánh).
