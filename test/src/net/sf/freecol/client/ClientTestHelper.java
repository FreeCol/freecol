/**
 *  Copyright (C) 2002-2019  The FreeCol Team
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

package net.sf.freecol.client;

import java.util.logging.Logger;

import static org.junit.Assert.*;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.common.model.Game.LogoutReason;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.common.debug.FreeColDebugger;


public class ClientTestHelper {

    public static final int port = 3541;

    private static final Logger logger = Logger.getLogger(FreeColClient.class.getName());

    private static FreeColClient client = null;

    public static FreeColClient startClient(FreeColServer freeColServer, Specification specification) {
        // This is not ideal, but headless mode allows cutting off
        // some excessive resource loading, especially in the sound
        // tests where the resource manager is exercised.
        System.setProperty("java.awt.headless", "true");
        FreeColDebugger.enableDebugMode(FreeColDebugger.DebugMode.MENUS);
        FreeColDebugger.setDebugRunTurns(1);

        FreeCol.setLocale(null);
        Messages.loadMessageBundle(FreeCol.getLocale());

        logger.info("Debug value: " + FreeColDebugger.isInDebugMode());

        client = FreeColClient.startTestClient(specification);
        assertNotNull(client);

        ConnectController connectController = client.getConnectController();
        client.setFreeColServer(freeColServer);
        client.setSinglePlayer(true);

        assertTrue(connectController.requestLogin("test",
                   freeColServer.getHost(),
                   freeColServer.getPort()));

        connectController.startSinglePlayerGame(specification);
        return client;
    }
    
    public static void stopClient(FreeColClient client) {
        client.getConnectController().requestLogout(LogoutReason.QUIT);
    }
}
