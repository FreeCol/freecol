/**
 * Copyright (C) 2002-2024   The FreeCol Team
 *
 * This file is part of FreeCol.
 *
 * FreeCol is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * FreeCol is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

/**
 * An object representing a specific traversal path within the High Seas.
 * Since it extends UnitLocation, it can physically contain units while 
 * they are in transit.
 */
public final class SeaRoute extends UnitLocation {

    private static final Logger logger = Logger.getLogger(SeaRoute.class.getName());

    public static final String TAG = "seaRoute";

    private Location destination;

    private int baseTravelTurns;
    private int piracyRisk; // 0–100
    private Direction direction; 
    private String weatherPatternId;

    /**
     * Create a new sea route.
     *
     * @param game The enclosing {@code Game}.
     */
    public SeaRoute(Game game) {
        super(game);
    }

    /**
     * Create a new sea route with a specific ID.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public SeaRoute(Game game, String id) {
        super(game, id);
    }

    public Location getDestination() {
        return destination;
    }

    public void setDestination(Location destination) {
        if (destination == null) {
            throw new IllegalArgumentException("SeaRoute destination cannot be null");
        }
        
        if (destination instanceof FreeColObject) {
            if (((FreeColObject)destination).getGame() != getGame()) {
                throw new IllegalArgumentException("SeaRoute destination belongs to different Game");
            }
        }
        
        this.destination = destination;
    }

    public int getBaseTravelTurns() {
        return baseTravelTurns;
    }

    public void setBaseTravelTurns(int baseTravelTurns) {
        if (baseTravelTurns < 0) {
            throw new IllegalArgumentException("Base travel turns cannot be negative");
        }
        this.baseTravelTurns = baseTravelTurns;
    }

    public int getPiracyRisk() {
        return piracyRisk;
    }

    public void setPiracyRisk(int piracyRisk) {
        if (piracyRisk < 0 || piracyRisk > 100) {
            throw new IllegalArgumentException("Piracy risk must be between 0 and 100");
        }
        this.piracyRisk = piracyRisk;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public String getWeatherPatternId() {
        return weatherPatternId;
    }

    public void setWeatherPatternId(String weatherPatternId) {
        this.weatherPatternId = weatherPatternId;
    }

    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.template("model.location.seaRoute.label")
            .addStringTemplate("%destination%", (destination == null) 
                ? StringTemplate.key("none") 
                : destination.getLocationLabel());
    }

    @Override
    public Location up() {
        return this;
    }

    @Override
    public int getRank() {
        return Location.LOCATION_RANK_HIGHSEAS;
    }

    @Override
    public String toShortString() {
        return "SeaRoute to " + (destination != null ? destination.getId() : "null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        // Only naval units can be "on" a sea route
        return (locatable instanceof Unit && ((Unit)locatable).isNaval())
            ? NoAddReason.NONE
            : NoAddReason.WRONG_TYPE;
    }

    // --- Serialization ---

    @Override
    public String getXMLTagName() {
        return TAG;
    }

    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);
        xw.writeLocationAttribute("destination", this.destination);
        xw.writeAttribute("baseTravelTurns", this.baseTravelTurns);
        xw.writeAttribute("piracyRisk", this.piracyRisk);
        if (this.direction != null) {
            xw.writeAttribute("direction", this.direction);
        }
        if (this.weatherPatternId != null) {
            xw.writeAttribute("weatherPatternId", this.weatherPatternId);
        }
    }

    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);
        final Game game = getGame();
        this.destination = xr.getLocationAttribute(game, "destination", true);
        this.baseTravelTurns = xr.getAttribute("baseTravelTurns", 0);
        this.piracyRisk = xr.getAttribute("piracyRisk", 0);
        this.direction = xr.getAttribute("direction", Direction.class, (Direction)null);
        this.weatherPatternId = xr.getAttribute("weatherPatternId", (String)null);
    }

    @Override
    public String toString() {
        return "SeaRoute[" + getId() 
            + ", destination=" + (destination != null ? destination.getId() : "null")
            + ", units=" + getUnitCount()
            + "]";
    }
}
