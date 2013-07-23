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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;


/**
 * A stop along a trade route.
 */
public class TradeRouteStop extends FreeColObject {

    private static final Logger logger = Logger.getLogger(TradeRouteStop.class.getName());

    /** The game in play. */
    private final Game game;

    /** The location of the stop. */
    private Location location;

    /** The cargo expected to be on board on leaving the stop. */
    private final List<GoodsType> cargo = new ArrayList<GoodsType>();


    /**
     * Create a stop for the given location from a stream.
     *
     * @param loc The <code>Location</code> of this stop.
     */
    public TradeRouteStop(Game game, Location loc) {
        setId("");
        setSpecification(game.getSpecification());

        this.game = game;
        this.location = loc;
        this.cargo.clear();
    }

    /**
     * Copy constructor.  Creates a stop based on the given one.
     *
     * @param other The other <code>TradeRouteStop</code>.
     */
    public TradeRouteStop(TradeRouteStop other) {
        this(other.game, other.location);
        this.setCargo(other.cargo);
    }

    /**
     * Create a new <code>TradeRouteStop</code> from a stream.
     *
     * @param game The enclosing <code>Game</code>.
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public TradeRouteStop(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this(game, (Location)null);

        readFromXML(xr);
    }


    /**
     * Get the location of this stop.
     *
     * @return The stop location.
     */
    public final Location getLocation() {
        return location;
    }

    /**
     * Is this stop valid?
     *
     * @return True if the stop is valid.
     */
    public boolean isValid(Player player) {
        return location != null
            && !((FreeColGameObject)location).isDisposed()
            && !((location instanceof Ownable)
                && !player.owns((Ownable)location));
    }

    /**
     * Get the current cargo for this stop.
     *
     * @return A list of cargo <code>GoodsType</code>s.
     */
    public final List<GoodsType> getCargo() {
        return cargo;
    }

    /**
     * Set the cargo value.
     *
     * @param newCargo A list of <code>GoodsType</code> defining the cargo.
     */
    public final void setCargo(List<GoodsType> newCargo) {
        cargo.clear();
        cargo.addAll(newCargo);
    }

    /**
     * Add cargo to this stop.
     *
     * @param newCargo The <code>GoodsType</code> to add.
     */
    public void addCargo(GoodsType newCargo) {
        cargo.add(newCargo);
    }


    // Disabled routines for a proposed functionality extension.
    /**
     * Whether the stop has been modified. This is of interest only to the
     * client and can be ignored for XML serialization.
     *
    private boolean modified = false;

    /**
     * The AbstractGoods to unload in this Location.
     *
    private List<AbstractGoods> goodsToUnload;

    /**
     * The AbstractGoods to load in this Location.
     *
    private List<AbstractGoods> goodsToLoad;

    /**
     * Get the <code>GoodsToLoad</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     *
    public final List<AbstractGoods> getGoodsToLoad() {
        return goodsToLoad;
    }

    /**
     * Set the <code>GoodsToLoad</code> value.
     *
     * @param newGoodsToLoad The new GoodsToLoad value.
     *
    public final void setGoodsToLoad(final List<AbstractGoods> newGoodsToLoad) {
        this.goodsToLoad = newGoodsToLoad;
    }

    /**
     * Get the <code>GoodsToUnload</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     *
    public final List<AbstractGoods> getGoodsToUnload() {
        return goodsToUnload;
    }

    /**
     * Set the <code>GoodsToUnload</code> value.
     *
     * @param newGoodsToUnload The new GoodsToUnload value.
     *
    public final void setGoodsToUnload(final List<AbstractGoods> newGoodsToUnload) {
        this.goodsToUnload = newGoodsToUnload;
    }

    /**
     * Get the <code>Modified</code> value.
     * 
     * @return a <code>boolean</code> value
     *
    public final boolean isModified() {
        return modified;
    }

    /**
     * Set the <code>Modified</code> value.
     * 
     * @param newModified The new Modified value.
     *
    public final void setModified(final boolean newModified) {
        this.modified = newModified;
    }
    */

    // Serialization

    private static final String CARGO_TAG = "cargo";
    private static final String LOCATION_TAG = "location";


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeLocationAttribute(LOCATION_TAG, location);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (GoodsType cargoType : cargo) {

            xw.writeStartElement(CARGO_TAG);
            
            xw.writeAttribute(FreeColObject.ID_ATTRIBUTE_TAG, cargoType);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        location = xr.getLocationAttribute(game, LOCATION_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        cargo.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (CARGO_TAG.equals(tag)) {
            cargo.add(xr.getType(spec, ID_ATTRIBUTE_TAG,
                                 GoodsType.class, (GoodsType)null));

            xr.closeTag(CARGO_TAG);

        } else {
            super.readChild(xr);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getXMLTagName())
            .append(" ").append(getLocation().getId());
        for (GoodsType goodsType : getCargo()) {
            sb.append(" ").append(goodsType.toString());
        }
        sb.append("]");
        return sb.toString();            
    }

    /**
     * Delegate to getXMLElementTagName.
     *
     * @return What getXMLElementTagName does.
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     * 
     * @return "tradeRouteStop".
     */
    public static String getXMLElementTagName() {
        return "tradeRouteStop";
    }
}
