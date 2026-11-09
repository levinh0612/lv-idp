# Day 10 – Generic OIDC (Keycloak) + SAML upstream

## Mục tiêu
Cho admin tự cấu hình bất kỳ OIDC IdP (Keycloak, Azure AD, Okta…) chỉ bằng `issuer`, và hỗ trợ SAML 2.0 làm upstream cho doanh nghiệp.

## Chuẩn bị
- Day 09 xong. Chạy Keycloak dev: `docker run -p 8180:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak start-dev` (kiểm tra biến theo phiên bản).
- Trong Keycloak: tạo realm `test`, client `lv-idp` (confidential), redirect URI callback của broker, 1 user thử.

## Steps

### Step 1 – GenericOidcConnector
- [ ] Config: `issuer`, `client_id`, `client_secret`, `scopes`, `pkce`, `userinfo|id_token` làm nguồn claim, `claim_mappings`.
- [ ] Discovery tự động từ `issuer`, cache JWKS có TTL và refresh khi gặp `kid` lạ.
- [ ] Hỗ trợ `prompt`, `login_hint`, `acr_values` truyền xuống upstream (tuỳ chọn).
- [ ] Validate `issuer` trong discovery trùng cấu hình (chống mix-up attack).

### Step 2 – Thử với Keycloak
- [ ] Tạo connection `keycloak-test` → đăng nhập user Keycloak → có `app_user` mới.
- [ ] Map `realm_access.roles` → role nội bộ (dùng claim mapper path JSON).
- [ ] Thử đổi tên/ email ở Keycloak, đăng nhập lại, kiểm tra chế độ sync.

### Step 3 – SAML 2.0 upstream (SP)
- [ ] Dùng Spring Security SAML2 (`RelyingPartyRegistration`) – **không** tự parse XML.
- [ ] Config theo connection: IdP metadata URL/XML, entityID của SP, ACS URL `/t/{tenant}/broker/{alias}/saml/acs`, signing/encryption cert.
- [ ] Expose SP metadata: `/t/{tenant}/broker/{alias}/saml/metadata`.
- [ ] Map attribute SAML → `ExternalIdentity` (NameID persistent/email làm subject).
- [ ] Thử với Keycloak làm SAML IdP (client protocol saml) hoặc `samltest.id`.

### Step 4 – Bảo mật SAML
- [ ] Bắt buộc validate chữ ký Assertion/Response, `Audience`, `Recipient`, `NotOnOrAfter`, `InResponseTo`.
- [ ] Chặn XXE (DocumentBuilder an toàn – thư viện đã lo, vẫn kiểm tra cấu hình).
- [ ] Replay protection: lưu `Assertion ID` đã dùng vào Redis đến khi hết hạn.

### Step 5 – Test & commit
- [ ] Integration test OIDC với Keycloak Testcontainers (module `testcontainers-keycloak`) hoặc WireMock.
- [ ] `feat(day10): generic oidc and saml upstream`.

## Definition of Done
- Thêm Keycloak chỉ bằng cấu hình (không sửa code) và login thành công.
- SAML login chạy với 1 IdP thử; assertion replay bị từ chối.

## Lỗi hay gặp
- Redirect URI Keycloak không khớp (host `localhost` vs `127.0.0.1`).
- Clock skew làm `NotOnOrAfter`/`exp` fail → cho phép sai lệch ~60s.
- Dùng cùng `kid` cache cho nhiều issuer → key theo `(issuer, kid)`.

## Tham khảo
- Keycloak docs, Spring Security SAML2 Login, OAuth 2.0 Mix-Up mitigation (RFC 9207).
