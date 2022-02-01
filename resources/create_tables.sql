-- Table: public.triples

-- DROP TABLE public.triples;

CREATE TABLE public.triples
(
    tripleid SERIAL, -- integer NOT NULL DEFAULT nextval('triples_tripleid_seq'::regclass)
    subject text COLLATE pg_catalog."default" NOT NULL,
    predicate text COLLATE pg_catalog."default" NOT NULL,
    object text COLLATE pg_catalog."default" NOT NULL,
    strikes integer DEFAULT 0,
    insertiontime integer,
    timeframe integer,
    CONSTRAINT triples_pkey PRIMARY KEY (subject, predicate, object)
)

    TABLESPACE pg_default;

ALTER TABLE public.triples
    OWNER to postgres;

-- Index: triple_id_index

-- DROP INDEX public.triple_id_index;

CREATE INDEX triple_id_index
    ON public.triples USING btree
    (tripleid)
    TABLESPACE pg_default;



-- #############################

-- Table: public.triplestimeframes

-- DROP TABLE public.triplestimeframes;

CREATE TABLE public.triplestimeframes
(
    tripleid integer,
    timeframe integer,
    strikes integer
)

    TABLESPACE pg_default;

ALTER TABLE public.triplestimeframes
    OWNER to postgres;

-- Index: triplestimeframes_id_index

-- DROP INDEX public.triplestimeframes_id_index;

CREATE INDEX triplestimeframes_id_index
    ON public.triplestimeframes USING btree
        (tripleid)
    TABLESPACE pg_default;


-- ############################

-- Table: public.lineage_cache

-- DROP TABLE public.lineage_cache;

CREATE TABLE public.lineage_cache
(
    query text COLLATE pg_catalog."default",
    subject text COLLATE pg_catalog."default",
    predicate text COLLATE pg_catalog."default",
    object text COLLATE pg_catalog."default"
)

    TABLESPACE pg_default;

ALTER TABLE public.lineage_cache
    OWNER to postgres;

-- Index: lineage_cache_index

-- DROP INDEX public.lineage_cache_index;

CREATE INDEX lineage_cache_index
    ON public.lineage_cache USING btree
        (query COLLATE pg_catalog."default")
    TABLESPACE pg_default;