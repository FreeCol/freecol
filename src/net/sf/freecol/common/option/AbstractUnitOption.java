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

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Specification.TypeSelector;

/**
 * Represents an option where the valid choice is an AbstractUnit.
 */
public class AbstractUnitOption extends AbstractOption<AbstractUnit> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(AbstractUnitOption.class.getName());

    private static final TypeSelector DEFAULT_SELECTOR = TypeSelector.UNITS;

    private AbstractUnit value;

    /**
     * The minimum number of units. Defaults to 1.
     */
    private int minimumNumber = 1;

    /**
     * The maximum number of units. Defaults to 1.
     */
    private int maximumNumber = 1;

    /**
     * Whether the Role of the Unit may be selected.
     */
    private boolean selectRole = true;

    /**
     * Determines which unit types are valid for this Option if no
     * choices are provided.
     */
    private TypeSelector validTypes = DEFAULT_SELECTOR;

    /**
     * A list of valid unit types for this Option.
     */
    private List<String> choices;

    /**
     * Creates a new <code>AbstractUnitOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public AbstractUnitOption(String id) {
        super(id);
    }

    /**
     * Creates a new  <code>AbstractUnitOption</code>.
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnitOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
    }


    /**
    * Returns the minimum allowed value.
    * @return The minimum value allowed by this option.
    */
    public int getMinimumValue() {
        return minimumNumber;
    }

    /**
     * Sets the minimum allowed value.
     * @param minimumNumber The minimum value to set
     */
    public void setMinimumValue(int minimumNumber) {
        this.minimumNumber = minimumNumber;
    }

    /**
    * Returns the maximum allowed value.
    * @return The maximum value allowed by this option.
    */
    public int getMaximumValue() {
        return maximumNumber;
    }

    /**
     * Sets the maximum allowed value.
     * @param maximumNumber the maximum value to set
     */
    public void setMaximumValue(int maximumNumber) {
        this.maximumNumber = maximumNumber;
    }

    /**
     * Gets the current value of this <code>AbstractUnitOption</code>.
     * @return The value.
     */
    public AbstractUnit getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>AbstractUnitOption</code>.
     * @param value The value to be set.
     */
    public void setValue(AbstractUnit value) {
        final AbstractUnit oldValue = this.value;
        this.value = value;

        if (!value.equals(oldValue) && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

    /**
     * Returns whether the Role of the Unit may be selected.
     *
     * @return whether to select the Role
     */
    public boolean getSelectRole() {
        return selectRole;
    }

    /**
     * Sets whether the Role of the Unit may be selected.
     *
     * @param value the value of the selectRole attribute
     */
    public void setSelectRole(boolean value) {
        selectRole = value;
    }

    /**
     * Returns the valid unit types for this option.
     *
     * @return the valid unit types for this option
     */
    public TypeSelector getValidTypes() {
        return validTypes;
    }

    /**
     * Set the valid unit types for this option.
     *
     * @param value the valid unit types for this option
     */
    public void setValidTypes(TypeSelector value) {
        if (value == null
            || value == TypeSelector.BUILDINGS
            || value == TypeSelector.FOUNDING_FATHERS) {
            validTypes = DEFAULT_SELECTOR;
            logger.warning("Invalid type selector for AbstractUnit: " + value
                           + ". Falling back to default selector: " + validTypes);
        } else {
            validTypes = value;
        }
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
     * Generate the choices to provide to the UI based on the
     * validTypes value.
     *
     * @param specification the Specification that defines the game
     * objects whose IDs will be generated
     */
    public void generateChoices(Specification specification) {
        if (validTypes == null) {
            if (choices == null || choices.isEmpty()) {
                choices = new ArrayList<String>();
                choices.add(value.getId());
            }
        } else {
            for (FreeColGameObjectType object : specification.getTypes(validTypes)) {
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
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("minimumNumber", Integer.toString(minimumNumber));
        out.writeAttribute("maximumNumber", Integer.toString(maximumNumber));
        out.writeAttribute("selectRole", Boolean.toString(selectRole));
        if (validTypes != null) {
            out.writeAttribute("validTypes", validTypes.toString());
        }
        value.toXML(out);
        if (choices != null && !choices.isEmpty()) {
            for (String choice : choices) {
                out.writeStartElement("choice");
                out.writeAttribute(VALUE_TAG, choice);
                out.writeEndElement();
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);

        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                                         + "> tag : no id attribute found.");
        }

        if (getId() == NO_ID) {
            setId(id);
        }

        minimumNumber = getAttribute(in, "minimumNumber", 1);
        maximumNumber = getAttribute(in, "maximumNumber", 1);
        selectRole = getAttribute(in, "selectRole", true);
        setValidTypes(getAttribute(in, "validTypes", TypeSelector.class, DEFAULT_SELECTOR));

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (AbstractUnit.getXMLElementTagName().equals(in.getLocalName())) {
                value = new AbstractUnit(in);
            } else if ("choice".equals(in.getLocalName())) {
                choices.add(in.getAttributeValue(null, VALUE_TAG));
            }
            in.nextTag();
        }

    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitOption".
     */
    public static String getXMLElementTagName() {
        return "unitOption";
    }
}
