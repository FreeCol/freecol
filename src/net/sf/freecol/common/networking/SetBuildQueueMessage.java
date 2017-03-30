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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent when setting the build queue.
 */
public class SetBuildQueueMessage extends AttributeMessage {

    public static final String TAG = "setBuildQueue";
    private static final String COLONY_TAG = "colony";


    /**
     * Create a new {@code SetBuildQueueMessage} for the
     * supplied colony and queue.
     *
     * @param colony The {@code Colony} where the queue is.
     * @param queue A list of {@code BuildableType}s to build.
     */
    public SetBuildQueueMessage(Colony colony, List<BuildableType> queue) {
        super(TAG, COLONY_TAG, colony.getId());

        setArrayAttributes(transform(queue, alwaysTrue(), BuildableType::getId));
    }

    /**
     * Create a new {@code SetBuildQueueMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public SetBuildQueueMessage(Game game, Element element) {
        super(TAG, COLONY_TAG, getStringAttribute(element, COLONY_TAG));

        setArrayAttributes(DOMUtils.getArrayAttributes(element));
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


    // Public interface

    /**
     * Get the colony that is building.
     *
     * @param player The {@code Player} that owns the colony.
     * @return The colony.
     */
    public Colony getColony(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(COLONY_TAG),
                                              Colony.class);
    }

    /**
     * Get the list of buildables defined by the array attributes.
     *
     * @param spec A {@code Specification} to use to make the buildable.
     * @return A list of {@code BuildableType}s.
     */
    public List<BuildableType> getQueue(Specification spec) {
        return transform(getArrayAttributes(), alwaysTrue(),
                         id -> spec.getType(id, BuildableType.class),
                         toListNoNulls());
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final Game game = freeColServer.getGame();
        final Specification spec = game.getSpecification();

        Colony colony;
        try {
            colony = getColony(serverPlayer);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        List<BuildableType> buildQueue;
        try {
            buildQueue = getQueue(spec);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }

        // Proceed to set the build queue.
        return freeColServer.getInGameController()
            .setBuildQueue(serverPlayer, colony, buildQueue);
    }
}
