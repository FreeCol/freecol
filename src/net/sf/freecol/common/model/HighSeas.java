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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * An object representing the high seas between continents.
 */
public class HighSeas extends UnitLocation {

    private static final Logger logger =  Logger.getLogger(HighSeas.class.getName());

    public static final String TAG = "highSeas";

    /** The destinations this HighSeas object connects. */
    private final List<Location> destinations = new ArrayList<>();


    /**
     * Simple constructor.
     *
     * @param game The enclosing {@code Game}.
     */
    public HighSeas(Game game) {
        super(game);
    }

    /**
     * Create a new high seas.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public HighSeas(Game game, String id) {
        super(game, id);
    }


    /**
     * Get the destinations connected by these seas.
     *
     * @return A list of {@code Location}s.
     */
    public final List<Location> getDestinations() {
        return this.destinations;
    }

    /**
     * Convenience debug function to collect the destination ids.
     *
     * @return A string representation of the destination identifiers.
     */
    public final String destinationsToString() {
        return join(",", transform(this.destinations, alwaysTrue(),
                                   Location::getId));
    }
        
    /**
     * Set the destination list.
     *
     * @param destinations The new list of destination {@code Location}s.
     */
    protected void setDestinations(List<Location> destinations) {
        this.destinations.clear();
        this.destinations.addAll(destinations);
    }

    /**
     * Add a single destination to this HighSeas instance.
     *
     * @param destination A destination {@code Location}.
     */
    public void addDestination(Location destination) {
        if (destination != null) {
            if (!this.destinations.contains(destination)) {
                this.destinations.add(destination);
            } else {
                logger.warning(getId() + " already included destination "
                    + destination.getId());
            }
        } else {
            logger.warning("Tried to add null destination to " + getId());
        }
    }

    /**
     * Remove a single destination from this HighSeas instance.
     *
     * @param destination A destination {@code Location}.
     */
    public void removeDestination(Location destination) {
        this.destinations.remove(destination);
    }


    // Interface Location (from UnitLocation)
    // Inherits
    //   FreeColObject.getId
    //   UnitLocation.getTile
    //   UnitLocation.getLocationLabelFor
    //   UnitLocation.add
    //   UnitLocation.remove
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   UnitLocation.getUnits
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   UnitLocation.getSettlement

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.key("model.tile.highSeas.name");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRank() {
        return Location.LOCATION_RANK_HIGHSEAS;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return "HighSeas";
    }


    // UnitLocation
    // Inherits
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        return (locatable instanceof Unit && ((Unit)locatable).isNaval())
            ? NoAddReason.NONE
            : NoAddReason.WRONG_TYPE;
    }


    // Override FreeColGameObject

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColGameObject getLinkTarget(Player player) {
        return player.getEurope();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        HighSeas o = copyInCast(other, HighSeas.class);
        if (o == null || !super.copyIn(o)) return false;
        final Game game = getGame();
        this.destinations.clear();
        for (Location loc : o.getDestinations()) {
            this.destinations.add(game.updateLocationRef(loc));
        }
        return true;
    }


    // Serialization

    private static final String DESTINATION_TAG = "destination";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Location loc : transform(this.destinations, isNotNull())) {
            xw.writeStartElement(DESTINATION_TAG);

            xw.writeLocationAttribute(ID_ATTRIBUTE_TAG, loc);
            
            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        this.destinations.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Game game = getGame();
        final String tag = xr.getLocalName();

        if (DESTINATION_TAG.equals(tag)) {
            addDestination(xr.getLocationAttribute(game, ID_ATTRIBUTE_TAG,
                                                   true));

            xr.closeTag(DESTINATION_TAG);

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
        return getId();
    }
}
