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

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.GameOptions;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to update the game options.
 */
public class UpdateGameOptionsMessage extends ObjectMessage {

    public static final String TAG = "updateGameOptions";


    /**
     * Create a new {@code UpdateGameOptionsMessage} with the supplied name.
     *
     * @param optionGroup The game options {@code OptionGroup}.
     */
    public UpdateGameOptionsMessage(OptionGroup optionGroup) {
        super(TAG);

        appendChild(optionGroup);
    }

    /**
     * Create a new {@code UpdateGameOptionsMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public UpdateGameOptionsMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        this(null);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        OptionGroup optionGroup = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (OptionGroup.TAG.equals(tag)) {
                    if (optionGroup == null) {
                        optionGroup = xr.readFreeColObject(game, OptionGroup.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(OptionGroup.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(optionGroup);
    }


    /**
     * Get the associated option group.
     *
     * @return The options.
     */
    private OptionGroup getGameOptions() {
        return getChild(0, OptionGroup.class);
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
        final OptionGroup gameOptions = getGameOptions();

        if (freeColClient.isInGame()) {
            ; // Ignored
        } else {
            pgc(freeColClient).updateGameOptionsHandler(gameOptions);
        }
        clientGeneric(freeColClient);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            return ChangeSet.clientError((ChangeSet.See)null, "Missing serverPlayer");
        } else if (!serverPlayer.isAdmin()) {
            return serverPlayer.clientError("Not an admin: " + serverPlayer);
        } else if (freeColServer.getServerState() != FreeColServer.ServerState.PRE_GAME) {
            return serverPlayer.clientError("Can not change game options, "
                + "server state = " + freeColServer.getServerState());
        }

        final OptionGroup gameOptions = getGameOptions();
        if (gameOptions == null) {
            return serverPlayer.clientError("No game options to merge");
        }

        return pgc(freeColServer)
            .updateGameOptions(serverPlayer, gameOptions);
    }
}
