CREATE TABLE movie_vote (
    user_id  bigint NOT NULL,
    movie_id bigint NOT NULL,
    liked    boolean NOT NULL,
    voted_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, movie_id)
);
