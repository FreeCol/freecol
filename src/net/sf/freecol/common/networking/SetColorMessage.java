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

import java.awt.Color;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message that changes the player color.
 */
public class SetColorMessage extends AttributeMessage {

    public static final String TAG = "setColor";
    private static final String COLOR_TAG = "color";
    private static final String NATION_TAG = "nation";
    

    /**
     * Create a new {@code SetColorMessage}.
     *
     * @param nation The {@code Nation} that is changing color.
     * @param color The new {@code Color}.
     */
    public SetColorMessage(Nation nation, Color color) {
        super(TAG, NATION_TAG, nation.getId(),
              COLOR_TAG, String.valueOf(color.getRGB()));
    }

    /**
     * Create a new {@code SetAvailableMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public SetColorMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, NATION_TAG, COLOR_TAG);
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
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Specification spec = game.getSpecification();
        final Nation nation = getNation(spec);
        final Color color = getColor();
      
        if (nation == null) {
            logger.warning("Invalid nation: " + toString());
            return;
        }
        if (color == null) {
            logger.warning("Invalid color: " + toString());
            return;
        }

        pgc(freeColClient).setColorHandler(nation, color);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        if (serverPlayer == null) {
            logger.warning("setColor from unknown connection");
        }
        
        final Specification spec = freeColServer.getGame().getSpecification();
        final Nation nation = getNation(spec);
        final Color color = getColor();

        if (nation == null) {
            return serverPlayer.clientError("Invalid nation: " + this.toString());
        } else if (color == null) {
            return serverPlayer.clientError("Invalid color: " + this.toString());
        }

        return pgc(freeColServer)
            .setColor(serverPlayer, nation, color);
    }


    // Public interface

    /**
     * Get the nation whose color is changing.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation}.
     */
    public Nation getNation(Specification spec) {
        return spec.getNation(getStringAttribute(NATION_TAG));
    }

    /**
     * Get the color.
     *
     * @return The new {@code Color}.
     */
    public Color getColor() {
        Color color = null;
        try {
            int rgb = getIntegerAttribute(COLOR_TAG, 0);
            color = new Color(rgb);
        } catch (NumberFormatException nfe) {}
        return color;
    }
}
