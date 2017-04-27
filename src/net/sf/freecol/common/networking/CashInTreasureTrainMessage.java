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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when cashing in a treasure train.
 */
public class CashInTreasureTrainMessage extends AttributeMessage {

    public static final String TAG = "cashInTreasureTrain";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code CashInTreasureTrainMessage} with the
     * supplied name.
     *
     * @param unit The {@code Unit} to cash in.
     */
    public CashInTreasureTrainMessage(Unit unit) {
        super(TAG, UNIT_TAG, unit.getId());
    }

    /**
     * Create a new {@code CashInTreasureTrainMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public CashInTreasureTrainMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG));
    }

    /**
     * Create a new {@code CashInTreasureTrainMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public CashInTreasureTrainMessage(Game game, FreeColXMLReader xr) {
        super(TAG, xr, UNIT_TAG);
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
        final String unitId = getStringAttribute(UNIT_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        if (!unit.canCarryTreasure()) {
            return serverPlayer.clientError("Can not cash in unit "
                + unitId + ", can not carry treasure.");
        } else if (!unit.canCashInTreasureTrain()) {
            return serverPlayer.clientError("Can not cash in unit "
                + unitId + ", unsuitable location.");
        }

        // Cash in
        return igc(freeColServer)
            .cashInTreasureTrain(serverPlayer, unit);
    }
}
