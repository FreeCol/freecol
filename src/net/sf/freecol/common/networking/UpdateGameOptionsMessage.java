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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to update the game options.
 */
public class UpdateGameOptionsMessage extends ObjectMessage {

    public static final String TAG = "updateGameOptions";

    /** The options. */
    private final OptionGroup options;


    /**
     * Create a new {@code UpdateGameOptionsMessage} with the
     * supplied name.
     *
     * @param options The game options {@code OptionGroup}.
     */
    public UpdateGameOptionsMessage(OptionGroup options) {
        super(TAG);

        this.options = options;
    }

    /**
     * Create a new {@code UpdateGameOptionsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateGameOptionsMessage(Game game, Element element) {
        this(DOMUtils.getChild(game, element, 0, false, OptionGroup.class));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }


    // Public interface

    /**
     * Get the associated option group.
     *
     * @return The options.
     */
    public OptionGroup getGameOptions() {
        return this.options;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null || !serverPlayer.isAdmin()) {
            return serverPlayer.clientError("Not an admin: " + serverPlayer);
        }
        final Specification spec = freeColServer.getGame().getSpecification();
        if (this.options == null) {
            return serverPlayer.clientError("No game options to merge");
        }
        if (!spec.mergeGameOptions(this.options, "server")) {
            return serverPlayer.clientError("Game option merge failed");
        }

        UpdateGameOptionsMessage message
            = new UpdateGameOptionsMessage(spec.getGameOptions());
        freeColServer.sendToAll(message, serverPlayer);
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // Suppress toXML for now
        throw new XMLStreamException(getType() + ".toXML NYI");
    }


    // Override DOMMessage

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.options).toXMLElement();
    }
}
