package net.sf.freecol.common.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SQLInteractionsTests {

    private static final String DB_PATH = "test_db.sqlite";

    private FreeColSQLWriter writer;
    private FreeColSQLReader reader;
    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        writer = new FreeColSQLWriter(DB_PATH, FreeColSQLWriter.WriteScopeType.SAVE);
        reader = new FreeColSQLReader(DB_PATH, FreeColSQLReader.ReadScopeType.CLIENT);
        connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);

        // Create the necessary tables for testing
        connection.createStatement().execute(
                "CREATE TABLE IF NOT EXISTS game (id INTEGER, options TEXT, turn INTEGER);"
        );
    }

    @AfterEach
    public void tearDown() throws IOException, SQLException {
        writer.close();
        reader.close();
        connection.close();

        // Remove the test database file
        File dbFile = new File(DB_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    public void testSaveGame() throws SQLException {
        Game game = new Game(1, "options");
        game.setTurn(5);

        writer.saveGameData(game);

        ResultSet resultSet = reader.executeQuery("SELECT * FROM game WHERE id = 1");
        resultSet.next();

        int turn = resultSet.getInt("turn");
        String options = resultSet.getString("options");

        assertEquals(5, turn, "Turn value should be 5");
        assertEquals("options", options, "Game options should be 'options'");
    }

    @Test
    public void testSavePlayer() throws SQLException {
        Game game = new Game(1, "options");
        game.addPlayer(new Player(game, "player1", "nation1"));
        game.addPlayer(new Player(game, "player2", "nation2"));

        writer.saveGameData(game);

        ResultSet resultSet = reader.executeQuery("SELECT * FROM player");
        int playerCount = 0;
        while (resultSet.next()) {
            String playerName = resultSet.getString("name");
            String nation = resultSet.getString("nation");

            assertEquals("player" + (playerCount + 1), playerName, "Check saved player name");
            assertEquals("nation" + (playerCount + 1), nation, "Check saved player nation");

            playerCount++;
        }

        assertEquals(2, playerCount, "There should be exactly 2 players saved in the database");
    }

    @Test
    public void testSaveTile() throws SQLException {
        Game game = new Game(1, "options");

        // Add some tiles to the game map
        game.getMap().addTile(new Tile(1, 1, "grass", "player1"));
        game.getMap().addTile(new Tile(1, 2, "forest", "player2"));

        writer.saveGameData(game);

        ResultSet resultSet = reader.executeQuery("SELECT * FROM tile");
        int tileCount = 0;
        while (resultSet.next()) {
            int x = resultSet.getInt("x");
            int y = resultSet.getInt("y");
            String terrainType = resultSet.getString("terrain_type");
            String owner = resultSet.getString("owner");

            assertEquals(1, x, "Check saved tile x-coordinate");
            assertEquals(tileCount + 1, y, "Check saved tile y-coordinate");
            assertEquals("player" + (tileCount + 1), owner, "Check saved tile owner");

            if (tileCount == 0) {
                assertEquals("grass", terrainType, "First tile should be grass");
            } else {
                assertEquals("forest", terrainType, "Second tile should be forest");
            }

            tileCount++;
        }

        assertEquals(2, tileCount, "There should be exactly 2 tiles saved in the database");
    }

    @Test
    public void testLoadGame() throws SQLException {
        // Save test game data first
        Game game = new Game(1, "options");
        game.setTurn(5);
        writer.saveGameData(game);

        // Now load it using reader
        Game loadedGame = reader.readGame();

        assertEquals(1, loadedGame.getId(), "Loaded game ID should be 1");
        assertEquals("options", loadedGame.getGameOptions().toString(), "Loaded game options should be 'options'");
        assertEquals(5, loadedGame.getTurn(), "Loaded game turn should be 5");
    }

    @Test
    public void testLoadPlayers() throws SQLException {
        // Save test game data first
        Game game = new Game(1, "options");
        game.addPlayer(new Player(game, "player1", "nation1"));
        game.addPlayer(new Player(game, "player2", "nation2"));
        writer.saveGameData(game);

        // Now load the saved game using the reader
        Game loadedGame = reader.readGame();

        assertEquals(game.getPlayers().size(), loadedGame.getPlayers().size(), "Both saved and loaded games have the same number of players");

        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player originalPlayer = game.getPlayers().get(i);
            Player loadedPlayer = loadedGame.getPlayers().get(i);

            assertEquals(originalPlayer.getId(), loadedPlayer.getId(), "Both saved and loaded player IDs should match");
            assertEquals(originalPlayer.getName(), loadedPlayer.getName(), "Both saved and loaded player names should match");
            assertEquals(originalPlayer.getNation().getName(), loadedPlayer.getNation().getName(), "Both saved and loaded player nations should match");
        }
    }

    // You can add more tests for loading tiles and other game components similarly
}