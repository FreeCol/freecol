/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.io.File;
import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.util.test.FreeColTestCase;

public class ServerTest extends FreeColTestCase {
    
    private static final String SERVER_NAME = "MyTestServer";
    private static final int SERVER_PORT = FreeCol.getDefaultPort();

    
    private FreeColServer startServer(boolean publicServer, boolean singleplayer, int port, String name) 
    {
        FreeColServer server = null;
        try {
            server = new FreeColServer(publicServer, singleplayer, port, name);
        } catch (NoRouteToServerException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
        assertNotNull(server);
        assertEquals(FreeColServer.GameState.STARTING_GAME, server.getGameState());
        return server;
    }
    
    private FreeColServer startServer(File file, boolean publicServer, boolean singleplayer, int port, String name)
    {
        FreeColServer server = null;
        try {
            server = new FreeColServer(file, publicServer, singleplayer, port, name);
        } catch (NoRouteToServerException e) {
            fail();
        } catch (FreeColException e) {
            fail();
        } catch (IOException e) {
            fail();
        }
        assertNotNull(server);
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        return server;
    }
    
    public void testServer() {
        
        // start a server
        FreeColServer server = startServer(false, true, SERVER_PORT, SERVER_NAME);

        // generate a random map
        Controller c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController)c;
        pgc.startGame();
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        
        // save the game as a file
        File file = new File("test/data/test.fsg");
        try {
            server.saveGame(file, "user");
        } catch (IOException e) {
            fail();
        }
        assertTrue(file.exists());
        
        // stop the server
        c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof InGameController);
        InGameController ic = (InGameController)c;
        ic.shutdown();
        
        // start a new server and read the file back
        server = startServer(false, true, SERVER_PORT, SERVER_NAME);
        try {
            server.loadGame(file);
        } catch (Exception e) {
            fail();
        }
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        
    }
}
