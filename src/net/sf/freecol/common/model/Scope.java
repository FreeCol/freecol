
package net.sf.freecol.common.model;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import org.w3c.dom.Element;


/**
 * The <code>Scope</code> class determines whether a given
 * <code>FreeColGameObjectType</code> fulfills certain requirements.
 */
public final class Scope extends FreeColObject implements Cloneable {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
    public static final String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
     * The ID of a <code>FreeColGameObjectType</code>.
     */
    private String type;

    /**
     * The ID of an <code>Ability</code>.
     */
    private String abilityID;

    /**
     * The value of an <code>Ability</code>.
     */
    private boolean abilityValue;

    /**
     * The name of an <code>Attribute</code>.
     */
    private String attributeName;

    /**
     * The <code>String</code> representation of the value of an
     * <code>Attribute</code>.
     */
    private String attributeValue;

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public void setType(final String newType) {
        this.type = newType;
    }

    /**
     * Get the <code>AbilityID</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getAbilityID() {
        return abilityID;
    }

    /**
     * Set the <code>AbilityID</code> value.
     *
     * @param newAbilityID The new AbilityID value.
     */
    public void setAbilityID(final String newAbilityID) {
        this.abilityID = newAbilityID;
    }

    /**
     * Get the <code>AbilityValue</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isAbilityValue() {
        return abilityValue;
    }

    /**
     * Set the <code>AbilityValue</code> value.
     *
     * @param newAbilityValue The new AbilityValue value.
     */
    public void setAbilityValue(final boolean newAbilityValue) {
        this.abilityValue = newAbilityValue;
    }

    /**
     * Get the <code>AttributeName</code> value.
     *
     * @return a <code>String</code> value
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Set the <code>AttributeName</code> value.
     *
     * @param newAttributeName The new AttributeName value.
     */
    public void setAttributeName(final String newAttributeName) {
        this.attributeName = newAttributeName;
    }

    /**
     * Get the <code>AttributeValue</code> value.
     *
     * @return an <code>String</code> value
     */
    public String getAttributeValue() {
        return attributeValue;
    }

    /**
     * Set the <code>AttributeValue</code> value.
     *
     * @param newAttributeValue The new AttributeValue value.
     */
    public void setAttributeValue(final String newAttributeValue) {
        this.attributeValue = newAttributeValue;
    }


    public boolean appliesTo(FreeColGameObjectType object) {
        if (type != null && type != object.getID()) {
            return false;
        }
        if (abilityID != null && object.hasAbility(abilityID) != abilityValue) {
            return false;
        }
        if (attributeName != null) {
            try {
                Field attribute = object.getClass().getField(attributeName);
                // TODO: somehow check this using reflection
            } catch(Exception e) {
                return false;
            }
        }
        return true;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        type = in.getAttributeValue(null, "type");
        abilityID = in.getAttributeValue(null, "ability-id");
        abilityValue = getAttribute(in, "ability-value", true);
        attributeName = in.getAttributeValue(null, "attribute-id");
        attributeValue = in.getAttributeValue(null, "attribute-value");
        in.nextTag();
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

        out.writeAttribute("type", type);
        out.writeAttribute("ability-id", abilityID);
        out.writeAttribute("ability-value", String.valueOf(abilityValue));
        out.writeAttribute("attribute-id", attributeName);
        out.writeAttribute("attribute-value", attributeValue);

        out.writeEndElement();
    }
    
    public static String getXMLElementTagName() {
        return "scope";
    }


}