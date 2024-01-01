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
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This code creates a `FreeColSQLReader` class that connects to an
 * SQLite database and is capable of executing queries.
 * It maintains a connection to the SQLite file and a statement object used to
 * execute the queries. The `ReadScopeType` is also included in the implementation,
 * similar to the XML implementation.
 */
public class FreeColSQLReader implements Closeable {

    private static final Logger logger = Logger.getLogger(FreeColSQLReader.class.getName());

    /**
     * Enum representing the scope of the data reading.
     */
    public enum ReadScopeType {
        CLIENT, // Only the client-visible information
        SERVER, // Full server-visible information
        LOAD    // Absolutely everything needed to load the game state
    }

    private Connection connection;
    private Statement statement;
    private final ReadScopeType readScope;

    public FreeColSQLReader(String dbPath, ReadScopeType scope) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        this.statement = connection.createStatement();
        this.readScope = (scope == null) ? ReadScopeType.LOAD : scope;
    }

    /**
     * Executes a query and returns the result set.
     *
     * @param query SQL query to execute.
     * @return ResultSet containing the result of the query.
     * @throws SQLException If the query execution fails.
     */
    public ResultSet executeQuery(String query) throws SQLException {
        return statement.executeQuery(query);
    }

    @Override
    public void close() {
        try {
            if (statement != null) statement.close();
            if (connection != null) connection.close();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to close resources", e);
        }
    }
}