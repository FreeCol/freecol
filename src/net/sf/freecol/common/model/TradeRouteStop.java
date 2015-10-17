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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A stop along a trade route.
 */
public class TradeRouteStop extends FreeColObject implements TradeLocation {

    private static final Logger logger = Logger.getLogger(TradeRouteStop.class.getName());

    /** The game in play. */
    private final Game game;

    /** The trade location of the stop. */
    private Location location;

    /** The cargo expected to be on board on leaving the stop. */
    private final List<GoodsType> cargo = new ArrayList<>();


    /**
     * Create a stop for the given location from a stream.
     *
     * @param location The <code>Location</code> of this stop.
     */
    public TradeRouteStop(Game game, Location location) {
        setId("");
        setSpecification(game.getSpecification());

        this.game = game;
        this.location = location;
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
     * @return The <code>Location</code> of this stop.
     */
    public final Location getLocation() {
        return location;
    }

    /**
     * Get the location of this stop as a TradeLocation.
     *
     * @return The <code>TradeLocation</code> for this stop.
     */
    public TradeLocation getTradeLocation() {
        return (TradeLocation)location;
    }

    /**
     * Is this stop valid?
     *
     * @return True if the stop is valid.
     */
    public boolean isValid(Player player) {
        return (location instanceof TradeLocation)
            && !((FreeColGameObject)location).isDisposed()
            && ((location instanceof Ownable)
                && player.owns((Ownable)location));
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

    /**
     * Get a list of the maximum abstract goods that could be loaded
     * at this stop.  That is, a list of all the cargo goods types
     * normally with amount equal to one CARGO_SIZE, but with
     * duplicates removed and amounts accumulated.
     *
     * @return A list of <code>AbstractGoods</code> to load.
     */
    public List<AbstractGoods> getCompactCargo() {
        List<AbstractGoods> result = new ArrayList<>();
        for (GoodsType type : getCargo()) {
            AbstractGoods ag = AbstractGoods.findByType(type, result);
            if (ag != null) {
                ag.setAmount(ag.getAmount() + GoodsContainer.CARGO_SIZE);
            } else {
                result.add(new AbstractGoods(type, GoodsContainer.CARGO_SIZE));
            }
        }
        return result;
    }
        
    /**
     * Create a template for this trade route stop.
     *
     * @param key A message key.
     * @param player The <code>Player</code> who will see the message.
     * @return A <code>StringTemplate</code> for this stop.
     */
    public StringTemplate getLabelFor(String key, Player player) {
        return StringTemplate.template(key)
            .addStringTemplate("%location%",
                this.getLocation().getLocationLabelFor(player));
    }

    /**
     * Create an invalid trade route stop label.
     *
     * @param player The <code>Player</code> who will see the message.
     * @return A <code>StringTemplate</code> for this stop.
     */
    public StringTemplate invalidStopLabel(Player player) {
        return getLabelFor("model.tradeRoute.invalidStop", player);
    }

    /**
     * Is there work for a unit to do at this stop?
     *
     * @param unit The <code>Unit</code> to test.
     * @param turns Account for production from this many turns.
     * @return True if this unit should load or unload cargo at the stop.
     */
    public boolean hasWork(Unit unit, int turns) {
        // Look for goods to load.
        List<AbstractGoods> stopGoods = getCompactCargo();
        // There is space on the unit to load some more of this goods
        // type, so return true if there is some available at the stop.
        if (any(stopGoods.stream()
                .filter(ag -> unit.getGoodsCount(ag.getType()) < ag.getAmount()),
                ag -> getExportAmount(ag.getType(), turns) > 0)) return true;

        // Look for goods to unload.
        if (any(unit.getCompactGoodsList().stream()
                .filter(ag -> !AbstractGoods.containsType(ag.getType(), stopGoods)),
                ag -> getImportAmount(ag.getType(), turns) > 0)) return true;

        return false;
    }


    // Interface TradeLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public int getGoodsCount(GoodsType goodsType) {
        return (location instanceof TradeLocation)
            ? ((TradeLocation)location).getGoodsCount(goodsType)
            : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getExportAmount(GoodsType goodsType, int turns) {
        return (location instanceof TradeLocation)
            ? ((TradeLocation)location).getExportAmount(goodsType, turns)
            : 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getImportAmount(GoodsType goodsType, int turns) {
        return (location instanceof TradeLocation)
            ? ((TradeLocation)location).getImportAmount(goodsType, turns)
            : 0;
    }


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
            sb.append(" ").append(goodsType);
        }
        sb.append("]");
        return sb.toString();            
    }

    /**
     * Delegate to getXMLElementTagName.
     *
     * @return What getXMLElementTagName does.
     */
    @Override
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
