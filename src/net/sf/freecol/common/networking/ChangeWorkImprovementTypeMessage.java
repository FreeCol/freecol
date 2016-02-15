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
public class ChangeWorkImprovementTypeMessage extends DOMMessage {

    public static final String TAG = "changeWorkImprovementType";
    private static final String IMPROVEMENT_TYPE_TAG = "improvementType";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit that is working. */
    private final String unitId;

    /** The identifier of the improvement type. */
    private final String improvementId;


    /**
     * Create a new <code>ChangeWorkImprovementTypeMessage</code> with the
     * supplied unit and improvement type.
     *
     * @param unit The <code>Unit</code> that is working.
     * @param type The new <code>TileImprovementType</code>.
     */
    public ChangeWorkImprovementTypeMessage(Unit unit,
                                            TileImprovementType type) {
        super(getTagName());

        this.unitId = unit.getId();
        this.improvementId = type.getId();
    }

    /**
     * Create a new <code>ChangeWorkImprovementTypeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChangeWorkImprovementTypeMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.improvementId = getStringAttribute(element, IMPROVEMENT_TYPE_TAG);
    }


    /**
     * Handle a "changeWorkImprovementType"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the changed unit, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        Tile tile = unit.getTile();
        if (tile == null) {
            return serverPlayer.clientError("Unit is not on the map: "
                + this.unitId)
                .build(serverPlayer);
        } else if (!unit.hasAbility(Ability.IMPROVE_TERRAIN)) {
            return serverPlayer.clientError("Unit can not improve tiles: "
                + this.unitId)
                .build(serverPlayer);
        }

        TileImprovementType type = server.getSpecification()
            .getTileImprovementType(this.improvementId);
        TileImprovement improvement;
        if (type == null) {
            return serverPlayer.clientError("Not a tile improvement type: "
                + this.improvementId)
                .build(serverPlayer);
        } else if (type.isNatural()) {
            return serverPlayer.clientError("ImprovementType must not be natural: "
                + this.improvementId)
                .build(serverPlayer);
        } else if (!type.isTileTypeAllowed(tile.getType())) {
            return serverPlayer.clientError("ImprovementType not allowed on tile: "
                + this.improvementId)
                .build(serverPlayer);
        } else if ((improvement = tile.getTileImprovement(type)) == null) {
            if (!type.isWorkerAllowed(unit)) {
                return serverPlayer.clientError("Unit can not create improvement: "
                    + this.improvementId)
                    .build(serverPlayer);
            }
        } else { // Has improvement, check if worker can contribute to it
            if (!improvement.isWorkerAllowed(unit)) {
                return serverPlayer.clientError("Unit can not work on improvement: "
                    + this.improvementId)
                    .build(serverPlayer);
            }
        }

        // Proceed to change.
        return server.getInGameController()
            .changeWorkImprovementType(serverPlayer, unit, type)
            .build(serverPlayer);
    }

    /**
     * Convert this ChangeWorkImprovementTypeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            IMPROVEMENT_TYPE_TAG, this.improvementId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "changeWorkImprovementType".
     */
    public static String getTagName() {
        return TAG;
    }
}
