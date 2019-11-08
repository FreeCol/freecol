/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


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
     * Create a new {@code SetAvailableMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException is the stream is corrupt.
     */
    public SetBuildQueueMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, getAttributeMap(xr));

        xr.closeTag(TAG);
    }


    /**
     * Get a map of attributes from the reader.
     *
     * @param xr The {@code FreeColXMLReader} to query.
     * @return A map of attributes.
     */
    private static Map<String, String> getAttributeMap(FreeColXMLReader xr) {
        Map<String, String> ret = xr.getArrayAttributeMap();
        ret.put(COLONY_TAG, xr.getAttribute(COLONY_TAG, (String)null));
        return ret;
    }

    /**
     * Get the colony that is building.
     *
     * @param player The {@code Player} that owns the colony.
     * @return The colony.
     */
    private Colony getColony(Player player) {
        return player.getOurFreeColGameObject(getStringAttribute(COLONY_TAG),
                                              Colony.class);
    }

    /**
     * Get the list of buildables defined by the array attributes.
     *
     * @param spec A {@code Specification} to use to make the buildable.
     * @return A list of {@code BuildableType}s.
     */
    private List<BuildableType> getQueue(Specification spec) {
        return transform(getArrayAttributes(), alwaysTrue(),
                         id -> spec.getBuildableType(id),
                         toListNoNulls());
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
        return igc(freeColServer)
            .setBuildQueue(serverPlayer, colony, buildQueue);
    }
}
