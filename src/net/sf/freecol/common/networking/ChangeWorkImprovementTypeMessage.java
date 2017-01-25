/**
 *  Copyright (C) 2002-2017   The FreeCol Team
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
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when changing a work improvement type.
 */
public class ChangeWorkImprovementTypeMessage extends AttributeMessage {

    public static final String TAG = "changeWorkImprovementType";
    private static final String IMPROVEMENT_TYPE_TAG = "improvementType";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code ChangeWorkImprovementTypeMessage} with the
     * supplied unit and improvement type.
     *
     * @param unit The {@code Unit} that is working.
     * @param type The new {@code TileImprovementType}.
     */
    public ChangeWorkImprovementTypeMessage(Unit unit,
                                            TileImprovementType type) {
        super(TAG, UNIT_TAG, unit.getId(), IMPROVEMENT_TYPE_TAG, type.getId());
    }

    /**
     * Create a new {@code ChangeWorkImprovementTypeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ChangeWorkImprovementTypeMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              IMPROVEMENT_TYPE_TAG, getStringAttribute(element, IMPROVEMENT_TYPE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.NORMAL;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getStringAttribute(UNIT_TAG);
        final String improvementId = getStringAttribute(IMPROVEMENT_TYPE_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return serverPlayer.clientError("Unit is not on the map: "
                + unitId);
        } else if (!unit.hasAbility(Ability.IMPROVE_TERRAIN)) {
            return serverPlayer.clientError("Unit can not improve tiles: "
                + unitId);
        }

        TileImprovementType type = freeColServer.getSpecification()
            .getTileImprovementType(improvementId);
        TileImprovement improvement;
        if (type == null) {
            return serverPlayer.clientError("Not a tile improvement type: "
                + improvementId);
        } else if (type.isNatural()) {
            return serverPlayer.clientError("ImprovementType must not be natural: "
                + improvementId);
        } else if (!type.isTileTypeAllowed(tile.getType())) {
            return serverPlayer.clientError("ImprovementType not allowed on tile: "
                + improvementId);
        } else if ((improvement = tile.getTileImprovement(type)) == null) {
            if (!type.isWorkerAllowed(unit)) {
                return serverPlayer.clientError("Unit can not create improvement: "
                    + improvementId);
            }
        } else { // Has improvement, check if worker can contribute to it
            if (!improvement.isWorkerAllowed(unit)) {
                return serverPlayer.clientError("Unit can not work on improvement: "
                    + improvementId);
            }
        }

        // Proceed to change.
        return freeColServer.getInGameController()
            .changeWorkImprovementType(serverPlayer, unit, type);
    }
}
