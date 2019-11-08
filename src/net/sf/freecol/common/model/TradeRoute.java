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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.model.Constants.*;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/**
 * A TradeRoute holds all information for a unit to follow along a trade route.
 */
public class TradeRoute extends FreeColGameObject
    implements Nameable, Ownable {

    private static final Logger logger = Logger.getLogger(TradeRoute.class.getName());

    public static final String TAG = "tradeRoute";

    /** The name of this trade route. */
    private String name;

    /**
     * The {@code Player} who owns this trade route.  This is
     * necessary to ensure that malicious clients can not modify the
     * trade routes of other players.
     */
    private Player owner;

    /** A list of stops. */
    private final List<TradeRouteStop> stops = new ArrayList<>();

    /** Silence the messaging for this trade route. */
    private boolean silent = false;


    /**
     * Creates a new {@code TradeRoute} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param name The name of the trade route.
     * @param player The owner {@code Player}.
     */
    public TradeRoute(Game game, String name, Player player) {
        super(game);
        this.name = name;
        this.owner = player;
        this.silent = false;
    }

    /**
     * Creates a new {@code TradeRoute} instance.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public TradeRoute(Game game, String id) {
        super(game, id);
    }


    /**
     * Does this trade route generate no messages to the player?
     *
     * @return True if this trade route is silent.
     */
    public boolean isSilent() {
        return this.silent;
    }

    /**
     * Set the silence status of this trade route.
     *
     * @param silent The new silence status of this trade route.
     */
    public void setSilent(boolean silent) {
        this.silent = silent;
    }

    /**
     * Get the number of stops in this trade route.
     *
     * @return The number of stops, or negative on error.
     */
    public final int getStopCount() {
        synchronized (this.stops) {
            return this.stops.size();
        }
    }

    /**
     * Get a stop in the trade route by index.
     *
     * @param index The index to look up.
     * @return The {@code TradeRouteStop} found, or null if the index
     *     is invalid.
     */
    public final TradeRouteStop getStop(int index) {
        synchronized (this.stops) {
            return (index >= 0 && index < this.stops.size())
                ? this.stops.get(index)
                : null;
        }
    }

    /**
     * Get the stops in this trade route.
     *
     * @return A list of {@code TradeRouteStop}s.
     */
    public final List<TradeRouteStop> getStopList() {
        synchronized (this.stops) {
            return new ArrayList<>(this.stops);
        }
    }

    /**
     * Get a list of the trade route stops in this trade route, starting
     * at a given stop (inclusive) and a final stop (exclusive).
     *
     * @param start The starting {@code TradeRouteStop}.
     * @param end The end {@code TradeRouteStop}.
     * @return A list of stops, or null on error.
     */
    public List<TradeRouteStop> getStopSublist(TradeRouteStop start,
                                               TradeRouteStop end) {
        int i0 = getIndex(start), in = getIndex(end);
        if (i0 < 0 || in < 0) return null;
        List<TradeRouteStop> result = new ArrayList<>();
        synchronized (this.stops) {
            while (i0 != in) {
                result.add(this.stops.get(i0));
                if (++i0 >= this.stops.size()) i0 = 0;
            }
        }
        return result;
    }

    /**
     * Add a new {@code TradeRouteStop} to this trade route.
     *
     * @param stop The {@code TradeRouteStop} to add.
     */
    public void addStop(TradeRouteStop stop) {
        synchronized (this.stops) {
            this.stops.add(stop);
        }
    }

    /**
     * Remove a {@code TradeRouteStop} from this trade route.
     *
     * @param stop The {@code TradeRouteStop} to remove.
     */
    public void removeStop(TradeRouteStop stop) {
        synchronized (this.stops) {
            this.stops.remove(stop);
        }
    }

    /**
     * Remove any stops matching a given location.
     *
     * @param loc The {@code Location} to match.
     * @return True if any stop was removed.
     */
    public boolean removeMatchingStops(Location loc) {
        synchronized (this.stops) {
            return removeInPlace(this.stops,
                trs -> Map.isSameLocation(trs.getLocation(), loc));
        }
    }

    /**
     * Get the index of a stop in this trade route.
     *
     * @param stop The {@code TradeRouteStop} to look for.
     * @return The index of the given stop, or negative on failure.
     */
    public int getIndex(TradeRouteStop stop) {
        synchronized (this.stops) {
            int i = 0;
            for (TradeRouteStop trs : this.stops) {
                if (trs == stop) return i;
                i++;
            }
        }
        return -1;
    }

    /**
     * Clear the stops in this trade route.
     */
    public void clearStops() {
        synchronized (this.stops) {
            this.stops.clear();
        }
    }

    /**
     * Get the units assigned to this route.
     *
     * @return A list of assigned {@code Unit}s.
     */
    public List<Unit> getAssignedUnits() {
        return transform(getOwner().getUnits(),
                         matchKey(this, Unit::getTradeRoute));
    }

    /**
     * Is a stop valid for a given unit?
     *
     * @param unit The {@code Unit} to check.
     * @param stop The {@code TradeRouteStop} to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Unit unit, TradeRouteStop stop) {
        return TradeRoute.isStopValid(unit.getOwner(), stop);
    }

    /**
     * Is a stop valid for a given player?
     *
     * @param player The {@code Player} to check.
     * @param stop The {@code TradeRouteStop} to check.
     * @return True if the stop is valid.
     */
    public static boolean isStopValid(Player player, TradeRouteStop stop) {
        return (stop == null) ? false : stop.isValid(player);
    }

    /**
     * Check the uniqueness of the trade route name.
     *
     * @return Null if the name is unique, or a {@code StringTemplate}
     *     containing an error message if not.
     */
    public StringTemplate verifyUniqueName() {
        return (getOwner().getTradeRouteByName(this.name, this) != null)
            ? StringTemplate.template("model.tradeRoute.duplicateName")
                .addName("%name%", this.name)
            : null;
    }

    /**
     * Check that the trade route is valid.
     *
     * @return Null if the route is valid, or a {@code StringTemplate}
     *     explaining the problem if invalid.
     */
    public StringTemplate verify() {
        if (this.name == null) {
            return StringTemplate.template("model.tradeRoute.nullName");
        }
        if (this.owner == null) {
            return StringTemplate.template("model.tradeRoute.nullOwner");
        }

        // Verify that it has at least two stops
        if (getStopCount() < 2) {
            return StringTemplate.template("model.tradeRoute.notEnoughStops");
        }

        // Check:
        // - all stops are valid
        // - there is at least one non-empty stop
        // - there is no goods that is present unmaintained at all stops
        Set<GoodsType> always = new HashSet<>(getStop(0).getCargo());
        boolean empty = true;
        int n = getStopCount();
        for (int i = 0; i < n; i++) {
            TradeRouteStop stop = getStop(i);
            if (!TradeRoute.isStopValid(owner, stop)) {
                return stop.invalidStopLabel(owner);
            }
            if (!stop.getCargo().isEmpty()) empty = false;
            always.retainAll(stop.getCargo());
        }
        final boolean enhancedTradeRoutes = getSpecification()
            .getBoolean(GameOptions.ENHANCED_TRADE_ROUTES);
        return (empty)
            ? StringTemplate.template("model.tradeRoute.allEmpty")
            : (!enhancedTradeRoutes && !always.isEmpty())
            ? StringTemplate.template("model.tradeRoute.alwaysPresent")
                .addNamed("%goodsType%", first(always))
            : null;
    }


    // Interface Nameable

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setName(final String newName) {
        this.name = newName;
    }


    // Interface Ownable

    /**
     * {@inheritDoc}
     */
    @Override
    public final Player getOwner() {
        return this.owner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setOwner(final Player newOwner) {
        this.owner = newOwner;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        StringTemplate ver = verify();
        if (ver != null) {
            lb.add("\n  In ", getId(), " ", Messages.message(ver));
            result = result.fail();
        }
        return result;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TradeRoute o = copyInCast(other, TradeRoute.class);
        if (o == null || !super.copyIn(o)) return false;
        this.name = o.getName();
        this.owner = o.getOwner();
        clearStops();
        for (TradeRouteStop trs : o.getStopList()) {
            addStop(new TradeRouteStop(trs));
        }
        return true;
    }

    // Serialization

    private static final String NAME_TAG = "name";
    private static final String OWNER_TAG = "owner";
    private static final String SILENT_TAG = "silent";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NAME_TAG, getName());

        xw.writeAttribute(OWNER_TAG, getOwner());

        xw.writeAttribute(SILENT_TAG, isSilent());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (TradeRouteStop stop : this.stops) stop.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.name = xr.getAttribute(NAME_TAG, (String)null);

        this.owner = xr.findFreeColGameObject(getGame(), OWNER_TAG,
                                              Player.class, (Player)null, true);

        this.silent = xr.getAttribute(SILENT_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearStops();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (TradeRouteStop.TAG.equals(tag)) {
            addStop(new TradeRouteStop(getGame(), xr));
            
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
            .append(" \"").append(this.name).append('"');
        if (this.owner != null) sb.append(" owner=").append(this.owner.getId());
        sb.append(" silent=").append(Boolean.toString(this.silent));
        for (TradeRouteStop stop : getStopList()) sb.append(' ').append(stop);
        sb.append(']');
        return sb.toString();
    }
}
