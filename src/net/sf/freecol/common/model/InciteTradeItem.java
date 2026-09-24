/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.util.Objects;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A trade item consisting of a player to incite war against.
 */
public class InciteTradeItem extends TradeItem {
    
    public static final String TAG = "inciteTradeItem";

    private static final double BASE_INCITE_COST = 50.0;

    /** The victim player. */
    private Player victim;

    
    /**
     * Creates a new {@code InciteTradeItem} incite.
     *
     * @param game The enclosing {@code Game}.
     * @param source The source {@code Player}.
     * @param destination The destination {@code Player}.
     * @param victim The {@code Player} to incite against.
     */
    public InciteTradeItem(Game game, Player source, Player destination,
                           Player victim) {
        super(game, Messages.nameKey("model.tradeItem.incite"),
              source, destination);

        this.victim = victim;
    }

    /**
     * Creates a new {@code InciteTradeItem} incite.
     *
     * @param game The enclosing {@code Game}.
     * @param xr A {@code FreeColXMLReader} to read from.
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
        return this.victim != null && this.victim != getSource()
            && this.victim != getDestination();
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
            .addStringTemplate("%nation%", this.victim.getNationLabel());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Player getVictim() {
        return this.victim;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int evaluateFor(Player player) {
        if (!isValid()) {
            return INVALID_TRADE_ITEM;
        }

        final Player victim = getVictim();
        switch (player.getStance(victim)) {
            case ALLIANCE: return INVALID_TRADE_ITEM;
            case WAR:      return 0;
            default:       break;
        }

        double ratio = player.getStrengthRatio(victim, false);

        // Handle error, undefined, or negative ratio → treat as "unknown strength"
        if (Double.isNaN(ratio) || ratio < 0.0) {
            ratio = 0.5; // neutral fallback (equal strength)
        }
        // Handle real zero strength → treat as extremely weak
        else if (ratio == 0.0) {
            ratio = 0.1; // very weak fallback
        }

        // Calculation: BASE_INCITE_COST (50.0) / ratio
        return -(int) Math.round(BASE_INCITE_COST / ratio);
    }
    

    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        InciteTradeItem o = copyInCast(other, InciteTradeItem.class);
        if (o == null || !super.copyIn(o)) return false;
        this.victim = getGame().updateRef(o.getVictim());
        return true;
    }


    // Serialization

    private static final String VICTIM_TAG = "victim";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VICTIM_TAG, this.victim);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.victim = xr.findFreeColGameObject(getGame(), VICTIM_TAG,
                                               Player.class, (Player)null, true);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        
        if (o == null || getClass() != o.getClass()) return false;
        
        InciteTradeItem other = (InciteTradeItem) o;
        return Objects.equals(this.victim, other.victim)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.victim);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName()
            + "[id=" + getId()
            + ", victim=" + ((victim == null) ? "null" : victim.getId())
            + "]";
    }
}
