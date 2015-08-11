/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.util.Utils;


/**
 * A trade item consisting of a player to incite war against.
 */
public class InciteTradeItem extends TradeItem {
    
    /** The victim player. */
    private Player victim;


    /**
     * Creates a new <code>InciteTradeItem</code> inincite.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param victim The <code>Player</code> to incite against.
     */
    public InciteTradeItem(Game game, Player source, Player destination,
                           Player victim) {
        super(game, Messages.nameKey("model.tradeItem.incite"),
              source, destination);

        this.victim = victim;
    }

    /**
     * Creates a new <code>InciteTradeItem</code> inincite.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr A <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    public InciteTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);
    }


    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid() {
        return victim != null && victim != getSource()
            && victim != getDestination();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUnique() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLabel() {
        return StringTemplate.template(Messages.descriptionKey("model.tradeItem.incite"))
            .addStringTemplate("%nation%", victim.getNationLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getVictim() {
        return victim;
    }

    /**
     * {@inheritDoc}
     */
    public int evaluateFor(Player player) {
        final Player victim = getVictim();
        switch (player.getStance(victim)) {
        case ALLIANCE:
            return Integer.MIN_VALUE;
        case WAR: // Not invalid, other player may not know our stance
            return 0;
        default:
            break;
        }
        double ratio = player.getStrengthRatio(victim, false);
        // FIXME: magic#, needs rebalancing
        int value = (int)Math.round(30 * ratio);
        return (getSource() == player) ? -value : value;
    }
    

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof InciteTradeItem) {
            return Utils.equals(this.victim, ((InciteTradeItem)other).victim)
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 37 * hash + Utils.hashCode(this.victim);
    }


    // Serialization

    private static final String VICTIM_TAG = "victim";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VICTIM_TAG, victim);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        victim = xr.findFreeColGameObject(game, VICTIM_TAG,
                                          Player.class, (Player)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" ").append(victim.getId()).append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "inciteTradeItem".
     */
    public static String getXMLElementTagName() {
        return "inciteTradeItem";
    }
}
