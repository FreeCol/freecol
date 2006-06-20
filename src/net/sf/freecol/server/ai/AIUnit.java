
package net.sf.freecol.server.ai;

import java.util.logging.Logger;

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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Objects of this class contains AI-information for a single {@link Unit}.
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

    private AIUnit transport = null;


    /**
     * Creates a new <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param unit The unit to make an {@link AIObject} for.
     */    
    public AIUnit(AIMain aiMain, Unit unit) {
        super(aiMain);

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
        super(aiMain);
        readFromXMLElement(element);
    }
    

    /**
    * Gets the <code>Unit</code> this <code>AIUnit</code> controls.
    * @return The <code>Unit</code>.
    */
    public Unit getUnit() {
        return unit;
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
        this.transport = transport;

        if (transport != null
            && transport.getMission() instanceof TransportMission
            && !((TransportMission) transport.getMission()).isOnTransportList(this)) {
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
        super.dispose();
    }
    
    
    /**
     * Returns the ID of this <code>AIObject</code>.
     * @return The same ID as the <code>Unit</code> this
     *      <code>AIObject</code> controls.
     */
    public String getID() {
        return unit.getID();
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());
        if (transport != null) {
            element.setAttribute("transport", transport.getUnit().getID());
        }
        if (mission != null) {
            element.appendChild(mission.toXMLElement(document));
        }

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>AIUnit</code>.
     * 
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        unit = (Unit) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
        
        if (unit == null) {
            logger.warning("Could not find unit: " + unit);
        }

        if (element.hasAttribute("transport")) {
            transport = (AIUnit) getAIMain().getAIObject(element.getAttribute("transport"));
        } else {
            transport = null;
        }

        if (element.getChildNodes().getLength() > 0) {
            Element missionElement = null;
            NodeList nl = element.getChildNodes();
            for (int i=0; i<nl.getLength(); i++) {
                if (nl.item(i) instanceof Element) {
                    missionElement = (Element) nl.item(i);
                    break;
                }
            }            
            if (missionElement != null) {
                if (missionElement.getTagName().equals(UnitWanderHostileMission.getXMLElementTagName())) {
                    mission = new UnitWanderHostileMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(UnitWanderMission.getXMLElementTagName())) {
                    mission = new UnitWanderMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(IndianBringGiftMission.getXMLElementTagName())) {
                    mission = new IndianBringGiftMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(BuildColonyMission.getXMLElementTagName())) {
                    mission = new BuildColonyMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(IndianDemandMission.getXMLElementTagName())) {                    
                    mission = new IndianDemandMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(TransportMission.getXMLElementTagName())) {
                    mission = new TransportMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(WishRealizationMission.getXMLElementTagName())) {
                    mission = new WishRealizationMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(UnitSeekAndDestroyMission.getXMLElementTagName())) {
                    mission = new UnitSeekAndDestroyMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(PioneeringMission.getXMLElementTagName())) {
                    mission = new PioneeringMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(DefendSettlementMission.getXMLElementTagName())) {
                    mission = new DefendSettlementMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(WorkInsideColonyMission.getXMLElementTagName())) {
                    mission = new WorkInsideColonyMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(ScoutingMission.getXMLElementTagName())) {
                    mission = new ScoutingMission(getAIMain(), missionElement);
                } else if (missionElement.getTagName().equals(CashInTreasureTrainMission.getXMLElementTagName())) {
                    mission = new CashInTreasureTrainMission(getAIMain(), missionElement);                    
                } else {
                    logger.warning("Could not find mission-class for: " + missionElement.getTagName());
                    mission = new UnitWanderHostileMission(getAIMain(), this);
                }
            }
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
