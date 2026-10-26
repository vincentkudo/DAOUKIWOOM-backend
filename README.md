# RoomFlow Backend

REST API đặt phòng họp cho DAOU KIWOOM.

Nhân viên nhập **số người trước**, hệ thống đề xuất phòng còn trống và vừa sức chứa. Ai giữ/đặt trước thì giữ phòng.

- Java 17 · Spring Boot 3 · PostgreSQL 16 · Redis 7 · Flyway
- JWT · Lombok · MapStruct

## Chạy

Cần Docker Desktop.

```bash
docker compose up --build
```

| Cổng | Dịch vụ |
| --- | --- |
| http://localhost:8080 | API |
| http://localhost:8080/swagger-ui.html | Swagger |
| http://localhost | UI (nếu compose có frontend) |
| 5432 | PostgreSQL |
| 6379 | Redis |

Tài khoản seed (chỉ tạo khi DB trống):

| Role | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | `admin@daoukiwoom.vn` | `Admin123!` |
| EMPLOYEE | `linh.tran@daoukiwoom.vn` | `Emp123!` |

Đăng nhập:

```http
POST /api/v1/auth/login
{ "email": "linh.tran@daoukiwoom.vn", "password": "Emp123!" }
```

Gọi API kèm `Authorization: Bearer <accessToken>`.

Biến môi trường: copy `.env.example` → `.env`. Quan trọng khi deploy: `JWT_SECRET`, `DB_*`, `REDIS_PASSWORD`, `CORS_ORIGINS`. TTL giữ chỗ: `ROOM_HOLD_SECONDS` (mặc định 90).

Dừng và xóa volume (reset seed):

```bash
docker compose down -v
```

## Thiết kế

Luồng chính: tìm phòng theo sĩ số → giữ chỗ mềm trên Redis → xác nhận booking → PostgreSQL chặn trùng giờ.

```
Client
  → Controller
  → Service
  → Repository / Redis HoldStore
  → PostgreSQL
```

Gói chính:

- `Controller` — HTTP, bọc `APIResponse`
- `Service` — luật nghiệp vụ
- `Repository` — JPA / Redis
- `Entity` — model DB
- `DTO` — request / response
- `Mapper` — MapStruct
- `Exception` — `ErrorCode` + `AppException`
- `Security` — JWT filter
- `Seed` — user và phòng demo

Mọi response cùng shape:

```json
{ "code": 1000, "message": "Success", "httpStatus": "OK", "result": {} }
```

`code = 1000` là thành công. Lỗi nghiệp vụ trả mã riêng (3xxx phòng, 4xxx booking) kèm HTTP status thật.

### API

| Method | Path | Việc |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | Đăng nhập |
| GET | `/api/v1/auth/me` | User hiện tại |
| GET | `/api/v1/rooms/suggestions?attendeeCount=&startAt=&endAt=` | Đề xuất phòng |
| GET | `/api/v1/rooms` | Danh sách phòng |
| POST/PUT/DELETE | `/api/v1/rooms` | Admin quản lý phòng |
| POST | `/api/v1/holds` | Giữ chỗ Redis |
| POST | `/api/v1/holds/{id}/heartbeat` | Gia hạn giữ chỗ |
| DELETE | `/api/v1/holds/{id}` | Nhả chỗ |
| POST | `/api/v1/bookings` | Đặt phòng |
| GET | `/api/v1/bookings/mine` | Lịch của tôi |
| POST | `/api/v1/bookings/{id}/cancel` | Hủy lịch |
| GET | `/api/v1/calendar?from=&to=` | Lịch tuần |

### Luật đặt phòng

- Mốc 15 phút, trong 07:00–21:00 (Asia/Ho_Chi_Minh)
- Dài 15 phút–8 giờ, cùng một ngày
- Không đặt quá khứ, không quá 30 ngày
- Sĩ số không vượt sức chứa
- Phòng `INACTIVE` không đặt được
- Không sửa/xóa phòng khi còn lịch `CONFIRMED` chưa kết thúc

