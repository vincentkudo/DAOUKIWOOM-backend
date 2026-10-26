# RoomFlow Backend

REST API đặt phòng họp — DAOU KIWOOM.

- Java 17 · Spring Boot 3 · PostgreSQL · Redis · Flyway
- Lombok + MapStruct

## Chạy local

```bash
docker compose up --build
```

Swagger: `http://localhost:8080/swagger-ui.html`

Demo:

- `admin@daoukiwoom.vn` / `Admin123!`
- `linh.tran@daoukiwoom.vn` / `Emp123!`

## Env quan trọng

Xem `.env.example`. TTL giữ chỗ mềm: `ROOM_HOLD_SECONDS` (mặc định 90).
