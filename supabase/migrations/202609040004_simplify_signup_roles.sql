-- Simplified MVP signup: users choose Admin or HR; invitation codes are no longer used.
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

drop table if exists public.organization_invites;
