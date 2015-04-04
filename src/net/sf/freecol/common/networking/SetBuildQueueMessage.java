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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent when setting the build queue.
 */
public class SetBuildQueueMessage extends DOMMessage {

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
        super(getXMLElementTagName());

        this.colonyId = colony.getId();
        this.queue = new String[queue.size()];
        for (int i = 0; i < queue.size(); i++) {
            this.queue[i] = queue.get(i).getId();
        }
    }

    /**
     * Create a new <code>SetBuildQueueMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public SetBuildQueueMessage(Game game, Element element) {
        super(getXMLElementTagName());

        this.colonyId = element.getAttribute("colony");
        int size;
        try {
            size = Integer.parseInt(element.getAttribute("size"));
        } catch (NumberFormatException e) {
            size = -1;
        }
        if (size >= 0) {
            this.queue = new String[size];
            for (int i = 0; i < size; i++) {
                this.queue[i] = element.getAttribute("x" + Integer.toString(i));
            }
        } else {
            this.queue = null;
        }
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
            colony = player.getOurFreeColGameObject(colonyId, Colony.class);
        } catch (Exception e) {
            return DOMMessage.clientError(e.getMessage());
        }

        if (queue == null) {
            return DOMMessage.clientError("Empty queue");
        }
        List<BuildableType> buildQueue = new ArrayList<>();
        for (int i = 0; i < queue.length; i++) {
            try {
                buildQueue.add(i, spec.getType(queue[i], BuildableType.class));
            } catch (Exception cce) {
                return DOMMessage.clientError("Not a buildable type: "
                    + queue[i]);
            }
        }

        // Proceed to set the build queue.
        return server.getInGameController()
            .setBuildQueue(serverPlayer, colony, buildQueue);
    }

    /**
     * Convert this SetBuildQueueMessage to XML.
     *
     * @return The XML representation of this message.
     */
    @Override
    public Element toXMLElement() {
        Element result = createMessage(getXMLElementTagName(),
            "colony", colonyId,
            "size", Integer.toString(queue.length));
        for (int i = 0; i < queue.length; i++) {
            result.setAttribute("x" + Integer.toString(i), queue[i]);
        }
        return result;
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "setBuildQueue".
     */
    public static String getXMLElementTagName() {
        return "setBuildQueue";
    }
}
