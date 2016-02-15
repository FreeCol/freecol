/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when renaming a FreeColGameObject.
 */
public class RenameMessage extends DOMMessage {

    public static final String TAG = "rename";
    private static final String NAMEABLE_TAG = "nameable";
    private static final String NAME_TAG = "name";

    /** The identifier of the object to be renamed. */
    private final String id;

    /** The new name. */
    private final String newName;


    /**
     * Create a new <code>RenameMessage</code> with the
     * supplied name.
     *
     * @param object The <code>FreeColGameObject</code> to rename.
     * @param newName The new name for the object.
     */
    public RenameMessage(FreeColGameObject object, String newName) {
        super(getTagName());

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
        super(getTagName());

        this.id = getStringAttribute(element, NAMEABLE_TAG);
        this.newName = getStringAttribute(element, NAME_TAG);
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
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        FreeColGameObject fcgo;
        try {
            fcgo = player.getOurFreeColGameObject(this.id, FreeColGameObject.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!(fcgo instanceof Nameable)) {
            return serverPlayer.clientError("Not a nameable: " + this.id)
                .build(serverPlayer);
        }

        // Proceed to rename.
        return server.getInGameController()
            .renameObject(serverPlayer, (Nameable)fcgo, this.newName)
            .build(serverPlayer);
    }

    /**
     * Convert this RenameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            NAMEABLE_TAG, this.id,
            NAME_TAG, this.newName).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "rename".
     */
    public static String getTagName() {
        return TAG;
    }
}
