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

import java.io.File;
import java.io.IOException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.networking.NoRouteToServerException;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.client.control.ConnectController;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.control.PreGameController;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.util.test.FreeColTestCase;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class ClientTestHelper {

    public static final int port = 3541;
    public static final String username = "test";

    public static FreeColClient startClient() {

        try {
            ImageLibrary imageLibrary = new ImageLibrary();
            FreeColClient client = new FreeColClient(false, null, imageLibrary, null, null);
            ConnectController connectController = client.getConnectController();
            FreeColServer freeColServer = new FreeColServer(false, true, port, null, 4, 0, false);
            client.setFreeColServer(freeColServer);
            client.setSingleplayer(true);
            boolean loggedIn = connectController.login(username, "127.0.0.1", port);
            assertTrue(loggedIn);
            client.getPreGameController().setReady(true);
            client.getClientOptions().putOption(new BooleanOption(ClientOptions.DISPLAY_ANIMATIONS, false));
            assertFalse(client.getClientOptions().getBoolean(ClientOptions.DISPLAY_ANIMATIONS));
            return client;
        } catch (NoRouteToServerException e) {
            fail("Illegal state: An exception occured that can only appear in public multiplayer games.");
        } catch (IOException e) {
            fail("server.couldNotStart");
        } catch(FreeColException e) {
            fail("Failed to create ImageLibrary.");
        }
        return null;
    }

}