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

import org.w3c.dom.Element;


/**
 * The message sent when setting the build queue.
 */
public class SetBuildQueueMessage extends DOMMessage {

    public static final String TAG = "setBuildQueue";
    private static final String COLONY_TAG = "colony";

    /** The identifier of the colony containing the queue. */
    private final String colonyId;

    /** The items in the build queue. */
    private final String[] queue;


    /**
     * Create a new <code>SetBuildQueueMessage</code> for the
     * supplied colony and queue.
     *
     * @param colony The <code>Colony</code> where the queue is.
     * @param queue A list of <code>BuildableType</code>s to build.
     */
    public SetBuildQueueMessage(Colony colony, List<BuildableType> queue) {
        super(getTagName());

        this.colonyId = colony.getId();
        this.queue = toList(map(queue, bt -> bt.getId()))
            .toArray(new String[0]);
    }

    /**
     * Create a new <code>SetBuildQueueMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetBuildQueueMessage(Game game, Element element) {
        super(getTagName());

        this.colonyId = getStringAttribute(element, COLONY_TAG);
        this.queue = getArrayAttributes(element).toArray(new String[0]);
    }


    // Public interface

    public Colony getColony(Player player) {
        return player.getOurFreeColGameObject(this.colonyId, Colony.class);
    }

    public List<BuildableType> getQueue(Specification spec) {
        List<BuildableType> ret = new ArrayList<>();
        for (int i = 0; i < this.queue.length; i++) {
            ret.add(i, spec.getType(this.queue[i], BuildableType.class));
        }
        return ret;
    }

    
    /**
     * Handle a "setBuildQueue"-message.
     *
     * @param server The <code>FreeColServer</code> handling the message.
     * @param player The <code>Player</code> the message applies to.
     * @param connection The <code>Connection</code> message was received on.
     *
     * @return An update containing the new queue
     *         or an error <code>Element</code> on failure.
     */
    public Element handle(FreeColServer server, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Game game = server.getGame();
        final Specification spec = game.getSpecification();

        Colony colony;
        try {
            colony = getColony(player);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        if (queue == null) {
            return serverPlayer.clientError("Empty queue")
                .build(serverPlayer);
        }
        List<BuildableType> buildQueue;
        try {
            buildQueue = getQueue(spec);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage())
                .build(serverPlayer);
        }

        // Proceed to set the build queue.
        return server.getInGameController()
            .setBuildQueue(serverPlayer, colony, buildQueue)
            .build(serverPlayer);
    }

    /**
     * Convert this SetBuildQueueMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName(),
            COLONY_TAG, this.colonyId)
            .setArrayAttributes(this.queue).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setBuildQueue".
     */
    public static String getTagName() {
        return TAG;
    }
}
