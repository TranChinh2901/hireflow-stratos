-- HireFlow MVP uses one isolated workspace per account.
-- New users are workspace admins; team invitations are outside this MVP.
create or replace function public.handle_new_user()
returns trigger language plpgsql security definer set search_path = public as $$
declare
  new_org uuid;
begin
  insert into public.organizations(name)
  values (coalesce(new.raw_user_meta_data ->> 'organization_name', split_part(new.email, '@', 1) || ' Workspace'))
  returning id into new_org;

  insert into public.profiles(id, organization_id, full_name, email, phone, role)
  values (
    new.id,
    new_org,
    coalesce(new.raw_user_meta_data ->> 'full_name', new.email),
    new.email,
    coalesce(new.raw_user_meta_data ->> 'phone', ''),
    'admin'::public.app_role
  );
  return new;
end $$;
