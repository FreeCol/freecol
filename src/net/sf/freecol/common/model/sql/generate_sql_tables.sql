-- Create game table
CREATE TABLE IF NOT EXISTS game (
    id INTEGER PRIMARY KEY,
    difficulty TEXT,
    mapFile TEXT,
    gameOptions TEXT,
    language TEXT,
    turns INTEGER,
    graphicalOptions TEXT,
    soundOptions TEXT,
    turn INTEGER
);

-- Create player table
CREATE TABLE IF NOT EXISTS player (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    name TEXT,
    color TEXT,
    flag TEXT,
    playerPreferences TEXT,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- Create native table
CREATE TABLE IF NOT EXISTS natives (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    name TEXT,
    color TEXT,
    type TEXT,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- Create faction table
CREATE TABLE IF NOT EXISTS factions (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    name TEXT,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- ... The existing unit, city, and tile table creation statements remain unchanged

-- Create building table
CREATE TABLE IF NOT EXISTS building (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    city_id INTEGER,
    type TEXT,
    x INTEGER,
    y INTEGER,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE,
    FOREIGN KEY (city_id) REFERENCES city (id) ON DELETE CASCADE
);

-- Create resource (goods) table
CREATE TABLE IF NOT EXISTS goods (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    type TEXT,
    available INTEGER,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);

-- Create highscore table
CREATE TABLE IF NOT EXISTS highScores (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    player_id INTEGER,
    score INTEGER,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE,
    FOREIGN KEY (player_id) REFERENCES player (id) ON DELETE CASCADE
);

-- Create foundingFathers table
CREATE TABLE IF NOT EXISTS foundingFathers (
    id INTEGER PRIMARY KEY,
    game_id INTEGER,
    name TEXT,
    bonus TEXT,
    improvement TEXT,
    FOREIGN KEY (game_id) REFERENCES game (id) ON DELETE CASCADE
);
