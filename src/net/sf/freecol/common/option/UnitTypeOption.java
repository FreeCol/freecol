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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.UnitTypeOption.TypeSelector;


/**
 * Option wrapping a UnitType.
 */
public class UnitTypeOption extends AbstractOption<UnitType> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(UnitTypeOption.class.getName());

    /**
     * TODO: replace with Predicates.
     */
    public static enum TypeSelector {
        UNITS, IMMIGRANTS, LAND_UNITS, NAVAL_UNITS
    }

    /**
     * The option value.
     */
    private UnitType value;

    /**
     * Whether to add "none" to the list of choices to be generated.
     */
    private boolean addNone;

    /**
     * Which choices to generate.
     */
    private TypeSelector generateChoices = TypeSelector.UNITS;

    /**
     * A list of choices to provide to the UI.
     */
    private List<UnitType> choices = new ArrayList<UnitType>();

    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public UnitTypeOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public UnitTypeOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *     should be found in an {@link OptionGroup}.
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public UnitTypeOption(String id, Specification specification) {
        super(id, specification);
    }

    public UnitTypeOption clone() {
        UnitTypeOption result = new UnitTypeOption(getId(), getSpecification());
        result.value = value;
        result.addNone = addNone;
        result.generateChoices = generateChoices;
        result.generateChoices();
        result.isDefined = true;
        return result;
    }

    /**
     * Gets the current value of this <code>UnitTypeOption</code>.
     * @return The value.
     */
    public UnitType getValue() {
        return value;
    }


    /**
     * Sets the current value of this <code>UnitTypeOption</code>.
     * @param value The value.
     */
    public void setValue(UnitType value) {
        final UnitType oldValue = this.value;
        this.value = value;

        if ( value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

    /**
     * Sets the value of this Option from the given string
     * representation. Both parameters must not be null at the same
     * time.
     *
     * @param valueString the string representation of the value of
     * this Option
     * @param defaultValueString the string representation of the
     * default value of this Option
     */
    protected void setValue(String valueString, String defaultValueString) {
        if (valueString != null) {
            setValue(getSpecification().getUnitType(valueString));
        } else if (defaultValueString != null) {
            setValue(getSpecification().getUnitType(defaultValueString));
        } else {
            setValue(null);
        }
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
     * @return a <code>List<UnitType></code> value
     */
    public final List<UnitType> getChoices() {
        return choices;
    }

    /**
     * Set the <code>Choices</code> value.
     *
     * @param newChoices The new Choices value.
     */
    public final void setChoices(final List<UnitType> newChoices) {
        this.choices = newChoices;
    }

    /**
     * Get the <code>GenerateChoices</code> value.
     *
     * @return a <code>Generate</code> value
     */
    public final TypeSelector getGenerateChoices() {
        return generateChoices;
    }

    /**
     * Set the <code>GenerateChoices</code> value.
     *
     * @param newGenerateChoices The new GenerateChoices value.
     */
    public final void setGenerateChoices(final TypeSelector newGenerateChoices) {
        this.generateChoices = newGenerateChoices;
    }

    /**
     * Generate the choices to provide to the UI based on the
     * generateChoices value.
     *
     */
    public void generateChoices() {
        if (generateChoices == null) {
            if (choices == null || choices.isEmpty()) {
                choices = new ArrayList<UnitType>();
                choices.add(getValue());
            }
        } else {
            List<UnitType> unitTypeList = getSpecification().getUnitTypeList();
            choices = new ArrayList<UnitType>();
            switch(generateChoices) {
            case UNITS:
                choices.addAll(unitTypeList);
                break;
            case IMMIGRANTS:
                for (UnitType unitType : unitTypeList) {
                    if (unitType.isRecruitable()) {
                        choices.add(unitType);
                    }
                }
                break;
            case NAVAL_UNITS:
                for (UnitType unitType : unitTypeList) {
                    if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                        choices.add(unitType);
                    }
                }
                break;
            case LAND_UNITS:
                for (UnitType unitType : unitTypeList) {
                    if (!unitType.hasAbility(Ability.NAVAL_UNIT)) {
                        choices.add(unitType);
                    }
                }
                break;
            }
            if (addNone) {
                choices.add(0, null);
            }
        }
    }

    /**
     * Returns whether <code>null</code> is an acceptable value for
     * this Option. This method always returns <code>true</code>.
     *
     * @return true
     */
    public boolean isNullValueOK() {
        return true;
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
        if (value != null) {
            out.writeAttribute(VALUE_TAG, value.getId());
        }
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
            for (UnitType choice : choices) {
                out.writeStartElement("choice");
                out.writeAttribute(VALUE_TAG, choice.getId());
                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        addNone = getAttribute(in, "addNone", false);
        String generate = in.getAttributeValue(null, "generate");
        if (generate != null) {
            generateChoices = TypeSelector.valueOf(generate);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitTypeOption".
     */
    public static String getXMLElementTagName() {
        return "unitTypeOption";
    }
}
