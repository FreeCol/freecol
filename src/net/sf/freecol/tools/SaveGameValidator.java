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

package net.sf.freecol.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;


/**
 * Validate a saved game.
 */
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SaveGameValidator {

    /**
     * Main method to validate game save.
     *
     * @param args command line arguments containing the SQLite database file paths
     * @throws Exception if there is any error during execution
     */
    public static void main(String[] args) throws Exception {
        // Load SQLite JDBC driver
        Class.forName("org.sqlite.JDBC");

        List<File> allFiles = new ArrayList<>();
        for (String name : args) {
            File file = new File(name);
            if (file.exists()) {
                if (file.isDirectory()) {
                    // You need to implement getSQLiteDatabaseFileList method to list SQLite files in the directory
                    allFiles.addAll(getSQLiteDatabaseFileList(file));
                } else {
                    // You can use any custom file filter, if required
                    allFiles.add(file);
                }
            }
        }

        int ret = 0;
        for (File file : allFiles) {
            System.out.println("Processing file " + file.getPath());
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath())) {
                ret = validateSaveGame(connection) ? 0 : Math.max(ret, 1);
                System.out.println("Successfully validated " + file.getName());
            } catch (Exception e) {
                System.out.println("Failed to read or validate " + file.getName());
                ret = 2;
            }
        }
        System.exit(ret);
    }

    /**
     * Validates game save based on the SQLite database file.
     *
     * @param connection SQLite database connection
     * @return true if the game save is valid, false otherwise
     * @throws Exception if any error occurs during validation
     */
    private static boolean validateSaveGame(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();
        ResultSet rs;

        // Validate game table
        rs = stmt.executeQuery("SELECT * FROM game");
        if (!rs.next()) {
            System.out.println("No game data found in the 'game' table");
            return false;
        }
        rs.close();

        // Validate player table
        rs = stmt.executeQuery("SELECT * FROM player");
        if (!rs.next()) {
            System.out.println("No player data found in the 'player' table");
            return false;
        }
        rs.close();
        

        // Validate natives table
        rs = stmt.executeQuery("SELECT * FROM natives");
        if (!rs.next()) {
            System.out.println("No native data found in the 'natives' table");
            return false;
        }
        rs.close();

        // Validate factions table
        rs = stmt.executeQuery("SELECT * FROM factions");
        if (!rs.next()) {
            System.out.println("No faction data found in the 'factions' table");
            return false;
        }
        rs.close();

        // Validate building table
        rs = stmt.executeQuery("SELECT * FROM building");
        if (!rs.next()) {
            System.out.println("No building data found in the 'building' table");
            return false;
        }
        rs.close();

        // Validate goods table
        rs = stmt.executeQuery("SELECT * FROM goods");
        if (!rs.next()) {
            System.out.println("No goods data found in the 'goods' table");
            return false;
        }
        rs.close();

        // Validate highScores table
        rs = stmt.executeQuery("SELECT * FROM highScores");
        if (!rs.next()) {
            System.out.println("No highScores data found in the 'highScores' table");
            return false;
        }
        rs.close();

        // Validate foundingFathers table
        rs = stmt.executeQuery("SELECT * FROM foundingFathers");
        if (!rs.next()) {
            System.out.println("No foundingFathers data found in the 'foundingFathers' table");
            return false;
        }
        rs.close();

        stmt.close();

        // All validations passed
        return true;
    }

    /**
     * Gets a list of SQLite database files in the given directory.
     *
     * @param directory Directory containing the SQLite database files
     * @return a List of SQLite database files
     */
    private static List<File> getSQLiteDatabaseFileList(File directory) {
        List<File> sqliteFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().toLowerCase().endsWith(".sqlite")) {
                    sqliteFiles.add(file);
                }
            }
        }

        return sqliteFiles;
    }
}
