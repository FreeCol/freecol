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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to equip a unit for a particular role.
 */
public class EquipForRoleMessage extends DOMMessage {

    public static final String TAG = "equipForRole";
    private static final String COUNT_TAG = "count";
    private static final String ROLE_TAG = "role";
    private static final String UNIT_TAG = "unit";

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
        super(getTagName());

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
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.roleId = getStringAttribute(element, ROLE_TAG);
        this.roleCount = getStringAttribute(element, COUNT_TAG);
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
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (unit.isInEurope()) {
            ; // Always OK
        } else if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + this.unitId)
                .build(serverPlayer);
        } else if (unit.getSettlement() == null) {
            return serverPlayer.clientError("Unit is not in a settlement: "
                + this.unitId)
                .build(serverPlayer);
        }

        Role role = game.getSpecification().getRole(this.roleId);
        if (role == null) {
            return serverPlayer.clientError("Not a role: " + this.roleId)
                .build(serverPlayer);
        }
        int count;
        try {
            count = Integer.parseInt(this.roleCount);
        } catch (NumberFormatException nfe) {
            return serverPlayer.clientError("Role count is not an integer: " +
                this.roleCount)
                .build(serverPlayer);
        }
        if (count < 0 || count > role.getMaximumCount()) {
            return serverPlayer.clientError("Invalid role count: "
                + this.roleCount)
                .build(serverPlayer);
        }

        // Proceed to equip.
        ChangeSet cs = server.getInGameController()
            .equipForRole(serverPlayer, unit, role, count);
        return (cs == null) ? null : cs.build(serverPlayer);
    }

    /**
     * Convert this EquipForRoleMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            ROLE_TAG, this.roleId,
            COUNT_TAG, this.roleCount).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "equipForRole".
     */
    public static String getTagName() {
        return TAG;
    }
}
