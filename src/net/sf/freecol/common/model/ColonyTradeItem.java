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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class ColonyTradeItem extends TradeItem {

    /**
     * The ID of the colony to change hands.
     */
    private String colonyId;

    /**
     * The colony to change hands.
     */
    private Colony colony;

    /**
     * The colony name, when the colony is unknown to the offer recipient.
     */
    private String colonyName;


    /**
     * Creates a new <code>ColonyTradeItem</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param source a <code>Player</code> value
     * @param destination a <code>Player</code> value
     * @param colony a <code>Colony</code> value
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
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     */
    public ColonyTradeItem(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXMLImpl(in);
    }

    /**
     * Returns whether this TradeItem is valid.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isValid() {
        return (colony.getOwner() == getSource() &&
                getDestination().isEuropean());
    }

    /**
     * Returns whether this TradeItem must be unique. This is true for
     * the StanceTradeItem and the GoldTradeItem, and false for all
     * others.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isUnique() {
        return false;
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


    /**
     * Get the colony to trade.
     *
     * @return The colony to trade.
     */
    @Override
    public Colony getColony() {
        return colony;
    }

    /**
     * Set the colony to trade.
     *
     * @param colony The new <code>Colony</code> to trade.
     */
    @Override
    public void setColony(Colony colony) {
        this.colony = colony;
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("colony", colonyId);
        out.writeAttribute("colonyName", colonyName);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        super.readFromXMLImpl(in);

        colonyName = in.getAttributeValue(null, "colonyName");

        colonyId = in.getAttributeValue(null, "colony");
        colony = game.getFreeColGameObject(colonyId, Colony.class);

        in.nextTag();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "colonyTradeItem".
     */
    public static String getXMLElementTagName() {
        return "colonyTradeItem";
    }
}
