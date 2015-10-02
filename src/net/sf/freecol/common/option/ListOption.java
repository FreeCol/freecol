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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Represents a list of Options.
 */
public abstract class ListOption<T> extends AbstractOption<List<AbstractOption<T>>> {

    private static final Logger logger = Logger.getLogger(ListOption.class.getName());

    /** The AbstractOption used to generate new values. */
    private AbstractOption<T> template;

    /** The maximum number of list entries. Defaults to Integer.MAX_VALUE. */
    private int maximumNumber = Integer.MAX_VALUE;

    /** The list of options. */
    private final List<AbstractOption<T>> value = new ArrayList<>();

    /**
     * Whether the list can include duplicates.  This was always true before
     * adding this variable so the default should remain == true.
     */
    protected boolean allowDuplicates = true;


    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public ListOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public ListOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Gets the maximum number of allowed values.
     *
     * @return The maximum number of allowed values for this option.
     */
    public int getMaximumValue() {
        return maximumNumber;
    }

    /**
     * Gets the generating template.
     *
     * @return The template.
     */
    public AbstractOption<T> getTemplate() {
        return template;
    }

    /**
     * Get the values of the current non-null options in the list.
     *
     * @return A list of option values.
     */
    public List<T> getOptionValues() {
        List<T> result = new ArrayList<>();
        for (AbstractOption<T> option : value) {
            if (option != null) result.add(option.getValue());
        }
        return result;
    }

    /**
     * Add a member to the values list.
     *
     * @param ao The new <code>AbstractOption</code> member to add.
     */
    private void addMember(AbstractOption<T> ao) {
        if (canAdd(ao)) this.value.add(ao);
    }

    /**
     * Does this list allow duplicates?
     *
     * @return True if duplicates are allowed.
     */
    public boolean allowsDuplicates() {
        return allowDuplicates;
    }

    /**
     * Set the deduplicatation flag.
     *
     * @param allowDuplicates The new deduplication flag;
     */
    public void setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
    }

    /**
     * Can an option be added to this list?
     *
     * @param ao The option to check.
     * @return True if the option can be added.
     */
    public boolean canAdd(AbstractOption<T> ao) {
        return (allowDuplicates) ? true
            : none(value, o -> o.equals(ao));
    }


    // Interface Option

    /**
     * Gets the current value of this <code>ListOption</code>.
     *
     * @return The value.
     */
    @Override
    public List<AbstractOption<T>> getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>ListOption</code>.
     *
     * @param value The value to be set.
     */
    @Override
    public void setValue(List<AbstractOption<T>> value) {
        // Fail fast: the list value may be empty, but it must not be null.
        if (value==null) throw new IllegalArgumentException("Null ListOption");

        List<AbstractOption<T>> oldValue = new ArrayList<>(this.value);
        this.value.clear();
        for (AbstractOption<T> op : value) addMember(op);

        if (isDefined && !value.equals(oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }


    // Serialization

    private static final String MAXIMUM_NUMBER_TAG = "maximumNumber";
    private static final String OPTION_VALUE_TAG = "optionValue";
    private static final String TEMPLATE_TAG = "template";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(MAXIMUM_NUMBER_TAG, maximumNumber);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        if (template != null) {
            xw.writeStartElement(TEMPLATE_TAG);
        
            template.toXML(xw);
            
            xw.writeEndElement();
        }

        for (AbstractOption option : value) {
            option.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        maximumNumber = xr.getAttribute(MAXIMUM_NUMBER_TAG, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        value.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override @SuppressWarnings("unchecked")
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (null != tag) // @compat 0.10.4
        switch (tag) {
            case OPTION_VALUE_TAG:
                String modId = xr.readId();
                logger.log(Level.FINEST, "Found old-style mod value: {0}",
                        modId);
                if (modId != null) {
                    FreeColModFile fcmf = Mods.getModFile(modId);
                    if (fcmf != null) {
                        ModOption modOption = new ModOption(getSpecification());
                        modOption.setValue(fcmf);
                        addMember((AbstractOption<T>)modOption);
                    }
                }
                // end @compat
                break;
            case TEMPLATE_TAG:
                xr.nextTag();
                template = (AbstractOption<T>)readOption(xr);
                xr.closeTag(TEMPLATE_TAG);
                break;
            default:
                AbstractOption<T> op = null;
                try {
                    op = (AbstractOption<T>)readOption(xr);
                } catch (XMLStreamException xse) {
                    logger.log(Level.WARNING, "Invalid option at: " + tag, xse);
                    xr.closeTag(tag);
            }   if (op != null) addMember(op);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId());
        if (value != null) {
            sb.append(" [");
            for (AbstractOption<T> ao : value) {
                sb.append(" ").append(ao);
            }
            sb.append(" ]");
        }
        sb.append("]");
        return sb.toString();
    }
}
