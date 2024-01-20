create table lists (
	_id         uuid      primary key default gen_random_uuid(),
	created_at  timestamp not null default current_timestamp,
	title       text      not null default 'Unammed List'
);

create table items (
	_id          uuid      primary key default gen_random_uuid(),
	list_id      uuid      references lists (_id),
	created_at   timestamp not null default current_timestamp,
	title        text      not null default 'Unammed Item',
	done         bool      not null default false
);