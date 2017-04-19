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
        this.attacker = getChild(game, element, 0, true, Unit.class);
        this.defender = getChild(game, element, 1, true, Unit.class);
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
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        return null; // Only sent to client
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        // Suppress toXML for now
        throw new XMLStreamException(getType() + ".toXML NYI");
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
