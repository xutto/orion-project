CREATE TABLE IF NOT EXISTS FILE(
                      ID INTEGER PRIMARY KEY,               -- Columna para el hash (PK)
                      HASH TEXT,
                      NAME TEXT,                -- Columna para el nombre del archivo
                      PATH TEXT,                -- Columna para la ruta del archivo
                      SIZE INTEGER,              -- Columna para el tamaño del archivo
                      STATUS TEXT DEFAULT 'STORED',
                      CONSTRAINT unique_path_hash UNIQUE (PATH, HASH)
);
CREATE INDEX IF NOT EXISTS idx_hash ON FILE (HASH);
CREATE INDEX IF NOT EXISTS idx_name ON FILE (NAME);
CREATE INDEX IF NOT EXISTS idx_size ON FILE (SIZE);

CREATE TABLE IF NOT EXISTS NODE_CONFIG(
    ID INTEGER PRIMARY KEY,
    PORT INTEGER NOT NULL DEFAULT 4050
);
INSERT OR IGNORE INTO NODE_CONFIG (ID, PORT) VALUES (1, 4050);
