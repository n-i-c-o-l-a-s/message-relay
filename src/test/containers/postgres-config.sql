CREATE ROLE messagerelay WITH
  LOGIN
  NOSUPERUSER
  INHERIT
  NOCREATEDB
  NOCREATEROLE
  NOREPLICATION
  ENCRYPTED PASSWORD 'md54cf64e155ec4170d094ff99487216aa0';

CREATE TABLE public.outbox
(
    id bigserial NOT NULL,
    key bytea NOT NULL,
    value bytea NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE public.outbox OWNER to admin;

GRANT SELECT, DELETE ON TABLE public.outbox TO messagerelay;

INSERT INTO outbox(key,value) VALUES ('1','1');
INSERT INTO outbox(key,value) VALUES ('2','2');
INSERT INTO outbox(key,value) VALUES ('3','3');
INSERT INTO outbox(key,value) VALUES ('4','4');
INSERT INTO outbox(key,value) VALUES ('5','5');
INSERT INTO outbox(key,value) VALUES ('6','6');
INSERT INTO outbox(key,value) VALUES ('7','7');
INSERT INTO outbox(key,value) VALUES ('8','8');
INSERT INTO outbox(key,value) VALUES ('9','9');
INSERT INTO outbox(key,value) VALUES ('10','10');
INSERT INTO outbox(key,value) VALUES ('11','11');
INSERT INTO outbox(key,value) VALUES ('12','12');
INSERT INTO outbox(key,value) VALUES ('13','13');
INSERT INTO outbox(key,value) VALUES ('14','14');
INSERT INTO outbox(key,value) VALUES ('15','15');
INSERT INTO outbox(key,value) VALUES ('16','16');
