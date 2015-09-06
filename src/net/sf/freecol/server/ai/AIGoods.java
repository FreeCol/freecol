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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.server.ai.mission.Mission;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Goods}.
 */
public class AIGoods extends TransportableAIObject {

    private static final Logger logger = Logger.getLogger(AIGoods.class.getName());

    /** The underlying goods. */
    private Goods goods;

    /** The destination location for the goods. */
    private Location destination;


    /**
     * Creates a new uninitialized <code>AIGoods</code>.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public AIGoods(AIMain aiMain, String id) {
        super(aiMain, id);

        this.goods = null;
        this.destination = null;
    }

    /**
     * Creates a new <code>AIGoods</code>.
     *
     * @param aiMain The main AI-object.
     * @param location The location of the goods.
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @param destination The destination of the goods. This is the
     *      <code>Location</code> to which the goods should be transported.
     */
    public AIGoods(AIMain aiMain, Location location, GoodsType type,
                   int amount, Location destination) {
        this(aiMain, getXMLElementTagName() + ":" + aiMain.getNextId());

        this.goods = new Goods(aiMain.getGame(), location, type, amount);
        this.destination = destination;

        uninitialized = false;
    }

    /**
     * Creates a new <code>AIGoods</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *       of a <code>Wish</code>.
     */
    public AIGoods(AIMain aiMain, Element element) {
        super(aiMain, element);

        uninitialized = getGoods() == null;
    }
    
    /**
     * Creates a new <code>AIGoods</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @exception XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public AIGoods(AIMain aiMain,
                   FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        uninitialized = getGoods() == null;
    }


    /**
     * Gets the goods this <code>AIGoods</code> is controlling.
     *
     * @return The <code>Goods</code>.
     */
    public final Goods getGoods() {
        return goods;
    }

    /**
     * Sets the goods this <code>AIGoods</code> is controlling.
     *
     * @param goods The new <code>Goods</code>.
     */
    public final void setGoods(Goods goods) {
        this.goods = goods;
    }

    /**
     * Gets the type of goods this <code>AIGoods</code> is controlling.
     *
     * @return The <code>GoodsType</code>.
     */
    public final GoodsType getGoodsType() {
        return goods.getType();
    }

    /**
     * Gets the amount of goods this <code>AIGoods</code> is controlling.
     *
     * @return The amount of goods.
     */
    public final int getGoodsAmount() {
        return goods.getAmount();
    }

    /**
     * Sets the amount of goods this <code>AIGoods</code> is controlling.
     *
     * @param amount The new amount of goods.
     */
    public final void setGoodsAmount(int amount) {
        goods.setAmount(amount);
    }


    // Internal

    /**
     * Goods leaves a ship.
     *
     * @param amount The amount of goods to unload.
     * @return True if the unload succeeds.
     */
    private boolean leaveTransport(int amount) {
        if (!(goods.getLocation() instanceof Unit)) return false;
        final Unit carrier = (Unit)goods.getLocation();
        final GoodsType type = goods.getType();
        if (carrier.getGoodsCount(type) < amount) return false;

        final AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        int oldAmount = carrier.getGoodsCount(type);
        boolean result = AIMessage.askUnloadGoods(type, amount, aiCarrier);
        if (result) {
            int newAmount = carrier.getGoodsCount(type);
            if (oldAmount - newAmount != amount) {
                // FIXME: sort this out.
                // For now, do not tolerate partial unloads.
                logger.warning("Partial goods unload, expected: " + amount
                    + ", got: " + (oldAmount - newAmount));
                result = false;
            }
            logger.fine("Unloaded " + amount + " " + type
                + " from " + oldAmount + " leaving " + newAmount
                + " off of " + carrier + " at " + carrier.getLocation());
        }   
        return result;
    }


