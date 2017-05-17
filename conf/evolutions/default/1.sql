# Users schema

# --- !Ups

create table "Members"
(
	email varchar(80) not null
		constraint "Members_pkey"
			primary key,
	name varchar(30) not null,
	nick varchar(12) not null,
	permission SMALLINT default 2 not null,
	level integer default 0 not null,
	exp integer default 0 not null,
	register_date timestamp default now() not null,
	update_date timestamp default now() not null,
	last_logged timestamp default now() not null
);
create table "Boards"
(
	seq bigserial not null
		constraint "Boards_pkey"
			primary key,
	name varchar(30) not null
		constraint "Boards_name_key"
			unique,
	description varchar(2000),
	status boolean not null,
	list_permission SMALLINT not null,
	read_permission SMALLINT not null,
	write_permission SMALLINT not null,
	author varchar(80) not null,
	register_date timestamp default now() not null
);
create table "Posts"
(
	seq bigserial not null
		constraint "Posts_pkey"
			primary key,
	board_seq bigint not null
		constraint posts_boards_seq_fk
			references "Boards"
				on delete cascade,
	thread bigint not null,
	depth integer not null,
	author varchar(80) not null
		constraint posts_members_email_fk
			references "Members"
				on delete cascade,
	subject varchar(80) not null,
	hit_count integer default 0 not null,
	content text,
	author_ip varchar(50) not null,
	write_date timestamp default now() not null,
	update_date timestamp default now() not null
);
create table "Permissions"
(
  permission_code SMALLINT not null
    constraint "Permissions_pkey"
    primary key,
  active boolean default true not null,
  content varchar(80) not null
);
create table "Attachements"
(
	seq bigserial not null
		constraint "Attachements_pkey"
			primary key,
	file_name varchar(127) not null,
	file_path varchar(255) not null,
	mime_type varchar(255) not null,
	size bigint not null,
	container_seq bigint not null
		constraint attachments_container_seq_fk
			references "Posts"
				on update restrict on delete cascade,
	uploaded_date timestamp default now() not null
);


# --- !Downs

DROP TABLE "Permissions";
DROP TABLE "Posts";
DROP TABLE "Boards";
DROP TABLE "Members";