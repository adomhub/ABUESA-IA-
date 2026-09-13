-- ABUESA — galerie photos/vidéos partagée par les visiteurs
-- À coller dans Supabase : Project > SQL Editor > New query > Run

create table if not exists gallery (
  id uuid primary key default gen_random_uuid(),
  name text,                 -- nom de la personne qui partage (facultatif)
  caption text,               -- légende / description
  media_url text not null,    -- URL publique du fichier (Supabase Storage)
  media_type text not null,   -- 'image' ou 'video'
  created_at timestamptz default now()
);

alter table gallery enable row level security;

create policy "Lecture publique de la galerie"
  on gallery for select
  using (true);

create policy "Ajout public à la galerie"
  on gallery for insert
  with check (true);

-- Comme pour les avis et les artistes : pas de suppression publique.
-- Pour retirer une photo, utilise le Table Editor Supabase (table "gallery")
-- ET supprime aussi le fichier correspondant dans Storage > media.

-- ============================================================
-- IMPORTANT — étape en plus, à faire une seule fois :
-- Storage > Create a new bucket
--   Nom du bucket : media
--   Public bucket : ACTIVÉ (coche la case)
-- C'est là que les vraies photos/vidéos seront stockées.
-- ============================================================
