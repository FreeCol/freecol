
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Unit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Represents the need for a worker within a <code>Colony</code>.
*/
public class WorkerWish extends Wish {
    private static final Logger logger = Logger.getLogger(WorkerWish.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private int unitType;
    private boolean expertNeeded;


    /**
    * Creates a new <code>WorkerWish</code>.
    *
    * @param destination The <code>Location</code> in which the
    *       {@link #getTransportable transportable} assigned to
    *       this <code>WorkerWish</code> will have to reach.
    * @param value The value identifying the importance of
    *       this <code>Wish</code>.
    * @param unitType The type of unit needed for releasing this wish
    *       completly.
    * @param expertNeeded Determines wether the <code>unitType</code> is
    *       required or not.
    */
    public WorkerWish(AIMain aiMain, Location destination, int value, int unitType, boolean expertNeeded) {
        super(aiMain);

        id = aiMain.getNextID();
        aiMain.addAIObject(id, this);

        this.destination = destination;
        this.value = value;
        this.unitType = unitType;
        this.expertNeeded = expertNeeded;
    }


    /**
    * Creates a new <code>WorkerWish</code> from the given XML-representation.
    *
    * @param element The root element for the XML-representation of a <code>WorkerWish</code>.
    */
    public WorkerWish(AIMain aiMain, Element element) {
        super(aiMain);
        aiMain.addAIObject(element.getAttribute("ID"), this);
        readFromXMLElement(element);
    }


    /**
    * Returns the type of unit needed for releasing this wish.
    * @return The {@link Unit#getType type of unit}.
    */
    public int getUnitType() {
        return unitType;
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", id);
        element.setAttribute("destination", destination.getID());
        if (transportable != null) {
            element.setAttribute("transportable", transportable.getID());
        }
        element.setAttribute("value", Integer.toString(value));

        element.setAttribute("unitType", Integer.toString(unitType));
        element.setAttribute("expertNeeded", Boolean.toString(expertNeeded));

        return element;
    }


    public void readFromXMLElement(Element element) {
        id = element.getAttribute("ID");
        destination = (Location) getAIMain().getFreeColGameObject(element.getAttribute("destination"));
        if (element.hasAttribute("transportable")) {
            transportable = (Transportable) getAIMain().getAIObject(element.getAttribute("transportable"));
        } else {
            transportable = null;
        }
        value = Integer.parseInt(element.getAttribute("value"));

        unitType = Integer.parseInt(element.getAttribute("unitType"));
        expertNeeded = Boolean.valueOf(element.getAttribute("expertNeeded")).booleanValue();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "workerWish"
    */
    public static String getXMLElementTagName() {
        return "workerWish";
    }
}
