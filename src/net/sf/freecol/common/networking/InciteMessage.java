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
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when inciting a native settlement.
 */
public class InciteMessage extends AttributeMessage {

    public static final String TAG = "incite";
    private static final String ENEMY_TAG = "enemy";
    private static final String GOLD_TAG = "gold";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code InciteMessage} with the
     * supplied name.
     *
     * @param unit The inciting {@code Unit}.
     * @param is The {@code IndianSettlement} to incite.
     * @param enemy The enemy {@code Player}.
     * @param gold The amount of gold in the bribe (negative for the
     *             initial inquiry).
     */
    public InciteMessage(Unit unit, IndianSettlement is, Player enemy,
                         int gold) {
        super(TAG, UNIT_TAG, unit.getId(), SETTLEMENT_TAG, is.getId(),
              ENEMY_TAG, enemy.getId(), GOLD_TAG, String.valueOf(gold));
    }

    /**
     * Create a new {@code InciteMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public InciteMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              SETTLEMENT_TAG, getStringAttribute(element, SETTLEMENT_TAG),
              ENEMY_TAG, getStringAttribute(element, ENEMY_TAG),
              GOLD_TAG, getStringAttribute(element, GOLD_TAG));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.NORMAL;
    }


    // Public interface

    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(UNIT_TAG), Unit.class);
    }

    public IndianSettlement getSettlement(Unit unit) {
        return unit.getAdjacentSettlement(getStringAttribute(SETTLEMENT_TAG),
                                          IndianSettlement.class);
    }

    public Player getEnemy(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ENEMY_TAG), Player.class);
    }

    public int getGold() {
        return getIntegerAttribute(GOLD_TAG, -1);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        
        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        IndianSettlement is;
        try {
            is = getSettlement(unit);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        MoveType type;
        ServerPlayer enemy = (ServerPlayer)getEnemy(game);
        if (enemy == null) {
            return serverPlayer.clientError("Not a player: "
                + getStringAttribute(ENEMY_TAG));
        } else if (enemy == serverPlayer) {
            return serverPlayer.clientError("Inciting against oneself!");
        } else if (!enemy.isEuropean()) {
            return serverPlayer.clientError("Inciting against non-European!");
        } else if ((type = unit.getMoveType(is.getTile()))
            != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        int gold = getGold();
        if (gold < 0) {
            return serverPlayer.clientError("Bad gold: "
                + getStringAttribute(GOLD_TAG));
        }

        // Valid, proceed to incite.
        return freeColServer.getInGameController()
            .incite(serverPlayer, unit, is, enemy, gold);
    }
}
