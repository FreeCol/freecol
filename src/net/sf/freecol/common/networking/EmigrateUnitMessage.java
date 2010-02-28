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

package net.sf.freecol.common.networking;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when a unit is to emigrate.
 */
public class EmigrateUnitMessage extends Message {
    /**
     * The slot from which to select the unit.
     */
    private int slot;

    /**
     * Create a new <code>EmigrateUnitMessage</code> with the supplied slot.
     *
     * @param slot The slot to select the migrant from.
     */
    public EmigrateUnitMessage(int slot) {
        this.slot = slot;
    }

    /**
     * Create a new <code>EmigrateUnitMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public EmigrateUnitMessage(Game game, Element element) {
        this.slot = Integer.parseInt(element.getAttribute("slot"));
    }

    /**
     * Handle a "emigrateUnit"-message.
     *
     * @param server The <code>FreeColServer</code> that is handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> the message was received on.
     *
     * @return A multiple element containing the updated <code>Europe</code>
     *         and any required <code>ModelMessages</code>,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player, Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Europe europe = player.getEurope();
        boolean fountain;

        if (europe == null) {
            return Message.clientError("No Europe for a unit to migrate from.");
        }
        int remaining = serverPlayer.getRemainingEmigrants();
        if (remaining > 0) {
            // Check fountain-of-Youth emigrants first because the client side
            // is making the right number of emigrant-selection-requests.
            fountain = true;
            serverPlayer.setRemainingEmigrants(remaining - 1);
        } else if (player.checkEmigrate()) {
            fountain = false;
        } else {
            return Message.clientError("No emigrants available.");
        }

        // Proceed to emigrate.
        InGameController igc = server.getInGameController();
        ModelMessage m = igc.emigrate(serverPlayer, slot, fountain);

        // Update with new state of Europe, the message, and potentially
        // the changed player immigration state.
        Element reply = igc.buildGeneralUpdate(serverPlayer, europe, m);
        if (!fountain) {
            Document doc = reply.getOwnerDocument();
            spliceIntoElement(reply, "update",
                              player.toXMLElementPartial(doc, "immigration",
                                                         "immigrationRequired"));
        }
        return reply;
    }

    /**
     * Convert this EmigrateUnitMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        Element result = createNewRootElement(getXMLElementTagName());
        result.setAttribute("slot", Integer.toString(slot));
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "emigrateUnit".
     */
    public static String getXMLElementTagName() {
        return "emigrateUnit";
    }
}
