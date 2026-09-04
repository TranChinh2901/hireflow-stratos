-- Profile owners may edit presentation fields, never their workspace or role.
revoke update on public.profiles from authenticated;
grant update(full_name, phone, department, job_title) on public.profiles to authenticated;
