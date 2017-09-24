# --- !Ups

INSERT INTO "boards" (name, description, status,  is_reply, is_comment, is_attachment, list_permission, read_permission, write_permission, author, priority) VALUES
  ('noti', '공지사항 게시판', true,false,false,false,0 , 0, 9,'admin', 99),
  ('free', '자유 게시판', true,true,true,true,0 , 0, 2,'admin', 19),
  ('secret', '비밀 게시판', true,true,true,true, 2 , 2 , 2,'admin', 11),
  ('qna', '질의응답 게시판', true,true,true,true,0 , 0, 2,'admin',18),
  ('data', '자료 게시판', true,true,true,true,0 , 2, 2,'admin', 17),
  ('contents', '자료 게시판', true,true,true,true,9, 0, 9,'admin', 10);


# --- !Downs

TRUNCATE "boards" CASCADE;