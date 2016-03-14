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
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerIndianSettlement;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when inciting a native settlement.
 */
public class InciteMessage extends DOMMessage {

    public static final String TAG = "incite";
    private static final String ENEMY_TAG = "enemy";
    private static final String GOLD_TAG = "gold";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit inciting. */
    private final String unitId;

    /** The identifier for the settlement. */
    private final String settlementId;

    /** The identifier of the enemy to incite against. */
    private final String enemyId;

    /** The amount of gold in the bribe. */
    private final String goldString;


    /**
     * Create a new <code>InciteMessage</code> with the
     * supplied name.
     *
     * @param unit The inciting <code>Unit</code>.
     * @param settlement The <code>IndianSettlement</code> to incite.
     * @param enemy The enemy <code>Player</code>.
     * @param gold The amount of gold in the bribe (negative for the
     *             initial inquiry).
     */
    public InciteMessage(Unit unit, IndianSettlement settlement, Player enemy,
                         int gold) {
        super(getTagName());

        this.unitId = unit.getId();
        this.settlementId = settlement.getId();
        this.enemyId = enemy.getId();
        this.goldString = Integer.toString(gold);
    }

    /**
     * Create a new <code>InciteMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public InciteMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.enemyId = getStringAttribute(element, ENEMY_TAG);
        this.goldString = getStringAttribute(element, GOLD_TAG);
    }


    // Public interface

    public Unit getUnit(Player player) {
        return player.getOurFreeColGameObject(this.unitId, Unit.class);
    }

    public IndianSettlement getSettlement(Unit unit) {
        return unit.getAdjacentIndianSettlementSafely(this.settlementId);
    }

    public Player getEnemy(Game game) {
        return game.getFreeColGameObject(this.enemyId, Player.class);
    }

    public int getGold() {
        return Integer.parseInt(this.goldString);
    }


    /**
     * Handle a "incite"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An element containing the result of the incite, or an
     *     error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();

        Unit unit;
        try {
            unit = getUnit(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        IndianSettlement is;
        try {
            is = (ServerIndianSettlement)getSettlement(unit);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        MoveType type;
        ServerPlayer enemy = (ServerPlayer)getEnemy(game);
        if (enemy == null) {
            return serverPlayer.clientError("Not a player: " + this.enemyId)
                .build(serverPlayer);
        } else if (enemy == player) {
            return serverPlayer.clientError("Inciting against oneself!")
                .build(serverPlayer);
        } else if (!enemy.isEuropean()) {
            return serverPlayer.clientError("Inciting against non-European!")
                .build(serverPlayer);
        } else if ((type = unit.getMoveType(is.getTile()))
            != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal())
                .build(serverPlayer);
        }

        int gold;
        try {
            gold = getGold();
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad gold: " + this.goldString)
                .build(serverPlayer);
        }

        // Valid, proceed to incite.
        return server.getInGameController()
            .incite(serverPlayer, unit, is, enemy, gold)
            .build(serverPlayer);
    }

    /**
     * Convert this InciteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId,
            ENEMY_TAG, this.enemyId,
            GOLD_TAG, this.goldString).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "incite".
     */
    public static String getTagName() {
        return TAG;
    }
}
