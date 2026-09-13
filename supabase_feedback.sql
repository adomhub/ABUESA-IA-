-- ABUESA — table du mur d'avis public
-- À coller dans Supabase : Project > SQL Editor > New query > Run

create table if not exists feedback (
  id uuid primary key default gen_random_uuid(),
  name text,
  message text not null,
  created_at timestamptz default now()
);

-- Active la sécurité au niveau des lignes
alter table feedback enable row level security;

-- Autorise tout le monde (visiteurs anonymes) à LIRE les avis
create policy "Lecture publique des avis"
  on feedback for select
  using (true);

-- Autorise tout le monde à AJOUTER un avis
create policy "Ajout public d'avis"
  on feedback for insert
  with check (true);

-- (Optionnel mais recommandé) empêche la modification/suppression par le public
-- -> seul toi, depuis le tableau de bord Supabase, pourras modérer les avis.
