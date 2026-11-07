-- ABUESA-IA v5.00 : « Mots proposés » par les visiteurs, validés par Alexi depuis l'admin
-- À coller dans Supabase > SQL Editor > Run (une seule fois).

-- 1) La table des mots en attente
create table if not exists public.mots_proposes (
  id bigint generated always as identity primary key,
  ikposso text not null,
  francais text,
  anglais text,
  exemple text,
  origine text not null default 'formulaire',
  created_at timestamptz not null default now()
);
alter table public.mots_proposes enable row level security;

-- Les visiteurs peuvent seulement AJOUTER un mot (pas lire, pas effacer).
-- La lecture, la validation et le refus passent par la fonction bright-action, protégée par le code admin.
drop policy if exists "mots_proposes ajout" on public.mots_proposes;
create policy "mots_proposes ajout" on public.mots_proposes for insert with check (
  char_length(ikposso) between 1 and 60
  and (francais is not null or anglais is not null)
  and coalesce(char_length(francais), 0) <= 120
  and coalesce(char_length(anglais), 0) <= 120
  and coalesce(char_length(exemple), 0) <= 200
  and origine in ('formulaire', 'conversation')
);

-- 2) Le dictionnaire garde aussi le sens en anglais (pour les Akposso du Ghana)
alter table public.dictionary_words add column if not exists en text;
