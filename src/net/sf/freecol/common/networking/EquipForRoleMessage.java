/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to equip a unit for a particular role.
 */
public class EquipForRoleMessage extends AttributeMessage {

    public static final String TAG = "equipForRole";
    private static final String COUNT_TAG = "count";
    private static final String ROLE_TAG = "role";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code EquipForRoleMessage} for the supplied
     * Unit and Role.
     *
     * @param unit The {@code Unit} to equip.
     * @param role The {@code Role} to equip for.
     * @param roleCount The role count.
     */
    public EquipForRoleMessage(Unit unit, Role role, int roleCount) {
        super(TAG, UNIT_TAG, unit.getId(), ROLE_TAG, role.getId(),
              COUNT_TAG, String.valueOf(roleCount));
    }

    /**
     * Create a new {@code EquipForRoleMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public EquipForRoleMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, ROLE_TAG, COUNT_TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final String unitId = getStringAttribute(UNIT_TAG);
        final String roleId = getStringAttribute(ROLE_TAG);
        final String countString = getStringAttribute(COUNT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (unit.isInEurope()) {
            ; // Always OK
        } else if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + unitId);
        } else if (unit.getSettlement() == null) {
            return serverPlayer.clientError("Unit is not in a settlement: "
                + unitId);
        }

        Role role = game.getSpecification().getRole(roleId);
        if (role == null) {
            return serverPlayer.clientError("Not a role: " + roleId);
        }
        int count;
        try {
            count = Integer.parseInt(countString);
        } catch (NumberFormatException nfe) {
            return serverPlayer.clientError("Role count is not an integer: "
                + countString);
        }
        if (count < 0 || count > role.getMaximumCount()) {
            return serverPlayer.clientError("Invalid role count: "
                + countString);
        }

        // Proceed to equip.
        return igc(freeColServer)
            .equipForRole(serverPlayer, unit, role, count);
    }
}
