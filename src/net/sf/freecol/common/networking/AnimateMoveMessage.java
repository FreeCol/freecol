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
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;

import org.w3c.dom.Element;


/**
 * The message sent to tell a client to show a movement animation.
 */
public class AnimateMoveMessage extends ObjectMessage {

    public static final String TAG = "animateMove";
    private static final String NEW_TILE_TAG = "newTile";
    private static final String OLD_TILE_TAG = "oldTile";
    private static final String UNIT_TAG = "unit";

    /**
     * The unit to move *if* it is not currently visible, or its carrier
     * if on a carrier.
     */
    private Unit unit = null;


    /**
     * Create a new {@code AnimateMoveMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} to move.
     * @param direction The {@code Direction} to move in.
     */
    public AnimateMoveMessage(Unit unit, Tile oldTile, Tile newTile,
                              boolean appears) {
        super(TAG, UNIT_TAG, unit.getId(),
              NEW_TILE_TAG, newTile.getId(), OLD_TILE_TAG, oldTile.getId());
        if (appears) {
            this.unit = (unit.isOnCarrier()) ? unit.getCarrier() : unit;
        }
    }

    /**
     * Create a new {@code AnimateMoveMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AnimateMoveMessage(Game game, Element element) {
        super(TAG, NEW_TILE_TAG, getStringAttribute(element, NEW_TILE_TAG),
              OLD_TILE_TAG, getStringAttribute(element, OLD_TILE_TAG),
              UNIT_TAG, getStringAttribute(element, UNIT_TAG));
        this.unit = getChild(game, element, 0, true, Unit.class);
    }

    /**
     * Create a new {@code AnimateMoveMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public AnimateMoveMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, NEW_TILE_TAG, OLD_TILE_TAG, UNIT_TAG);

        this.unit = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Unit.TAG.equals(tag)) {
                if (this.unit == null) {
                    this.unit = xr.readFreeColObject(game, Unit.class);
                } else {
                    expected(TAG, tag);
                }
            } else {
                expected(Unit.TAG, tag);
            }
            xr.expectTag(tag);
        }
        xr.expectTag(TAG);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.ANIMATION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void aiHandler(FreeColServer freeColServer, AIPlayer aiPlayer) {
        // Ignored
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final Game game = freeColClient.getGame();
        final Player player = freeColClient.getMyPlayer();
        final Unit unit = getUnit(game);
        final Tile oldTile = getOldTile(game);
        final Tile newTile = getNewTile(game);

        if (unit == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing Unit.");
            return;
        }
        if (oldTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing old Tile.");
            return;
        }
        if (newTile == null) {
            logger.warning("Animation for: " + player.getId()
                + " missing new Tile.");
            return;
        }

        // This only performs animation, if required.  It does not
        // actually change unit positions, which happens in an "update".
        igc(freeColClient).animateMoveHandler(unit, oldTile, newTile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.unit != null) this.unit.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            NEW_TILE_TAG, getStringAttribute(NEW_TILE_TAG),
            OLD_TILE_TAG, getStringAttribute(OLD_TILE_TAG),
            UNIT_TAG, getStringAttribute(UNIT_TAG))
            .add(this.unit).toXMLElement();
    }


    // Public interface

    /**
     * Get the unit that is moving.
     *
     * @return The {@code Unit} that moves.
     */
    public Unit getUnit(Game game) {
        return game.getFreeColGameObject(getStringAttribute(UNIT_TAG),
                                         Unit.class);
    }

    /**
     * Get the tile to move to.
     *
     * @return The new {@code Tile}.
     */
    public Tile getNewTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(NEW_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the tile to move from.
     *
     * @return The old {@code Tile}.
     */
    public Tile getOldTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OLD_TILE_TAG),
                                         Tile.class);
    }
}
