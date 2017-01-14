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
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationOptions.NationState;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message that changes the player availability.
 */
public class SetAvailableMessage extends AttributeMessage {

    public static final String TAG = "setAvailable";
    private static final String NATION_TAG = "nation";
    private static final String STATE_TAG = "state";
    

    /**
     * Create a new {@code SetAvailableMessage}.
     *
     */
    public SetAvailableMessage(Nation nation, NationState state) {
        super(TAG, NATION_TAG, nation.getId(),
              STATE_TAG, state.toString());
    }

    /**
     * Create a new {@code SetAvailableMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param element The {@code Element} to use to create the message.
     */
    public SetAvailableMessage(@SuppressWarnings("unused") Game game,
                               Element element) {
        super(TAG, NATION_TAG, getStringAttribute(element, NATION_TAG),
              STATE_TAG, getStringAttribute(element, STATE_TAG));
    }
        

    // Public interface

    /**
     * Get the nation whose availability is changing.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation} found.
     */
    public Nation getNation(Specification spec) {
        return spec.getNation(getStringAttribute(NATION_TAG));
    }

    /**
     * Get the new nation availabiility.
     *
     * @return The new {@code NationState}.
     */
    public NationState getNationState() {
        return Enum.valueOf(NationState.class, getStringAttribute(STATE_TAG));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final Specification spec = game.getSpecification();
        
        if (serverPlayer != null) {
            Nation nation = getNation(spec);
            NationState state = getNationState();
            game.getNationOptions().setNationState(nation, state);
            freeColServer.sendToAll(new SetAvailableMessage(nation, state),
                                    serverPlayer);
        } else {
            logger.warning("setAvailable from unknown player.");
        }
        return null;
    }
}
