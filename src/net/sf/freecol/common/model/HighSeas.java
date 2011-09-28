/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


public class HighSeas extends UnitLocation {

    private static final Logger logger =  Logger.getLogger(HighSeas.class.getName());


    /**
     * The destinations this HighSeas object connects.
     */
    private List<Location> destinations = new ArrayList<Location>();



    public HighSeas(Game game) {
        super(game);
    }

    public HighSeas(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    public HighSeas(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    public HighSeas(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns the name of this location.
     *
     * @return The name of this location.
     */
    @Override
    public StringTemplate getLocationName() {
        return StringTemplate.key("model.tile.highSeas.name");
    }

    /**
     * Get the <code>Destinations</code> value.
     *
     * @return a <code>List<Location></code> value
     */
    public final List<Location> getDestinations() {
        return destinations;
    }

    /**
     * Add a single destination to this HighSeas instance.
     *
     * @param destination a <code>Location</code> value
     */
    public void addDestination(Location destination) {
        if (destination != null) {
            if (!destinations.contains(destination)) {
                destinations.add(destination);
            } else {
                logger.warning(getId() + " already included destination " + destination.getId());
            }
        } else {
            logger.warning("Tried to add null destination to " + getId());
        }
    }

    /**
     * Remove a single destination from this HighSeas instance.
     *
     * @param destination a <code>Location</code> value
     */
    public void removeDestination(Location destination) {
        destinations.remove(destination);
    }


    /**
     * {@inheritDoc}
     */
    public boolean canAdd(Locatable locatable) {
        if (locatable instanceof Unit) {
            Unit unit = (Unit) locatable;
            return unit.isNaval();
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player,
                             boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        writeAttributes(out);

        // Add child elements:
        writeChildren(out);

        // End element:
        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    protected void writeChildren(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        super.writeChildren(out, player, showAll, toSavedGame);
        for (Location destination : destinations) {
            if(destination != null) {
                out.writeStartElement("destination");
                out.writeAttribute(ID_ATTRIBUTE, destination.getId());
                out.writeEndElement();
            } else {
                logger.warning("Tried to write out null destination from " + getId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        destinations.clear();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if ("destination".equals(in.getLocalName())) {
            destinations.add(newLocation(in.getAttributeValue(null, ID_ATTRIBUTE)));
            in.nextTag();
        } else {
            super.readChild(in);
        }
    }

    /**
     * Returns the tag name of this Object.
     *
     * @return "highSeas"
     */
    public static String getXMLElementTagName() {
        return "highSeas";
    }



}