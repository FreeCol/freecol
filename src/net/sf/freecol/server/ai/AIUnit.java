
package net.sf.freecol.server.ai;

import net.sf.freecol.server.ai.mission.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* Objects of this class contains AI-information for a single {@link Unit}.
*/
public class AIUnit extends AIObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    /**
    * The FreeColGameObject this AIObject contains AI-information for:
    */
    private Unit unit;

    /**
    * The mission this unit has been assigned.
    */
    private Mission mission;


    public AIUnit(AIMain aiMain, Unit unit) {
        super(aiMain);

        this.unit = unit;

        mission = new UnitWanderHostileMission(this);
    }

    public AIUnit(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }
    

    /**
    * Gets the <code>Unit</code> this <code>AIUnit</code> controls.
    */
    public Unit getUnit() {
        return unit;
    }


    /**
    * Gets the mission this unit has been assigned.
    */
    public Mission getMission() {
        return mission;
    }
    
    
    /**
    * Assignes a mission to unit.
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
        if (getMission() != null) {
            getMission().doMission(connection);
        }
    }
    

    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", unit.getID());

        return element;
    }


    public void readFromXMLElement(Element element) {
        unit = (Unit) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
        
        // TODO-INSTEAD: Store mission:
        mission = new UnitWanderHostileMission(this);
    }    
    
    
    /**
    * Returns the tag name of the root element representing this object.
    * @return "aiUnit"
    */
    public static String getXMLElementTagName() {
        return "aiUnit";
    }
}
