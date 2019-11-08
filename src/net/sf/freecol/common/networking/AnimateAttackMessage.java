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

import java.util.ArrayList;
import java.util.List;

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
 * The message sent to tell a client to show an attack animation.
 */
public class AnimateAttackMessage extends ObjectMessage {

    public static final String TAG = "animateAttack";
    private static final String ATTACKER_TAG = "attacker";
    private static final String ATTACKER_TILE_TAG = "attackerTile";
    private static final String DEFENDER_TAG = "defender";
    private static final String DEFENDER_TILE_TAG = "defenderTile";
    private static final String SUCCESS_TAG = "success";


    /**
     * Create a new {@code AnimateAttackMessage} for the supplied attacker,
     * defender, result and visibility information.
     *
     * @param attacker The attacking {@code Unit}.
     * @param defender The defending {@code Unit}.
     * @param result Whether the attack succeeds.
     * @param addAttacker Whether to attach the attacker unit.
     * @param addDefender Whether to attach the defender unit.
     */
    public AnimateAttackMessage(Unit attacker, Unit defender, boolean result,
                                boolean addAttacker, boolean addDefender) {
        super(TAG, ATTACKER_TAG, attacker.getId(),
              ATTACKER_TILE_TAG, attacker.getTile().getId(),
              DEFENDER_TAG, defender.getId(),
              DEFENDER_TILE_TAG, defender.getTile().getId(),
              SUCCESS_TAG, Boolean.toString(result));

        if (addAttacker) {
            appendChild((attacker.isOnCarrier()) ? attacker.getCarrier()
                : attacker);
        }
        if (addDefender) {
            appendChild((defender.isOnCarrier()) ? defender.getCarrier()
                : defender);
        }
    }

    /**
     * Create a new {@code AnimateAttackMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public AnimateAttackMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, ATTACKER_TAG, ATTACKER_TILE_TAG,
              DEFENDER_TAG, DEFENDER_TILE_TAG, SUCCESS_TAG);

        FreeColXMLReader.ReadScope rs
            = xr.replaceScope(FreeColXMLReader.ReadScope.NOINTERN);
        List<Unit> units = new ArrayList<>();
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (Unit.TAG.equals(tag)) {
                    Unit u =  xr.readFreeColObject(game, Unit.class);
                    if (u != null) units.add(u);
                } else {
                    expected(Unit.TAG, tag);
                }
                xr.expectTag(tag);
            }
            xr.expectTag(TAG);
        } finally {
            xr.replaceScope(rs);
        }
        appendChildren(units);
    }


    /**
     * Get a unit by key.
     *
     * @param game The {@code Game} to look up the unit in.
     * @param key An attribute key to extract the unit identifier with.
     * @return The attacker {@code Unit}.
     */
    private Unit getUnit(Game game, String key) {
        final String id = getStringAttribute(key);
        if (id == null) return null;
        
        Unit unit = game.getFreeColGameObject(id, Unit.class);
        if (unit != null) return unit;
        
        for (Unit u : getChildren(Unit.class)) {
            if (id.equals(u.getId())) {
                u.intern();
                return u;
            }
            if ((u = u.getCarriedUnitById(id)) != null) {
                u.intern();
                return u;
            }
        }  
        return null;
    }

    /**
     * Get the attacker unit.
     *
     * @param game The {@code Game} to look up the unit in.
     * @return The attacker {@code Unit}.
     */
    private Unit getAttacker(Game game) {
        return getUnit(game, ATTACKER_TAG);
    }
    
    /**
     * Get the defender unit.
     *
     * @param game The {@code Game} to look up the unit in.
     * @return The defender {@code Unit}.
     */
    private Unit getDefender(Game game) {
        return getUnit(game, DEFENDER_TAG);
    }

    /**
     * Get the attacker tile.
     *
     * @param game The {@code Game} to look up the tile in.
     * @return The attacker {@code Tile}.
     */
    private Tile getAttackerTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ATTACKER_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the defender tile.
     *
     * @param game The {@code Game} to look up the tile in.
     * @return The defender {@code Tile}.
     */
    private Tile getDefenderTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(DEFENDER_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the result of the attack.
     *
     * @return The result.
     */
    private boolean getResult() {
        return getBooleanAttribute(SUCCESS_TAG, Boolean.FALSE);
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
        final Tile attackerTile = getAttackerTile(game);
        final Tile defenderTile = getDefenderTile(game);
        final boolean result = getResult();
        final Unit attacker = getAttacker(game);
        final Unit defender = getDefender(game);

        if (attacker == null) {
            logger.warning("Attack animation missing attacker unit.");
            return;
        }
        if (defender == null) {
            logger.warning("Attack animation missing defender unit.");
            return;
        }
        if (attackerTile == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " omitted attacker tile.");
        }
        if (defenderTile == null) {
            logger.warning("Attack animation for: " + player.getId()
                + " omitted defender tile.");
        }

        // This only performs animation, if required.  It does not
        // actually perform an attack.
        igc(freeColClient)
            .animateAttackHandler(attacker, defender,
                                  attackerTile, defenderTile, result);
        clientGeneric(freeColClient);
    }
}
