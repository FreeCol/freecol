/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when naming a new land.
 */
public class NewLandNameMessage extends DOMMessage {

    /** The unit that has come ashore. */
    private final String unitId;

    /** The name to use. */
    private final String newLandName;


    /**
     * Create a new <code>NewLandNameMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> that has come ashore.
     * @param newLandName The new land name.
     */
    public NewLandNameMessage(Unit unit, String newLandName) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.newLandName = newLandName;
    }

    /**
     * Create a new <code>NewLandNameMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NewLandNameMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.newLandName = element.getAttribute("newLandName");
    }


    // Public interface

    /**
     * Public accessor for the unit.
     *
     * @param player The <code>Player</code> who owns the unit.
     * @return The <code>Unit</code> of this message.
     */
    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(unitId, Unit.class);
    }

    /**
     * Public accessor for the new land name.
     *
     * @return The new land name of this message.
     */
    public String getNewLandName() {
        return newLandName;
    }


    /**
     * Handle a "newLandName"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update setting the new land name, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = getUnit(player);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return DOMMessage.clientError("Unit is not on the map: " + unitId);
        } else if (!tile.isLand()) {
            return DOMMessage.clientError("Unit is not in the new world: "
                + unitId);
        }

        if (newLandName == null || newLandName.isEmpty()) {
            return DOMMessage.clientError("Empty new land name");
        }

        // Set name.
        return server.getInGameController()
            .setNewLandName(serverPlayer, unit, newLandName);
    }

    /**
     * Convert this NewLandNameMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "newLandName", newLandName);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "newLandName".
     */
    public static String getXMLElementTagName() {
        return "newLandName";
    }
}
