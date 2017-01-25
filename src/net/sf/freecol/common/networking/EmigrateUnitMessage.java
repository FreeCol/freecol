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

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Europe.MigrationType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when a unit is to emigrate.
 */
public class EmigrateUnitMessage extends AttributeMessage {

    public static final String TAG = "emigrateUnit";
    private static final String SLOT_TAG = "slot";


    /**
     * Create a new {@code EmigrateUnitMessage} with the supplied slot.
     *
     * @param slot The slot to select the migrant from.
     */
    public EmigrateUnitMessage(int slot) {
        super(TAG, SLOT_TAG, String.valueOf(slot));
    }

    /**
     * Create a new {@code EmigrateUnitMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public EmigrateUnitMessage(Game game, Element element) {
        super(TAG, SLOT_TAG, getStringAttribute(element, SLOT_TAG));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.NORMAL;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final String slotString = getStringAttribute(SLOT_TAG);

        Europe europe = serverPlayer.getEurope();
        if (europe == null) {
            return serverPlayer.clientError("No Europe to migrate from.");
        }
        int slot;
        try {
            slot = Integer.parseInt(slotString);
        } catch (NumberFormatException e) {
            return serverPlayer.clientError("Bad slot: " + slotString);
        }
        if (!MigrationType.validMigrantSlot(slot)) {
            return serverPlayer.clientError("Invalid slot for recruitment: "
                + slot);
        }
            
        MigrationType type;
        if (serverPlayer.getRemainingEmigrants() > 0) {
            if (MigrationType.unspecificMigrantSlot(slot)) {
                return serverPlayer.clientError("Specific slot expected for FoY migration");
            }
            type = MigrationType.FOUNTAIN;
        } else if (serverPlayer.checkEmigrate()) {
            if (MigrationType.specificMigrantSlot(slot)
                && !serverPlayer.hasAbility(Ability.SELECT_RECRUIT)) {
                return serverPlayer.clientError("selectRecruit ability absent");
            }
            type = MigrationType.NORMAL;
        } else {
            if (!serverPlayer.checkGold(europe.getRecruitPrice())) {
                return serverPlayer.clientError("No migrants available at cost "
                    + europe.getRecruitPrice()
                    + " for player with " + serverPlayer.getGold() + " gold");
            }
            type = MigrationType.RECRUIT;
        }

        // Proceed to emigrate.
        return freeColServer.getInGameController()
            .emigrate(serverPlayer, slot, type);
    }
}
