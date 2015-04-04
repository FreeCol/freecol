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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a unit is to emigrate.
 */
public class EmigrateUnitMessage extends DOMMessage {

    /** The slot from which to select the unit. */
    private final String slotString;


    /**
     * Create a new <code>EmigrateUnitMessage</code> with the supplied slot.
     *
     * @param slot The slot to select the migrant from.
     */
    public EmigrateUnitMessage(int slot) {
        super(getXMLElementTagName());

        this.slotString = Integer.toString(slot);
    }

    /**
     * Create a new <code>EmigrateUnitMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public EmigrateUnitMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.slotString = element.getAttribute("slot");
    }


    /**
     * Handle a "emigrateUnit"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An <code>Element</code> encapsulating the change,
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Europe europe = player.getEurope();
        if (europe == null) {
            return DOMMessage.clientError("No Europe to migrate from.");
        }
        int slot;
        try {
            slot = Integer.parseInt(slotString);
        } catch (NumberFormatException e) {
            return DOMMessage.clientError("Bad slot: " + slotString);
        }
        if (!MigrationType.validMigrantSlot(slot)) {
            return DOMMessage.clientError("Invalid slot for recruitment: "
                + slot);
        }
            
        MigrationType type;
        if (serverPlayer.getRemainingEmigrants() > 0) {
            if (MigrationType.unspecificMigrantSlot(slot)) {
                return DOMMessage.clientError("Specific slot expected for FoY migration.");
            }
            type = MigrationType.FOUNTAIN;
        } else if (player.checkEmigrate()) {
            if (MigrationType.specificMigrantSlot(slot)
                && !player.hasAbility(Ability.SELECT_RECRUIT)) {
                return DOMMessage.clientError("selectRecruit ability absent.");
            }
            type = MigrationType.NORMAL;
        } else {
            if (!player.checkGold(europe.getRecruitPrice())) {
                return DOMMessage.clientError("No migrants available at cost "
                    + europe.getRecruitPrice()
                    + " for player with " + player.getGold() + " gold.");
            }
            type = MigrationType.RECRUIT;
        }

        // Proceed to emigrate.
        return server.getInGameController()
            .emigrate(serverPlayer, slot, type);
    }

    /**
     * Convert this EmigrateUnitMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "slot", slotString);
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
