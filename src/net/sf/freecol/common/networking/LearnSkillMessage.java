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
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when learning for the skill taught at a settlement.
 */
public class LearnSkillMessage extends AttributeMessage {

    public static final String TAG = "learnSkill";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code LearnSkillMessage} with the
     * supplied unit and direction.
     *
     * @param unit The {@code Unit} that is learning.
     * @param direction The {@code Direction} the unit is looking.
     */
    public LearnSkillMessage(Unit unit, Direction direction) {
        super(TAG, UNIT_TAG, unit.getId(),
              DIRECTION_TAG, String.valueOf(direction));
    }

    /**
     * Create a new {@code LearnSkillMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public LearnSkillMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DIRECTION_TAG, getStringAttribute(element, DIRECTION_TAG));
    }

    /**
     * Create a new {@code LearnSkillMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     */
    public LearnSkillMessage(Game game, FreeColXMLReader xr) {
        super(TAG, xr, UNIT_TAG, DIRECTION_TAG);
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
        final String directionString = getStringAttribute(DIRECTION_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        IndianSettlement is = tile.getIndianSettlement();
        if (is == null) {
            return serverPlayer.clientError("There is no native settlement at: "
                + tile.getId());
        }

        // Do not use getMoveType (checking moves left) as the
        // preceding AskLearnSkill transaction will have already
        // zeroed the moves.
        // Consider using a transaction, so that declining restores the moves?
        MoveType type = unit.getSimpleMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        // Learn the skill if possible.
        return freeColServer.getInGameController()
            .learnFromIndianSettlement(serverPlayer, unit, is);
    }
}
