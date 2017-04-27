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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import net.sf.freecol.common.util.DOMUtils;
import org.w3c.dom.Element;


/**
 * The message sent to update the map generator options.
 */
public class UpdateMapGeneratorOptionsMessage extends ObjectMessage {

    public static final String TAG = "updateMapGeneratorOptions";

    /** The options. */
    private final OptionGroup options;


    /**
     * Create a new {@code UpdateMapGeneratorOptionsMessage} with the
     * supplied name.
     *
     * @param options The map generator options {@code OptionGroup}.
     */
    public UpdateMapGeneratorOptionsMessage(OptionGroup options) {
        super(TAG);

        this.options = options;
    }

    /**
     * Create a new {@code UpdateMapGeneratorOptionsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateMapGeneratorOptionsMessage(Game game, Element element) {
        this(DOMUtils.getChild(game, element, 0, false, OptionGroup.class));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return MessagePriority.NORMAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final OptionGroup mapOptions = getMapGeneratorOptions();

        if (freeColClient.isInGame()) {
            ; // Ignore
        } else {
            pgc(freeColClient).updateMapGeneratorOptionsHandler(mapOptions);
        }
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
        final OptionGroup optionGroup = getMapGeneratorOptions();
        if (optionGroup == null) {
            return serverPlayer.clientError("No game options to merge");
        }

        return pgc(freeColServer)
            .updateMapGeneratorOptions(serverPlayer, optionGroup);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.options != null) this.options.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG)
            .add(this.options).toXMLElement();
    }

    
    // Public interface

    /**
     * Get the associated option group.
     *
     * @return The options.
     */
    public OptionGroup getMapGeneratorOptions() {
        return this.options;
    }
}