### Fit khi đề xuất

Chỉ xét phòng `ACTIVE`, đủ chỗ, đủ tiện ích. Xếp phòng ít dư chỗ lên trước.

| Fit | Ý nghĩa |
| --- | --- |
| PERFECT / Vừa | Đúng sĩ số |
| TIGHT / Tốt | Dư ít (≤ 2 chỗ hoặc ≤ 20%) |
| GOOD / Ổn | Dư vừa |
| SPACIOUS / Rộng | Dư nhiều |

Mỗi phòng còn gắn trạng thái khung giờ: `AVAILABLE`, `HELD_BY_ME`, `HELD_BY_OTHER`, `BOOKED`.

## Quyết định quan trọng và lý do

Chỉ những quyết định gắn đề bài: đề xuất phòng theo sức chứa, giữ chỗ Redis, không trùng lịch, toàn vẹn dữ liệu đặt phòng.

**1. Tìm theo số người, không theo phòng**  
Chọn: `GET /rooms/suggestions?attendeeCount=` rồi mới lọc phòng còn trống và đủ chỗ.  
Lý do: đề bài là gợi ý phòng theo sức chứa. Nhu cầu là “6 người họp 9h cần phòng nào”, không phải lần lượt thử từng mã phòng. Xếp phòng ít dư chỗ lên trước để 3 người không chiếm phòng 16 chỗ.

**2. Redis giữ chỗ mềm 90 giây**  
Chọn: khi bấm đặt thì `SET` Redis TTL 90s; form còn mở thì heartbeat; hủy hoặc hết giờ thì nhả; đặt xong thì xóa hold.  
Lý do: đề bài yêu cầu soft-hold. Hai người có thể mở cùng phòng trước khi ai xác nhận. Chỉ chặn lúc ghi booking thì người sau vẫn điền form rồi mới lỗi. Hold báo ngay phòng đang bị giữ và còn bao nhiêu giây.

**3. DB chặn hai lịch CONFIRMED chồng giờ**  
Chọn: `EXCLUDE gist` trên khoảng `[start_at, end_at)`, chỉ với `CONFIRMED`, kèm khóa dòng phòng lúc insert.  
Lý do: đề bài không cho hai cuộc họp cùng phòng cùng giờ. Redis có thể mất; hai request vẫn có thể insert cùng lúc. Constraint trên PostgreSQL là lớp cuối.  
Khoảng nửa mở `[start, end)` để 09:00–10:00 và 10:00–11:00 không đụng nhau — họp liền kề vẫn được.

**4. Không sửa sức chứa / xóa phòng khi còn lịch họp**  
Chọn: phòng có booking `CONFIRMED` mà `end_at > now` thì chỉ xem, không sửa, không xóa.  
Lý do: lịch đã chốt 6 người trên phòng 6 chỗ. Đổi xuống 5 chỗ thì booking không còn đúng sức chứa. Đó là mất toàn vẹn dữ liệu đặt phòng.

**5. Luật giờ họp: 07:00–21:00, mốc 15 phút, cùng ngày**  
Chọn: timezone `Asia/Ho_Chi_Minh`, tối thiểu 15 phút, tối đa 8 giờ, đặt trước tối đa 30 ngày.  
Lý do: tự thiết kế luật nghiệp vụ cho văn phòng. Slot 15 phút khớp lịch họp. Giới hạn giờ và ngày để không chiếm phòng ngoài giờ làm hoặc cả tháng.

**6. Hủy lịch thì trả phòng, không xóa dòng**  
Chọn: đổi `CANCELLED`; constraint overlap không tính lịch hủy.  
Lý do: phòng phải trống lại cho người khác đặt. Xóa dòng thì mất dấu vết ai đã hủy cuộc họp đó.
