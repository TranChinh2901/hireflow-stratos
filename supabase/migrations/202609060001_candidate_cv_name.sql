-- Lưu tên file CV gốc để hiển thị đúng tên user đã chọn từ Drive/Downloads.
alter table public.candidates add column if not exists cv_name text;
