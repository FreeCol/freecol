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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to update game objects.
 */
public class UpdateMessage extends DOMMessage {

    public static final String TAG = "update";

    /** The player to specialize the objects for. */
    private final ServerPlayer destination;

    /** The objects to update. */
    private final List<FreeColGameObject> fcgos = new ArrayList<>();


    /**
     * Create a new {@code UpdateMessage}.
     *
     * @param destination The destination {@code ServerPlayer}.
     * @param fcgos The list of {@code FreeColGameObject}s to add.
     */
    public UpdateMessage(ServerPlayer destination,
                         List<FreeColObject> fcos) {
        super(TAG);

        this.destination = destination;
        this.fcgos.clear();
        if (fcos != null) {
            for (FreeColObject fco : fcos) {
                if (fco != null
                    && FreeColGameObject.class.isAssignableFrom(fco.getClass()))
                    this.fcgos.add((FreeColGameObject)fco);
            }
        }
    }

    /**
     * Create a new {@code UpdateMessage} from a supplied
     * element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateMessage(Game game, Element element) {
        this((ServerPlayer)null, DOMUtils.getChildren(game, element));
    }


    /**
     * {@inheritDoc}
     */
    public static MessagePriority getMessagePriority() {
        return Message.MessagePriority.UPDATE;
    }


    // Public interface

    /**
     * Get the objects attached to this message.
     *
     * @return The list of {@code FreeColGameObject}s.
     */
    public List<FreeColGameObject> getObjects() {
        return this.fcgos;
    }

    
    // No server handler required.
    // This message is only sent to the client.

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        DOMMessage message = new DOMMessage(TAG);
        message.setStringAttributes(this.getStringAttributes());
        if (!this.fcgos.isEmpty()) message.add(this.fcgos, this.destination);
        return message.toXMLElement();
    }
}
