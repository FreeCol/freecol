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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;


/**
 * Represents the need for goods within a {@code Colony}.
 */
public final class GoodsWish extends Wish {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(GoodsWish.class.getName());

    public static final String TAG = "goodsWish";

    /** The type of goods required. */
    private GoodsType goodsType;

    /** The amount of goods required. */
    private int amountRequested;


    /**
     * Creates a new uninitialized {@code GoodsWish}.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public GoodsWish(AIMain aiMain, String id) {
        super(aiMain, id);

        this.goodsType = null;
        this.amountRequested = -1;
        this.initialized = false;
    }

    /**
     * Creates a new {@code GoodsWish}.
     *
     * @param aiMain The main AI-object.
     * @param destination The {@code Location} in which the
     *       {@link Wish#getTransportable transportable} assigned to
     *       this {@code GoodsWish} will have to reach.
     * @param value The value identifying the importance of
     *       this {@code Wish}.
     * @param amountRequested The amount requested.
     * @param goodsType The type of goods needed for releasing this wish
     *       completly.
     */
    public GoodsWish(AIMain aiMain, Location destination, int value,
                     int amountRequested, GoodsType goodsType) {
        this(aiMain, TAG + ":" + aiMain.getNextId());

        if (destination == null) {
            throw new NullPointerException("destination == null: " + this);
        }

        this.destination = destination;
        setValue(value);
        this.goodsType = goodsType;
        this.amountRequested = amountRequested;
        setInitialized();
    }

    /**
     * Creates a new {@code GoodsWish} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public GoodsWish(AIMain aiMain,
                     FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);
        
        setInitialized();
    }


    /**
     * {@inheritDoc}
     */
    public void setInitialized() {
        this.initialized = getGoodsType() != null && getGoodsAmount() > 0;
    }

    /**
     * Updates this {@code GoodsWish} with the given attributes.
     *
     * @param goodsType The {@code GoodsType} to wish for.
     * @param amount The amount of goods.
     * @param value The urgency of the wish.
     */
    public void update(GoodsType goodsType, int amount, int value) {
        this.goodsType = goodsType;
        this.amountRequested = amount;
        setValue(value);
        if (transportable != null) transportable.incrementTransportPriority();
    }

    /**
     * Checks if this {@code Wish} needs to be stored in a savegame.
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
     * {@inheritDoc}
     */
    public boolean satisfiedBy(Unit unit) {
        return false;
    }

    /**
     * Does some specified goods satisfy this wish?
     *
     * @param <T> The base type of the goods.
     * @param goods The goods to test.
     * @return True if the goods type matches and amount is not less than
     *     that requested.
     */
    public <T extends AbstractGoods> boolean satisfiedBy(T goods) {
        return goods.getType() == goodsType
            && goods.getAmount() >= amountRequested;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (goodsType == null) {
            lb.add("\n  GoodsWish without type: ", getId());
            result = result.fail();
        } else if (amountRequested <= 0) {
            lb.add("\n  GoodsWish with non-positive requested: ", getId());
            result = result.fail();
        }            
        return result;
    }


    // Serialization

    private static final String AMOUNT_REQUESTED_TAG = "amountRequested";
    private static final String GOODS_TYPE_TAG = "goodsType";
    private static final String TRANSPORTABLE_TAG = "transportable";


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

        final AIMain aiMain = getAIMain();
        final Specification spec = getSpecification();

        // Delegated from Wish
        transportable = (xr.hasAttribute(TRANSPORTABLE_TAG))
            ? xr.makeAIObject(aiMain, TRANSPORTABLE_TAG,
                              AIGoods.class, (AIGoods)null, true)
            : null;

        goodsType = xr.getType(spec, GOODS_TYPE_TAG,
                               GoodsType.class, (GoodsType)null);

        amountRequested = xr.getAttribute(AMOUNT_REQUESTED_TAG,
                                          GoodsContainer.CARGO_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof GoodsWish)) return false;
        GoodsWish other = (GoodsWish)o;
        return this.amountRequested == other.amountRequested
            && Utils.equals(this.goodsType, other.goodsType)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(this.goodsType);
        return 37 * hash + this.amountRequested;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(32);
        lb.add("[", getId(),
            " ", amountRequested,
            " ", ((goodsType == null) ? "null" : goodsType.getSuffix()),
            " -> ", destination,
            " (", getValue(), ")]");
        return lb.toString();
    }
}
