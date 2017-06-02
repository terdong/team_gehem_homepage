# --- !Ups

INSERT INTO "boards" (name, description, status,  is_reply, is_comment, is_attachment, list_permission, read_permission, write_permission, author) VALUES
  ('noti', '공지사항 게시판', true,false,false,false,0 , 0, 9,'동희'),
  ('free', '자유 게시판', true,true,true,true,0 , 0, 2,'동희'),
  ('secret', '비밀 게시판', true,true,true,true, 2 , 2 , 2,'동희'),
  ('Q&A', '질의응답 게시판', false,true,true,true,0 , 0, 2,'동희'),
  ('data', '자료 게시판', true,true,true,true,0 , 2, 2,'동희');


# --- !Downs

TRUNCATE "boards" CASCADE;