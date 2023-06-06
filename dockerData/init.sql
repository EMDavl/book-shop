create sequence if not exists books_id_seq START WITH 1;
create sequence if not exists authors_id_seq START WITH 1;

create table authors
(
    id numeric not null default nextval('authors_id_seq'::regclass),
    first_name varchar(256) not null,
    last_name varchar(256) not null,

    PRIMARY KEY (id)
);

create table books
(
    id           numeric      not null default nextval('books_id_seq'::regclass),
    book_name    varchar(512) not null,
    author_id    numeric      not null REFERENCES authors (id),
    publish_date varchar(128),

    PRIMARY KEY (id)
);

insert into authors(first_name, last_name) VALUES ('Emil', 'Davlyatov'),
                                                  ('Davlyat', 'Emilev'),
                                                  ('Emda', 'Ilvlyatov'),
                                                  ('Lime', 'Votaylvad');

insert into books(book_name, author_id, publish_date) VALUES
                                                          ('How to do whole project in one day', 1, now()),
                                                          ('how to stay up all night because you left a project to the last day', 2, now()),
                                                          ('Start postponing things for later, rest now', 3, now()),
                                                          ('Start early? Am I weak?', 1, now())