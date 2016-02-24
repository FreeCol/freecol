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

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * The message sent to update the map generator options.
 */
public class UpdateMapGeneratorOptionsMessage extends DOMMessage {

    public static final String TAG = "updateMapGeneratorOptions";

    /** The options. */
    private final OptionGroup options;


    /**
     * Create a new <code>UpdateMapGeneratorOptionsMessage</code> with the
     * supplied name.
     *
     * @param options The map generator options <code>OptionGroup</code>.
     */
    public UpdateMapGeneratorOptionsMessage(OptionGroup options) {
        super(getTagName());

        this.options = options;
    }

    /**
     * Create a new <code>UpdateMapGeneratorOptionsMessage</code> from a
     * supplied element.
     *
     * @param game The <code>Game</code> this message belongs to.
     * @param element The <code>Element</code> to use to create the message.
     */
    public UpdateMapGeneratorOptionsMessage(Game game, Element element) {
        this(getChild(game, element, 0, OptionGroup.class));
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

    /**
     * Merge the options in this message into the given game.
     *
     * @param game The <code>Game</code> to merge to.
     * @return True if the merge succeeds.
     */
    public boolean mergeOptions(Game game) {
        boolean result = false;        
        if (this.options != null) {
            OptionGroup mapGeneratorOptions = game.getMapGeneratorOptions();
            result = mapGeneratorOptions.merge(this.options);
        }
        return result;
    }


    /**
     * Handle a "updateMapGeneratorOptions"-message.
     *
     * @param freeColServer The <code>FreeColServer</code> handling
     *     the message.
     * @param player The <code>Player</code> that sent the message.
     * @param connection The <code>Connection</code> message was received on.
     * @return Null (although other players are updated on success) or
     *     an error message if the options do not update.
     */
    public Element handle(FreeColServer freeColServer, Player player,
                          Connection connection) {
        final ServerPlayer serverPlayer = freeColServer.getPlayer(connection);
        if (player == null || !player.isAdmin()) {
            return serverPlayer.clientError("Not an admin: " + player)
                .build(serverPlayer);
        }
        final Game game = freeColServer.getGame();
        if (!mergeOptions(game)) {
            return serverPlayer.clientError("Option merge failed")
                .build(serverPlayer);
        }

        DOMMessage message = new UpdateMapGeneratorOptionsMessage(game.getMapGeneratorOptions());
        freeColServer.sendToAll(message, connection);
        return null;
    }


    // Override DOMMessage

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(getTagName())
            .add(this.options).toXMLElement();
    }

    /**
     * The tag name of the root element representing this object.
     *
     * @return "updateMapGeneratorOptions".
     */
    public static String getTagName() {
        return TAG;
    }
}
