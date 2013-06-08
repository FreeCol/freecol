/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A trade item consisting of a colony.
 */
public class ColonyTradeItem extends TradeItem {

    /** The identifier of the colony to change hands. */
    private String colonyId;

    /** The colony to change hands. */
    private Colony colony;

    /** The colony name, when the colony is unknown to the offer recipient. */
    private String colonyName;


    /**
     * Creates a new <code>ColonyTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param source The source <code>Player</code>.
     * @param destination The destination <code>Player</code>.
     * @param colony The <code>Colony</code> to trade.
     */
    public ColonyTradeItem(Game game, Player source, Player destination,
                           Colony colony) {
        super(game, "tradeItem.colony", source, destination);
        this.colony = colony;
        colonyId = colony.getId();
        colonyName = colony.getName();
    }

    /**
     * Creates a new <code>ColonyTradeItem</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
     */
    public ColonyTradeItem(Game game, FreeColXMLReader xr) throws XMLStreamException {
        super(game, xr);

        readFromXML(xr);
    }


    /**
     * Extract the colony name.  Necessary as the colony may not actually be
     * known by a recipient of an offer.
     *
     * @return The colony name.
     */
    public String getColonyName() {
        return colonyName;
    }

    // Interface TradeItem

    /**
     * {@inheritDoc}
     */
    public boolean isValid() {
        return (colony.getOwner() == getSource() &&
                getDestination().isEuropean());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUnique() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Colony getColony() {
        return colony;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setColony(Colony colony) {
        this.colony = colony;
    }


    // Serialization

    private static final String COLONY_TAG = "colony";
    private static final String COLONY_NAME_TAG = "colonyName";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLONY_TAG, colonyId);

        xw.writeAttribute(COLONY_NAME_TAG, colonyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        Colony colony = xr.getAttribute(getGame(), COLONY_TAG,
                                        Colony.class, (Colony)null);
        colonyId = (colony == null) ? null : colony.getId();

        colonyName = xr.getAttribute(COLONY_NAME_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colonyTradeItem".
     */
    public static String getXMLElementTagName() {
        return "colonyTradeItem";
    }
}
