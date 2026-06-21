CREATE TABLE movie (
    id                bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title             text NOT NULL,
    original_title    text NOT NULL,
    overview          text,
    release_year      smallint NOT NULL,
    runtime_minutes   smallint NOT NULL,
    original_language char(2),
    poster_path       text,
    backdrop_path     text,
    tmdb_id           bigint NOT NULL,
    CONSTRAINT uq_movie_tmdb_id UNIQUE (tmdb_id)
);

CREATE TABLE person (
    id   int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE genre (
    id   int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE keyword (
    id   int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name text NOT NULL UNIQUE
);

CREATE TABLE movie_director (
    movie_id  bigint   NOT NULL REFERENCES movie (id)       ON DELETE CASCADE,
    person_id int      NOT NULL REFERENCES person (id)    ON DELETE CASCADE,
    position  smallint NOT NULL CHECK (position >= 1),
    PRIMARY KEY (movie_id, position),
    CONSTRAINT uq_movie_director_person UNIQUE (movie_id, person_id)
);

CREATE TABLE movie_cast (
    movie_id  bigint   NOT NULL REFERENCES movie (id)       ON DELETE CASCADE,
    person_id int      NOT NULL REFERENCES person (id)    ON DELETE CASCADE,
    position  smallint NOT NULL CHECK (position >= 1),
    PRIMARY KEY (movie_id, position),
    CONSTRAINT uq_movie_cast_person UNIQUE (movie_id, person_id)
);

CREATE TABLE movie_genre (
    movie_id bigint   NOT NULL REFERENCES movie (id)       ON DELETE CASCADE,
    genre_id int      NOT NULL REFERENCES genre (id)      ON DELETE CASCADE,
    position smallint NOT NULL CHECK (position >= 1),
    PRIMARY KEY (movie_id, position),
    CONSTRAINT uq_movie_genre_genre UNIQUE (movie_id, genre_id)
);

CREATE TABLE movie_keyword (
    movie_id   bigint   NOT NULL REFERENCES movie (id)       ON DELETE CASCADE,
    keyword_id int      NOT NULL REFERENCES keyword (id)      ON DELETE CASCADE,
    position   smallint NOT NULL CHECK (position >= 1),
    PRIMARY KEY (movie_id, position),
    CONSTRAINT uq_movie_keyword_keyword UNIQUE (movie_id, keyword_id)
);

CREATE INDEX idx_movie_director_person ON movie_director (person_id);
CREATE INDEX idx_movie_cast_person     ON movie_cast (person_id);
CREATE INDEX idx_movie_genre_genre     ON movie_genre (genre_id);
CREATE INDEX idx_movie_keyword_kw      ON movie_keyword (keyword_id);

CREATE TABLE app_user (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      varchar(64) NOT NULL UNIQUE,
    password_hash text NOT NULL,
    role          varchar(16) NOT NULL DEFAULT 'USER'
);
