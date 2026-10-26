package com.daoukiwoom.roomflow.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(9998, "Invalid message key", HttpStatus.BAD_REQUEST),

    INVALID_CREDENTIALS(1004, "Email hoặc mật khẩu không đúng.", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1001, "Phiên đăng nhập không hợp lệ.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(1002, "Bạn không có quyền thực hiện thao tác này.", HttpStatus.FORBIDDEN),
    USER_NOT_FOUND(1010, "Không tìm thấy người dùng.", HttpStatus.NOT_FOUND),
    USER_INACTIVE(1011, "Tài khoản đã bị vô hiệu hóa.", HttpStatus.FORBIDDEN),

    VALIDATION_FAILED(2000, "Dữ liệu gửi lên không hợp lệ.", HttpStatus.BAD_REQUEST),
    INVALID_TIME_RANGE(2001, "Khung giờ đặt phòng không hợp lệ.", HttpStatus.BAD_REQUEST),
    OUTSIDE_WORKING_HOURS(2002, "Chỉ được đặt phòng trong giờ làm việc 07:00–21:00.", HttpStatus.BAD_REQUEST),
    SLOT_NOT_ALIGNED(2003, "Thời gian phải khớp mốc 15 phút.", HttpStatus.BAD_REQUEST),
    BOOKING_TOO_SHORT(2004, "Thời lượng tối thiểu là 15 phút.", HttpStatus.BAD_REQUEST),
    BOOKING_TOO_LONG(2005, "Thời lượng tối đa là 8 giờ.", HttpStatus.BAD_REQUEST),
    BOOKING_IN_PAST(2006, "Không thể đặt phòng trong quá khứ.", HttpStatus.BAD_REQUEST),
    BOOKING_TOO_FAR(2007, "Chỉ được đặt trước tối đa 30 ngày.", HttpStatus.BAD_REQUEST),

    ROOM_NOT_FOUND(3001, "Không tìm thấy phòng họp.", HttpStatus.NOT_FOUND),
    ROOM_INACTIVE(3002, "Phòng họp đang ngưng sử dụng.", HttpStatus.CONFLICT),
    ROOM_CODE_EXISTS(3003, "Mã phòng đã tồn tại.", HttpStatus.CONFLICT),
    ROOM_CAPACITY_EXCEEDED(3004, "Số người vượt quá sức chứa của phòng.", HttpStatus.CONFLICT),
    ROOM_ALREADY_BOOKED(3005, "Phòng đã được đặt trong khung giờ này.", HttpStatus.CONFLICT),
    ROOM_HELD_BY_OTHER(3006, "Phòng đang được người khác giữ chỗ.", HttpStatus.CONFLICT),
    ROOM_HOLD_BUSY(3007, "Phòng đang được giữ chỗ, vui lòng thử lại.", HttpStatus.CONFLICT),
    HOLD_NOT_FOUND(3008, "Phiên giữ chỗ đã hết hạn hoặc không tồn tại.", HttpStatus.NOT_FOUND),
    HOLD_EXPIRED(3009, "Hết thời gian giữ chỗ. Hãy chọn lại phòng.", HttpStatus.CONFLICT),

    BOOKING_NOT_FOUND(4001, "Không tìm thấy lịch đặt phòng.", HttpStatus.NOT_FOUND),
    BOOKING_ALREADY_CANCELLED(4002, "Lịch đặt này đã được hủy.", HttpStatus.CONFLICT),
    BOOKING_ALREADY_STARTED(4003, "Không thể sửa hoặc hủy lịch đã bắt đầu.", HttpStatus.CONFLICT);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
