# RoomFlow Backend

API đặt phòng họp DAOU KIWOOM. Nhân viên nhập số người và khung giờ; hệ thống đề xuất phòng còn trống, vừa sức chứa. Ai giữ hoặc đặt trước thì giữ phòng.

Java 17 · Spring Boot 3 · PostgreSQL · Redis

## Chạy

Cần Docker Desktop.

```bash
docker compose up --build
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html

| Role | Email | Mật khẩu |
| --- | --- | --- |
| ADMIN | `admin@daoukiwoom.vn` | `Admin123!` |
| EMPLOYEE | `linh.tran@daoukiwoom.vn` | `Emp123!` |

## Nghiệp vụ

Luồng: chọn sĩ số + giờ → hệ thống xếp phòng vừa nhất còn trống → giữ chỗ 90s → xác nhận → lịch `CONFIRMED`. Hai lịch xác nhận không được chồng giờ trên cùng phòng.

### Ai được làm gì

- Nhân viên: tìm phòng, giữ chỗ, đặt, xem / hủy lịch của mình.
- Quản trị: thêm các việc trên, cộng quản lý phòng.
- Chỉ người tạo lịch (hoặc admin) mới sửa / hủy.
- Không sửa hoặc hủy lịch đã bắt đầu.

### Đề xuất phòng

Chỉ lấy phòng đang dùng, sức chứa ≥ số người, đủ tiện ích nếu có lọc.

Ba nhóm kết quả cùng khung giờ:

| Nhóm | Ý nghĩa |
| --- | --- |
| Còn trống | Đặt được. Phòng ít dư chỗ đứng trước (6 người thấy phòng 6 trước phòng 16). |
| Đang bị giữ | Người khác đang giữ Redis, hiện còn bao nhiêu giây. |
| Đã có lịch | Đã có `CONFIRMED` chồng khung giờ. |

Mức vừa chỗ: **Vừa** đúng sĩ số · **Tốt** dư ít · **Ổn** dư vừa · **Rộng** dư nhiều.

### Giữ chỗ

Bấm đặt là giữ phòng 90 giây trên Redis. Người khác thấy phòng đó “đang bị giữ”, không đặt được. Hết giờ hoặc hủy form thì nhả. Đặt xong thì hết hold. Một người chỉ giữ một phòng tại một thời điểm.

### Luật một lịch họp

- Trong 07:00–21:00, theo giờ Việt Nam, cùng một ngày.
- Bắt đầu / kết thúc khớp mốc 15 phút (09:00 được, 09:07 không).
- Dài từ 15 phút đến 8 giờ; không đặt quá khứ; không đặt trước quá 30 ngày.
- Số người không vượt sức chứa. Phòng ngưng dùng không đặt được.
- Hai cuộc liền kề được: 09:00–10:00 và 10:00–11:00 không coi là chồng.
- Hủy đổi thành `CANCELLED` — phòng trống lại, dòng lịch vẫn còn.

### Phòng và toàn vẹn dữ liệu

Phòng còn lịch `CONFIRMED` chưa kết thúc thì không sửa sức chứa và không xóa. Ví dụ đã chốt 6 người / 6 chỗ rồi hạ xuống 5 chỗ là sai dữ liệu.

## Quyết định và lý do

**Tìm theo số người.** Đề bài là gợi ý theo sức chứa: “6 người lúc 9h cần phòng nào”, không phải thử từng mã phòng.

**Redis giữ 90s.** Hai người có thể mở cùng phòng trước khi ai xác nhận. Chỉ chặn lúc ghi DB thì người sau vẫn điền form rồi mới lỗi.

**DB cấm hai lịch xác nhận chồng giờ.** Redis có thể mất; hai request vẫn có thể ghi cùng lúc. Constraint PostgreSQL là lớp cuối. Khoảng `[start, end)` để họp liền kề vẫn được.

**Khóa sửa phòng khi còn họp.** Đổi 6 chỗ → 5 chỗ lúc đã có booking 6 người làm hỏng lịch đã chốt.

**Giờ làm + mốc 15 phút.** Luật văn phòng tự đặt: trong giờ tòa nhà, khớp lịch họp, không chiếm phòng cả ngày / cả tháng.

**Hủy thì trả phòng, không xóa dòng.** Người khác đặt được ngay; vẫn biết ai đã hủy cuộc họp đó.
