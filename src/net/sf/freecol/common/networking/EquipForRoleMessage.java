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
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to equip a unit for a particular role.
 */
public class EquipForRoleMessage extends DOMMessage {

    /** The identifier of the unit to equip. */
    private final String unitId;

    /** The Role identifier. */
    private final String roleId;

    /** The role count. */
    private final String roleCount;


    /**
     * Create a new <code>EquipForRoleMessage</code> for the supplied
     * Unit and Role.
     *
     * @param unit The <code>Unit</code> to equip.
     * @param role The <code>Role</code> to equip for.
     * @param roleCount The role count.
     */
    public EquipForRoleMessage(Unit unit, Role role, int roleCount) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.roleId = role.getId();
        this.roleCount = String.valueOf(roleCount);
    }

    /**
     * Create a new <code>EquipForRoleMessage</code> from a supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public EquipForRoleMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unit");
        this.roleId = element.getAttribute("role");
        this.roleCount = element.getAttribute("count");
    }


    /**
     * Handle a "equipForRole"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message received on.
     * @return An update encapsulating the equipForRole location change
     *     or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

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

        Role role = game.getSpecification().getRole(roleId);
        if (role == null) {
            return DOMMessage.clientError("Not a role: " + roleId);
        }
        int count;
        try {
            count = Integer.parseInt(roleCount);
        } catch (NumberFormatException nfe) {
            return DOMMessage.clientError("Role count is not an integer: " +
                roleCount);
        }
        if (count < 0 || count > role.getMaximumCount()) {
            return DOMMessage.clientError("Invalid role count: " + roleCount);
        }

        // Proceed to equip.
        return server.getInGameController()
            .equipForRole(serverPlayer, unit, role, count); 
    }

    /**
     * Convert this EquipForRoleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unit", unitId,
            "role", roleId,
            "count", roleCount);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "equipForRole".
     */
    public static String getXMLElementTagName() {
        return "equipForRole";
    }
}
