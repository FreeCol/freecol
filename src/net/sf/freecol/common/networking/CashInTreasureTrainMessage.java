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
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when cashing in a treasure train.
 */
public class CashInTreasureTrainMessage extends DOMMessage {

    public static final String TAG = "cashInTreasureTrain";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the treasure train unit. */
    private final String unitId;


    /**
     * Create a new <code>CashInTreasureTrainMessage</code> with the
     * supplied name.
     *
     * @param unit The <code>Unit</code> to cash in.
     */
    public CashInTreasureTrainMessage(Unit unit) {
        super(getTagName());

        this.unitId = unit.getId();
    }

    /**
     * Create a new <code>CashInTreasureTrainMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public CashInTreasureTrainMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
    }


    /**
     * Handle a "cashInTreasureTrain"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update resulting from cashing in the treasure train,
     *     or an error <code>Element</code> on failure.
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
        if (!unit.canCarryTreasure()) {
            return serverPlayer.clientError("Can not cash in unit "
                + this.unitId + ", can not carry treasure.")
                .build(serverPlayer);
        } else if (!unit.canCashInTreasureTrain()) {
            return serverPlayer.clientError("Can not cash in unit "
                + this.unitId + ", unsuitable location.")
                .build(serverPlayer);
        }

        // Cash in
        return server.getInGameController()
            .cashInTreasureTrain(serverPlayer, unit)
            .build(serverPlayer);
    }

    /**
     * Convert this CashInTreasureTrainMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "cashInTreasureTrain".
     */
    public static String getTagName() {
        return TAG;
    }
}
