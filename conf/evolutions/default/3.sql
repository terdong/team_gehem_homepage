# --- !Ups

insert into "Members" (email, name, nick, permission, level, exp) values
  ('terdong@gmail.com', '김동희', 'terdong',9, 99, 99999),
  ('terdong@naver.com', 'Theodore Kim','um', 2, 1, 0 );

# --- !Downs

TRUNCATE "Members" CASCADE