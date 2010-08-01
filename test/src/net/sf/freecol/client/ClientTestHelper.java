/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

import static junit.framework.Assert.assertTrue;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.server.FreeColServer;

public class ClientTestHelper {

    public static final int port = 3541;
    public static final String username = "test";

    public static FreeColClient startClient(FreeColServer freeColServer) {

        //ImageLibrary imageLibrary = new ImageLibrary();
        FreeColClient client = new FreeColClient(null, null, false, null, false, null);
        ConnectController connectController = client.getConnectController();
        client.setFreeColServer(freeColServer);
        client.setSingleplayer(true);
        client.setHeadless(true);
        boolean connected = connectController.login(username, "127.0.0.1", port);
        assertTrue(connected);
        client.getPreGameController().setReady(true);
        //client.getClientOptions().putOption(new RangeOption(ClientOptions.ANIMATION_SPEED, 0));
        //assertEquals(0, client.getClientOptions().getInt(ClientOptions.ANIMATION_SPEED));
        return client;
    }
    
    public static void stopClient(FreeColClient client) {
        
        client.getConnectController().quitGame(false);
    }

}