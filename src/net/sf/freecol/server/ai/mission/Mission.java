
package net.sf.freecol.server.ai.mission;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* A mission describes what a unit should do; attack, build colony, wander etc.
* Every {@link AIUnit} should have a mission. By extending this class,
* you create different missions.
*/
public abstract class Mission {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private AIUnit aiUnit;
    
    
    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public Mission(AIUnit aiUnit) {
        this.aiUnit = aiUnit;
    }

    
    /**
    * Performs the mission. This method should be implemented by a subclass.
    * @param connection The <code>Connection</code> to the server.
    */
    public abstract void doMission(Connection connection);
    
    
    /**
    * Gets the unit this mission has been created for.
    */
    public Unit getUnit() {
        return aiUnit.getUnit();
    }


    /**
    * Gets the AI-unit this mission has been created for.
    */
    public AIUnit getAIUnit() {
        return aiUnit;
    }
}
