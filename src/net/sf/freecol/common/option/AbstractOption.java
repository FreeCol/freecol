/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;


/**
 * The super class of all options.  GUI components making use of this
 * class can refer to its name and shortDescription properties.  The
 * complete keys of these properties consist of the identifier of the
 * option group (if any), followed by a "."  unless the option group
 * is null, followed by the identifier of the option object, followed
 * by a ".", followed by "name" or "shortDescription".
 */
public abstract class AbstractOption<T> extends FreeColObject
    implements Option<T> {

    private static final Logger logger = Logger.getLogger(AbstractOption.class.getName());

    /** The option group prefix. */
    private String optionGroup = "";

    /**
     * Determine if the option has been defined.  When defined an
     * option won't change when a default value is read from an XML file.
     */
    protected boolean isDefined = false;


    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The object identifier.
     */
    public AbstractOption(String id) {
        setId(id);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public AbstractOption(Specification specification) {
        this(null, specification);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public AbstractOption(String id, Specification specification) {
        setId(id);
        setSpecification(specification);
    }


    /**
     * Gets the string prefix that identifies the group of this
     * <code>Option</code>.
     *
     * @return The string prefix provided by the OptionGroup.
     */
    public String getGroup() {
        return optionGroup;
    }

    /**
     * Set the option group prefix.
     *
     * @param group The prefix to set.
     */
    public void setGroup(String group) {
        optionGroup = (group == null) ? "" : group;
    }

    /**
     * Sets the values from another option.
     *
     * @param source The other <code>AbstractOption</code>.
     */
    protected void setValues(AbstractOption<T> source) {
        setId(source.getId());
        setSpecification(source.getSpecification());
        setValue(source.getValue());
        setGroup(source.getGroup());
        isDefined = source.isDefined;
    }

    /**
     * Sets the value of this option from the given string
     * representation.  Both parameters must not be null at the same
     * time.  This method does nothing.  Override it if the option has
     * a suitable string representation.
     *
     * @param valueString The string representation of the value of
     *     this <code>Option</code>.
     * @param defaultValueString The string representation of the
     *     default value of this <code>Option</code>.
     * @exception XMLStreamException if the value is invalid.
     */
    protected void setValue(String valueString, String defaultValueString)
        throws XMLStreamException {
        throw new XMLStreamException("Unsupported method: setValue.");
    }

    /**
     * Generate the choices to provide to the UI.
     *
     * Override if the subclass needs to determine its choices dynamically.
     */
    public void generateChoices() {
        // do nothing
    }

    /**
     * Is null an acceptable value for this option?
     *
     * Override this in subclasses where necessary.
     *
     * @return False.
     */
    public boolean isNullValueOK() {
        return false;
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract AbstractOption<T> clone() throws CloneNotSupportedException;

    /**
     * Gets the value of this option.
     *
     * @return The value of this <code>Option</code>.
     */
    @Override
    public abstract T getValue();

    /**
     * Sets the value of this option.
     *
     * @param value The new value of this <code>Option</code>.
     */
    @Override
    public abstract void setValue(T value);


    // Serialization

    protected static final String ACTION_TAG = "action";
    protected static final String DEFAULT_VALUE_TAG = "defaultValue";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        String defaultValue = xr.getAttribute(DEFAULT_VALUE_TAG, (String)null);

        String value = xr.getAttribute(VALUE_TAG, (String)null);
        if (defaultValue == null && value == null) {
            if (!isNullValueOK()) {
                throw new XMLStreamException("invalid option " + getId()
                    + ": no value nor default value found.");
            }
        } else {
            setValue(value, defaultValue);
        }
    }

    // Note: writeAttributes() is not needed/present.
    // - The identifier is correctly written by the super class.
    // - The default value does not need to be written in general.
    // - The value *must* be written by the implementing subclass.

    /**
     * General option reader routine.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @return An option.
     */
    protected AbstractOption readOption(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();
        AbstractOption option = null;

        if (ACTION_TAG.equals(tag)) {
            // FIXME: load FreeColActions from client options?
            logger.finest("Skipping action " + xr.readId());
            xr.nextTag();

        } else if (AbstractUnitOption.getXMLElementTagName().equals(tag)) {
            option = new AbstractUnitOption(spec);

        } else if (AudioMixerOption.getXMLElementTagName().equals(tag)) {
            option = new AudioMixerOption(spec);

        } else if (BooleanOption.getXMLElementTagName().equals(tag)) {
            option = new BooleanOption(spec);

        } else if (FileOption.getXMLElementTagName().equals(tag)) {
            option = new FileOption(spec);

        } else if (IntegerOption.getXMLElementTagName().equals(tag)) {
            option = new IntegerOption(spec);

        } else if (LanguageOption.getXMLElementTagName().equals(tag)) {
            option = new LanguageOption(spec);

        } else if (ModListOption.getXMLElementTagName().equals(tag)) {
            option = new ModListOption(spec);

        } else if (ModOption.getXMLElementTagName().equals(tag)) {
            option = new ModOption(spec);

        } else if (OptionGroup.getXMLElementTagName().equals(tag)) {
            option = new OptionGroup(spec);

        } else if (PercentageOption.getXMLElementTagName().equals(tag)) {
            option = new PercentageOption(spec);

        } else if (RangeOption.getXMLElementTagName().equals(tag)) {
            option = new RangeOption(spec);

        } else if (SelectOption.getXMLElementTagName().equals(tag)) {
            option = new SelectOption(spec);

        } else if (StringOption.getXMLElementTagName().equals(tag)) {
            option = new StringOption(spec);

        } else if (UnitListOption.getXMLElementTagName().equals(tag)) {
            option = new UnitListOption(spec);

        } else if (UnitTypeOption.getXMLElementTagName().equals(tag)) {
            option = new UnitTypeOption(spec);

        } else if (TextOption.getXMLElementTagName().equals(tag)) {
            option = new TextOption(spec);

        } else {
            logger.warning("Not an option type: " + tag);
            xr.nextTag();
        }

        if (option != null) option.readFromXML(xr);
        return option;
    }
}
