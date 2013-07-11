/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to equip a unit.
 */
public class EquipUnitMessage extends DOMMessage {

    /**
     * The identifier of the unit.
     */
    private String unitId;

    /**
     * The identifier of the equipment type.
     */
    private String typeId;

    /**
     * The amount of equipment.
     */
    private String amountString;


    /**
     * Create a new <code>EquipUnitMessage</code> for the supplied unit and
     * equipment type and amount.
     *
     * @param unit The <code>Unit</code> to equip.
     * @param type The <code>EquipmentType</code> to equip with.
     * @param amount The amount of equipment.
     */
    public EquipUnitMessage(Unit unit, EquipmentType type, int amount) {
        this.unitId = unit.getId();
        this.typeId = type.getId();
        this.amountString = Integer.toString(amount);
    }

    /**
     * Create a new <code>EquipUnitMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public EquipUnitMessage(Game game, Element element) {
        this.unitId = element.getAttribute("unit");
        this.typeId = element.getAttribute("type");
        this.amountString = element.getAttribute("amount");
    }

    /**
     * Handle a "equipUnit"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     * @return An update encapsulating the equipUnit location change
     *     or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        ServerPlayer serverPlayer = server.getPlayer(connection);
        Game game = server.getGame();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }
        if (unit.isInEurope()) {
            ; // Always OK
        } else if (!unit.hasTile()) {
            return DOMMessage.clientError("Unit is not on the map: "
                + unitId);
        } else if (unit.getSettlement() == null) {
            return DOMMessage.clientError("Unit is not in a settlement: "
                + unitId);
        }

        EquipmentType type = game.getSpecification().getEquipmentType(typeId);
        if (type == null) {
            return DOMMessage.clientError("Bad equipment type: " + typeId);
        }

        int amount;
        try {
            amount = Integer.parseInt(amountString);
        } catch (NumberFormatException e) {
            return DOMMessage.clientError("Bad amount: " + amountString);
        }
        if (amount == 0) {
            return DOMMessage.clientError("Amount must be non-zero: "
                + amountString);
        } else if (amount > 0) {
            if (!unit.canBeEquippedWith(type)) {
                return DOMMessage.clientError("Unable to equip unit " + unitId
                    + " with " + typeId);
            }
        } else {
            if (-amount > unit.getEquipmentCount(type)) {
                return DOMMessage.clientError("Too much to remove ("
                    + (-amount) + ") of " + typeId
                    + " from unit " + unitId);
            }
        }

        // Proceed to equip.
        return server.getInGameController()
            .equipUnit(serverPlayer, unit, type, amount);
    }

    /**
     * Convert this EquipUnitMessage to XML.
     *
     * @return The XML representation of this message.
     */
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "type", typeId,
            "amount", amountString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "equipUnit".
     */
    public static String getXMLElementTagName() {
        return "equipUnit";
    }
}
