-- ABUESA-IA v4.91 : la recherche automatique de chaque matin appelle la fonction « bright-action »
-- ⚠️ Avant de passer ce fichier dans Supabase, remplace CODE_ADMIN par ton vrai code admin.
-- (nom choisi par Supabase pour la fonction infos-auto). À coller dans SQL Editor > Run.
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
