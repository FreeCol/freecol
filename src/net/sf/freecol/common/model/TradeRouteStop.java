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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A stop along a trade route.
 */
public class TradeRouteStop extends FreeColGameObject implements TradeLocation {

    private static final Logger logger = Logger.getLogger(TradeRouteStop.class.getName());

    public static final String TAG = "tradeRouteStop";

    /** The trade location of the stop. */
    private Location location;

    /** The cargo expected to be on board on leaving the stop. */
    private final List<GoodsType> cargo = new ArrayList<>();


    /**
     * Create an empty trade route stop.
     *
     * @param game The enclosing {@code Game}.
     */
    public TradeRouteStop(Game game) {
        super(game, ""); // Identifier not required
    }

    /**
     * Create a stop for the given location from a stream.
     *
     * @param game The enclosing {@code Game}.
     * @param location The {@code Location} of this stop.
     */
    public TradeRouteStop(Game game, Location location) {
        this(game);

        this.location = location;
        this.cargo.clear();
    }

    /**
     * Copy constructor.  Creates a stop based on the given one.
     *
     * @param other The other {@code TradeRouteStop}.
     */
    public TradeRouteStop(TradeRouteStop other) {
        this(other.getGame(), other.location);

        this.setCargo(other.cargo);
    }

    /**
     * Create a new {@code TradeRouteStop} from a stream.
     *
     * @param game The enclosing {@code Game}.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public TradeRouteStop(Game game, FreeColXMLReader xr) throws XMLStreamException {
        this(game);

        readFromXML(xr);
    }


    /**
     * Get the location of this stop.
     *
     * @return The {@code Location} of this stop.
     */
    public final Location getLocation() {
        return location;
    }

    /**
     * Get the location of this stop as a TradeLocation.
     *
     * @return The {@code TradeLocation} for this stop.
     */
    public TradeLocation getTradeLocation() {
        return (TradeLocation)location;
    }

    /**
     * Is this stop valid?
     *
     * @param player The {@code Player} that owns this route.
     * @return True if the stop is valid.
     */
    public boolean isValid(Player player) {
        return (location instanceof TradeLocation)
            && !((FreeColGameObject)location).isDisposed()
            && ((FreeColGameObject)location).isInitialized()
            && ((location instanceof Ownable)
                && player.owns((Ownable)location));
    }

    /**
     * Get the current cargo for this stop.
     *
     * @return A list of cargo {@code GoodsType}s.
     */
    public final List<GoodsType> getCargo() {
        return cargo;
    }

    /**
     * Set the cargo value.
     *
     * @param newCargo A list of {@code GoodsType} defining the cargo.
     */
    public final void setCargo(List<GoodsType> newCargo) {
        cargo.clear();
        cargo.addAll(newCargo);
    }

    /**
     * Add cargo to this stop.
     *
     * @param newCargo The {@code GoodsType} to add.
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
     * @return A list of {@code AbstractGoods} to load.
     */
    public List<AbstractGoods> getCompactCargo() {
        List<AbstractGoods> result = new ArrayList<>();
        for (GoodsType type : getCargo()) {
            AbstractGoods ag = find(result, AbstractGoods.matches(type));
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
     * @param player The {@code Player} who will see the message.
     * @return A {@code StringTemplate} for this stop.
     */
    public StringTemplate getLabelFor(String key, Player player) {
        return StringTemplate.template(key)
            .addStringTemplate("%location%",
                this.getLocation().getLocationLabelFor(player));
    }

    /**
     * Create an invalid trade route stop label.
     *
     * @param player The {@code Player} who will see the message.
     * @return A {@code StringTemplate} for this stop.
     */
    public StringTemplate invalidStopLabel(Player player) {
        return getLabelFor("model.tradeRoute.invalidStop", player);
    }

    /**
     * Is there work for a unit to do at this stop?
     *
     * @param unit The {@code Unit} to test.
     * @param turns Account for production from this many turns.
     * @return True if this unit should load or unload cargo at the stop.
     */
    public boolean hasWork(Unit unit, int turns) {
        final List<AbstractGoods> stopGoods = getCompactCargo();
        // Look for goods to load.
        // If there is space on the unit to load some more of this goods
        // type and there is some available at the stop, return true.
        
        final Predicate<AbstractGoods> loadPred = ag ->
            unit.getGoodsCount(ag.getType()) < ag.getAmount()
                && getExportAmount(ag.getType(), turns) > 0;
        if (any(stopGoods, loadPred)) return true;

        // Look for goods to unload.
        // For all goods the unit has loaded, and if the type of goods
        // is not to be loaded here, and there is demand here, return true.
        final Predicate<Goods> unloadPred = g ->
            !any(stopGoods, AbstractGoods.matches(g.getType()))
                && getImportAmount(g.getType(), turns) > 0;
        if (any(unit.getCompactGoodsList(), unloadPred)) return true;

        return false; // Otherwise no work here.
    }


    // Interface TradeLocation

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAvailableGoodsCount(GoodsType goodsType) {
        return (location instanceof TradeLocation)
            ? ((TradeLocation)location).getAvailableGoodsCount(goodsType)
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocationName(TradeLocation tradeLocation) {
        return null;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isInternable() {
        return false;
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TradeRouteStop o = copyInCast(other, TradeRouteStop.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.location = game.updateLocationRef(o.getLocation());
        this.setCargo(o.getCargo());
        return true;
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
        location = xr.getLocationAttribute(getGame(), LOCATION_TAG, true);
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getId())
            .append(' ').append(getLocation().getId());
        for (GoodsType goodsType : getCargo()) {
            sb.append(' ').append(goodsType);
        }
        sb.append(']');
        return sb.toString();            
    }
}
