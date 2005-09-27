
package net.sf.freecol.server.ai;

import net.sf.freecol.server.ai.mission.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;
import java.util.logging.Logger;


/**
* Objects of this class contains AI-information for a single {@link Goods}.
*/
public class AIGoods extends AIObject implements Transportable {
    private static final Logger logger = Logger.getLogger(AIGoods.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    public static final int IMPORTANT_DELIVERY = 110;
    public static final int FULL_DELIVERY = 100;

    private String id;

    private Goods goods;
    private Location destination;
    private int transportPriority;
    private AIUnit transport = null;


    public AIGoods(AIMain aiMain, Location location, int type, int amount, Location destination) {
        super(aiMain);

        id = aiMain.getNextID();
        aiMain.addAIObject(id, this);

        goods = new Goods(aiMain.getGame(), location, type, amount);
        this.destination = destination;
    }


    public AIGoods(AIMain aiMain, Element element) {
        super(aiMain);
        aiMain.addAIObject(element.getAttribute("ID"), this);
        readFromXMLElement(element);
    }


    /**
    * Returns the source for this <code>Transportable</code>.
    * This is normally the location of the
    * {@link #getTransportLocatable locatable}.
    *
    * @return The source for this <codeTransportable</code>.
    */
    public Location getTransportSource() {
        return goods.getLocation();
    }


    /**
    * Returns the destination for this <code>Transportable</code>.
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the tansport is determined by
    * {@link TransportMission} in the latter case.
    *
    * @return The destination for this <codeTransportable</code>.
    */
    public Location getTransportDestination() {
        return destination;
    }
    

    /**
    * Gets the <code>Locatable</code> which should be transported.
    * @return The <code>Locatable</code>.
    */
    public Locatable getTransportLocatable() {
        return getGoods();
    }


    /**
    * Gets the priority of transporting this <code>Transportable</code>
    * to it's destination.
    *
    * @return The priority of the transport.
    */
    public int getTransportPriority() {
        if (goods.getAmount() <= 100) {
            return goods.getAmount();
        } else {
            return transportPriority;
        }
    }

    
    /**
    * Increases the transport priority of this <code>Transportable</code>.
    * This method gets called every turn the <code>Transportable</code>
    * have not been put on a carrier's transport list.
    */    
    public void increaseTransportPriority() {
        transportPriority++;
    }

    
    /**
    * Gets the carrier responsible for transporting this <code>Transportable</code>.
    *
    * @return The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
    */
    public AIUnit getTransport() {
        return transport;
    }

    
    /**
    * Sets the carrier responsible for transporting this <code>Transportable</code>.
    *
    * @param transport The <code>AIUnit</code> which has this <code>Transportable</code>
    *         in it's transport list. This <code>Transportable</code> has not been
    *         scheduled for transport if this value is <code>null</code>.
    *
    */
    public void setTransport(AIUnit transport) {
        this.transport = transport;

        if (transport.getMission() instanceof TransportMission
            && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
            ((TransportMission) transport.getMission()).addToTransportList(this);
        }
    }
    

    public void setTransportPriority(int transportPriority) {
        this.transportPriority = transportPriority;
    }

    
    public Goods getGoods() {
        return goods;
    }


    public void setGoods(Goods goods) {
        if (goods == null) {
            throw new NullPointerException();
        }
        this.goods = goods;
    }
    
    
    public String getID() {
        return id;
    }


    public Element toXMLElement(Document document) {
        if (document == null) {
            throw new NullPointerException("document == null");
        }
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", id);
        if (destination != null) {
            element.setAttribute("destination", destination.getID());
        }
        element.setAttribute("transportPriority", Integer.toString(transportPriority));
        if (transport != null) {
            element.setAttribute("transport", transport.getID());
        }
        element.appendChild(goods.toXMLElement(null, document));

        return element;
    }


    public void readFromXMLElement(Element element) {
        id = element.getAttribute("ID");
        if (element.hasAttribute("destination")) {
            destination = (Location) getAIMain().getFreeColGameObject(element.getAttribute("destination"));
        } else {
            destination = null;
        }
        if (destination == null) {
            logger.warning("Could not find destination: " + destination);
        }
        transportPriority = Integer.parseInt(element.getAttribute("transportPriority"));
        if (goods != null) {
            goods.readFromXMLElement(Message.getChildElement(element, Goods.getXMLElementTagName()));
        } else {
            goods = new Goods(getAIMain().getGame(), Message.getChildElement(element, Goods.getXMLElementTagName()));
        }
        if (element.hasAttribute("transport")) {
            transport = (AIUnit) getAIMain().getAIObject(element.getAttribute("transport"));
        } else {
            transport = null;
        }
    }

    
    public String toString() {
        return "AIGoods@" + hashCode() + " type: " + getGoods().getName() + " amount: " + getGoods().getAmount();
    }
    

    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiGoods"
    */
    public static String getXMLElementTagName() {
        return "aiGoods";
    }
}
