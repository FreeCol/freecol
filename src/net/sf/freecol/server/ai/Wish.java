
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.server.ai.mission.WishRealizationMission;

import org.w3c.dom.Element;


/**
* Represents a need for something at a given <code>Location</code>.
*/
public abstract class Wish extends AIObject {
    private static final Logger logger = Logger.getLogger(Wish.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    protected String id;
    protected Location destination = null;
    protected int value;

    
    /**
    * The <code>Transportable</code> which will realize the wish,
    * or <code>null</code> if no <code>Transportable</code> has
    * been chosen.
    */
    protected Transportable transportable = null;



    /**
    * Creates a new <code>Wish</code>.
    */
    public Wish(AIMain aiMain) {
        super(aiMain);
    }


    /**
    * Creates a new <code>Wish</code> from the given XML-representation.
    *
    * @param element The root element for the XML-representation of a <code>Wish</code>.
    */
    public Wish(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    

    /**
    * Returns the ID for this <code>Wish</code>.
    */
    public String getID() {
        return id;
    }


    /**
    * Returns the value for this <code>Wish</code>.
    * @return The value identifying the importance of
    *         this <code>Wish</code>.
    */
    public int getValue() {
        return value;
    }


    /**
    * Assigns a <code>Transportable</code> to this <code>Wish</code>.
    * @param transportable The <code>Transportable</code> which should
    *        realize this wish.
    * @see #getTransportable
    * @see WishRealizationMission
    */
    public void setTransportable(Transportable transportable) {
        this.transportable = transportable;
    }


    /**
    * Gets the <code>Transportable</code> assigned to this <code>Wish</code>.
    * @return The <code>Transportable</code> which will realize this wish,
    *         or <code>null</code> if none has been assigned.
    * @see #setTransportable
    * @see WishRealizationMission
    */
    public Transportable getTransportable() {
        return transportable;
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
