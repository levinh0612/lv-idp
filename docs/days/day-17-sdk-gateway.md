# Day 17 – Starter SDK, gateway, sample apps

## Mục tiêu
Giúp ứng dụng khác tích hợp IDP trong vài dòng cấu hình: Spring Boot starter cho resource server, tích hợp API gateway, và bộ sample app chứng minh end-to-end.

## Chuẩn bị
- Day 16 xong.

## Steps

### Step 1 – `idp-spring-boot-starter`
- [ ] Auto-configuration: đọc `lv.idp.issuer` → cấu hình `JwtDecoder` (JWKS qua discovery, cache), validate `iss`, `aud`, `exp`, `typ=at+jwt`.
- [ ] `JwtAuthenticationConverter` map `roles`/`scope` → `GrantedAuthority`.
- [ ] Annotation `@RequirePermission("invoice:approve")` + Aspect gọi Check API (kèm resource id từ tham số `@ResourceId`); client credentials tự động.
- [ ] Xử lý lỗi chuẩn: 401 `WWW-Authenticate: Bearer error="invalid_token"`, 403.
- [ ] Mẫu `application.yml` tối giản trong README của starter.

### Step 2 – Gateway
- [ ] Spring Cloud Gateway (hoặc Nginx/Envoy `ext_authz`): validate JWT tại edge, chuyển header `X-User-Id`, `X-Tenant` xuống service (chỉ tin header khi nội bộ, xoá header giả từ bên ngoài).
- [ ] Tuỳ chọn: pattern **BFF** cho SPA – gateway giữ token phía server, SPA chỉ có cookie session (an toàn hơn để token trong browser).

### Step 3 – Sample apps (`samples/`)
- [ ] `samples/resource-api` (Spring Boot): dùng starter, 3 endpoint với quyền khác nhau.
- [ ] `samples/spa` (từ Day 06): gọi `resource-api` bằng access token.
- [ ] `samples/python-fastapi`: verify JWT bằng `authlib`/`python-jose` qua JWKS – chứng minh IDP trung lập ngôn ngữ (khớp stack Python của bạn).
- [ ] `samples/m2m-service`: client credentials gọi API.

### Step 4 – Tài liệu tích hợp
- [ ] `docs/integration.md`: cách đăng ký client, cấu hình Spring/FastAPI/React/Node, xử lý refresh, logout.

### Step 5 – Test & commit
- [ ] Test end-to-end bằng Docker Compose: SPA → IDP → API; token hết hạn → refresh; revoke → API từ chối (nếu bật introspection).
- [ ] `feat(day17): starter sdk, gateway, samples`.

## Definition of Done
- Một Spring Boot API mới được bảo vệ chỉ bằng dependency + 2 dòng cấu hình.
- FastAPI sample xác thực token của IDP thành công.

## Lỗi hay gặp
- Không validate `aud` → token của API khác dùng được ở API này.
- Gateway và service cùng tin header `X-User-Id` từ bên ngoài → giả mạo. Xoá ở edge.
- Cache JWKS không refresh khi rotate key → lỗi sau khi xoay khóa; refresh khi gặp `kid` lạ (có giới hạn tần suất).

## Tham khảo
- Spring Security OAuth2 Resource Server, Spring Cloud Gateway, BCP "OAuth 2.0 for Browser-Based Applications" (RFC 10017 – xác nhận số RFC tại rfc-editor.org; khuyến nghị mô hình BFF).
