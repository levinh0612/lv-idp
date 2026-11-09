# LV-IDP – Lộ trình 20 ngày

Xem thiết kế tổng thể: [`../plan.html`](../plan.html) (mở bằng trình duyệt).

## Cách dùng
- Mỗi file `day-XX-*.md` = 1 ngày (≈ 3–5h). Làm tuần tự, đừng nhảy cóc.
- Mỗi ngày có: **Mục tiêu → Chuẩn bị → Steps (checkbox) → Kiểm tra (Definition of Done) → Lỗi hay gặp → Tham khảo**.
- Quy ước đường dẫn: `src` = `src/main/java/dev/lv/idp/...`, `test` = `src/test/java/dev/lv/idp/...`.
- Mỗi ngày kết thúc bằng 1 commit nhỏ (`feat(dayXX): ...`). Có test chạy xanh mới sang ngày mới.
- Gặp vấn đề → ghi vào `docs/journal.md` (bug, quyết định, thứ học được).

## Mục lục

| Phase | Day | Nội dung | Demo cuối ngày |
|---|---|---|---|
| 0 Nền tảng | [01](day-01-project-setup.md) | Repo, multi-module, Docker Compose, CI | `docker compose up` + `/actuator/health` |
| | [02](day-02-domain-schema.md) | Domain & DB schema (Flyway) | Migration chạy, test repository |
| | [03](day-03-password-auth.md) | User, password Argon2id, session | Đăng ký + đăng nhập form |
| 1 OAuth2/OIDC | [04](day-04-authorization-server.md) | Authorization Server, client, Code + PKCE | Lấy được `code` rồi đổi token |
| | [05](day-05-tokens-keys.md) | JWT, JWKS, rotation, revoke, introspect | Rotate key không làm hỏng token cũ |
| | [06](day-06-oidc.md) | Discovery, UserInfo, consent, logout | SPA mẫu login/logout |
| | [07](day-07-grants-multitenant.md) | Client credentials, device, token exchange, multi-tenant | 2 tenant, issuer riêng |
| 2 Federation | [08](day-08-broker-google.md) | Broker framework + Google | Login Google |
| | [09](day-09-github-facebook-linking.md) | GitHub, Facebook, account linking, mapping | 1 user, nhiều provider |
| | [10](day-10-oidc-saml-upstream.md) | Generic OIDC (Keycloak), SAML upstream | Login qua Keycloak |
| | [11](day-11-user-storage-spi.md) | External JDBC, LDAP, lazy migration | Login bằng user DB cũ |
| 3 Strong auth | [12](day-12-mfa-totp-flow.md) | Flow engine, TOTP, recovery codes | Bật/tắt MFA |
| | [13](day-13-webauthn.md) | WebAuthn / Passkeys | Login bằng passkey |
| | [14](day-14-account-security.md) | Reset pwd, verify email, brute-force, step-up | Khóa mềm sau N lần sai |
| 4 Authorization | [15](day-15-rbac.md) | RBAC, permission, tenant scoping | Role trong token |
| | [16](day-16-check-api-policy-rebac.md) | Check API, ABAC, ReBAC | `/authz/check` trả allow/deny |
| | [17](day-17-sdk-gateway.md) | Starter SDK, gateway, sample app | API bảo vệ bằng `@RequirePermission` |
| 5 Admin & Prod | [18](day-18-admin.md) | Admin API + UI | Tạo client/provider từ UI |
| | [19](day-19-audit-observability.md) | Audit, webhook, observability, GDPR | Dashboard + audit log |
| | [20](day-20-hardening-release.md) | Hardening, conformance, load test, Helm | Release v0.1.0 |

## Nguyên tắc xuyên suốt
1. **Test trước khi sang ngày mới** (JUnit 5 + Testcontainers).
2. **Không tự viết crypto/protocol** – dùng thư viện đã kiểm chứng.
3. **Secure by default** – mọi thứ tắt/siết chặt, bật khi cần.
4. **Tenant filter** có mặt từ Day 02, đừng "thêm sau".
5. Kiểm tra phiên bản thư viện mới nhất trước khi thêm dependency (Spring Boot, Spring Security, webauthn4j...).
