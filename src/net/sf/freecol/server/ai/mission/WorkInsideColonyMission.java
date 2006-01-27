package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Mission for working inside a <code>Colony</code>.
 */
public class WorkInsideColonyMission extends Mission{
    private static final Logger logger = Logger.getLogger(WorkInsideColonyMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private AIColony aiColony;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param aiColony The <code>AIColony</code> the unit should be
    *        working in.
    */
    public WorkInsideColonyMission(AIMain aiMain, AIUnit aiUnit, AIColony aiColony) {
        super(aiMain, aiUnit);
        this.aiColony = aiColony;
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public WorkInsideColonyMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Disposes this <code>Mission</code>.
    */
    public void dispose() {
        super.dispose();
    }


    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        // Nothing to do yet.
    }



    /**
     * Checks if this mission is still valid to perform.
     *
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */
    public boolean isValid() {
        return !aiColony.getColony().isDisposed();
    }


    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("colony", aiColony.getID());

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>WorkInsideColonyMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        aiColony = (AIColony) getAIMain().getAIObject(element.getAttribute("colony"));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "workInsideColonyMission".
    */
    public static String getXMLElementTagName() {
        return "workInsideColonyMission";
    }
}
