/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.generator.MapGenerator;
import net.sf.freecol.util.test.FreeColTestCase;


public class SaveLoadTest extends FreeColTestCase {
    
    private FreeColServer server = null;

    @Override
    public void tearDown() throws Exception {
        if (server != null) { // Make sure that the server is stopped.
            ServerTestHelper.stopServer(server);
            server = null;
        }
        super.tearDown();
    }

    public void testDelayedLoading() {
        File file = ServerTestHelper.createRandomSaveGame();
        ServerTestHelper.stopServer(server);

        server = ServerTestHelper.startServer(false, true);
        try {
            server.loadGame(new FreeColSavegameFile(file));
        } catch (Exception e) {
            fail();
        }
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
    }

    public void testImmediateLoading() {
        File file = ServerTestHelper.createRandomSaveGame();
        ServerTestHelper.stopServer(server);

        server = ServerTestHelper.startServer(file, false, true);
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
    }
    
    public void testImport() {
        File file = ServerTestHelper.createRandomSaveGame();
        ServerTestHelper.stopServer(server);

        server = ServerTestHelper.startServer(false, true);
        MapGenerator mapGenerator = server.getMapGenerator();
        ((FileOption) mapGenerator.getMapGeneratorOptions()
            .getOption(MapGeneratorOptions.IMPORT_FILE)).setValue(file);
        Controller c = server.getController();
        assertNotNull(c);
        assertTrue(c instanceof PreGameController);
        PreGameController pgc = (PreGameController)c;
        try {
            pgc.startGame();
        } catch (FreeColException e) {
            fail(e.getMessage());
        }

        assertEquals(FreeColServer.GameState.IN_GAME, server.getGameState());
        assertNotNull(server.getGame());
        assertNotNull(server.getGame().getMap());
        file.delete();
        assertFalse(file.exists());
    }
}
