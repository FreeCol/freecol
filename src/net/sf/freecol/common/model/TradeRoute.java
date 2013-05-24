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

import org.w3c.dom.Element;


/**
 * A TradeRoute holds all information for a unit to follow along a trade route.
 */
public class TradeRoute extends FreeColGameObject
    implements Cloneable, Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(TradeRoute.class.getName());

    /**
     * A stop along a trade route.
     */
    public class Stop {

        /** Where to stop. */
        private Location location;

        /** The cargo expected to be on board on leaving the stop. */
        private final List<GoodsType> cargo = new ArrayList<GoodsType>();


        /**
         * Create a stop for the given location from a stream.
         *
         * @param loc The <code>Location</code> of this stop.
         */
        public Stop(Location loc) {
            this.location = loc;
            this.cargo.clear();
        }

        /**
         * Copy constructor.  Creates a stop based on the given one.
         *
         * @param other The other <code>Stop</code>.
         */
        public Stop(Stop other) {
            this.location = other.location;
            this.setCargo(other.cargo);
        }

        /**
         * Create a stop by reading a stream.
         *
         * @param xr The <code>FreeColXMLReader</code> to read from.
         * @exception XMLStreamException if there is a problem reading the
         *     stream.
         */
        public Stop(FreeColXMLReader xr) throws XMLStreamException {
            this((Location)null);

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
        public boolean isValid() {
            return isValid(getOwner());
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
                    && (!player.owns((Ownable)location)));
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


        // Serialization

        private static final String CARGO_TAG = "cargo";
        private static final String LOCATION_TAG = "location";
        // Public as required in TradeRoute.readChild().
        public static final String TRADE_ROUTE_STOP_TAG = "tradeRouteStop";


        /**
         * Write an XML-representation of this object to the given stream.
         *
         * @param xw The <code>FreeColXMLWriter</code> to write to.
         * @exception XMLStreamException if there are any problems writing
         *      to the stream.
         */
        protected void toXML(FreeColXMLWriter xw) throws XMLStreamException {
            xw.writeStartElement(TRADE_ROUTE_STOP_TAG);

            xw.writeAttribute(LOCATION_TAG, (FreeColGameObject)location);

            for (GoodsType cargoType : cargo) {
                xw.writeStartElement(CARGO_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, cargoType.getId());

                xw.writeEndElement();
            }

            xw.writeEndElement();
        }

        /**
         * Initializes this object from its XML-representation.
         *
         * @param xr The input stream with the XML.
         * @exception XMLStreamException if there are any problems reading
         *     the stream.
         */
        protected void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
            final Specification spec = getSpecification();
            final Game game = getGame();

            location = xr.makeLocationAttribute(game, LOCATION_TAG);

            cargo.clear();
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                final String tag = xr.getLocalName();

                if (tag.equals(CARGO_TAG)) {
                    String id = xr.readId();
                    cargo.add(spec.getGoodsType(id));
                    xr.closeTag(CARGO_TAG);
                } else {
                    logger.warning("Bogus Stop tag: " + tag);
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return (isValid()) ? getLocation().toString()
                : "invalid stop";
        }
    };


    /** The name of this trade route. */
    private String name;

    /**
     * The <code>Player</code> who owns this trade route.  This is
     * necessary to ensure that malicious clients can not modify the
     * trade routes of other players.
     */
    private Player owner;

    /** A list of stops. */
    private final List<Stop> stops = new ArrayList<Stop>();


    /**
     * Creates a new <code>TradeRoute</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param name The name of the trade route.
     * @param player The owner <code>Player</code>.
     */
    public TradeRoute(Game game, String name, Player player) {
        super(game);
        this.name = name;
        this.owner = player;
    }

    /**
     * Creates a new <code>TradeRoute</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public TradeRoute(Game game, String id) {
        super(game, id);
    }

    /**
     * Creates a new <code>TradeRoute</code> instance.
     *
     * @param game The enclosing <code>Game</code>.
     * @param e An <code>Element</code> to read from.
     */
    public TradeRoute(Game game, Element e) {
        super(game, null);

        readFromXMLElement(e);
    }


    /**
     * Copy all fields from another trade route to this one.  This is
     * useful when an updated route is received on the server side
     * from the client.
     *
     * @param other The <code>TradeRoute</code> to copy from.
     */
    public synchronized void updateFrom(TradeRoute other) {
        setName(other.getName());
        stops.clear();
        for (Stop otherStop : other.getStops()) {
            addStop(new Stop(otherStop));
        }
    }

    /**
     * Get the name of this trade route.
     *
     * @return The name of this trade route.
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the name of the trade route.
     *
     * @param newName The new trade route name.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Get the stops in this trade route.
     *
     * @return A list of <code>Stop</code>s.
     */
    public final List<Stop> getStops() {
        return stops;
    }

    /**
     * Add a new <code>Stop</code> to this trade route.
     *
     * @param stop The <code>Stop</code> to add.
     */
    public void addStop(Stop stop) {
        stops.add(stop);
    }

    /**
     * Clear the stops in this trade route.
     */
    public void clearStops() {
        stops.clear();
    }

    /**
     * Replace all the stops for this trade route with the stops passed from
     * another trade route.
     *
     * This method will create a deep copy as it creates new stops
     * based on the given ones.
     *
     * @param otherStops The list of new <code>Stop</code>s to use.
     * @see #clone()
     */
    private void replaceStops(List<Stop> otherStops) {
        clearStops();
        for (Stop otherStop : otherStops) {
            addStop(new Stop(otherStop));
        }
    }

    /**
     * Clone the trade route and return a deep copy.
     * <p>
     * The copied trade route has no reference back to the original and can
     * safely be used as a temporary copy.  It is NOT registered with the game,
     * but will have the same unique identifier as the original.
     *
     * @return deep copy of trade route.
     */
    public TradeRoute clone() {
        try {
            TradeRoute copy = (TradeRoute) super.clone();
            copy.replaceStops(getStops());
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Clone should be supported!", e);
        }
    }

    /**
     * Get the units assigned to this route.
     *
     * @return A list of assigned <code>Unit</code>s.
     */
    public List<Unit> getAssignedUnits() {
        List<Unit> list = new ArrayList<Unit>();
        for (Unit unit : owner.getUnits()) {
            if (unit.getTradeRoute() == this) list.add(unit);
        }
        return list;
    }

    /**
     * Is a stop valid for a given unit?
     *
     * @param unit The <code>Unit</code> to check.
     * @param stop The <code>Stop</code> to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Unit unit, Stop stop) {
        return TradeRoute.isStopValid(unit.getOwner(), stop);
    }

    /**
     * Is a stop valid for a given player?
     *
     * @param player The <code>Player</code> to check.
     * @param stop The <code>Stop</code> to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Player player, Stop stop) {
        return (stop == null) ? false : stop.isValid(player);
    }

    // Interface Ownable

    /**
     * Get the owner of this trade route.
     *
     * @return The owning player.
     */
    public final Player getOwner() {
        return owner;
    }

    /**
     * Set the owner.
     *
     * @param newOwner The new owner.
     */
    public final void setOwner(final Player newOwner) {
        this.owner = newOwner;
    }


    // Serialization

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw, Player player,
                                   boolean showAll,
                                   boolean toSavedGame) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NAME_TAG, getName());

        xw.writeAttribute(OWNER_TAG, getOwner());

        for (Stop stop : stops) stop.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        name = xr.getAttribute(NAME_TAG, (String)null);

        owner = xr.makeFreeColGameObject(getGame(), OWNER_TAG,
                                         Player.class, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        stops.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (Stop.TRADE_ROUTE_STOP_TAG.equals(tag)) {
            stops.add(new Stop(xr));
            
        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tradeRoute".
     */
    public static String getXMLElementTagName() {
        return "tradeRoute";
    }
}
