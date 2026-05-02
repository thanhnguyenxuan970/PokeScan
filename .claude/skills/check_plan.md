---
name: check_plan
description: Quy trình lặp kiểm tra → tối ưu plan vô tận cho đến khi không còn lỗi, conflict, hay vấn đề nào
---

Hãy thực hiện quy trình check_plan cho plan hiện tại. Đây là vòng lặp kiểm soát chất lượng plan chạy liên tục cho đến khi một vòng kiểm tra không phát hiện thêm issue nào thì dừng.

## Trước khi bắt đầu

Đọc plan hiện tại từ file plan (thường ở `C:\Users\Admin\.claude\plans\*.md`) hoặc hỏi user nếu không rõ plan nào cần check.

## Quy trình

### Vòng lặp: Kiểm tra → Tối ưu (lặp đến khi sạch)

**Bước 1 — KIỂM TRA**
Thực hiện như vòng đầu tiên — không nhớ và không dựa vào kết quả kiểm tra của bất kỳ vòng trước nào.

Đọc kỹ toàn bộ plan và kiểm tra các vấn đề sau:

**Lỗi logic & kỹ thuật (CRITICAL):**
- Bước nào phụ thuộc vào bước chưa được thực hiện trước đó?
- File nào được tham chiếu nhưng không tồn tại hoặc sai path?
- API / function / component nào được dùng nhưng chưa được import hay định nghĩa?
- Bước nào sẽ gây lỗi runtime hoặc build nếu thực hiện đúng như mô tả?
- Có circular dependency nào không?

**Conflict & mâu thuẫn (WARNING):**
- Có bước nào mâu thuẫn với bước khác trong cùng plan không?
- Có quyết định thiết kế nào xung đột với code/pattern đã có trong codebase không?
- Có mock data nào dùng slug/id trùng với mock data ở trang khác (có thể gây nhầm lẫn)?
- Thứ tự implement có hợp lý không (dependency đúng thứ tự)?
- CSS class mới có đụng tên class đã có trong `globals.css` không?

**Thiếu sót & vấn đề (INFO):**
- Có edge case nào chưa được xử lý (slug không tồn tại → 404, API lỗi → fallback)?
- Verification section có đủ để test tất cả tính năng mô tả không?
- Có section nào mô tả quá mơ hồ, có thể dẫn đến implement sai không?
- Có component/utility nào đã có sẵn trong codebase mà plan đang định tạo lại?
- Mock data có đủ realistic để test UI không?

**Bước 2 — KIỂM TRA ĐIỀU KIỆN DỪNG**
```
Nếu Bước 1 không phát hiện bất kỳ CRITICAL, WARNING, hay INFO issue nào → DONE ✅
Nếu còn issue → tiếp tục Bước 3
```

**Bước 3 — TỐI ƯU PLAN**

Chỉnh sửa trực tiếp file plan để fix tất cả issues tìm được:
- CRITICAL: fix ngay, đảm bảo logic đúng và không có lỗi kỹ thuật
- WARNING: giải quyết conflict, làm rõ mâu thuẫn
- INFO: bổ sung chi tiết còn thiếu, thêm edge case handling, làm rõ mô tả mơ hồ

Nguyên tắc khi tối ưu:
- Không thêm scope mới ngoài những gì plan đã đề ra
- Không thay đổi quyết định thiết kế lớn — chỉ làm rõ và fix vấn đề
- Nếu cần quyết định từ user (ambiguous design choice), ghi `[CẦN XÁC NHẬN: ...]` vào plan và bỏ qua trong vòng tiếp theo

**Bước 4 — QUAY LẠI Bước 1**
Bắt đầu vòng lặp tiếp theo với kiểm tra toàn bộ lại từ đầu.
Lý do: tối ưu ở vòng trước có thể tạo ra issue mới hoặc conflict mới.

## Điều kiện dừng duy nhất
Vòng lặp chỉ dừng khi một vòng kiểm tra hoàn chỉnh (Bước 1) **không phát hiện thêm issue nào** — không có CRITICAL, không có WARNING, không có INFO.

Nếu một issue cần quyết định từ user (ambiguous architecture, design tradeoff), đánh dấu `[CẦN XÁC NHẬN]` trong plan và bỏ qua issue đó trong vòng tiếp theo để tránh vòng lặp vô hạn.

## Báo cáo cuối
Sau khi hoàn thành, trình bày:
- Tổng số vòng lặp đã chạy
- Issue tìm/fix từng vòng: Vòng 1: N issues → Vòng 2: M issues → ... → Vòng cuối: 0 issues
- Tổng số issue đã fix (theo loại: CRITICAL / WARNING / INFO)
- Danh sách `[CẦN XÁC NHẬN]` nếu có (các điểm cần user quyết định)
- Trạng thái: ✅ PLAN SẠCH — không còn issue nào được phát hiện
