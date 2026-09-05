# HireFlow

HireFlow là ứng dụng Mini ATS offline-first trên Android, giúp HR quản lý ứng viên từ lúc nhận CV đến phỏng vấn, đánh giá và đưa ra kết quả tuyển dụng. Room hỗ trợ làm việc khi mất mạng, còn Supabase đồng bộ workspace riêng của tài khoản khi có kết nối.

## Tính năng đã triển khai

- Dashboard công việc hôm nay, thống kê nhanh và đánh dấu task hoàn thành.
- Thêm, sửa, tìm kiếm, lọc và xem chi tiết ứng viên.
- Lưu email, số điện thoại, kinh nghiệm, kỹ năng, trạng thái và ghi chú nội bộ.
- Chọn và mở CV PDF bằng Android Storage Access Framework.
- Candidate Pipeline gồm `Applied → Screening → Interview → Waiting Decision → Offer → Hired`, có nhánh `Rejected`.
- Chuyển vòng có kiểm tra CV, lịch đã hoàn thành, scorecard và kết luận; lịch sử được lưu local và đồng bộ cloud.
- Chỉ tạo lịch tương lai cho ứng viên ở vòng Interview, chặn lịch trùng interviewer và cho phép đánh dấu hoàn thành.
- Gửi local notification trước lịch phỏng vấn 15 phút bằng AlarmManager và lên lịch lại khi mở app.
- Interview Scorecard với 4 tiêu chí, điểm trung bình, nhận xét và kết luận.
- Blind Review Mode ẩn tên và avatar, chỉ giữ lại dữ liệu liên quan đến năng lực.
- Dark mode được lưu bằng DataStore.
- Đăng nhập/đăng ký bằng Supabase Auth; mỗi tài khoản MVP có một workspace riêng biệt.
- Trang hồ sơ HR với thống kê hoạt động, thông tin công việc, cài đặt và chỉnh sửa hồ sơ.
- Đồng bộ offline-first: Room ghi trước, WorkManager đẩy lên PostgreSQL khi có mạng.
- Candidate, interview và scorecard nhận cập nhật Realtime trong workspace đang đăng nhập.
- CV được upload vào Supabase Storage private bucket có RLS, tự retry và có thể tải trên thiết bị khác.
- Dữ liệu demo được seed trong vùng offline riêng và không được upload vào tài khoản cloud.
- Unit test cho logic tính điểm scorecard.

## Kiến trúc và công nghệ

- Kotlin, Jetpack Compose, Material 3
- MVVM, ViewModel, StateFlow
- Room Database + Repository
- Navigation Compose
- Preferences DataStore
- AlarmManager + local notification
- Storage Access Framework
- JUnit 4
- Supabase Auth, PostgREST, Realtime và Storage
- WorkManager

```text
com.hireflow.app
├── data          # Room entities, DAO, database, repository
├── cloud         # Supabase client, DTO và engine đồng bộ
├── domain        # Logic tính điểm độc lập để test
├── preferences   # DataStore cho thiết lập giao diện
├── reminder      # AlarmManager và BroadcastReceiver
├── sync          # WorkManager offline sync
└── ui
    ├── components
    ├── screens
    └── theme
```

## Chạy dự án

Yêu cầu Java 21 và Android SDK 37.

Để dùng cloud, sao chép `local.properties.example` thành `local.properties` và điền thông tin Supabase. Nếu chưa có backend, ứng dụng vẫn cho phép vào demo offline.

```bash
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.hireflow.app/.MainActivity
```

APK debug nằm tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Kịch bản demo gợi ý

1. Mở Dashboard và tick một công việc đã hoàn thành.
2. Vào Ứng viên, nhấn `+` để thêm hồ sơ mới.
3. Mở hồ sơ và đính kèm CV PDF từ thiết bị.
4. Nhấn chuyển vòng hoặc thao tác trực tiếp trong Pipeline.
5. Tạo lịch phỏng vấn và nhập checklist câu hỏi.
6. Đánh dấu lịch đã hoàn thành, mở phiếu đánh giá, bật Blind Review Mode và chấm đủ bốn tiêu chí.
7. Lưu kết luận, quay lại hồ sơ rồi chuyển ứng viên sang vòng tiếp theo.
8. Chạm avatar trên Dashboard để mở Hồ sơ, đổi giao diện hoặc cập nhật thông tin cá nhân.

Chế độ demo vẫn hoạt động hoàn toàn offline. Nếu chưa cấu hình cloud, chọn **Tiếp tục với demo offline**. Để bật Supabase, xem [supabase/README.md](supabase/README.md).
