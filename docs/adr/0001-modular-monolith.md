# ADR-0001: Modular monolith, không microservice ngay

- Trạng thái: Chấp nhận
- Ngày: 2026-11-09

## Bối cảnh
IdP cần nhất quán mạnh và độ trễ thấp (đăng nhập, cấp token). Dự án do một người
phát triển, vận hành càng đơn giản càng tốt.

## Quyết định
Một ứng dụng triển khai duy nhất, chia thành các module Maven có ranh giới rõ:
`idp-core` <- `idp-persistence` <- `idp-server`. Chưa tách microservice.

## Hệ quả
- Tốt: một đơn vị triển khai, giao dịch DB đơn giản, ít độ trễ mạng nội bộ.
- Đánh đổi: muốn tách service sau này phải giữ kỷ luật ranh giới module.
  Maven chặn vòng phụ thuộc, nhưng không chặn được việc module "rò" logic sang nhau.
