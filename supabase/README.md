# Cấu hình Supabase

## 1. Tạo backend Supabase

```bash
supabase link --project-ref YOUR_PROJECT_REF
supabase db push
```

Migration tạo PostgreSQL schema, RLS cho `Admin / HR / Interviewer`, private Storage bucket `candidate-cvs` và bật Realtime cho các bảng chính.
Migration `202609040002_profile_details.sql` bổ sung số điện thoại, phòng ban, vị trí công việc và quyền tự cập nhật hồ sơ mà không cho phép đổi role hoặc workspace.

Thêm vào file `local.properties` không commit:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_PUBLISHABLE_KEY=sb_publishable_xxx
```

Không đưa `service_role` key vào ứng dụng Android.

## 2. Quy tắc phân quyền

- `admin`: quản lý ứng viên, pipeline và toàn bộ scorecard.
- `hr`: quản lý ứng viên, pipeline, lịch và xem scorecard.
- `interviewer`: chỉ xem ứng viên/lịch được gán và tạo hoặc sửa scorecard của chính mình.

Màn đăng ký MVP cho phép chọn `admin` hoặc `hr`. Trigger chỉ chấp nhận đúng hai giá trị này; mọi giá trị khác được đưa về `admin`. Mỗi tài khoản mới tạo một workspace riêng.

RLS là lớp bảo vệ chính. Việc ẩn nút trong Android chỉ nhằm cải thiện trải nghiệm, không thay thế RLS.

## 3. Offline-first

Room là nguồn dữ liệu UI. Mỗi bản ghi local có `remoteId`, `updatedAt` và `syncState`. Thay đổi được ghi local trước; WorkManager đồng bộ ngay khi có mạng và chạy định kỳ 15 phút. Realtime merge thay đổi từ cloud về Room.

Nhắc lịch phỏng vấn là notification local: khi tạo lịch, Android lên lịch bằng AlarmManager và hiển thị thông báo trước 15 phút. Không cần dịch vụ push phía server.
