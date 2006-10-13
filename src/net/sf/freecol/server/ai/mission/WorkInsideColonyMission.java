package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

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
        if (aiColony == null) {
            throw new NullPointerException("aiColony == null");
        }
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
     * Creates a new <code>WorkInsideColonyMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public WorkInsideColonyMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
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
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("unit", getUnit().getID());
        out.writeAttribute("colony", aiColony.getID());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        aiColony = (AIColony) getAIMain().getAIObject(in.getAttributeValue(null, "colony"));
        if (aiColony == null) {
            aiColony = new AIColony(getAIMain(), in.getAttributeValue(null, "colony"));
        }
        in.nextTag();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "workInsideColonyMission".
    */
    public static String getXMLElementTagName() {
        return "workInsideColonyMission";
    }
}
