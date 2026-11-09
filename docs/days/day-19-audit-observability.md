# Day 19 – Audit, webhook, observability, GDPR

## Mục tiêu
Mọi sự kiện quan trọng được ghi vết bất biến và đẩy ra ngoài (webhook); có metrics/traces/logs để vận hành; hỗ trợ quyền riêng tư (export/xoá dữ liệu).

## Chuẩn bị
- Day 18 xong. Docker Compose bổ sung Prometheus + Grafana + (OTel Collector, Loki/Tempo tuỳ chọn).

## Steps

### Step 1 – Event model
- [ ] `IdpEvent(id, time, tenantId, type, actor, target, clientId, ip, userAgent, outcome, details)`.
- [ ] Loại sự kiện: `LOGIN_SUCCESS/FAILED`, `MFA_*`, `TOKEN_ISSUED/REFRESHED/REVOKED`, `TOKEN_REUSE_DETECTED`, `USER_CREATED/UPDATED/DELETED`, `ROLE_ASSIGNED`, `CLIENT_*`, `CONNECTION_*`, `KEY_ROTATED`, `ADMIN_*`.
- [ ] Phát qua `EventListener` SPI (đồng bộ nhẹ) → ghi **outbox** trong cùng transaction để không mất sự kiện.

### Step 2 – Audit log
- [ ] Bảng `audit_event` **append-only**, partition theo tháng (PostgreSQL declarative partitioning), index `(tenant_id, time desc)`, `(actor)`, `(type)`.
- [ ] Quyền DB: app user chỉ `INSERT/SELECT`, không `UPDATE/DELETE`.
- [ ] Retention: job tự drop partition cũ theo chính sách (vd. 13 tháng).
- [ ] Che dữ liệu nhạy cảm (không ghi password/token/secret).

### Step 3 – Webhook
- [ ] `webhook_endpoint(tenant_id, url, secret, event_types[], enabled)`.
- [ ] Worker đọc outbox, POST JSON, ký **HMAC-SHA256** (`X-IDP-Signature`, kèm timestamp chống replay), retry với backoff, dead-letter sau N lần.
- [ ] SSRF guard cho URL webhook (chặn IP nội bộ).
- [ ] UI xem lịch sử gửi & gửi lại.

### Step 4 – Observability
- [ ] Micrometer metrics: `idp_login_total{outcome,method}`, `idp_token_issued_total{grant}`, `idp_login_latency`, `idp_authz_check_latency`, `idp_rate_limited_total`.
- [ ] Tracing OpenTelemetry (javaagent hoặc Micrometer Tracing): trace xuyên broker → upstream.
- [ ] Logging JSON có `traceId`, `tenant`; **không** log secret (test bằng grep trong test).
- [ ] Health: liveness/readiness (kiểm DB, Redis).
- [ ] Dashboard Grafana: tỉ lệ login lỗi, p95 latency, token reuse, rate limited. Alert: tăng đột biến `LOGIN_FAILED`, `TOKEN_REUSE_DETECTED`.

### Step 5 – Quyền riêng tư (GDPR)
- [ ] Export dữ liệu user (`GET /admin/.../users/{id}/export`): hồ sơ, liên kết, consent, session.
- [ ] Xoá/ẩn danh: xoá credential & PII, giữ `audit_event` nhưng thay actor bằng hash.
- [ ] Ghi nhận consent điều khoản, thời điểm, phiên bản.

### Step 6 – Test & commit
- [ ] Test: thao tác → có đúng event; webhook ký đúng & retry; user bị xoá không còn PII.
- [ ] `feat(day19): audit, webhooks, observability, gdpr`.

## Definition of Done
- Trên Grafana thấy metric login theo thời gian thực; mỗi lần đăng nhập có 1 dòng audit.
- Webhook nhận được `LOGIN_SUCCESS` với chữ ký hợp lệ.

## Lỗi hay gặp
- Ghi audit ngoài transaction → mất sự kiện khi lỗi; dùng outbox.
- Label metric có `userId`/`email` → bùng nổ cardinality.
- Log chứa token → rò rỉ qua hệ thống log.

## Tham khảo
- Micrometer, OpenTelemetry Java, Postgres partitioning, Stripe webhook signing (mẫu).
