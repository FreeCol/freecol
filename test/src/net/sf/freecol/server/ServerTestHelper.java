/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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
package net.sf.freecol.server;

import junit.framework.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.model.ServerGame;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockMapGenerator;


public final class ServerTestHelper {

    private static final String SERVER_NAME = "MyTestServer";
    private static final int SERVER_PORT = -1;
    private static final String TEST_FILE = "test/data/test.fsg";

    private static FreeColServer server = null;
    private static Random random = null;


    public static FreeColServer getServer() {
        return server;
    }

    public static void setServer(FreeColServer newServer) {
        server = newServer;
    }

    public static InGameController getInGameController() {
        return server.getInGameController();
    }

    public static void stopServer() {
        if (server != null) {
            Controller c = server.getController();
            assertNotNull(c);
            c.shutdown();
            server = null;
        }
    }

    public static FreeColServer startServer(boolean publicServer,
                                            boolean singlePlayer) {
        return startServer(publicServer, singlePlayer, FreeColTestCase.spec());
    }

    public static FreeColServer startServer(boolean publicServer,
                                            boolean singlePlayer,
                                            Specification spec) {
        return startServer(publicServer, singlePlayer, spec,
                           SERVER_PORT, SERVER_NAME);
    }

    public static FreeColServer startServer(boolean publicServer,
                                            boolean singlePlayer,
                                            Specification spec,
                                            int port, String name) {
        stopServer();
        try {
            // FIXME: Pass tc
            server = new FreeColServer(publicServer, singlePlayer,
                                       spec, port, name);
        } catch (IOException ex) {
            fail(ex.getMessage());
        }
        assertNotNull(server);
        assertEquals(FreeColServer.GameState.STARTING_GAME,
                     server.getGameState());
        return server;
    }

    public static FreeColServer startServer(File file, boolean publicServer,
                                            boolean singlePlayer) {
        return startServer(file, publicServer, singlePlayer,
                           SERVER_PORT, SERVER_NAME);
    }

    public static FreeColServer startServer(File file, boolean publicServer,
                                            boolean singlePlayer, int port,
                                            String name) {
        stopServer();
        try {
            server = new FreeColServer(new FreeColSavegameFile(file), 
                                       null, port, name);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertNotNull(server);
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        return server;
    }

    public static File createRandomSaveGame() {
        // start a server
        FreeColServer serv = startServer(false, true);

        // generate a random map
        Controller c = serv.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController) c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail(e.getMessage());
        }
        assertEquals(FreeColServer.GameState.IN_GAME, serv.getGameState());
        assertNotNull(serv.getGame());
        assertNotNull(serv.getGame().getMap());

        // save the game as a file
        File file = new File(TEST_FILE);
        try {
            serv.saveGame(file, null);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        assertTrue(file.exists());
        stopServer();

        return file;
    }


    public static void newTurn() {
        ServerGame game = (ServerGame) server.getGame();
        game.csNewTurn(random, new LogBuilder(0), new ChangeSet());
    }

    /**
     * Start a new server game, using a *copy* of a supplied map.
     *
     * @param map The <code>Map</code> to copy.
     * @return The new running server game.
     */
    public static Game startServerGame(Map map) {
        return startServerGame(map, FreeColTestCase.spec());
    }

    /**
     * Start a new server game, using a *copy* of a supplied map.
     *
     * @param map The <code>Map</code> to copy.
     * @param spec The <code>Specification</code> to use.
     * @return The new running server game.
     */
    public static Game startServerGame(Map map, Specification spec) {
        stopServerGame();
        FreeColServer serv = startServer(false, true, spec);
        serv.setMapGenerator(new MockMapGenerator(serv.getGame(), map));
        PreGameController pgc = (PreGameController) serv.getController();
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail("Failed to start game: " + e.getMessage());
        }

        Game game = serv.getGame();
        FreeColTestCase.setGame(game);
        if (game.getCurrentPlayer() == null) {
            game.setCurrentPlayer(game.getFirstPlayer());
        }
        random = new Random();
        return game;
    }

    public static void stopServerGame() {
        stopServer();
        FreeColTestCase.setGame(null);
    }

    public static void setRandom(Random newRandom) {
        random = newRandom;
    }
}
