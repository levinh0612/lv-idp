# Day 20 – Hardening, conformance, load test, Helm, release

## Mục tiêu
Đưa hệ thống lên mức production-ready: rà soát bảo mật, kiểm thử tuân thủ chuẩn, đo tải, đóng gói Docker/Helm, tài liệu vận hành và phát hành v0.1.0.

## Chuẩn bị
- Day 19 xong. Skill `security-review` có thể chạy trên nhánh hiện tại.

## Steps

### Step 1 – Rà soát bảo mật
- [ ] Đi hết checklist `plan.html` mục 10; tick từng dòng, ghi lại chỗ chưa đạt vào `docs/security-todo.md`.
- [ ] OWASP ASVS L2: đối chiếu chương V2 (Authentication), V3 (Session), V4 (Access control), V8 (Data protection), V13 (API).
- [ ] Dependency scan: `mvn org.owasp:dependency-check-maven:check` hoặc Trivy/Dependabot; SAST (CodeQL/Semgrep) trong CI.
- [ ] Header bảo mật (CSP, HSTS, X-Content-Type-Options, Referrer-Policy) – test bằng securityheaders hoặc test tự động.
- [ ] Rà soát secrets: không có trong repo/log; `git log -p | grep -i secret` (hoặc gitleaks).
- [ ] Pen-test tay các kịch bản: redirect_uri tampering, code interception không PKCE, state thiếu, refresh reuse, account linking takeover, IDOR chéo tenant, SSRF qua discovery/webhook, SAML replay.

### Step 2 – Kiểm thử tuân thủ chuẩn
- [ ] Chạy **OpenID Connect Conformance Suite** (certification.openid.net, bản local qua Docker) với profile Basic OP / Config OP; ghi kết quả, sửa lỗi.
- [ ] (Tuỳ chọn, P2) Nếu hướng tới fintech: chạy thêm profile **FAPI 2.0 Security Profile (Final)** của cùng conformance suite; tra danh sách profile hiện hành tại openid.net/certification.
- [ ] Kiểm tra tương thích thực tế: login thành công với ít nhất 3 thư viện RP khác nhau (Spring, `oidc-client-ts`, `authlib`/`passport-openidconnect`).

### Step 3 – Load test (k6)
- [ ] Kịch bản: login form, token endpoint (code → token), refresh, introspect, Check API.
- [ ] Mục tiêu khởi điểm (điều chỉnh theo máy): token endpoint p95 < 200ms ở 200 rps; Check API p95 < 20ms (cache).
- [ ] Tìm điểm nghẽn: Argon2 (CPU), pool DB, Redis; ghi nhận và tune (giới hạn đồng thời cho hash bằng semaphore/bulkhead).
- [ ] `EXPLAIN ANALYZE` các query nóng (lookup user, token, authz); bổ sung index; kiểm tra autovacuum cho bảng token/session có churn cao.

### Step 4 – Đóng gói & triển khai
- [ ] Dockerfile multi-stage (JRE slim, user non-root, read-only FS), healthcheck; `mvn spring-boot:build-image` là phương án thay thế.
- [ ] Helm chart `deploy/helm/lv-idp`: Deployment (≥2 replica), Service, Ingress (TLS), HPA, PodDisruptionBudget, ConfigMap/Secret, probes, resource limit.
- [ ] Biến cấu hình bắt buộc: master key (KMS/Secret), DB, Redis, SMTP, issuer public URL.
- [ ] Migration Flyway chạy bằng Job/initContainer, không chạy song song nhiều pod.
- [ ] Quy trình: backup/restore Postgres, **rotate master key**, rotate signing key, disaster recovery (RPO/RTO mục tiêu).

### Step 5 – Tài liệu
- [ ] `README.md` gốc: giới thiệu, quickstart 5 phút (compose up + sample).
- [ ] `docs/operations.md` (runbook): triển khai, nâng cấp, sao lưu, xử lý sự cố (Redis chết, DB chậm, upstream IdP lỗi).
- [ ] `docs/architecture.md` (tóm tắt từ plan) + danh sách ADR đã chốt.
- [ ] `CHANGELOG.md`, `SECURITY.md` (cách báo lỗi bảo mật).

### Step 6 – Release
- [ ] Tag `v0.1.0`, build image đẩy registry, GitHub Release kèm changelog.
- [ ] Retrospective: ghi `docs/retro.md` – điều gì tốt, nợ kỹ thuật, kế hoạch P2 (DPoP, PAR, SAML IdP, SCIM, Hooks/Actions, WebAuthn attestation policy, custom domain/tenant, HA đa vùng).

## Definition of Done
- Conformance suite: các test bắt buộc của profile Basic OP đạt.
- Load test đạt mục tiêu hoặc có ghi chú tune tiếp; không lỗ hổng mức High còn mở.
- `helm install` lên cụm K8s thử (kind/minikube) chạy được toàn bộ sample.

## Lỗi hay gặp
- Chạy 1 replica rồi mới phát hiện state lưu trong memory → test với ≥2 replica sớm.
- Quên `forward-headers-strategy` sau reverse proxy → issuer/redirect sai scheme (`http` thay vì `https`).
- Rotate master key không có kịch bản re-encrypt → khoá chết. Test quy trình trước khi cần.

## Tham khảo
- OpenID Conformance Suite, OWASP ASVS, k6, Helm best practices, 12-factor.
