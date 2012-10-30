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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;

import org.w3c.dom.Element;


/**
 * Represents the need for goods within a <code>Colony</code>.
 */
public class GoodsWish extends Wish {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GoodsWish.class.getName());

    /** The type of goods required. */
    private GoodsType goodsType;

    /** The amount of goods required. */
    private int amountRequested;


    /**
     * Creates a new uninitialized <code>GoodsWish</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */
    public GoodsWish(AIMain aiMain, String id) {
        super(aiMain, id);

        goodsType = null;
        amountRequested = -1;
    }

    /**
     * Creates a new <code>GoodsWish</code>.
     *
     * @param aiMain The main AI-object.
     * @param destination The <code>Location</code> in which the
     *       {@link Wish#getTransportable transportable} assigned to
     *       this <code>GoodsWish</code> will have to reach.
     * @param value The value identifying the importance of
     *       this <code>Wish</code>.
     * @param amountRequested The amount requested.
     * @param goodsType The type of goods needed for releasing this wish
     *       completly.
     */
    public GoodsWish(AIMain aiMain, Location destination, int value,
                     int amountRequested, GoodsType goodsType) {
        this(aiMain, getXMLElementTagName() + ":" + aiMain.getNextId());

        if (destination == null) {
            throw new NullPointerException("destination == null");
        }

        this.destination = destination;
        setValue(value);
        this.goodsType = goodsType;
        this.amountRequested = amountRequested;
        uninitialized = false;
    }

    /**
     * Creates a new <code>GoodsWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation
     *       of a <code>Wish</code>.
     */
    public GoodsWish(AIMain aiMain, Element element) {
        super(aiMain, element);

        uninitialized = goodsType == null;
    }

    /**
     * Creates a new <code>GoodsWish</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsWish(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);

        uninitialized = goodsType == null;
    }


    /**
     * Checks if this <code>Wish</code> needs to be stored in a savegame.
     *
     * @return True.  We always store goods wishes.
     */
    @Override
    public boolean shouldBeStored() {
        return true;
    }

    /**
     * Gets the type of goods wished for.
     *
     * @return The type of goods wished for.
     */
    public GoodsType getGoodsType() {
        return goodsType;
    }

    /**
     * Does some specified goods satisfy this wish?
     *
     * @param goods The <code>Goods</code> to test.
     * @return True if the goods type matches and amount is not less than
     *     that requested.
     */
    public boolean satisfiedBy(Goods goods) {
        return goods.getType() == goodsType
            && goods.getAmount() >= amountRequested;
    }

    /**
     * Gets the amount of goods wished for.
     *
     * @return The amount of goods wished for.
     */
    public int getGoodsAmount() {
        return amountRequested;
    }

    /**
     * Sets the amount of goods wished for.
     * Called in AIColony when the colony needs to change the required goods
     * amount.
     *
     * @param amount The new amount of goods wished for.
     */
    public void setGoodsAmount(int amount) {
        amountRequested = amount;
    }

    /**
     * Checks the integrity of this AI object.
     *
     * @return True if this <code>GoodsWish</code> is valid.
     */
    @Override
    public boolean checkIntegrity() {
        return super.checkIntegrity()
            && goodsType != null
            && amountRequested > 0;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        writeAttributes(out);

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("goodsType", goodsType.getId());

        out.writeAttribute("amountRequested", Integer.toString(amountRequested));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "goodsType");
        goodsType = getSpecification().getGoodsType(str);

        amountRequested = getAttribute(in, "amountRequested",
                                       GoodsContainer.CARGO_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "[" + getId() + " for " + destination
            + " on " + transportable + " " + amountRequested
            + " " + goodsType + " (" + getValue() + ")]";
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goodsWish"
     */
    public static String getXMLElementTagName() {
        return "goodsWish";
    }
}