    // Implement TransportableAIObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Locatable getTransportLocatable() {
        return getGoods();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportSource() {
        return (goods == null) ? null : goods.getLocation();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location getTransportDestination() {
        return this.destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setTransportDestination(Location destination) {
        this.destination = destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getDeliveryPath(Unit carrier, Location dst) {
        if (dst == null) dst = Location.upLoc(getTransportDestination());

        PathNode path = (goods.getLocation() == carrier) ? carrier.findPath(dst)
            : (goods.getLocation() instanceof Unit) ? null
            : carrier.findPath(goods.getLocation(), dst, null, null);
        if (path != null) path.convertToGoodsDeliveryPath();
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathNode getIntermediatePath(Unit carrier, Location dst) {
        return null; // NYI
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean carriableBy(Unit carrier) {
        return carrier.couldCarry(getGoods());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canMove() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean leaveTransport() {
        return leaveTransport(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean leaveTransport(Direction direction) {
        if (direction != null) return false;
        return leaveTransport(goods.getAmount());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean joinTransport(Unit carrier, Direction direction) {
        if (direction != null) return false;
        final AIUnit aiCarrier = getAIMain().getAIUnit(carrier);
        if (aiCarrier == null) return false;

        final GoodsType type = goods.getType();
        boolean failed = false;
        int oldAmount = carrier.getGoodsCount(type),
            goodsAmount = goods.getAmount(),
            amount = Math.min(goodsAmount, carrier.getLoadableAmount(type));
        if (AIMessage.askLoadGoods(goods.getLocation(), type, amount,
                                   aiCarrier)) {
            setGoods(new Goods(getGame(), carrier, type, amount));
            final Colony colony = carrier.getColony();
            if (colony != null) {
                getAIMain().getAIColony(colony).removeExportGoods(this);
            }
        }
        logger.fine("Loaded " + amount + " " + type.getSuffix()
            + " over " + oldAmount + " leaving " + (goodsAmount - amount)
            + " onto " + carrier + " at " + carrier.getLocation());
        return !failed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String invalidReason() {
        String reason = Mission.invalidTransportableReason(this);
        Settlement s;
        return (reason != null)
            ? reason
            : (goods.getLocation() instanceof Unit
                && destination instanceof Settlement
                && !((Unit)goods.getLocation())
                    .getOwner().owns(s = (Settlement)destination))
            ? "transportableDestination-" + s.getName() + "-captured-by-"
                + s.getOwner().getDebugName()
            : null;
    }


    // Override AIObject

    /**
     * Disposes this object.
     */
    @Override
    public void dispose() {
        dropTransport();
        if (destination != null) {
            if (destination instanceof Colony) {
                AIColony aic = getAIMain().getAIColony((Colony)destination);
                if (aic != null) aic.removeExportGoods(this);
            } else if (destination instanceof Europe) {
                // Nothing to remove.
            } else {
                logger.warning("Unknown type of destination: " + destination);
            }
            destination = null;
        }
        goods = null;
        super.dispose();
    }

    /**
     * Checks the integrity of a this AIGoods.
     *
     * @param fix Fix problems if possible.
     * @return Negative if there are problems remaining, zero if
     *     problems were fixed, positive if no problems found at all.
     */
    @Override
    public int checkIntegrity(boolean fix) {
        int result = super.checkIntegrity(fix);
        String why = (result < 0) ? "super"
            : (goods == null) ? "null-goods"
            : (goods.getType() == null) ? "null-goods-type"
            : (goods.getAmount() <= 0) ? "non-positive-goods-amount"
            : (goods.getLocation() == null) ? "null-location"
            : (((FreeColGameObject)goods.getLocation()).isDisposed()) ? "disposed-location"
            : null;
        if (destination != null
            && ((FreeColGameObject)destination).isDisposed()) {
            if (fix) {
                logger.warning("Fixing disposed destination for " + this);
                destination = null;
                if (result > 0) result = 0;
            } else {
                why = "disposed-destination";
            }
        }
        if (why != null) {
            logger.finest("checkIntegrity(" + this + ") = " + why);
            result = -1;
        }
        return result;
    }


    // Serialization

    private static final String DESTINATION_TAG = "destination";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (destination != null) {
            xw.writeAttribute(DESTINATION_TAG, destination.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (goods != null) goods.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Game game = getAIMain().getGame();

        destination = xr.getLocationAttribute(game, DESTINATION_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        super.readChildren(xr);

        if (getGoods() != null) uninitialized = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (Goods.getXMLElementTagName().equals(tag)) {
            if (goods != null) {
                goods.readFromXML(xr);
            } else {
                goods = new Goods(getAIMain().getGame(), xr);
            }

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("[", getId(), " ", goods);
        if (goods != null) lb.add(" at ", goods.getLocation());
        lb.add(" -> ", destination);
        AIUnit transport = getTransport();
        if (transport != null) lb.add(" using ", transport.getUnit());
        lb.add("/", getTransportPriority(), "]");
        return lb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "aiGoods"
     */
    public static String getXMLElementTagName() {
        return "aiGoods";
    }
}
