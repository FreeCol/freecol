/**
 *  Copyright (C) 2002-2022   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.io;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * `FreeColSQLWriter` class to save game data into an SQLite database.
 * It consists of the `saveGameData` method that saves general game data,
 * player data, turn data, and tile data. Additional game components can be saved
 * in a similar manner using prepared statements. The `close()`
 * method ensures that the database connection is closed when done.
 */
public class FreeColSQLWriter implements Closeable {

    private static final Logger logger = Logger.getLogger(FreeColSQLWriter.class.getName());

    /**
     * Enum representing the scope of the data writing.
     */
    public enum WriteScopeType {
        CLIENT,  // Only the client-visible information
        SERVER,  // Full server-visible information
        SAVE     // Absolutely everything needed to save the game state
    };

    private Connection connection;
    private WriteScopeType writeScope;

    public FreeColSQLWriter(String dbPath, WriteScopeType scope) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.writeScope = (scope == null) ? WriteScopeType.SAVE : scope;
    }

    /**
     * Executes an SQL update statement.
     *
     * @param update SQL update statement to execute.
     * @throws SQLException If the statement execution fails.
     */
    public void executeUpdate(String update) throws SQLException {
        try (PreparedStatement pstmt = connection.prepareStatement(update)) {
            pstmt.executeUpdate();
        }
    }

    /**
     * Save game data to the SQLite database.
     *
     * @param game The Game object to save.
     * @throws SQLException If any error occurs during saving.
     */
    public void saveGameData(Game game) throws SQLException {
        // Save general game data
        String sqlGame = "INSERT OR REPLACE INTO game (id, options) VALUES(?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlGame)) {
            pstmt.setInt(1, game.getId());
            pstmt.setString(2, game.getGameOptions().toString());
            pstmt.executeUpdate();
        }

        // Save player data
        String sqlPlayer = "INSERT OR REPLACE INTO player (id, name, nation) VALUES(?, ?, ?)";
        for (Player player : game.getPlayers()) {
            try (PreparedStatement pstmt = connection.prepareStatement(sqlPlayer)) {
                pstmt.setInt(1, player.getId());
                pstmt.setString(2, player.getName());
                pstmt.setString(3, player.getNation().getName());
                pstmt.executeUpdate();
            }
        }

        // Save turn data
        String sqlTurn = "UPDATE game SET turn = ? WHERE id
        // Save turn data
        String sqlTurn = "UPDATE game SET turn = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sqlTurn)) {
            pstmt.setInt(1, game.getTurn());
            pstmt.setInt(2, game.getId());
            pstmt.executeUpdate();
        }

        // Save tile data
        String sqlTile = "INSERT OR REPLACE INTO tile (x, y, terrain_type, owner, game_id) VALUES(?, ?, ?, ?, ?)";
        for (Tile tile : game.getMap().getAllTiles()) {
            try (PreparedStatement pstmt = connection.prepareStatement(sqlTile)) {
                pstmt.setInt(1, tile.getX());
                pstmt.setInt(2, tile.getY());
                pstmt.setString(3, tile.getTerrainType().getName());
                pstmt.setString(4, tile.getOwner().getName());
                pstmt.setInt(5, game.getId());
                pstmt.executeUpdate();
            }
        }

        // Additional game components can be saved similarly with prepared statements
    }

    @Override
    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close resources", e);
        }
    }
}