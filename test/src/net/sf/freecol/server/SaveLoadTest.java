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

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.generator.IMapGenerator;
import net.sf.freecol.server.generator.MapGeneratorOptions;

public class SaveLoadTest extends ServerTest {

    public void testDelayedLoading() {
        
        File file = createRandomSaveGame();
        
        // start a new server and read the file back
        FreeColServer server = startServer(false, true, SERVER_PORT, SERVER_NAME);
        try {
            server.loadGame(file);
        } catch (Exception e) {
            fail();
        }
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
        
        stopServer(server);
    }

    public void testImmediateLoading() {
        
        File file = createRandomSaveGame();
        
        // start a new server and read the file back
        FreeColServer server = startServer(file, false, true, SERVER_PORT, SERVER_NAME);
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
        
        stopServer(server);
    }
    
    public void testImport() {
        
        File file = createRandomSaveGame();
        
        // start a new server and import the file
        FreeColServer server = startServer(false, true, SERVER_PORT, SERVER_NAME);
        IMapGenerator mapGenerator = server.getMapGenerator();
        mapGenerator.getMapGeneratorOptions().setFile(MapGeneratorOptions.IMPORT_FILE, file);
        Controller c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            stopServer(server);
            fail();
        }
        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
        
        stopServer(server);
    }
    
}
