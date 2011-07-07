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


package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Location;

import org.w3c.dom.Element;


/**
* Represents a need for something at a given <code>Location</code>.
*/
public abstract class Wish extends ValuedAIObject {

    private static final Logger logger = Logger.getLogger(Wish.class.getName());

    protected Location destination = null;

    /**
    * The <code>Transportable</code> which will realize the wish,
    * or <code>null</code> if no <code>Transportable</code> has
    * been chosen.
    */
    protected Transportable transportable = null;



    /**
    * Creates a new <code>Wish</code>.
    * @param aiMain The main AI-object.
    * @param id The unique ID of this object.
    */
    public Wish(AIMain aiMain, String id) {
        super(aiMain, id);
    }


    /**
    * Creates a new <code>Wish</code> from the given XML-representation.
    *
    * @param aiMain The main AI-object.
    * @param element The root element for the XML-representation
    *       of a <code>Wish</code>.
    */
    public Wish(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute(ID_ATTRIBUTE));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>Wish</code> from the given XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public Wish(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, ID_ATTRIBUTE));
        readFromXML(in);
    }


    /**
     * Checks if this <code>Wish</code> needs to be stored in a savegame.
     * @return The result.
     */
    public boolean shouldBeStored() {
        return (transportable != null);
    }

    /**
    * Assigns a <code>Transportable</code> to this <code>Wish</code>.
    * @param transportable The <code>Transportable</code> which should
    *        realize this wish.
    * @see #getTransportable
    * @see net.sf.freecol.server.ai.mission.WishRealizationMission
    */
    public void setTransportable(Transportable transportable) {
        this.transportable = transportable;
    }


    /**
    * Gets the <code>Transportable</code> assigned to this <code>Wish</code>.
    * @return The <code>Transportable</code> which will realize this wish,
    *         or <code>null</code> if none has been assigned.
    * @see #setTransportable
    * @see net.sf.freecol.server.ai.mission.WishRealizationMission
    */
    public Transportable getTransportable() {
        return transportable;
    }

    /**
     * Disposes this <code>AIObject</code> by removing
     * any referances to this object.
     */
    public void dispose() {
        if (destination instanceof Colony) {
            AIColony ac = (AIColony) getAIMain().getAIColony((Colony) destination);
            ac.removeWish(this);
        } else {
            logger.warning("Unknown destination: " + destination);
        }
        if (transportable != null) {
            Transportable temp = transportable;
            transportable = null;
            temp.abortWish(this);
        }
        super.dispose();
    }

    /**
    * Gets the destination of this <code>Wish</code>.
    * @return The <code>Location</code> in which the
    *       {@link #getTransportable transportable} assigned to
    *       this <code>Wish</code> will have to reach.
    */
    public Location getDestination() {
        return destination;
    }
}
