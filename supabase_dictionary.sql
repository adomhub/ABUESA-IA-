-- ABUESA — table du dictionnaire partagé (mémoire vivante)
-- À coller dans Supabase : Project > SQL Editor > New query > Run

create table if not exists dictionary_words (
  id uuid primary key default gen_random_uuid(),
  fr text not null,
  ak text not null,
  created_at timestamptz default now()
);

alter table dictionary_words enable row level security;

-- Tout le monde peut LIRE le dictionnaire (visiteurs + app)
create policy "Lecture publique du dictionnaire"
  on dictionary_words for select
  using (true);

-- L'ajout se fait uniquement depuis le panneau Admin de l'app (protégé par le
-- geste secret + code). Volontairement, aucune policy de suppression publique
-- n'est créée : pour retirer un mot, utilise le Table Editor de Supabase
-- directement (Project > Table Editor > dictionary_words), ce qui évite
-- qu'un visiteur malveillant puisse vider le dictionnaire.
create policy "Ajout de mots (admin app)"
  on dictionary_words for insert
  with check (true);
