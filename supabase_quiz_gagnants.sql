-- ABUESA-IA v4.85 : gagnant(e) du mini-quiz du jour
-- À coller dans Supabase > SQL Editor > Run (une seule fois).
create table if not exists public.quiz_gagnants (
  id bigint generated always as identity primary key,
  prenom text not null,
  village text,
  genre text default 'f',
  reponse text,
  message text,
  created_at timestamptz not null default now()
);
alter table public.quiz_gagnants enable row level security;
drop policy if exists "quiz_gagnants lecture" on public.quiz_gagnants;
create policy "quiz_gagnants lecture" on public.quiz_gagnants for select using (true);
drop policy if exists "quiz_gagnants ajout" on public.quiz_gagnants;
create policy "quiz_gagnants ajout" on public.quiz_gagnants for insert with check (char_length(prenom) between 1 and 60);
-- Première gagnante
insert into public.quiz_gagnants (prenom, village, genre, reponse, created_at)
values ('YDA', 'Ebeva', 'f', 'B', '2026-11-04 20:00:00+00');
