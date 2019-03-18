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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


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
     * Create a new {@code InciteMessage} from a stream.
     *
     * @param game The {@code Game} to read within.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public InciteMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, UNIT_TAG, SETTLEMENT_TAG, ENEMY_TAG, GOLD_TAG);
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
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Unit unit = getUnit(player);
        final IndianSettlement is = getSettlement(unit);
        final Player enemy = getEnemy(game);
        final int gold = getGold();
        
        igc(freeColClient).inciteHandler(unit, is, enemy, gold);
        clientGeneric(freeColClient);
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
        Player enemy = getEnemy(game);
        if (enemy == null) {
            return serverPlayer.clientError("Not a player: "
                + getStringAttribute(ENEMY_TAG));
        } else if (enemy == (Player)serverPlayer) {
            return serverPlayer.clientError("Inciting against oneself!");
        } else if (!enemy.isEuropean()) {
            return serverPlayer.clientError("Inciting against non-European!");
        } else if ((type = unit.getMoveType(is.getTile()))
            != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        final int gold = getGold(); // Negative gold is the initial query

        // Valid, proceed to incite.
        return igc(freeColServer)
            .incite(serverPlayer, unit, is, enemy, gold);
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
}
