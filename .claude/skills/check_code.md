---
name: check_code
description: Quy trình lặp review → fix → optimize vô tận cho đến khi không còn phát hiện lỗi nào trong code
---

Hãy thực hiện quy trình check_code đầy đủ cho codebase Konbini. Đây là vòng lặp kiểm soát chất lượng chạy liên tục cho đến khi vòng lặp nào không phát hiện thêm issue nào thì dừng.

## Quy trình

### Vòng lặp: Review → Fix → Optimize (lặp đến khi sạch)

**Bước 1 — REVIEW**
Thực hiện như vòng đầu tiên — không nhớ và không dựa vào kết quả check của bất kỳ vòng trước nào.
Thực hiện toàn bộ checklist trong `review skill`:
- Cấu trúc & config
- Frontend (Next.js)
- State management & cart
- Backend (FastAPI)
- Database & migrations
- Cross-cutting concerns

Phân loại tất cả issues tìm được thành:
- CRITICAL — lỗi sẽ gây build fail, runtime error, hoặc data corruption
- WARNING — code không đúng, sẽ gây bug hoặc behavior sai
- INFO — code hoạt động được nhưng có thể tối ưu hơn

**Bước 2 — KIỂM TRA ĐIỀU KIỆN DỪNG**
```
Nếu REVIEW ở Bước 1 không phát hiện bất kỳ CRITICAL, WARNING, hay INFO issue nào → DONE ✅
Nếu còn issue → tiếp tục Bước 3
```

**Bước 3 — FIX**
Sửa tất cả CRITICAL và WARNING issues tìm được ở Bước 1.
Theo nguyên tắc của `fix skill`.

**Bước 4 — OPTIMIZE**
Xử lý INFO issues và tối ưu code theo `optimize skill`.
Tập trung vào những gì còn lại sau khi fix.

**Bước 5 — QUAY LẠI Bước 1**
Bắt đầu vòng lặp tiếp theo với REVIEW toàn bộ lại từ đầu.
Lý do: fix ở vòng trước có thể tạo ra issue mới.

## Điều kiện dừng duy nhất
Vòng lặp chỉ dừng khi một vòng REVIEW hoàn chỉnh (Bước 1) **không phát hiện thêm issue nào** — không có CRITICAL, không có WARNING, không có INFO.

Nếu một issue không thể tự động fix (cần input từ user, cần credential thực, cần quyết định thiết kế), ghi nhận lý do và bỏ qua issue đó trong các vòng tiếp theo để tránh vòng lặp vô hạn.

## Báo cáo cuối
Sau khi hoàn thành, trình bày:
- Tổng số vòng lặp đã chạy
- Issue tìm/fix từng vòng: Vòng 1: N issues → Vòng 2: M issues → ... → Vòng cuối: 0 issues
- Tổng số issue đã fix (theo loại: CRITICAL / WARNING / INFO)
- Trạng thái: ✅ SẠCH — không còn issue nào được phát hiện
