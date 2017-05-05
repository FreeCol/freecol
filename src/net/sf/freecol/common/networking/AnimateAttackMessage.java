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
     * The attacker unit *if* it is not currently visible, or on a carrier.
     */
    private Unit attacker = null;

    /**
     * The defender unit *if* it is not currently visible, or on a carrier.
     */
    private Unit defender = null;
    

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

        this.attacker = (addAttacker)
            ? ((attacker.isOnCarrier()) ? attacker.getCarrier() : attacker)
            : null;
        this.defender = (addDefender)
            ? ((defender.isOnCarrier()) ? defender.getCarrier() : defender)
            : null;
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

        // Not necessarily correct, but we are using interning reads
        // to whatever is here will go into the client game, and then
        // getAttacker/Defender will work as they use the correct identifiers.
        this.attacker = getChild(game, element, 0, false, Unit.class);
        this.defender = getChild(game, element, 1, false, Unit.class);
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

        this.attacker = this.defender = null;
        while (xr.moreTags()) {
            String tag = xr.getLocalName();
            if (Unit.TAG.equals(tag)) {
                if (this.attacker == null) {
                    this.attacker = xr.readFreeColObject(game, Unit.class);
                } else if (this.defender == null) {
                    this.defender = xr.readFreeColObject(game, Unit.class);
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
        final Tile attackerTile = getAttackerTile(game);
        final Tile defenderTile = getDefenderTile(game);
        final boolean result = getResult();
        Unit attacker = getAttacker(game);
        Unit defender = getDefender(game);

        if (attacker == null) {
            if (this.attacker == null) {
                logger.warning("Attack animation for: " + player.getId()
                    + " missing attacker.");
                return;
            }
            attacker = this.attacker;
            attacker.intern();
            String att = getStringAttribute(ATTACKER_TAG);
            if (!attacker.getId().equals(att)) { // actually on carrier
                attacker = attacker.getCarriedUnitById(att);
                if (attacker == null) {
                    logger.warning("Attack animation for: " + player.getId()
                        + " missing attacker with identifier: " + att);
                    return;
                }
            }
        }
        if (defender == null) {
            if (this.defender == null) {
                logger.warning("Attack animation for: " + player.getId()
                    + " omitted defender.");
                return;
            }
            defender = this.defender;
            defender.intern();
            String def = getStringAttribute(DEFENDER_TAG);
            if (!defender.getId().equals(def)) { // actually on carrier
                defender = defender.getCarriedUnitById(def);
                if (defender == null) {
                    logger.warning("Attack animation for: " + player.getId()
                        + " missing defender with identifier: " + def);
                    return;
                }
            }                
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
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (this.attacker != null) this.attacker.toXML(xw);
        if (this.defender != null) this.defender.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return new DOMMessage(TAG,
            ATTACKER_TAG, getStringAttribute(ATTACKER_TAG),
            ATTACKER_TILE_TAG, getStringAttribute(ATTACKER_TILE_TAG),
            DEFENDER_TAG, getStringAttribute(DEFENDER_TAG),
            DEFENDER_TILE_TAG, getStringAttribute(DEFENDER_TILE_TAG),
            SUCCESS_TAG, getStringAttribute(SUCCESS_TAG))
            .add(this.attacker).add(this.defender).toXMLElement();
    }


    // Public interface

    /**
     * Get the attacker unit.
     *
     * @return The attacker {@code Unit}.
     */
    public Unit getAttacker(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ATTACKER_TAG),
                                         Unit.class);
    }

    /**
     * Get the defender unit.
     *
     * @return The defender {@code Unit}.
     */
    public Unit getDefender(Game game) {
        return game.getFreeColGameObject(getStringAttribute(DEFENDER_TAG),
                                         Unit.class);
    }

    /**
     * Get the attacker tile.
     *
     * @return The attacker {@code Tile}.
     */
    public Tile getAttackerTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(ATTACKER_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the defender tile.
     *
     * @return The defender {@code Tile}.
     */
    public Tile getDefenderTile(Game game) {
        return game.getFreeColGameObject(getStringAttribute(DEFENDER_TILE_TAG),
                                         Tile.class);
    }

    /**
     * Get the result of the attack.
     *
     * @return The result.
     */
    public boolean getResult() {
        return getBooleanAttribute(SUCCESS_TAG, false);
    }
}
