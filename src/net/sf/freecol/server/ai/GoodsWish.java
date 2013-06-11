/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Specification;

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
     * @param id The object identifier.
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
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsWish(AIMain aiMain, FreeColXMLReader xr)
        throws XMLStreamException {
        super(aiMain, xr);

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
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        if (goodsType == null || amountRequested <= 0) result = -1;
        return result;
    }


    // Serialization

    private static final String AMOUNT_REQUESTED_TAG = "amountRequested";
    private static final String GOODS_TYPE_TAG = "goodsType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(GOODS_TYPE_TAG, goodsType);

        xw.writeAttribute(AMOUNT_REQUESTED_TAG, amountRequested);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        goodsType = xr.getType(spec, GOODS_TYPE_TAG,
                               GoodsType.class, (GoodsType)null);

        amountRequested = xr.getAttribute(AMOUNT_REQUESTED_TAG,
                                          GoodsContainer.CARGO_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (goodsType != null && amountRequested > 0) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" for ").append(destination)
            .append(" on ").append(transportable)
            .append(" ").append(amountRequested)
            .append(" ").append(goodsType)
            .append(" (").append(getValue())
            .append(")]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "goodsWish"
     */
    public static String getXMLElementTagName() {
        return "goodsWish";
    }
}
