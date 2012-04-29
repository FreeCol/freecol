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

package net.sf.freecol.common.networking;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when renaming a FreeColGameObject.
 */
public class RenameMessage extends DOMMessage {

    /**
     * The id of the object to be renamed.
     */
    private String id;

    /**
     * The new name.
     */
    private String newName;

    /**
     * Create a new <code>RenameMessage</code> with the
     * supplied name.
     *
     * @param object The <code>FreeColGameObject</code> to rename.
     * @param newName The new name for the object.
     */
    public RenameMessage(FreeColGameObject object, String newName) {
        this.id = object.getId();
        this.newName = newName;
    }

    /**
     * Create a new <code>RenameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public RenameMessage(Game game, Element element) {
        this.id = element.getAttribute("nameable");
        this.newName = element.getAttribute("name");
    }

    /**
     * Handle a "rename"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the renamed unit,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = player.getGame();

        FreeColGameObject fcgo;
        try {
            fcgo = player.getFreeColGameObject(id, FreeColGameObject.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (!(fcgo instanceof Nameable)) {
            return DOMMessage.clientError("Not a nameable: " + id);
        }

        // Proceed to rename.
        return server.getInGameController()
            .renameObject(serverPlayer, (Nameable)fcgo, newName);
    }

    /**
     * Convert this RenameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "nameable", id,
            "name", newName);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "rename".
     */
    public static String getXMLElementTagName() {
        return "rename";
    }
}
