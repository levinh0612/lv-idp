# lv-idp: Ôn phỏng vấn AuthN/AuthZ qua 3 bài toán

Mục tiêu: tự tay làm 3 bài (Claims, Banking, Multi-tenant) để trả lời chắc 3 câu hỏi phỏng vấn:

1. Quyền này dựa trên **role**, **sở hữu** hay **điều kiện** (số tiền, tenant, mức xác thực)?
2. Kiểm tra ở **tầng nào**, và vì sao không để frontend quyết định?
3. Nếu quyền thay đổi giữa chừng, **token cũ còn hiệu lực bao lâu**?

> Quy tắc: tự code, tự viết test. File này chỉ là khung và checklist.

**Stack gợi ý:** Spring Boot 3 + Spring Security (Resource Server) + Keycloak (Docker) + PostgreSQL + Testcontainers.

## Cấu trúc đề xuất

```
lv-idp/
├── infra/             # docker-compose: keycloak, postgres, realm export JSON
├── common-security/   # JwtAuthConverter, CurrentUser helper
├── claims-service/
├── banking-service/
├── docs-service/      # multi-tenant
└── notes/             # ADR + câu trả lời phỏng vấn mỗi bài
```

## 3 loại quyền (ghi cho từng endpoint)

| Loại | Trả lời câu hỏi | Ví dụ |
|---|---|---|
| Role | Bạn là ai? | `ADJUSTER` |
| Ownership | Resource này của ai? | `claim.customerId == sub` |
| Condition | Trạng thái/ngữ cảnh có cho phép không? | `amount <= 50tr`, `acr` đủ mạnh, tenant khớp |

## Thứ tự làm

0. Nền chung → 1. Claims → 2. Multi-tenant → 3. Banking (nhiều khái niệm nhất nên làm cuối).

Vòng lặp mỗi bài: viết test (đỏ) → làm xanh → cố ý bỏ check xem test có đỏ lại không → viết 5 dòng "tôi sẽ nói gì khi bị hỏi".

---

## 0. Nền chung

- [ ] Keycloak chạy bằng docker-compose, realm import từ file JSON (không cấu hình tay trên UI)
- [ ] Lấy được token (password hoặc auth-code flow)
- [ ] Tự decode JWT, chỉ ra: `sub`, `realm_access.roles`, `acr`, `exp`
- [ ] Spring validate JWT qua JWKS. Tự trả lời: ai ký token, Spring lấy public key ở đâu, key rotate thì sao?
- [ ] Tự viết converter map roles sang `GrantedAuthority`
- [ ] Phân biệt 401 vs 403, và khi nào trả 404 thay vì 403 (không lộ sự tồn tại của resource)

---

## 1. Claims: quyền theo điều kiện + separation of duties

**Vai trò:** `CUSTOMER`, `ADJUSTER` (duyệt ≤ 50tr), `MANAGER` (duyệt mọi hồ sơ).

**Domain tự thiết kế:** `Claim(id, customerId, amount, status, submittedBy, assignedAdjusterId, approvedBy)`.
Gợi ý status: `SUBMITTED → ASSIGNED → APPROVED / REJECTED`.

**Ma trận cần tự điền (ô = điều kiện):**

| Endpoint | CUSTOMER | ADJUSTER | MANAGER |
|---|---|---|---|
| `POST /claims` | | | |
| `GET /claims/{id}` | | | |
| `POST /claims/{id}/approve` | | | |

**Câu hỏi thiết kế:**
- [ ] Ngưỡng 50tr để ở đâu (hằng số, config, DB)? Đổi ngưỡng thì sao?
- [ ] Approve có nhận `amount` trong body không? Nếu có thì dùng làm gì?
- [ ] Hai người approve cùng lúc thì sao? (optimistic lock, `@Version`)
- [ ] Approve claim đã `REJECTED` thì sao (state machine)?

**Test bắt buộc:**
- [ ] Adjuster approve claim 60tr → 403
- [ ] Adjuster approve claim đúng 50tr (boundary)
- [ ] Adjuster approve claim do chính mình nộp → 403
- [ ] Adjuster approve claim không được phân công cho mình
- [ ] Customer A xem claim của customer B → 404 hay 403? Chọn và giải thích
- [ ] Client gửi `amount: 1` trong body để lách ngưỡng → vẫn bị chặn (đọc amount từ DB)
- [ ] Approve hai lần → lần hai lỗi

**Tự vấn:** check nằm ở controller (`@PreAuthorize`) hay service? Điều kiện nào SpEL biểu đạt được, điều kiện nào phải đưa vào service? Vì sao check nghiệp vụ không nên chỉ dựa vào annotation?

