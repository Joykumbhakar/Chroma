-- Supabase Realtime Database Migration: Live Studio Lounge
-- Enables real-time collaborative notes, color palette drops, live polls, presence & reactions.

create table if not exists public.live_studio_events (
    id uuid primary key default gen_random_uuid(),
    room_id text not null,
    event_type text not null default 'note' check (event_type in ('note', 'palette', 'poll')),
    author_uid text not null,
    author_name text not null default '',
    author_handle text not null default '',
    author_avatar_url text not null default '',
    author_verified boolean not null default false,
    author_verified_tone text,
    body text not null default '',
    palette_hexes text[] not null default '{}',
    poll_question text not null default '',
    poll_options_json jsonb not null default '[]'::jsonb,
    liked_by text[] not null default '{}',
    likes_count integer not null default 0,
    created_at_ms bigint not null default ((extract(epoch from now()) * 1000)::bigint)
);

create index if not exists idx_live_studio_events_room_time
    on public.live_studio_events (room_id, created_at_ms desc);

-- Required so Postgres DELETE and UPDATE events broadcast full old/new records over Realtime
alter table public.live_studio_events replica identity full;

-- Add table to Supabase Realtime publication idempotently
do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'live_studio_events'
    ) then
        alter publication supabase_realtime add table public.live_studio_events;
    end if;
end
$$;

-- Expose table cleanly via Data API with RLS enabled
grant usage on schema public to anon, authenticated;
grant select, insert, update, delete on public.live_studio_events to anon, authenticated;

alter table public.live_studio_events enable row level security;

drop policy if exists "live_studio_events_select_all" on public.live_studio_events;
create policy "live_studio_events_select_all"
    on public.live_studio_events
    for select
    to anon, authenticated
    using (true);

drop policy if exists "live_studio_events_insert_nonempty_author" on public.live_studio_events;
create policy "live_studio_events_insert_nonempty_author"
    on public.live_studio_events
    for insert
    to anon, authenticated
    with check (length(trim(author_uid)) > 0 and length(trim(room_id)) > 0);

drop policy if exists "live_studio_events_update_all" on public.live_studio_events;
create policy "live_studio_events_update_all"
    on public.live_studio_events
    for update
    to anon, authenticated
    using (true)
    with check (length(trim(author_uid)) > 0);

drop policy if exists "live_studio_events_delete_author" on public.live_studio_events;
create policy "live_studio_events_delete_author"
    on public.live_studio_events
    for delete
    to anon, authenticated
    using (length(trim(author_uid)) > 0);
