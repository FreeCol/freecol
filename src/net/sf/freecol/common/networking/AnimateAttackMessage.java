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
            add1((attacker.isOnCarrier()) ? attacker.getCarrier() : attacker);
        }
        if (addDefender) {
            add1((defender.isOnCarrier()) ? defender.getCarrier() : defender);
        }
    }

    /**
     * Create a new {@code AnimateAttackMessage} from a
     * supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public AnimateAttackMessage(Game game, Element element) {
        super(TAG, ATTACKER_TAG, getStringAttribute(element, ATTACKER_TAG),
              ATTACKER_TILE_TAG, getStringAttribute(element, ATTACKER_TILE_TAG),
              DEFENDER_TAG, getStringAttribute(element, DEFENDER_TAG),
              DEFENDER_TILE_TAG, getStringAttribute(element, DEFENDER_TILE_TAG),
              SUCCESS_TAG, getStringAttribute(element, SUCCESS_TAG));

        addUnits(getChild(game, element, 0, false, Unit.class),
                 getChild(game, element, 1, false, Unit.class));
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
        Unit attacker = null, defender = null;
        try {
            while (xr.moreTags()) {
                String tag = xr.getLocalName();
                if (Unit.TAG.equals(tag)) {
                    if (attacker == null) {
                        attacker = xr.readFreeColObject(game, Unit.class);
                    } else if (defender == null) {
                        defender = xr.readFreeColObject(game, Unit.class);
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
        addUnits(attacker, defender);
    }


    /**
     * Add the units.
     *
     * @param u0 The first {@code Unit} found.
     * @param u1 The second {@code Unit} found.
     */
    private void addUnits(Unit u0, Unit u1) {
        if (u0 != null && u1 == null
            && u0.getId().equals(getStringAttribute(DEFENDER_TAG))) {
            u1 = u0;
            u0 = null;
        }
        add1(u0);
        add1(u1);
    }
        
    /**
     * Get the attacker unit.
     *
     * @return The attacker {@code Unit}.
     */
    private Unit getAttacker(Game game) {
        final String att = getStringAttribute(ATTACKER_TAG);
        Unit unit = game.getFreeColGameObject(att, Unit.class);
        if (unit == null) {
            if ((unit = getChild(0, Unit.class)) == null) {
                logger.warning("Attack animation missing attacker: " + att);
            } else {
                unit.intern();
                if (!unit.getId().equals(att)) { // actually on carrier
                    unit = unit.getCarriedUnitById(att);
                    if (unit == null) {
                        logger.warning("Attack animation missing carried attacker: " + att);
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Get the defender unit.
     *
     * @return The defender {@code Unit}.
     */
    private Unit getDefender(Game game) {
        final String def = getStringAttribute(DEFENDER_TAG);
        Unit unit = game.getFreeColGameObject(def, Unit.class);
        if (unit == null) {
            if ((unit = getChild(1, Unit.class)) == null) {
                logger.warning("Attack animation missing defender: " + def);
            } else {
                unit.intern();
                if (!unit.getId().equals(def)) { // actually on carrier
                    unit = unit.getCarriedUnitById(def);
                    if (unit == null) {
                        logger.warning("Attack animation missing carried defender: " + def);
                    }
                }
            }
        }
        return unit;
    }

    /**
     * Get the attacker tile.
     *
     * @return The attacker {@code Tile}.
     */
    private Tile getAttackerTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ATTACKER_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the defender tile.
     *
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
        return getBooleanAttribute(SUCCESS_TAG, false);
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

        if (attacker == null || defender == null) return;
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
    }
}
