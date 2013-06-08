/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.client.control;

import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.MessageHandler;

import org.w3c.dom.Element;


/**
 * Provides common methods for input handlers.
 */
public abstract class InputHandler implements MessageHandler {

    private static final Logger logger = Logger.getLogger(InputHandler.class.getName());

    /** The main FreeCol client object. */
    private final FreeColClient freeColClient;

    /** The GUI to display to. */
    protected GUI gui;


    /**
     * The constructor to use.
     *
     * @param freeColClient The main <code>FreeColClient</code> object.
     * @param gui The <code>GUI</code> to display to.
     */
    public InputHandler(FreeColClient freeColClient, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;
    }


    /**
     * Gets the main freecol client object.
     *
     * @return The main freecol client object.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Gets the Game.
     *
     * @return The <code>Game</code>.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Deals with incoming messages that have just been received.
     *
     * @param connection The <code>Connection</code> the message was
     *     received on.
     * @param element The root <code>Element</code> of the message.
     * @return The reply.
     */
    public abstract Element handle(Connection connection, Element element);


    // Useful handlers

    /**
     * Handles a "disconnect"-message.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    protected Element disconnect(Element element) {
        // Updating the GUI should always be done in the EDT:
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (gui.containsInGameComponents()) {
                        if (freeColClient.getFreeColServer() == null) {
                            gui.returnToTitle();
                        } else {
                            gui.removeInGameComponents();
                        }
                    }
                }
            });

        return null;
    }

    /**
     * Handles a message of unknown type.
     *
     * @param element The element (root element in a DOM-parsed XML tree) that
     *                holds all the information.
     * @return Null.
     */
    protected Element unknown(Element element) {
        logger.warning("Unknown message type: " + element.getTagName());
        return null;
    }
}
