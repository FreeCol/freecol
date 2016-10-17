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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to update the game options.
 */
public class UpdateGameOptionsMessage extends DOMMessage {

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
     * Internal constructor from element.
     *
     * @param element The {@code Element} to read from.
     */
    protected UpdateGameOptionsMessage(Element element) {
        this(new OptionGroup(GameOptions.TAG));

        readFromXMLElement(this.options, element);
    }
    
    /**
     * Create a new {@code UpdateGameOptionsMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public UpdateGameOptionsMessage(Game game, Element element) {
        this(getChildElement(element, OptionGroup.TAG));
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
     * Handle a "updateGameOptions"-message.
     *
     * @param freeColServer The {@code FreeColServer} handling
     *     the message.
     * @param connection The {@code Connection} message was received on.
     * @return Null (although other players are updated on success) or
     *     an error message if the options do not update.
     */
    public Element handle(FreeColServer freeColServer, Connection connection) {
        final ServerPlayer serverPlayer = freeColServer.getPlayer(connection);
        if (serverPlayer == null || !serverPlayer.isAdmin()) {
            return serverPlayer.clientError("Not an admin: " + serverPlayer)
                .build(serverPlayer);
        }
        final Specification spec = freeColServer.getGame().getSpecification();
        if (this.options == null) {
            return serverPlayer.clientError("No game options to merge")
                .build(serverPlayer);
        }
        if (!spec.mergeGameOptions(this.options, "server")) {
            return serverPlayer.clientError("Game option merge failed")
                .build(serverPlayer);
        }

        UpdateGameOptionsMessage message
            = new UpdateGameOptionsMessage(spec.getGameOptions());
        freeColServer.sendToAll(message, connection);
        return null;
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
