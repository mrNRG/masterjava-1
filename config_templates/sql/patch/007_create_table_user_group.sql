--liquibase formatted sql
--changeset alpere:007
--comment CREATE TABLE user_groups

CREATE TABLE user_group (
  user_id  INTEGER NOT NULL REFERENCES users (id) ON DELETE CASCADE,
  group_id INTEGER NOT NULL REFERENCES groups (id),
  CONSTRAINT users_group_idx UNIQUE (user_id, group_id)
);

--rollback DROP TABLE user_group;
