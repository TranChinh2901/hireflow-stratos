-- HireFlow cloud schema: multi-tenant Auth, PostgreSQL, Storage and Realtime.
create extension if not exists pgcrypto;

create type public.app_role as enum ('admin', 'hr', 'interviewer');
create type public.recruitment_stage as enum (
  'applied', 'screening', 'interview', 'waiting_decision', 'offer', 'hired', 'rejected'
);

create table public.organizations (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  created_at timestamptz not null default now()
);

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  full_name text not null,
  email text,
  phone text not null default '',
  department text not null default 'Human Resources',
  job_title text not null default 'Recruitment Specialist',
  role public.app_role not null default 'interviewer',
  created_at timestamptz not null default now()
);
create index profiles_organization_idx on public.profiles(organization_id);

create table public.candidates (
  id uuid primary key,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  full_name text not null,
  position text not null,
  email text not null default '',
  phone text not null default '',
  experience_years integer not null default 0 check (experience_years >= 0),
  skills text[] not null default '{}',
  stage public.recruitment_stage not null default 'applied',
  notes text not null default '',
  cv_path text,
  created_by uuid references auth.users(id),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz
);
create index candidates_org_stage_idx on public.candidates(organization_id, stage) where deleted_at is null;

create table public.interviews (
  id uuid primary key,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  candidate_id uuid not null references public.candidates(id) on delete cascade,
  scheduled_at timestamptz not null,
  duration_minutes integer not null default 60 check (duration_minutes between 15 and 480),
  format text not null check (format in ('online', 'onsite')),
  interviewer_name text not null,
  interviewer_id uuid references auth.users(id),
  round text not null,
  checklist text[] not null default '{}',
  completed boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index interviews_org_time_idx on public.interviews(organization_id, scheduled_at);
create index interviews_interviewer_idx on public.interviews(interviewer_id, scheduled_at);

create table public.scorecards (
  id uuid primary key,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  candidate_id uuid not null references public.candidates(id) on delete cascade,
  evaluator_id uuid not null references auth.users(id),
  technical smallint not null check (technical between 1 and 5),
  communication smallint not null check (communication between 1 and 5),
  problem_solving smallint not null check (problem_solving between 1 and 5),
  culture_fit smallint not null check (culture_fit between 1 and 5),
  strengths text not null default '',
  improvements text not null default '',
  notes text not null default '',
  conclusion text not null check (conclusion in ('Strong Hire', 'Hire', 'Consider', 'Reject')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(candidate_id, evaluator_id)
);

create table public.stage_history (
  id uuid primary key,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  candidate_id uuid not null references public.candidates(id) on delete cascade,
  from_stage public.recruitment_stage not null,
  to_stage public.recruitment_stage not null,
  actor_id uuid not null references auth.users(id),
  changed_at timestamptz not null default now()
);

create table public.hr_tasks (
  id uuid primary key,
  organization_id uuid not null references public.organizations(id) on delete cascade,
  title text not null,
  subtitle text not null default '',
  type text not null,
  completed boolean not null default false,
  due_at timestamptz not null,
  assignee_id uuid references auth.users(id),
  updated_at timestamptz not null default now()
);

create or replace function public.my_org_id()
returns uuid language sql stable security definer set search_path = public
as $$ select organization_id from public.profiles where id = auth.uid() $$;

create or replace function public.my_role()
returns public.app_role language sql stable security definer set search_path = public
as $$ select role from public.profiles where id = auth.uid() $$;

create or replace function public.can_access_candidate(candidate_uuid uuid)
returns boolean language sql stable security definer set search_path = public
as $$
  select public.my_role() in ('admin', 'hr') or exists (
    select 1 from public.interviews i
    where i.candidate_id = candidate_uuid and i.interviewer_id = auth.uid()
  )
$$;

create or replace function public.touch_updated_at()
returns trigger language plpgsql set search_path = public as $$
begin new.updated_at = now(); return new; end $$;

create trigger candidates_touch before update on public.candidates for each row execute function public.touch_updated_at();
create trigger interviews_touch before update on public.interviews for each row execute function public.touch_updated_at();
create trigger scorecards_touch before update on public.scorecards for each row execute function public.touch_updated_at();
create trigger tasks_touch before update on public.hr_tasks for each row execute function public.touch_updated_at();

-- Each account creates its own workspace and may choose the Admin or HR role.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  new_org uuid;
  selected_role public.app_role;
begin
  selected_role := case
    when new.raw_user_meta_data ->> 'requested_role' = 'hr' then 'hr'::public.app_role
    else 'admin'::public.app_role
  end;
  insert into public.organizations(name)
  values (coalesce(new.raw_user_meta_data ->> 'organization_name', split_part(new.email, '@', 1) || ' Workspace'))
  returning id into new_org;
  insert into public.profiles(id, organization_id, full_name, email, phone, role)
  values (
    new.id, new_org, coalesce(new.raw_user_meta_data ->> 'full_name', new.email), new.email,
    coalesce(new.raw_user_meta_data ->> 'phone', ''), selected_role
  );
  return new;
end $$;

create trigger on_auth_user_created after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.organizations enable row level security;
alter table public.profiles enable row level security;
alter table public.candidates enable row level security;
alter table public.interviews enable row level security;
alter table public.scorecards enable row level security;
alter table public.stage_history enable row level security;
alter table public.hr_tasks enable row level security;

create policy org_select on public.organizations for select to authenticated using (id = public.my_org_id());
create policy profiles_select on public.profiles for select to authenticated using (organization_id = public.my_org_id());
create policy profiles_admin_update on public.profiles for update to authenticated
  using (organization_id = public.my_org_id() and public.my_role() = 'admin')
  with check (organization_id = public.my_org_id());
create policy profiles_self_update on public.profiles for update to authenticated
  using (id = auth.uid())
  with check (
    id = auth.uid() and organization_id = public.my_org_id() and role = public.my_role()
  );
create policy candidates_member_select on public.candidates for select to authenticated
  using (organization_id = public.my_org_id() and public.can_access_candidate(id));
create policy candidates_manager_insert on public.candidates for insert to authenticated
  with check (organization_id = public.my_org_id() and public.my_role() in ('admin', 'hr'));
create policy candidates_manager_update on public.candidates for update to authenticated
  using (organization_id = public.my_org_id() and public.my_role() in ('admin', 'hr'))
  with check (organization_id = public.my_org_id());
create policy candidates_admin_delete on public.candidates for delete to authenticated
  using (organization_id = public.my_org_id() and public.my_role() = 'admin');

create policy interviews_member_select on public.interviews for select to authenticated
  using (organization_id = public.my_org_id() and (public.my_role() in ('admin', 'hr') or interviewer_id = auth.uid()));
create policy interviews_manager_insert on public.interviews for insert to authenticated
  with check (organization_id = public.my_org_id() and public.my_role() in ('admin', 'hr'));
create policy interviews_manager_update on public.interviews for update to authenticated
  using (organization_id = public.my_org_id() and public.my_role() in ('admin', 'hr'))
  with check (organization_id = public.my_org_id());

create policy scorecards_member_select on public.scorecards for select to authenticated
  using (organization_id = public.my_org_id() and (public.my_role() in ('admin', 'hr') or evaluator_id = auth.uid()));
create policy scorecards_evaluator_insert on public.scorecards for insert to authenticated
  with check (
    organization_id = public.my_org_id() and evaluator_id = auth.uid() and
    (public.my_role() in ('admin', 'hr') or exists (
      select 1 from public.interviews i where i.candidate_id = scorecards.candidate_id and i.interviewer_id = auth.uid()
    ))
  );
create policy scorecards_evaluator_update on public.scorecards for update to authenticated
  using (organization_id = public.my_org_id() and (evaluator_id = auth.uid() or public.my_role() in ('admin', 'hr')))
  with check (organization_id = public.my_org_id());

create policy history_member_select on public.stage_history for select to authenticated
  using (organization_id = public.my_org_id() and public.can_access_candidate(candidate_id));
create policy history_manager_insert on public.stage_history for insert to authenticated
  with check (organization_id = public.my_org_id() and actor_id = auth.uid() and public.my_role() in ('admin', 'hr'));
create policy tasks_assignee_select on public.hr_tasks for select to authenticated
  using (organization_id = public.my_org_id() and (public.my_role() in ('admin', 'hr') or assignee_id = auth.uid()));
create policy tasks_manager_all on public.hr_tasks for all to authenticated
  using (organization_id = public.my_org_id() and public.my_role() in ('admin', 'hr'))
  with check (organization_id = public.my_org_id());
grant usage on schema public to authenticated;
grant select on public.organizations to authenticated;
grant select on public.profiles to authenticated;
grant update(full_name, phone, department, job_title) on public.profiles to authenticated;
grant select, insert, update, delete on public.candidates to authenticated;
grant select, insert, update on public.interviews to authenticated;
grant select, insert, update on public.scorecards to authenticated;
grant select, insert on public.stage_history to authenticated;
grant select, insert, update, delete on public.hr_tasks to authenticated;
grant execute on function public.my_org_id() to authenticated;
grant execute on function public.my_role() to authenticated;
grant execute on function public.can_access_candidate(uuid) to authenticated;

insert into storage.buckets(id, name, public, file_size_limit, allowed_mime_types)
values ('candidate-cvs', 'candidate-cvs', false, 10485760, array['application/pdf'])
on conflict (id) do nothing;

create policy cv_member_read on storage.objects for select to authenticated
  using (bucket_id = 'candidate-cvs' and (storage.foldername(name))[1] = public.my_org_id()::text);
create policy cv_manager_insert on storage.objects for insert to authenticated
  with check (bucket_id = 'candidate-cvs' and (storage.foldername(name))[1] = public.my_org_id()::text and public.my_role() in ('admin', 'hr'));
create policy cv_manager_update on storage.objects for update to authenticated
  using (bucket_id = 'candidate-cvs' and (storage.foldername(name))[1] = public.my_org_id()::text and public.my_role() in ('admin', 'hr'));

alter publication supabase_realtime add table public.candidates;
alter publication supabase_realtime add table public.interviews;
alter publication supabase_realtime add table public.scorecards;
