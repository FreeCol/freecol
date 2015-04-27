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

package net.sf.freecol.client;

import junit.framework.*;
import static org.junit.Assert.*;

import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.server.FreeColServer;


public class ClientTestHelper {

    public static final int port = 3541;

    public static FreeColClient startClient(FreeColServer freeColServer) {
        // This is not ideal, but headless mode allows cutting off
        // some excessive resource loading, especially in the sound
        // tests where the resource manager is exercised.
        System.setProperty("java.awt.headless", "true"); 

        FreeColClient client = new FreeColClient(null, null);
        client.startClient(null, null, false, false, null, null);
        ConnectController connectController = client.getConnectController();
        client.setFreeColServer(freeColServer);
        client.setSinglePlayer(true);
        boolean connected = connectController.login("test",
            freeColServer.getHost(), freeColServer.getPort());
        assertTrue(connected);
        client.getPreGameController().setReady(true);
        return client;
    }
    
    public static void stopClient(FreeColClient client) {
        client.getConnectController().quitGame(false);
    }
}
