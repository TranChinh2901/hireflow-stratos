-- Dot 3: phieu theo buoi, trang thai lich, ly do dong ho so va phan hoi offer.
-- Tuong thich du lieu cu: status suy tu completed, phieu cu giu interview_id NULL.

alter table public.interviews
  add column if not exists status text not null default 'scheduled'
  check (status in ('scheduled', 'completed', 'cancelled', 'no_show'));

-- Backfill truoc khi rang buoc du lieu moi (idempotent nho dieu kien where).
update public.interviews set status = 'completed' where completed and status = 'scheduled';

alter table public.scorecards
  add column if not exists interview_id uuid references public.interviews(id) on delete cascade;

-- Phieu dinh danh theo bo ba ung vien/nguoi danh gia/buoi (NULL cho phep phieu tong hop cu).
alter table public.scorecards drop constraint if exists scorecards_candidate_id_evaluator_id_key;
drop index if exists public.scorecards_session_unique;
create unique index scorecards_session_unique
  on public.scorecards(candidate_id, evaluator_id, interview_id);

alter table public.candidates
  add column if not exists close_reason text,
  add column if not exists offer_sent_at timestamptz,
  add column if not exists offer_response text check (offer_response in ('accepted', 'declined'));
