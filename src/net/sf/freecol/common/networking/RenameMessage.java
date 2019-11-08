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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent when renaming a FreeColGameObject.
 */
public class RenameMessage extends AttributeMessage {

    public static final String TAG = "rename";
    private static final String NAMEABLE_TAG = "nameable";
    private static final String NAME_TAG = "name";


    /**
     * Create a new {@code RenameMessage} with the
     * supplied name.
     *
     * @param object The {@code FreeColGameObject} to rename.
     * @param newName The new name for the object.
     */
    public RenameMessage(FreeColGameObject object, String newName) {
        super(TAG, NAMEABLE_TAG, object.getId(), NAME_TAG, newName);
    }

    /**
     * Create a new {@code RenameMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public RenameMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, NAMEABLE_TAG, NAME_TAG);
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
        final String nameableId = getStringAttribute(NAMEABLE_TAG);
        
        FreeColGameObject fcgo;
        try {
            fcgo = serverPlayer.getOurFreeColGameObject(nameableId, FreeColGameObject.class);
        } catch (Exception e) {
            return serverPlayer.clientError(e.getMessage());
        }
        if (!(fcgo instanceof Nameable)) {
            return serverPlayer.clientError("Not a nameable: " + nameableId);
        }

        // Proceed to rename.
        return igc(freeColServer)
            .renameObject(serverPlayer, (Nameable)fcgo,
                          getStringAttribute(NAME_TAG));
    }
}
