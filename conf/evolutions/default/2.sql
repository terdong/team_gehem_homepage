# --- !Ups

insert into "Permissions" values
  (00, true, '방문자'),
  (01, true, '인증 전'),
  (02, true, '멤버'),
  (09, true, '관리자'),
  (99, true, '사용금지');

# --- !Downs

TRUNCATE "Permissions" CASCADE