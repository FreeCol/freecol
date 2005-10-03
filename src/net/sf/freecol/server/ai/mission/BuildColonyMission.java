
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Mission for building a <code>Colony</code>.
* @see net.sf.freecol.common.model.Colony Colony
*/
public class BuildColonyMission extends Mission {
    private static final Logger logger = Logger.getLogger(BuildColonyMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The <code>Tile</code> where the <code>Colony</code> should be built. */
    private Tile target;

    /** The value of the target <code>Tile</code>. */
    private int colonyValue;
    
    private boolean colonyBuilt = false;



    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param target The <code>Tile</code> where the <code>Colony</code> should be built.
    * @param colonyValue The value of the <code>Tile</code> to build a <code>Colony</code>
    * 		 upon. This mission will be invalidated if <code>target.getColonyValue()</code>
    * 		 is less than this value.
    */
    public BuildColonyMission(AIMain aiMain, AIUnit aiUnit, Tile target, int colonyValue) {
        super(aiMain, aiUnit);

        this.target = target;
        this.colonyValue = colonyValue;

        if (!getUnit().isColonist()) {
            logger.warning("Only colonists can build a new Colony.");
            throw new IllegalArgumentException("Only colonists can build a new Colony.");
        }
    }


    /**
     * Creates a new <code>BuildColonyMission</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     * 		XML-representation of this object.
     */
    public BuildColonyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();

        if (!isValid()) {
            return;
        }
        
        // Move towards the target.
        if (getUnit().getTile() != null) {
            if (target != getUnit().getTile()) {
                int r = moveTowards(connection, target);
                if (r >= 0 && (unit.getMoveType(r) == Unit.MOVE || unit.getMoveType(r) == Unit.DISEMBARK)) {
                    move(connection, r);
                }
            }
            if (getUnit().canBuildColony() && target == getUnit().getTile() && getUnit().getMovesLeft() > 0) {
                Element buildColonyElement = Message.createNewRootElement("buildColony");
                buildColonyElement.setAttribute("name", unit.getOwner().getDefaultColonyName());
                buildColonyElement.setAttribute("unit", unit.getID());

                try {
                    connection.ask(buildColonyElement);
                    colonyBuilt = true;
                } catch (IOException e) {
                    logger.warning("Could not send \"buildColonyElement\"-message!");
                }
            }
        }
    }

    
    
    /**
    * Returns the destination for this <code>Transportable</code>.
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the transport is determined by
    * {@link TransportMission} in the latter case.
    *
    * @return The destination for this <code>Transportable</code>.
    */    
    public Tile getTransportDestination() {
        if (getUnit().getLocation() instanceof Unit) {
            return target;
        } else if (getUnit().getLocation().getTile() == target) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(target) == null) {
            return target;
        } else {
            return null;
        }
    }


    /**
    * Returns the priority of getting the unit to the
    * transport destination.
    *
    * @return The priority.
    */
    public int getTransportPriority() {
        if (getUnit().getLocation() instanceof Unit) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else if (getUnit().getLocation().getTile() == target) {
            return 0;
        } else if (getUnit().getTile() == null || getUnit().findPath(target) == null) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else {
            return 0;
        }
    }


    /**
    * Checks if this mission is still valid to perform.
    *
    * <BR><BR>
    *
    * This mission will be invalidated when the colony has been built or
    * if the <code>target.getColonyValue()</code> decreases.
    *
    * @return <code>true</code> if this mission is still valid to perform
    *         and <code>false</code> otherwise.
    */
    public boolean isValid() {
        return (!colonyBuilt && colonyValue <= target.getColonyValue());
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     * 		the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("target", target.getID());
        element.setAttribute("colonyBuilt", Boolean.toString(colonyBuilt));

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>BuildColonyMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        target = (Tile) getGame().getFreeColGameObject(element.getAttribute("target"));
        colonyBuilt = Boolean.valueOf(element.getAttribute("colonyBuilt")).booleanValue();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "buildColonyMission".
    */
    public static String getXMLElementTagName() {
        return "buildColonyMission";
    }
}
