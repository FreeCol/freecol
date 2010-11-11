/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.server.ai.mission.TransportMission;

import org.w3c.dom.Element;


/**
* Objects of this class contains AI-information for a single {@link Goods}.
*/
public class AIGoods extends AIObject implements Transportable {
    private static final Logger logger = Logger.getLogger(AIGoods.class.getName());


    public static final int IMPORTANT_DELIVERY = 110;
    public static final int FULL_DELIVERY = 100;
    
    /**
     * The priority of tools intended for a Colony with none stored
     * at the present (and with no special needs).
     */
    public static final int TOOLS_FOR_COLONY_PRIORITY = 10;
    
    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * for each ColonyTile needing a terrain improvement.
     */
    public static final int TOOLS_FOR_IMPROVEMENT = 10;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a Pioneer is lacking tools
     */
    public static final int TOOLS_FOR_PIONEER = 90;

    /**
     * The extra priority value added to the base value of
     * {@link #TOOLS_FOR_COLONY_PRIORITY}
     * if a building is lacking tools. The number of tools
     * is also added to the total amount.
     */
    public static final int TOOLS_FOR_BUILDING = 100;
    
    private Goods goods;
    private Location destination;
    private int transportPriority;
    private AIUnit transport = null;


    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param location The location of the goods.
     * @param type The type of goods.
     * @param amount The amount of goods.
     * @param destination The destination of the goods. This is the
     *      <code>Location</code> to which the goods should be transported.
     */
    public AIGoods(AIMain aiMain, Location location, GoodsType type, int amount, Location destination) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextID());

        goods = new Goods(aiMain.getGame(), location, type, amount);
        this.destination = destination;
    }


    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */    
    public AIGoods(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */    
    public AIGoods(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }
    
    /**
     * Creates a new <code>AIGoods</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */    
    public AIGoods(AIMain aiMain, String id) {
        super(aiMain, id);
        uninitialized = true;
    }

    /**
     * Aborts the given <code>Wish</code>.
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w) {
        if (destination == w.getDestination()) {
            destination = null;
        }
        if (w.getTransportable() == this) {
            w.dispose();
        }
    }

    /**
    * Returns the source for this <code>Transportable</code>.
    * This is normally the location of the
    * {@link #getTransportLocatable locatable}.
    *
    * @return The source for this <code>Transportable</code>.
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
    * @return The destination for this <code>Transportable</code>.
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
     * Disposes this object.
     */
    public void dispose() {
        setTransport(null);
        if (destination != null) {
            if (destination instanceof Colony) {
                ((AIColony) getAIMain().getAIObject((Colony) destination)).removeAIGoods(this);
            } else if (destination instanceof Europe) {
                // Nothing to remove.
            } else {
                logger.warning("Unknown type of destination: " + destination);
            }
        }
        super.dispose();
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
        AIUnit oldTransport = this.transport;
        this.transport = transport;
        
        if (oldTransport != null) {
            // Remove from old carrier:
            if (oldTransport.getMission() != null
                    && oldTransport.getMission() instanceof TransportMission) {
                TransportMission tm = (TransportMission) oldTransport.getMission();
                if (tm.isOnTransportList(this)) {
                    tm.removeFromTransportList(this);
                }
            }
        }
            
        if (transport != null
                && transport.getMission() instanceof TransportMission
                && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
            // Add to new carrier:
            ((TransportMission) transport.getMission()).addToTransportList(this);
        }
    }
    

    /**
     * Sets the priority of getting the goods to the {@link #getTransportDestination}.
     * @param transportPriority The priority.
     */
    public void setTransportPriority(int transportPriority) {
        this.transportPriority = transportPriority;
    }

    
    /**
    * Gets the goods this <code>AIGoods</code> is controlling.
    * @return The <code>Goods</code>.
    */
    public Goods getGoods() {
        return goods;
    }


    /**
     * Sets the goods this <code>AIGoods</code> is controlling.
     * @param goods The <code>Goods</code>.
     */    
    public void setGoods(Goods goods) {        
        if (goods == null) {
            throw new NullPointerException();
        }
        this.goods = goods;
    }
    
    
    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("ID", getId());
        if (destination != null) {
            out.writeAttribute("destination", destination.getId());
        }
        out.writeAttribute("transportPriority", Integer.toString(transportPriority));
        if (transport != null) {
            if (getAIMain().getAIObject(transport.getId()) == null) {
                logger.warning("broken reference to transport");
            } else if (transport.getMission() != null
                    && transport.getMission() instanceof TransportMission
                    && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
                logger.warning("We should not be on the transport list.");
            } else {
                out.writeAttribute("transport", transport.getId());
            }
        }
        goods.toXML(out);

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        final String destinationStr = in.getAttributeValue(null, "destination");
        if (destinationStr != null) {
            destination = (Location) getAIMain().getFreeColGameObject(destinationStr);
            if (destination == null) {
                logger.warning("Could not find destination: " + destinationStr);
            }
        } else {
            destination = null;
        }
        transportPriority = Integer.parseInt(in.getAttributeValue(null, "transportPriority"));

        final String transportStr = in.getAttributeValue(null, "transport");
        if (transportStr != null) {
            transport = (AIUnit) getAIMain().getAIObject(transportStr);
            if (transport == null) {
                transport = new AIUnit(getAIMain(), transportStr);
            }
        } else {
            transport = null;
        }
        
        in.nextTag();

        if (goods != null) {
            goods.readFromXML(in);
        } else {
            goods = new Goods(getAIMain().getGame(), in);
        }
        in.nextTag();
    }

    
    /**
     * Returns a <code>String</code>-representation of this object.
     * @return A <code>String</code> representing this objecy for debugging purposes.
     */
    public String toString() {
        return "AIGoods@" + hashCode() + ": " + goods + " (" + transportPriority + ")";
    }
    

    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiGoods"
    */
    public static String getXMLElementTagName() {
        return "aiGoods";
    }
}
