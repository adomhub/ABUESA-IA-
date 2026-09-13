-- ABUESA — table des artistes qui se font connaître eux-mêmes
-- À coller dans Supabase : Project > SQL Editor > New query > Run

create table if not exists artist_submissions (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  songs text,                 -- titres de musiques, séparés par des virgules
  current_song_title text,    -- chanson d'actualité
  current_song_link text,     -- lien YouTube / Spotify / TikTok vers la chanson
  contact text,               -- téléphone ou email (facultatif, pour qu'Alexi recontacte)
  created_at timestamptz default now()
);

alter table artist_submissions enable row level security;

create policy "Lecture publique des artistes soumis"
  on artist_submissions for select
  using (true);

create policy "Soumission publique d'artiste"
  on artist_submissions for insert
  with check (true);

-- Seul toi (depuis le tableau de bord Supabase) pourras modérer / supprimer.
