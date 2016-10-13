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

import java.awt.Color;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


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
     * Create a new {@code SetColorMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to (null here).
     * @param element The {@code Element} to use to create the message.
     */
    public SetColorMessage(Game game, Element element) {
        super(TAG, NATION_TAG, getStringAttribute(element, NATION_TAG),
              COLOR_TAG, getStringAttribute(element, COLOR_TAG));
    }


    // Public interface

    /**
     * Get the nation whose color is changing.
     *
     * @param spec The {@code Specification} to look up the nation in.
     * @return The {@code Nation}.
     */
    public Nation getNation(Specification spec) {
        return spec.getNation(getAttribute(NATION_TAG));
    }

    /**
     * Get the color.
     *
     * @return The new {@Color}.
     */
    public Color getColor() {
        Color color = null;
        try {
            int rgb = Integer.decode(getAttribute(COLOR_TAG));
            color = new Color(rgb);
        } catch (NumberFormatException nfe) {}
        return color;
    }


    /**
     * Handle a "setColor"-message from a client.
     * 
     * @param server The {@code FreeColServer} that handles the message.
     * @param connection The {@code Connection} the message is from.
     * @return Null.
     */
    public Element handle(FreeColServer server, Connection connection) {
        final ServerPlayer serverPlayer = server.getPlayer(connection);
        final Specification spec = serverPlayer.getGame().getSpecification();
        
        if (serverPlayer != null) {
            Nation nation = getNation(spec);
            if (nation != null) {
                Color color = getColor();
                if (color != null) {
                    nation.setColor(color);
                    server.sendToAll(new SetColorMessage(nation, color),
                                     connection);
                } else {
                    logger.warning("Invalid color: " + this.toString());
                }
            } else {
                logger.warning("Invalid nation: " + this.toString());
            }
        } else {
            logger.warning("setColor from unknown connection.");
        }
        return null;
    }
}
