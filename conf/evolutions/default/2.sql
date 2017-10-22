# --- !Ups

insert into "permissions" values
  (00, true, 'account.permission.visitor'),
  (01, true, 'account.permission.waiting_auth'),
  (02, true, 'account.permission.member'),
  (07, true, 'account.permission.developer'),
  (08, true, 'account.permission.semi_admin'),
  (09, true, 'account.permission.admin'),
  (99, true, 'account.permission.max');

# --- !Downs

TRUNCATE "permissions" CASCADE;