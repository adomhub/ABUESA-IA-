-- ABUESA-IA v4.90 : « Examens, concours et infos utiles » + recherche automatique chaque matin
-- ⚠️ Avant de passer ce fichier dans Supabase, remplace CODE_ADMIN par ton vrai code admin.
-- À coller dans Supabase > SQL Editor > Run (une seule fois).

-- 1) La table des annonces
create table if not exists public.infos_utiles (
  id bigint generated always as identity primary key,
  categorie text default 'Info',
  titre text not null,
  description text,
  lien text,
  date_limite date,
  auto boolean not null default false,
  source text,
  created_at timestamptz not null default now()
);
alter table public.infos_utiles add column if not exists auto boolean not null default false;
alter table public.infos_utiles add column if not exists source text;
alter table public.infos_utiles enable row level security;
drop policy if exists "infos_utiles lecture" on public.infos_utiles;
create policy "infos_utiles lecture" on public.infos_utiles for select using (true);
drop policy if exists "infos_utiles ajout" on public.infos_utiles;
create policy "infos_utiles ajout" on public.infos_utiles for insert with check (char_length(titre) between 1 and 200);

-- 2) La recherche automatique : chaque jour à 6 h 07 (heure de Lomé), ABUESA-IA cherche les nouvelles annonces.
create extension if not exists pg_net;
create extension if not exists pg_cron;
select cron.unschedule('abuesa-infos-auto') where exists (select 1 from cron.job where jobname = 'abuesa-infos-auto');
select cron.schedule(
  'abuesa-infos-auto',
  '7 6 * * *',
  $$ select net.http_post(
       url := 'https://wyssiljzdvsxlnylqcia.supabase.co/functions/v1/bright-action?key=CODE_ADMIN',
       headers := '{"Content-Type":"application/json"}'::jsonb,
       body := '{}'::jsonb,
       timeout_milliseconds := 120000
     ) $$
);
