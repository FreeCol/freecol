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
import net.sf.freecol.common.model.TradeRouteStop;

import org.w3c.dom.Element;


/**
 * A TradeRoute holds all information for a unit to follow along a trade route.
 */
public class TradeRoute extends FreeColGameObject
    implements Cloneable, Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(TradeRoute.class.getName());

    /** The name of this trade route. */
    private String name;

    /**
     * The <code>Player</code> who owns this trade route.  This is
     * necessary to ensure that malicious clients can not modify the
     * trade routes of other players.
     */
    private Player owner;

    /** A list of stops. */
    private final List<TradeRouteStop> stops = new ArrayList<TradeRouteStop>();


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
        for (TradeRouteStop otherStop : other.getStops()) {
            addStop(new TradeRouteStop(otherStop));
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
     * @return A list of <code>TradeRouteStop</code>s.
     */
    public final List<TradeRouteStop> getStops() {
        return stops;
    }

    /**
     * Add a new <code>TradeRouteStop</code> to this trade route.
     *
     * @param stop The <code>TradeRouteStop</code> to add.
     */
    public void addStop(TradeRouteStop stop) {
        stops.add(stop);
    }

    /**
     * Clear the stops in this trade route.
     */
    public void clearStops() {
        stops.clear();
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
     * @param stop The <code>TradeRouteStop</code> to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Unit unit, TradeRouteStop stop) {
        return TradeRoute.isStopValid(unit.getOwner(), stop);
    }

    /**
     * Is a stop valid for a given player?
     *
     * @param player The <code>Player</code> to check.
     * @param stop The <code>TradeRouteStop</code> to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Player player, TradeRouteStop stop) {
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
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NAME_TAG, getName());

        xw.writeAttribute(OWNER_TAG, getOwner());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (TradeRouteStop stop : stops) stop.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        name = xr.getAttribute(NAME_TAG, (String)null);

        owner = xr.findFreeColGameObject(getGame(), OWNER_TAG,
                                         Player.class, (Player)null, true);
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

        if (TradeRouteStop.getXMLElementTagName().equals(tag)) {
            stops.add(new TradeRouteStop(getGame(), xr));
            
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
