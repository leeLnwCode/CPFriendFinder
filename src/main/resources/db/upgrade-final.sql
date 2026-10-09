-- Run once on an existing PostgreSQL database after backup, before deploying this version.
-- Transactional and repeatable. Does not delete accounts, chat history or uploads.
BEGIN;
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS deleted_at timestamp(6) with time zone;
CREATE TABLE IF NOT EXISTS user_profiles (
 user_id uuid PRIMARY KEY REFERENCES users(id),
 bio varchar(500)
);
INSERT INTO user_profiles(user_id,bio) SELECT id,bio FROM users WHERE bio IS NOT NULL
 ON CONFLICT(user_id) DO NOTHING;

create index if not exists ix_room_members_room_active on room_members(room_id, left_at);
create index if not exists ix_room_members_user_active on room_members(user_id, left_at);
create index if not exists ix_messages_room_created on messages(room_id, created_at desc) where deleted_at is null;
create index if not exists ix_notifications_user_read on notifications(user_id, is_read, created_at desc);
create index if not exists ix_friend_requests_receiver_status on friend_requests(receiver_id, status);
create index if not exists ix_friend_requests_sender_status on friend_requests(sender_id, status);
create index if not exists ix_friendships_user on friendships(user_id);
create index if not exists ix_friendships_friend on friendships(friend_id);
create index if not exists ix_chat_rooms_discovery on chat_rooms(room_type, deleted_at, created_at desc);
create index if not exists ix_user_interests_interest on user_interests(interest_id);
COMMIT;
