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

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a native delivers a gift to a Colony.
 */
public class NativeGiftMessage extends DOMMessage {

    public static final String TAG = "nativeGift";
    private static final String COLONY_TAG = "colony";
    private static final String UNIT_TAG = "unit";

    /** The object identifier of the unit that is nativeing the gift. */
    private final String unitId;

    /** The object identifier of the colony the gift is going to. */
    private final String colonyId;


    /**
     * Create a new <code>NativeGiftMessage</code>.
     *
     * @param unit The <code>Unit</code> that is trading.
     * @param colony The <code>Colony</code> that is trading.
     */
    public NativeGiftMessage(Unit unit, Colony colony) {
        super(getTagName());

        this.unitId = unit.getId();
        this.colonyId = colony.getId();
    }

    /**
     * Create a new <code>NativeGiftMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public NativeGiftMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.colonyId = getStringAttribute(element, COLONY_TAG);
    }


    /**
     * Handle a "nativeGift"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the unit and colony, or an
     *     error <code>Element</code> on failure.
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

        Colony colony;
        try {
            colony = (Colony)unit.getAdjacentSettlementSafely(this.colonyId);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Proceed to deliver.
        return server.getInGameController()
            .nativeGift(serverPlayer, unit, colony)
            .build(serverPlayer);
    }

    /**
     * Convert this NativeGiftMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            COLONY_TAG, this.colonyId,
            UNIT_TAG, this.unitId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "nativeGift".
     */
    public static String getTagName() {
        return TAG;
    }
}
