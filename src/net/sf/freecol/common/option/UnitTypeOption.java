/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Option wrapping a UnitType.
 */
public class UnitTypeOption extends AbstractOption<UnitType> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(UnitTypeOption.class
            .getName());

    public static final String TAG = "unitTypeOption";
    
    /**
     * FIXME: replace with Predicates.
     */
    public static enum TypeSelector {
        UNITS, IMMIGRANTS, LAND_UNITS, NAVAL_UNITS
    }

    /** The option value. */
    private UnitType value;

    /** Whether to add "none" to the list of choices to be generated. */
    private boolean addNone;

    /** Which choices to generate. */
    private TypeSelector selector = TypeSelector.UNITS;

    /** A list of choices to provide to the UI. */
    private final List<UnitType> choices = new ArrayList<>();


    /**
     * Creates a new {@code UnitTypeOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public UnitTypeOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code UnitTypeOption}.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
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
        return this.addNone;
    }

    /**
     * Get the list of choices for this option.
     *
     * @return A list of {@code UnitType}s.
     */
    public final List<UnitType> getChoices() {
        return this.choices;
    }

    /**
     * Get the type of choices to generate.
     *
     * @return The type of choices to generate.
     */
    public final TypeSelector getSelector() {
        return this.selector;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public UnitTypeOption cloneOption() {
        UnitTypeOption ret = new UnitTypeOption(getId(), getSpecification());
        ret.value = this.value;
        ret.addNone = this.addNone;
        ret.selector = this.selector;
        ret.choices.clear();
        ret.choices.addAll(this.choices);
        return ret;
    }

    /**
     * Gets the current value of this {@code UnitTypeOption}.
     *
     * @return The {@code UnitType} value.
     */
    @Override
    public UnitType getValue() {
        return this.value;
    }

    /**
     * Sets the current value of this {@code UnitTypeOption}.
     *
     * @param value The new {@code UnitType} value.
     */
    @Override
    public void setValue(UnitType value) {
        final UnitType oldValue = this.value;
        this.value = value;
        
        if (value != oldValue && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        this.isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public boolean isNullValueOK() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateChoices() {
        if (this.selector == null) {
            UnitType val = getValue();
            if (val != null && this.choices.isEmpty()) this.choices.add(val);
        } else {
            List<UnitType> unitTypeList = getSpecification().getUnitTypeList();
            this.choices.clear();
            switch (this.selector) {
            case UNITS:
                this.choices.addAll(unitTypeList);
                break;
            case IMMIGRANTS:
                this.choices.addAll(transform(unitTypeList,
                                              UnitType::isRecruitable));
                break;
            case NAVAL_UNITS:
                this.choices.addAll(transform(unitTypeList,
                                              UnitType::isNaval));
                break;
            case LAND_UNITS:
                this.choices.addAll(transform(unitTypeList,
                                              ut -> !ut.isNaval()));
                break;
            }
            if (this.addNone) this.choices.add(0, null);
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
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value);
        }

        if (selector != null) {
            xw.writeAttribute(GENERATE_TAG, selector);
        }

        if (addNone) {
            xw.writeAttribute(ADD_NONE_TAG, addNone);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (choices != null && !choices.isEmpty()) {
            for (UnitType choice : choices) {
                xw.writeStartElement(CHOICE_TAG);

                xw.writeAttribute(VALUE_TAG, choice);

                xw.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr); // value is read here

        selector = xr.getAttribute(GENERATE_TAG,
                                   TypeSelector.class, (TypeSelector)null);

        addNone = xr.getAttribute(ADD_NONE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        this.choices.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (CHOICE_TAG.equals(tag)) {
            choices.add(xr.getType(spec, VALUE_TAG,
                                   UnitType.class, (UnitType)null));
            xr.closeTag(CHOICE_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getId())
            .append(" value=").append(this.value)
            .append(" addNone=").append(this.addNone)
            .append(" selector=").append(this.selector)
            .append(" choices=[");
        for (UnitType choice : this.choices) sb.append(' ').append(choice);
        sb.append(" ]]");
        return sb.toString();
    }
}
