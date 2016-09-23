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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when speaking to a chief.
 */
public class ScoutSpeakToChiefMessage extends DOMMessage {

    public static final String TAG = "scoutSpeakToChief";
    private static final String RESULT_TAG = "result";
    private static final String SETTLEMENT_TAG = "settlement";
    private static final String UNIT_TAG = "unit";

    /** The identifier of the unit that is speaking. */
    private final String unitId;

    /** The identifier of the settlement to talk to. */
    private final String settlementId;

    /** The result of speaking to the chief. */
    private final String result;


    /**
     * Create a new {@code ScoutSpeakToChiefMessage} with the
     * supplied unit, settlement and result.
     *
     * Result is null in a request.
     *
     * @param unit The {@code Unit} that is learning.
     * @param is The {@code IndianSettlement} to talk to.
     * @param result The result of speaking.
     */
    public ScoutSpeakToChiefMessage(Unit unit, IndianSettlement is,
                                    String result) {
        super(TAG);

        this.unitId = unit.getId();
        this.settlementId = is.getId();
        this.result = result;
    }

    /**
     * Create a new {@code ScoutSpeakToChiefMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ScoutSpeakToChiefMessage(Game game, Element element) {
        super(TAG);

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.settlementId = getStringAttribute(element, SETTLEMENT_TAG);
        this.result = getStringAttribute(element, RESULT_TAG);
    }


    // Public interface

    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(this.unitId, Unit.class);
    }

    public IndianSettlement getSettlement(Game game) {
        return game.getFreeColGameObject(this.settlementId,
                                         IndianSettlement.class);
    }

    public String getResult() {
        return (this.result == null) ? "" : this.result;
    }

    
    /**
     * Handle a "scoutSpeakToChief"-message.
     *
     * @param server The {@code FreeColServer} handling the message.
     * @param player The {@code Player} the message applies to.
     * @param connection The {@code Connection} message was received on.
     * @return An element containing the result of the scouting
     *     action, or an error {@code Element} on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!unit.hasAbility(Ability.SPEAK_WITH_CHIEF)) {
            return serverPlayer.clientError("Unit lacks ability to speak to chief: "
                + this.unitId)
                .build(serverPlayer);
        }

        IndianSettlement is;
        try {
            is = unit.getAdjacentSettlement(this.settlementId,
                                            IndianSettlement.class);
        } catch (RuntimeException e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        MoveType type = unit.getMoveType(is.getTile());
        if (type != MoveType.ENTER_INDIAN_SETTLEMENT_WITH_SCOUT) {
            return serverPlayer.clientError("Unable to enter "
                + is.getName() + ": " + type.whyIllegal())
                .build(serverPlayer);
        }

        // Valid request, do the scouting.
        return server.getInGameController()
            .scoutSpeakToChief(serverPlayer, unit, is)
            .build(serverPlayer);
    }

    /**
     * Convert this ScoutSpeakToChiefMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            UNIT_TAG, this.unitId,
            SETTLEMENT_TAG, this.settlementId,
            RESULT_TAG, this.result).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "scoutSpeakToChief".
     */
    public static String getTagName() {
        return TAG;
    }
}
