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
public class ChangeWorkTypeMessage extends AttributeMessage {

    public static final String TAG = "changeWorkType";
    private static final String UNIT_TAG = "unit";
    private static final String WORK_TYPE_TAG = "workType";


    /**
     * Create a new {@code ChangeWorkTypeMessage} with the
     * supplied unit and produce.
     *
     * @param unit The {@code Unit} that is working.
     * @param workType The {@code GoodsType} to produce.
     */
    public ChangeWorkTypeMessage(Unit unit, GoodsType workType) {
        super(TAG, UNIT_TAG, unit.getId(), WORK_TYPE_TAG, workType.getId());
    }

    /**
     * Create a new {@code ChangeWorkTypeMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ChangeWorkTypeMessage(Game game, Element element) {
        super(TAG, UNIT_TAG, getStringAttribute(element, UNIT_TAG),
              WORK_TYPE_TAG, getStringAttribute(element, WORK_TYPE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Specification spec = freeColServer.getSpecification();
        final String unitId = getAttribute(UNIT_TAG);
        final String workTypeId = getAttribute(WORK_TYPE_TAG);

        Unit unit;
        try {
            unit = serverPlayer.getOurFreeColGameObject(unitId, Unit.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!unit.hasTile()) {
            return serverPlayer.clientError("Unit is not on the map: "
                + unitId);
        }

        GoodsType type = spec.getGoodsType(workTypeId);
        if (type == null) {
            return serverPlayer.clientError("Not a goods type: " + workTypeId);
        }

        // Proceed to changeWorkType.
        return freeColServer.getInGameController()
            .changeWorkType(serverPlayer, unit, type);
    }
}

