-- Add editable profile details for projects that already applied the initial schema.
alter table public.profiles
  add column if not exists phone text not null default '',
  add column if not exists department text not null default 'Human Resources',
  add column if not exists job_title text not null default 'Recruitment Specialist';

drop policy if exists profiles_self_update on public.profiles;
create policy profiles_self_update on public.profiles for update to authenticated
  using (id = auth.uid())
  with check (
    id = auth.uid() and organization_id = public.my_org_id() and role = public.my_role()
  );

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
