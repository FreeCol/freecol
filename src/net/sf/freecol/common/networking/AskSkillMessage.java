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
 * The message sent when asking for the skill taught at a settlement.
 */
public class AskSkillMessage extends AttributeMessage {

    public static final String TAG = "askSkill";
    private static final String DIRECTION_TAG = "direction";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code AskSkillMessage} with the
     * supplied unit and direction.
     *
     * @param unit The {@code Unit} that is asking.
     * @param direction The {@code Direction} the unit is looking.
     */
    public AskSkillMessage(Unit unit, Direction direction) {
        super(TAG, UNIT_TAG, unit.getId(),
              DIRECTION_TAG, String.valueOf(direction));
    }

    /**
     * Create a new {@code AskSkillMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AskSkillMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              DIRECTION_TAG, getStringAttribute(element, DIRECTION_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String unitId = getAttribute(UNIT_TAG);
        final String directionString = getAttribute(DIRECTION_TAG);

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

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_FREE_COLONIST) {
            return serverPlayer.clientError("Unable to enter " + is.getName()
                + ": " + type.whyIllegal());
        }

        // Update the skill
        return freeColServer.getInGameController()
            .askLearnSkill(serverPlayer, unit, is);
    }
}
