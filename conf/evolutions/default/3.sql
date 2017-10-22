# --- !Ups

insert into "members" (email, id, name, nick, permission, level, exp) values
--- ('terdong@gmail.com', '김동희', 'terdong',9, 99, 99999),
 ('terdong@naver.com','1234567890', 'admin','admin', 9, 99, 99999 );

# --- !Downs

TRUNCATE "members" CASCADE;