
package net.sf.freecol.common.model;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * Describe unitType here.
     */
    private String unitTypeID;

    /**
     * Describe armed here.
     */
    private boolean armed;

    /**
     * Describe mounted here.
     */
    private boolean mounted;

    /**
     * Describe missionary here.
     */
    private boolean missionary;

    /**
     * Describe tools here.
     */
    private int tools;

    /**
     * Creates a new <code>AbstractUnit</code> instance.
     *
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnit(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }


    /**
     * Get the <code>UnitType</code> value.
     *
     * @return an <code>UnitType</code> value
     */
    public final UnitType getUnitType() {
        return FreeCol.getSpecification().getUnitType(unitTypeID);
    }

    /**
     * Set the <code>UnitType</code> value.
     *
     * @param newUnitType The new UnitType value.
     */
    public final void setUnitTypeID(final String newUnitType) {
        this.unitTypeID = newUnitType;
    }

    /**
     * Get the <code>Armed</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isArmed() {
        return armed;
    }

    /**
     * Set the <code>Armed</code> value.
     *
     * @param newArmed The new Armed value.
     */
    public final void setArmed(final boolean newArmed) {
        this.armed = newArmed;
    }

    /**
     * Get the <code>Mounted</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isMounted() {
        return mounted;
    }

    /**
     * Set the <code>Mounted</code> value.
     *
     * @param newMounted The new Mounted value.
     */
    public final void setMounted(final boolean newMounted) {
        this.mounted = newMounted;
    }

    /**
     * Get the <code>Missionary</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isMissionary() {
        return missionary;
    }

    /**
     * Set the <code>Missionary</code> value.
     *
     * @param newMissionary The new Missionary value.
     */
    public final void setMissionary(final boolean newMissionary) {
        this.missionary = newMissionary;
    }

    /**
     * Get the <code>Tools</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getTools() {
        return tools;
    }

    /**
     * Set the <code>Tools</code> value.
     *
     * @param newTools The new Tools value.
     */
    public final void setTools(final int newTools) {
        this.tools = newTools;
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        unitTypeID = in.getAttributeValue(null, "id");
        armed = getAttribute(in, "armed", false);
        mounted = getAttribute(in, "mounted", false);
        missionary = getAttribute(in, "missionary", false);
        tools = getAttribute(in, "tools", 0);
        in.nextTag(); // close this element
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("id", unitTypeID);
        out.writeAttribute("armed", String.valueOf(armed));
        out.writeAttribute("mounted", String.valueOf(mounted));
        out.writeAttribute("missionary", String.valueOf(missionary));
        out.writeAttribute("tools", String.valueOf(tools));

        out.writeEndElement();
    }
    
    public static String getXMLElementTagName() {
        return "abstractUnit";
    }

}

