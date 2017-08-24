# --- !Ups

create table "members"
(
  seq bigserial not null
		constraint "members_pkey"
			primary key,
	email varchar(80) not null
	constraint "members_email_key"
			unique,
	name varchar(30) not null,
	nick varchar(12) not null,
	permission SMALLINT default 2 not null,
	level integer default 0 not null,
	exp integer default 0 not null,
	register_date timestamp default now() not null,
	update_date timestamp default now() not null,
	last_logged timestamp default now() not null
);
create table "boards"
(
	seq bigserial not null
		constraint "boards_pkey"
			primary key,
	name varchar(30) not null
		constraint "boards_name_key"
			unique,
	description varchar(2000),
	status boolean not null,
	is_reply boolean not null,
	is_comment boolean not null,
	is_attachment boolean not null,
	list_permission SMALLINT not null,
	read_permission SMALLINT not null,
	write_permission SMALLINT not null,
	author varchar(80) not null,
	priority integer default 0 not null,
	register_date timestamp default now() not null
);
create table "posts"
(
	seq bigserial not null
		constraint "posts_pkey"
			primary key,
	board_seq bigint not null
		constraint posts_boards_seq_fk
			references "boards"
				on delete cascade,
	thread bigint not null,
	depth integer not null,
	author_seq bigint not null
		constraint posts_members_seq_fk
			references "members"
				on delete cascade,
	subject varchar(80) not null,
	hit_count integer default 0 not null,
	content text,
	author_ip varchar(50) not null,
	write_date timestamp default now() not null,
	update_date timestamp default now() not null
);
create table "permissions"
(
  permission_code SMALLINT not null
    constraint "permissions_pkey"
    primary key,
  active boolean default true not null,
  content varchar(80) not null
);
create table "attachments"
(
	seq bigserial not null
		constraint "attachments_pkey"
		primary key,
	hash varchar(32) not null,
	name varchar(255) not null,
	sub_path varchar(64) not null,
	mime_type varchar(255) not null,
	size bigint not null,
	container_seq bigint default 0 not null,
	download_count int default 0 not null,
	uploaded_date timestamp default now() not null
);
create table "comments"
(
	seq bigserial not null
		constraint "comments_pkey"
			primary key,
	post_seq bigint not null
		constraint comments_posts_seq_fk
			references "posts"
				on update restrict on delete cascade,
	thread int not null,
	author_seq bigint not null
		constraint comments_members_seq_fk
		references "members"
			on delete cascade,
	reply_comment_seq bigint,
	content varchar(4000) not null,
	author_ip varchar(50) not null,
	write_date timestamp default now() not null
);

# --- !Downs

DROP TABLE "comments";
DROP TABLE "attachments";
DROP TABLE "permissions";
DROP TABLE "posts";
DROP TABLE "boards";
DROP TABLE "members";