---

## 2. Banking: step-up auth + IDOR chiều ghi + idempotency

**Domain:** `Account(id, ownerId, balance)`, `Transfer(id, fromAccountId, toAccountId, amount, idempotencyKey, status)`.

### A. Step-up (acr / amr)
- [ ] Keycloak: authentication flow có điều kiện OTP, map `acr` level
- [ ] Tự định nghĩa: amount ≥ bao nhiêu thì cần `acr` nào?
- [ ] Spring đọc `acr` từ JWT và so sánh. Khi thiếu thì trả gì? (tra RFC 9470, `WWW-Authenticate` + `acr_values`)
- [ ] `acr` trong token cũ có "hết hạn" không? Ép xác thực lại gần đây thế nào (`auth_time`, `max_age`)?

### B. IDOR chiều ghi
- [ ] Check `fromAccount.ownerId == jwt.sub` đặt ở đâu? Trước hay sau khi lock row?
- [ ] `toAccount` cần check gì (tồn tại, không bị khóa, khác `from`)?

### C. Idempotency
- [ ] Header `Idempotency-Key`; unique constraint trên `(user, key)`
- [ ] Cùng key + cùng payload → trả kết quả cũ. Cùng key + payload khác → lỗi gì?
- [ ] Request 2 đến khi request 1 đang xử lý dở thì sao?
- [ ] Trừ tiền: `SELECT ... FOR UPDATE` hay optimistic lock? Lock theo thứ tự nào để tránh deadlock A→B và B→A?

**Test bắt buộc:**
- [ ] Chuyển số tiền lớn với token `acr` thấp → bị từ chối
- [ ] Chuyển từ tài khoản của người khác → bị chặn
- [ ] Gửi cùng request 2 lần → chỉ trừ tiền 1 lần
- [ ] 10 request song song cùng key (`ExecutorService`) → balance đúng
- [ ] Hai chuyển khoản đồng thời → số dư không âm

---

## 3. Multi-tenant: quản lý tài liệu đa công ty

**Chọn hướng và ghi lý do vào `notes/ADR-tenancy.md`:**

| | Realm riêng / tenant | 1 realm + claim `tenant_id` |
|---|---|---|
| Mức cô lập | | |
| Vận hành khi có 1000 tenant | | |
| Rủi ro lỗi code | | |

Gợi ý: làm hướng 2 (khó và hay bị hỏi hơn), phân tích hướng 1 bằng lời.

**Domain:** `Tenant`, `UserTenantMembership(userId, tenantId, role)`, `Document(id, tenantId, ...)`, `TenantSettings`.

**Câu hỏi thiết kế:**
- [ ] `tenant_id` lấy từ token hay header/URL? User thuộc nhiều tenant thì chọn tenant hiện tại an toàn thế nào?
- [ ] Role theo tenant (ADMIN của A khác ADMIN của B): biểu diễn ra sao? Token chỉ có role chung thì thiếu gì?
- [ ] Chống quên filter tenant: Hibernate `@Filter` / `@TenantId` hay Postgres Row-Level Security? So sánh
- [ ] `TenantContext` bằng ThreadLocal: nhớ clear. Async / virtual thread thì sao?

**Test bắt buộc:**
- [ ] User tenant A `GET /documents/{id}` của tenant B → 404
- [ ] ADMIN của A `PUT /tenants/B/settings` → 403
- [ ] User đa-tenant gửi tenant không thuộc về mình → 403
- [ ] List / search / phân trang không bao giờ trả dữ liệu tenant khác
- [ ] Cố ý bỏ filter ở một repository method, xem test nào bắt được
- [ ] Query lọc ở DB (`findByIdAndTenantId`), không phải đọc xong mới check

---

## Câu hỏi 3: token cũ (trả lời cho từng bài trong `notes/`)

- [ ] Access token TTL bao lâu? Refresh token xử lý thế nào?
- [ ] Revoke / logout / back-channel logout có tác dụng với JWT đã phát hành không?
- [ ] Introspection mọi request (chính xác nhưng chậm) vs verify JWT local (nhanh nhưng stale)
- [ ] Khi đổi role của user trên Keycloak, bao lâu thì API thấy?

## Mẫu ghi chú cho mỗi bài (`notes/<bai>.md`)

```
Loại quyền:
Tầng kiểm tra + lý do:
Token cũ hiệu lực bao lâu:
Lỗi dễ sai tôi đã gặp:
Câu trả lời 30 giây khi bị hỏi:
```

## Tiến độ

- [ ] 0. Nền chung
- [ ] 1. Claims
- [ ] 2. Multi-tenant
- [ ] 3. Banking
