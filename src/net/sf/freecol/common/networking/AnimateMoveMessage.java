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
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;


/**
 * The message sent to tell a client to show a movement animation.
 */
public class AnimateMoveMessage extends ObjectMessage {

    public static final String TAG = "animateMove";
    private static final String NEW_TILE_TAG = "newTile";
    private static final String OLD_TILE_TAG = "oldTile";
    private static final String UNIT_TAG = "unit";


    /**
     * Create a new {@code AnimateMoveMessage} for the supplied unit and
     * direction.
     *
     * @param unit The {@code Unit} to move.
     * @param oldTile The {@code Tile} to move from.
     * @param newTile The {@code Tile} to move to.
     * @param appears If true, the unit is newly appearing, and either it
     *     or its carrier must be present in this message.
     */
    public AnimateMoveMessage(Unit unit, Tile oldTile, Tile newTile,
                              boolean appears) {
        super(TAG, UNIT_TAG, unit.getId(),
              NEW_TILE_TAG, newTile.getId(), OLD_TILE_TAG, oldTile.getId());

        if (appears) {
            appendChild((unit.isOnCarrier()) ? unit.getCarrier() : unit);
        }
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

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        Unit unit = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (Unit.TAG.equals(tag)) {
                    if (unit == null) {
                        unit = xr.readFreeColObject(game, Unit.class);
                    } else {
                        expected(TAG, tag);
                    }
                } else {
                    expected(Unit.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChild(unit);
    }


    /**
     * Get the unit that is moving.
     *
     * @param game The {@code Game} to look up the unit in.
     * @return The {@code Unit} that moves.
     */
    private Unit getUnit(Game game) {
        final String uid = getStringAttribute(UNIT_TAG);
        Unit unit = game.getFreeColGameObject(uid, Unit.class);
        if (unit == null) {
            if ((unit = getChild(0, Unit.class)) == null) {
                logger.warning("Move animation missing unit: " + uid);
            } else {
                unit.intern();
                if (!unit.getId().equals(uid)) { // actually on carrier
                    unit = unit.getCarriedUnitById(uid);
                    if (unit == null) {
                        logger.warning("Move animation missing carried unit: "
                            + uid);
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Get the tile to move to.
     *
     * @param game The {@code Game} to look up the tile in.
     * @return The new {@code Tile}.
     */
    private Tile getNewTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(NEW_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the tile to move from.
     *
     * @param game The {@code Game} to look up the tile in.
     * @return The old {@code Tile}.
     */
    private Tile getOldTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(OLD_TILE_TAG),
                                         Tile.class);
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
        final Tile oldTile = getOldTile(game);
        final Tile newTile = getNewTile(game);
        final Unit unit = getUnit(game);

        if (unit == null) return;
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
        clientGeneric(freeColClient);
    }
}
