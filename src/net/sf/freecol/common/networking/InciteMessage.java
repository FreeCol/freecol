/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import net.sf.freecol.common.model.Direction;
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

    /** The identifier of the unit inciting. */
    private final String unitId;

    /** The direction to the settlement. */
    private final String directionString;

    /** The identifier of the enemy to incite against. */
    private final String enemyId;

    /** The amount of gold in the bribe. */
    private final String goldString;


    /**
     * Create a new <code>InciteMessage</code> with the
     * supplied name.
     *
     * @param unit The inciting <code>Unit</code>.
     * @param direction The <code>Direction</code> to the settlement.
     * @param enemy The enemy <code>Player</code>.
     * @param gold The amount of gold in the bribe (negative for the
     *             initial inquiry).
     */
    public InciteMessage(Unit unit, Direction direction, Player enemy,
                         int gold) {
        super(getXMLElementTagName());

        this.unitId = unit.getId();
        this.directionString = String.valueOf(direction);
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
        super(getXMLElementTagName());

        this.unitId = element.getAttribute("unitId");
        this.directionString = element.getAttribute("direction");
        this.enemyId = element.getAttribute("enemyId");
        this.goldString = element.getAttribute("gold");
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
            unit = player.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        Tile tile;
        try {
            tile = unit.getNeighbourTile(directionString);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        ServerIndianSettlement is
            = (ServerIndianSettlement)tile.getIndianSettlement();
        if (is == null) {
            return DOMMessage.clientError("There is no native settlement at: "
                + tile.getId());
        }

        MoveType type;
        ServerPlayer enemy = game.getFreeColGameObject(enemyId,
            ServerPlayer.class);
        if (enemy == null) {
            return DOMMessage.clientError("Not a player: " + enemyId);
        } else if (enemy == player) {
            return DOMMessage.clientError("Inciting against oneself!");
        } else if (!enemy.isEuropean()) {
            return DOMMessage.clientError("Inciting against non-European!");
        } else if ((type = unit.getMoveType(is.getTile()))
            != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_MISSIONARY) {
            return DOMMessage.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal());
        }

        int gold;
        try {
            gold = Integer.parseInt(goldString);
        } catch (NumberFormatException e) {
            return DOMMessage.clientError("Bad gold: " + goldString);
        }

        // Valid, proceed to incite.
        return server.getInGameController()
            .incite(serverPlayer, unit, is, enemy, gold);
    }

    /**
     * Convert this InciteMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return createMessage(getXMLElementTagName(),
            "unitId", unitId,
            "direction", directionString,
            "enemyId", enemyId,
            "gold", goldString);
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "incite".
     */
    public static String getXMLElementTagName() {
        return "incite";
    }
}
