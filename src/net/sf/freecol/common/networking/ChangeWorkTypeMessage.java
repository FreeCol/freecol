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
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when changing the work type of a unit.
 */
public class ChangeWorkTypeMessage extends DOMMessage {

    public static final String TAG = "changeWorkType";
    private static final String UNIT_TAG = "unit";
    private static final String WORK_TYPE_TAG = "workType";

    /** The identifier of the unit that is working. */
    private final String unitId;

    /** The goods type to produce. */
    private final String workTypeId;


    /**
     * Create a new <code>ChangeWorkTypeMessage</code> with the
     * supplied unit and produce.
     *
     * @param unit The <code>Unit</code> that is working.
     * @param workType The <code>GoodsType</code> to produce.
     */
    public ChangeWorkTypeMessage(Unit unit, GoodsType workType) {
        super(getTagName());

        this.unitId = unit.getId();
        this.workTypeId = workType.getId();
    }

    /**
     * Create a new <code>ChangeWorkTypeMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public ChangeWorkTypeMessage(Game game, Element element) {
        super(getTagName());

        this.unitId = getStringAttribute(element, UNIT_TAG);
        this.workTypeId = getStringAttribute(element, WORK_TYPE_TAG);
    }


    /**
     * Handle a "changeWorkType"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     * @return An update containing the changes, or an error
     *     <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Specification spec = server.getSpecification();

        Unit unit;
        try {
            unit = player.getOurFreeColGameObject(this.unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }
        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + this.unitId)
                .build(serverPlayer);
        }

        GoodsType type = spec.getGoodsType(this.workTypeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: "
                + this.workTypeId)
                .build(serverPlayer);
        }

        // Proceed to changeWorkType.
        return server.getInGameController()
            .changeWorkType(serverPlayer, unit, type)
            .build(serverPlayer);
    }

    /**
     * Convert this ChangeWorkTypeMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            UNIT_TAG, this.unitId,
            WORK_TYPE_TAG, this.workTypeId).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "changeWorkType".
     */
    public static String getTagName() {
        return TAG;
    }
}

