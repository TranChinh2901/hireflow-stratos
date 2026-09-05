# Cấu hình Supabase

## 1. Tạo backend Supabase

```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
```

Migration tạo PostgreSQL schema, RLS, private Storage bucket `candidate-cvs` và bật Realtime cho các bảng chính.
Migration `202609040002_profile_details.sql` bổ sung số điện thoại, phòng ban, vị trí công việc và quyền tự cập nhật hồ sơ mà không cho phép đổi role hoặc workspace.
Migration `202609050001_single_user_workspaces.sql` chốt phạm vi MVP: mỗi tài khoản mới là admin của một workspace riêng.

Thêm vào file `local.properties` không commit:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
```

Không đưa `service_role` key vào ứng dụng Android.

## 2. Phạm vi tài khoản MVP

- Tài khoản đăng ký mới là `admin` của workspace do chính tài khoản đó tạo.
- Dữ liệu Room được lọc theo `organization_id`; dữ liệu demo có scope riêng và không được đưa lên cloud.
- Luồng mời HR/Interviewer vào cùng workspace chưa nằm trong MVP hiện tại.

RLS vẫn là lớp bảo vệ chính. Việc lọc dữ liệu trong Android ngăn dữ liệu cache của tài khoản khác xuất hiện trong UI, nhưng không thay thế RLS.

## 3. Offline-first

Room là nguồn dữ liệu UI. Mỗi bản ghi local có `remoteId`, `organizationId`, `updatedAt` và `syncState`. Thay đổi được ghi local trước; WorkManager chỉ đẩy bản ghi thuộc workspace đang đăng nhập, đồng bộ ngay khi có mạng và chạy định kỳ 15 phút. Candidate, interview và scorecard được merge Realtime; history và task được đồng bộ theo chu kỳ.

Nhắc lịch phỏng vấn là notification local: Android đồng bộ lại alarm từ các lịch tương lai mỗi khi app mở hoặc cài đặt thông báo thay đổi, rồi hiển thị thông báo trước 15 phút. Không cần dịch vụ push phía server.
