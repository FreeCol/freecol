/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;


/**
 * Represents an option that can be either <i>true</i>
 * or <i>false</i>.
 */
public class StringOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(StringOption.class.getName());

    public static final String NONE = "none";

    public static enum Generate {
        UNITS, IMMIGRANTS, LAND_UNITS, NAVAL_UNITS, BUILDINGS, FOUNDING_FATHERS
    }

    private String value;

    /**
     * Describe addNone here.
     */
    private boolean addNone;

    /**
     * Describe generateChoices here.
     */
    private Generate generateChoices;

    /**
     * Describe choices here.
     */
    private List<String> choices;

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public StringOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>StringOption</code>.
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public StringOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXMLImpl(in);
    }

    /**
     * Gets the current value of this <code>StringOption</code>.
     * @return The value.
     */
    public String getValue() {
        return value;
    }


    /**
     * Sets the current value of this <code>StringOption</code>.
     * @param value The value.
     */
    public void setValue(String value) {
        final String oldValue = this.value;
        this.value = value;

        if ( value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

    /**
     * Get the <code>AddNone</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean addNone() {
        return addNone;
    }

    /**
     * Set the <code>AddNone</code> value.
     *
     * @param newAddNone The new AddNone value.
     */
    public final void setAddNone(final boolean newAddNone) {
        this.addNone = newAddNone;
    }

    /**
     * Get the <code>Choices</code> value.
     *
     * @return a <code>List<String></code> value
     */
    public final List<String> getChoices() {
        return choices;
    }

    /**
     * Set the <code>Choices</code> value.
     *
     * @param newChoices The new Choices value.
     */
    public final void setChoices(final List<String> newChoices) {
        this.choices = newChoices;
    }

    /**
     * Get the <code>GenerateChoices</code> value.
     *
     * @return a <code>Generate</code> value
     */
    public final Generate getGenerateChoices() {
        return generateChoices;
    }

    /**
     * Set the <code>GenerateChoices</code> value.
     *
     * @param newGenerateChoices The new GenerateChoices value.
     */
    public final void setGenerateChoices(final Generate newGenerateChoices) {
        this.generateChoices = newGenerateChoices;
    }

    public void generateChoices(Specification specification) {
        if (generateChoices == null) {
            if (choices == null || choices.isEmpty()) {
                choices = new ArrayList<String>();
                choices.add(getValue());
            }
        } else {
            List<FreeColObject> objects = new ArrayList<FreeColObject>();
            switch(generateChoices) {
            case UNITS:
                objects.addAll(specification.getUnitTypeList());
                break;
            case IMMIGRANTS:
                for (UnitType unitType : specification.getUnitTypeList()) {
                    if (unitType.isRecruitable()) {
                        objects.add(unitType);
                    }
                }
                break;
            case NAVAL_UNITS:
                for (UnitType unitType : specification.getUnitTypeList()) {
                    if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                        objects.add(unitType);
                    }
                }
                break;
            case LAND_UNITS:
                for (UnitType unitType : specification.getUnitTypeList()) {
                    if (!unitType.hasAbility(Ability.NAVAL_UNIT)) {
                        objects.add(unitType);
                    }
                }
                break;
            case BUILDINGS:
                objects.addAll(specification.getBuildingTypeList());
                break;
            case FOUNDING_FATHERS:
                objects.addAll(specification.getFoundingFathers());
                break;
            }
            choices = new ArrayList<String>(objects.size() + (addNone ? 1 : 0));
            if (addNone) {
                choices.add(StringOption.NONE);
            }
            for (FreeColObject object : objects) {
                choices.add(object.getId());
            }
        }
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute(VALUE_TAG, value);
        if (generateChoices != null) {
            out.writeAttribute("generate", generateChoices.toString());
        }
        if (addNone) {
            out.writeAttribute("addNone", Boolean.toString(addNone));
        }
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        if (choices != null && !choices.isEmpty()) {
            for (String choice : choices) {
                out.writeStartElement("choice");
                out.writeAttribute(VALUE_TAG, choice);
                out.writeEndElement();
            }
        }
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String value = in.getAttributeValue(null, VALUE_TAG);

        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                                         + "> tag : no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                                         + "> tag : no value nor default value found.");
        }

        if(getId() == NO_ID) {
            setId(id);
        }
        if(value != null) {
            setValue(value);
        } else {
            setValue(defaultValue);
        }

        addNone = getAttribute(in, "addNone", false);
        String generate = in.getAttributeValue(null, "generate");
        if (generate != null) {
            generateChoices = Enum.valueOf(StringOption.Generate.class, generate);
        }

        choices = new ArrayList<String>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("choice".equals(in.getLocalName())) {
                choices.add(in.getAttributeValue(null, VALUE_TAG));
                in.nextTag();
            }
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "StringOption".
     */
    public static String getXMLElementTagName() {
        return "stringOption";
    }
}
