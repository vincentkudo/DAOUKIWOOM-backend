# RoomFlow Backend

API đặt phòng họp DAOU KIWOOM. Nhân viên nhập số người và khung giờ; hệ thống đề xuất phòng còn trống, vừa sức chứa.

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

Sĩ số + giờ → xếp phòng vừa nhất còn trống / đang bị giữ / đã có lịch → giữ 90s trên Redis → xác nhận `CONFIRMED`. Hai lịch xác nhận không chồng giờ trên cùng phòng. Không sửa / hủy lịch đã bắt đầu. Nhân viên chỉ đụng lịch mình; admin sửa / hủy được lịch người khác.

Đề xuất chỉ lấy phòng đang dùng, chỗ ≥ sĩ số. Còn trống xếp ít dư trước (6 người thấy phòng 6 trước phòng 16). Mức vừa chỗ: **Vừa** đúng sĩ số · **Tốt** dư ít · **Ổn** dư vừa · **Rộng** dư nhiều.

Luật một lịch: 07:00–21:00 giờ VN, cùng ngày, **T7 / CN vẫn đặt**; mốc 15 phút; dài 15 phút–8 giờ; không quá khứ; không quá 30 ngày. Liền kề được (09:00–10:00 và 10:00–11:00). Hủy thành `CANCELLED`.

Còn lịch `CONFIRMED` chưa kết thúc — đang họp **và lịch tương lai** — thì không đổi sức chứa, không xóa phòng.

## Quyết định và lý do

**Tìm theo số người.** Đề bài là gợi ý theo sức chứa: “6 người lúc 9h cần phòng nào”, không phải thử từng mã phòng.

**Redis giữ 90s.** Hai người có thể mở cùng phòng trước khi ai xác nhận. Chỉ chặn lúc ghi DB thì người sau vẫn điền form rồi mới lỗi.

**WebSocket khi giữ / đặt / hủy.** Nhiều người cùng tìm một khung giờ: ai giữ hoặc đặt thì những người đang xem khung đó phải thấy ngay, không F5, không đợi hết 90s rồi mới biết phòng hết. Server bắn socket `/ws/slots` (`HELD` / `RELEASED` / `BOOKED` / `FREED`) cho mọi tab đang mở; tab xem giờ khác bỏ qua. Mất socket thì tải lại mỗi 4 giây. Redis + constraint DB vẫn là lớp chặn — UI tươi chỉ để khỏi bấm nhầm.

**List có trang, xem tiếp thì gọi API tiếp.** Phòng / lịch / đề xuất không trả cả bảng. Lịch tuần tối đa 7 ngày, mỗi lần một trang phòng. 10 phòng thì nhẹ; hàng chục nghìn phòng không kéo một request là nổ.

**Khi dữ liệu lớn — đề bài vài chục phòng, thực tế là nhiều tầng / nhiều năm lịch.** Bottleneck không phải “thêm RAM”. Đề xuất không được `SELECT` hết phòng rồi lọc trong app: lọc `ACTIVE` + `capacity >= n` + chưa chồng giờ trên DB (index sức chứa + cấm chồng giờ theo từng phòng), tiện ích là điều kiện phụ. Lịch tuần không vẽ cả tòa: người dùng tìm / lọc phòng trước, API chỉ lấy một trang phòng trong tối đa 7 ngày — đúng hướng đang làm, scale thì giữ vậy chứ bỏ trang. Socket không broadcast mọi `HELD`/`BOOKED` cho mọi tab mãi được; thực tế subscribe theo khung giờ hoặc tầng, tab chỉ nhận slot mình đang xem. Mất socket vẫn poll 4s. Redis hold 90s và exclude DB **không thay**: UI tươi chỉ để khỏi bấm nhầm, hai request cùng lúc vẫn thua constraint. Bảng nóng chỉ lịch còn hiệu lực; `CANCELLED` cũ archive theo năm để kiểm tra chồng giờ khỏi phình. Nhiều cơ sở thì tách dữ liệu theo tòa: phòng Hà Nội không cần so giờ với phòng HCM.

**DB cấm hai lịch xác nhận chồng giờ.** Redis có thể mất; hai request vẫn có thể ghi cùng lúc. Constraint PostgreSQL là lớp cuối. Khoảng `[start, end)` để họp liền kề vẫn được.

**Khóa phòng khi còn lịch tương lai, không chỉ lúc đang họp.** Booking tuần sau vẫn gắn sức chứa đã chốt. Đổi 6 → 5 chỗ lúc đó cũng sai dữ liệu.

**Thứ 7 / CN vẫn đặt được.** Lịch đủ 7 ngày; ngoài giờ 07:00–21:00 mới cấm, không cấm cuối tuần.

**Admin sửa / hủy lịch người khác.** Nhân viên chỉ đụng lịch mình. Admin xử lý nhầm lịch / trả phòng giúp.

**Giờ làm + mốc 15 phút.** Trong giờ tòa nhà, khớp lịch họp, không chiếm phòng cả ngày / cả tháng.

**Hủy thì trả phòng, không xóa dòng.** Người khác đặt được ngay; vẫn biết ai đã hủy cuộc họp đó.
