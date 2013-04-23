/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;


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

    /** The option value. */
    private UnitType value;

    /** Whether to add "none" to the list of choices to be generated. */
    private boolean addNone;

    /** Which choices to generate. */
    private TypeSelector generateChoices = TypeSelector.UNITS;

    /** A list of choices to provide to the UI. */
    private final List<UnitType> choices = new ArrayList<UnitType>();


    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     */
    public UnitTypeOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public UnitTypeOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>UnitTypeOption</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     * @param specification The enclosing <code>Specification</code>.
     */
    public UnitTypeOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is "none" a valid choice for this option?
     *
     * @return True if "none" is a valid choice.
     */
    public final boolean addNone() {
        return addNone;
    }

    /**
     * Get the list of choices for this option.
     *
     * @return A list of <code>UnitType</code>s.
     */
    public final List<UnitType> getChoices() {
        return choices;
    }

    /**
     * Get the type of choices to generate.
     *
     * @return The type of choices to generate.
     */
    public final TypeSelector getGenerateChoices() {
        return generateChoices;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
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
     *
     * @return The <code>UnitType</code> value.
     */
    public UnitType getValue() {
        return value;
    }

    /**
     * Sets the current value of this <code>UnitTypeOption</code>.
     *
     * @param value The new <code>UnitType</code> value.
     */
    public void setValue(UnitType value) {
        final UnitType oldValue = this.value;
        this.value = value;
        
        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
     */
    public boolean isNullValueOK() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void generateChoices() {
        if (generateChoices == null) {
            choices.add(getValue());
        } else {
            List<UnitType> unitTypeList = getSpecification().getUnitTypeList();
            choices.clear();
            switch (generateChoices) {
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


    // Serialization

    private static final String ADD_NONE_TAG = "addNone";
    private static final String CHOICE_TAG = "choice";
    private static final String GENERATE_TAG = "generate";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (value != null) {
            writeAttribute(out, VALUE_TAG, value);
        }

        if (generateChoices != null) {
            writeAttribute(out, GENERATE_TAG, generateChoices);
        }

        if (addNone) {
            writeAttribute(out, ADD_NONE_TAG, addNone);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (choices != null && !choices.isEmpty()) {
            for (UnitType choice : choices) {
                out.writeStartElement(CHOICE_TAG);

                writeAttribute(out, VALUE_TAG, choice);

                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in); // value is read here

        generateChoices = getAttribute(in, GENERATE_TAG,
                                       TypeSelector.class, (TypeSelector)null);

        addNone = getAttribute(in, ADD_NONE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear container.
        choices.clear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (CHOICE_TAG.equals(tag)) {
            UnitType type = spec.getType(in, VALUE_TAG,
                                         UnitType.class, (UnitType)null);
            if (type != null) choices.add(type);
            closeTag(in, CHOICE_TAG);

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId())
            .append(" value=").append(value)
            .append(" addNone=").append(addNone)
            .append(" generateChoices=").append(generateChoices)
            .append("]");
        return sb.toString();
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
