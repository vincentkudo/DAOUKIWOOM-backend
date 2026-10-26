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

**1. Tìm theo số người, không theo phòng**  
Chọn: `GET /rooms/suggestions?attendeeCount=` rồi mới lọc phòng.  
Không chọn: UI chọn một phòng rồi hỏi còn trống không.  
Lý do: nhu cầu thật là “6 người họp 9h–10h, phòng nào vừa và còn trống”. Nếu đi từ phòng, người dùng phải thử từng mã. Sắp xếp theo chỗ dư tăng dần để ưu tiên phòng vừa, tránh chiếm All-hands 16 cho 3 người.

**2. Redis giữ chỗ mềm 90 giây, không dùng hàng đợi**  
Chọn: `SET` Redis TTL 90s khi bấm đặt, heartbeat ~30s, hủy form thì `DELETE`, đặt xong thì consume. Một user chỉ giữ một phòng.  
Không chọn: Kafka / outbox.  
Lý do: hai người có thể mở cùng phòng trước khi ai bấm xác nhận. Nếu chỉ chặn lúc `POST /bookings` thì người sau vẫn điền form rồi mới thấy lỗi. Hold báo ngay “đang được người khác giữ, còn Xs”. TTL tự hết nếu đóng tab. Kafka thừa: không có consumer khác, không cần replay sự kiện, chỉ cần state ngắn sống.

**3. PostgreSQL `EXCLUDE gist` là lớp cuối chống trùng giờ**  
Chọn: `tstzrange(start_at, end_at, '[)')` + `WHERE status = CONFIRMED`, kèm `SELECT … FOR UPDATE`.  
Lý do: Redis có thể restart, hold có thể hết, hai request vẫn có thể tới `INSERT` cùng lúc. Constraint DB không phụ thuộc app — request thứ hai fail, không bao giờ có hai lịch CONFIRMED chồng nhau.  
Khoảng nửa mở `[start, end)` vì họp 09:00–10:00 và 10:00–11:00 không chồng, phòng dùng liền được. Chỉ `CONFIRMED` nằm trong constraint để lịch `CANCELLED` trả phòng.

**4. Không sửa / xóa phòng khi còn lịch CONFIRMED chưa hết**  
Chọn: `locked` khi `end_at > now()`, API `3010`. Admin xem được, không sửa sức chứa / xóa.  
Lý do: booking đã xác nhận “6 người / phòng 6 chỗ”. Sửa xuống 5 chỗ thì dữ liệu tự mâu thuẫn. Xóa phòng thì mất khóa ngoại lịch sử. Chặn ở service, không tin UI.

**5. Flyway giữ schema, Hibernate chỉ `validate`**  
Chọn: `V1__init.sql` là nguồn thật. `ddl-auto: validate`.  
Không chọn: Hibernate tự `update` cột.  
Lý do: `update` làm lệch local / Docker / prod (đã gặp `SMALLINT` vs `INTEGER` với `floor`). Flyway chạy cùng một file ở mọi môi trường, app từ chối khởi động nếu lệch.

**6. JWT stateless, hai role ADMIN / EMPLOYEE**  
Chọn: Bearer JWT, không session server.  
Lý do: API đứng sau nginx / nhiều instance. Session dính máy. Nhân viên chỉ đặt và xem lịch mình. Admin thêm CRUD phòng. Ít role cho đúng take-home, không làm IAM phức tạp.

**7. Múi giờ `Asia/Ho_Chi_Minh`, slot 15 phút, giờ 07:00–21:00**  
Chọn: lưu `TIMESTAMPTZ`, luật theo VN. Slot 15 phút. Tối đa 8 giờ, trước 30 ngày.  
Lý do: văn phòng VN, UTC thuần dễ lệch ngày. 15 phút khớp lịch thật, tránh 09:07–09:22. 07:00–21:00 là giờ tòa nhà. Giới hạn 30 ngày và 8 giờ để không chiếm phòng cả tháng / cả ngày.

**8. Hủy mềm, không xóa dòng booking**  
Chọn: `status = CANCELLED`, giữ `cancel_reason`.  
Lý do: cần biết ai hủy, lúc nào. Xóa cứng mất audit. Constraint overlap chỉ áp `CONFIRMED` nên hủy là phòng trống lại ngay, không cần job dọn.

**9. Response bọc `APIResponse { code, message, httpStatus, result }`**  
Chọn: `code = 1000` thành công; lỗi có mã nghiệp vụ (3006 hold, 3010 phòng đang họp).  
Lý do: frontend phân nhánh theo `code`, không parse chuỗi tiếng Anh của Bean Validation. HTTP status vẫn đúng cho Swagger / proxy.

**10. Seed chỉ khi DB trống**  
Chọn: `SEED_ENABLED`, tạo admin / nhân viên / dải phòng 3–30 chỗ một lần.  
Lý do: reviewer chạy `docker compose up` là có data. Seed lại mỗi lần restart sẽ đè booking demo. `docker compose down -v` khi cần reset.

**11. Docker Compose đủ postgres + redis + api**  
Chọn: một lệnh chạy cả stack.  
Lý do: reviewer không cần cài JDK / Maven / Postgres. Cùng image với cách nộp.

**12. MapStruct cho DTO, không trả Entity ra API**  
Chọn: Entity ở persistence; DTO ở controller.  
Lý do: không lộ `password_hash`, không kéo lazy `organizer` ra JSON. Đổi cột DB không phá contract nếu mapper giữ field cũ.
