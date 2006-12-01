
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;

import org.w3c.dom.Element;


/**
* Objects of this class contains AI-information for a single {@link Unit}.
* 
* <br><br>
* 
* The method {@link #doMission(Connection)} is called once each turn,
* by {@link AIPlayer#startWorking()}, to perform the assigned
* <code>Mission</code>. Most of the methods in this class just delegates
* the call to that mission.
* 
* @see Mission
*/
public class AIUnit extends AIObject implements Transportable {
    private static final Logger logger = Logger.getLogger(AIUnit.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
    * The FreeColGameObject this AIObject contains AI-information for.
    */
    private Unit unit;

    /**
    * The mission this unit has been assigned.
    */
    private Mission mission;

    /**
     * The <code>AIUnit</code> which has this <code>Transportable</code>
     * in it's transport list. This <code>Transportable</code> has not been
     * scheduled for transport if this value is <code>null</code>.
     */
    private AIUnit transport = null;


    /**
     * Creates a new <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param unit The unit to make an {@link AIObject} for.
     */    
    public AIUnit(AIMain aiMain, Unit unit) {
        super(aiMain, unit.getID());

        this.unit = unit;
        if (unit == null) {
            throw new NullPointerException("unit == null");
        }
        
        mission = new UnitWanderHostileMission(aiMain, this);
    }

    
    /**
     * Creates a new <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */    
    public AIUnit(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */    
    public AIUnit(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }

    /**
     * Creates a new <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param id The unique ID of this object.
     */    
    public AIUnit(AIMain aiMain, String id) {
        super(aiMain, id);
        unit = (Unit) getAIMain().getFreeColGameObject(id);        
        if (unit == null) {
            logger.warning("Could not find unit: " + id);
        }
        uninitialized = true;
    }


    /**
    * Gets the <code>Unit</code> this <code>AIUnit</code> controls.
    * @return The <code>Unit</code>.
    */
    public Unit getUnit() {
        return unit;
    }
      
    /**
     * Aborts the given <code>Wish</code>.
     * @param w The <code>Wish</code> to be aborted.
     */
    public void abortWish(Wish w) {
        if (mission instanceof WishRealizationMission) {
            mission = null;
        }
        if (w.getTransportable() == this) {
            w.dispose();
        }
    }
    
    /**
    * Gets the <code>Locatable</code> which should be transported.
    * @return The <code>Locatable</code>.
    */    
    public Locatable getTransportLocatable() {
        return unit;
    }

    
    /**
    * Returns the source for this <code>Transportable</code>.
    * This is normally the location of the
    * {@link #getTransportLocatable locatable}.
    *
    * @return The source for this <code>Transportable</code>.
    */
    public Location getTransportSource() {
        return getUnit().getLocation();
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
        if (mission == null) {
            return null;
        } else {
            return mission.getTransportDestination();
        }
    }


    /**
    * Gets the priority of transporting this <code>Transportable</code>
    * to it's destination.
    *
    * @return The priority of the transport.
    */
    public int getTransportPriority() {
        if (mission == null) {
            return 0;
        } else {
            return mission.getTransportPriority();
        }
    }

    
    /**
    * Increases the transport priority of this <code>Transportable</code>.
    * This method gets called every turn the <code>Transportable</code>
    * have not been put on a carrier's transport list.
    */
    public void increaseTransportPriority() {
        // TODO
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
    * Gets the mission this unit has been assigned.
    * @return The <code>Mission</code>.
    */
    public Mission getMission() {
        return mission;
    }


    /**
    * Checks if this unit has been assigned a mission.
    * @return <code>true</code> if this unit has a mission.
    */
    public boolean hasMission() {
        return (mission != null);
    }
    
    
    /**
    * Assignes a mission to unit.
    * @param mission The new <code>Mission</code>.
    */
    public void setMission(Mission mission) {
        final Mission oldMission = this.mission;
        if (oldMission != null) {
            oldMission.dispose();
        }
        this.mission = mission;
    }
    

    /**
    * Performs the mission this unit has been assigned.
    * @param connection The <code>Connection</code> to use
    *        when communicating with the server.
    */
    public void doMission(Connection connection) {
        if (getMission() != null && getMission().isValid()) {
            getMission().doMission(connection);
        }
    }
    
    
    /**
     * Disposes this object and any attached mission.
     */
    public void dispose() {
        if (hasMission()) {
            getMission().dispose();
        }
        setTransport(null);
        super.dispose();
    }
    
    
    /**
     * Returns the ID of this <code>AIObject</code>.
     * @return The same ID as the <code>Unit</code> this
     *      <code>AIObject</code> controls.
     */
    public String getID() {
        if (unit != null) {
            return unit.getID();
        } else {
            logger.warning("unit == null");
            return null;
        }
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

        out.writeAttribute("ID", getID());
        if (transport != null) {
            if (transport.getUnit() == null) {
                logger.warning("transport.getUnit() == null");
            } else if (getAIMain().getAIObject(transport.getID()) == null) {
                logger.warning("broken reference to transport");
            } else if (transport.getMission() != null
                    && transport.getMission() instanceof TransportMission
                    && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
                logger.warning("We should not be on the transport list.");
            } else {
                out.writeAttribute("transport", transport.getUnit().getID());
            }
        }
        if (mission != null) {
            mission.toXML(out);
        }

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String inID =  in.getAttributeValue(null, "ID");
        unit = (Unit) getAIMain().getFreeColGameObject(inID);
        
        if (unit == null) {
            logger.warning("Could not find unit: " + inID);
        }

        final String transportStr = in.getAttributeValue(null, "transport");
        if (transportStr != null) {
            transport = (AIUnit) getAIMain().getAIObject(transportStr);
            if (transport == null) {
                transport = new AIUnit(getAIMain(), transportStr);
            }
        } else {
            transport = null;
        }
                
        if (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(UnitWanderHostileMission.getXMLElementTagName())) {
                mission = new UnitWanderHostileMission(getAIMain(), in);
            } else if (in.getLocalName().equals(UnitWanderMission.getXMLElementTagName())) {
                mission = new UnitWanderMission(getAIMain(), in);
            } else if (in.getLocalName().equals(IndianBringGiftMission.getXMLElementTagName())) {
                mission = new IndianBringGiftMission(getAIMain(), in);
            } else if (in.getLocalName().equals(BuildColonyMission.getXMLElementTagName())) {
                mission = new BuildColonyMission(getAIMain(), in);
            } else if (in.getLocalName().equals(IndianDemandMission.getXMLElementTagName())) {                    
                mission = new IndianDemandMission(getAIMain(), in);
            } else if (in.getLocalName().equals(TransportMission.getXMLElementTagName())) {
                mission = new TransportMission(getAIMain(), in);
            } else if (in.getLocalName().equals(WishRealizationMission.getXMLElementTagName())) {
                mission = new WishRealizationMission(getAIMain(), in);
            } else if (in.getLocalName().equals(UnitSeekAndDestroyMission.getXMLElementTagName())) {
                mission = new UnitSeekAndDestroyMission(getAIMain(), in);
            } else if (in.getLocalName().equals(PioneeringMission.getXMLElementTagName())) {
                mission = new PioneeringMission(getAIMain(), in);
            } else if (in.getLocalName().equals(DefendSettlementMission.getXMLElementTagName())) {
                mission = new DefendSettlementMission(getAIMain(), in);
            } else if (in.getLocalName().equals(WorkInsideColonyMission.getXMLElementTagName())) {
                mission = new WorkInsideColonyMission(getAIMain(), in);
            } else if (in.getLocalName().equals(ScoutingMission.getXMLElementTagName())) {
                mission = new ScoutingMission(getAIMain(), in);
            } else if (in.getLocalName().equals(CashInTreasureTrainMission.getXMLElementTagName())) {
                mission = new CashInTreasureTrainMission(getAIMain(), in);                    
            } else {
                logger.warning("Could not find mission-class for: " + in.getLocalName());
                mission = new UnitWanderHostileMission(getAIMain(), this);
                return;
            }
            
            in.nextTag();            
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiUnit"
    */
    public static String getXMLElementTagName() {
        return "aiUnit";
    }
}
