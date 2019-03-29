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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
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
     * Creates a new {@code ListOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public ListOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code ListOption}.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public ListOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Gets the generating template.
     *
     * @return The template.
     */
    public AbstractOption<T> getTemplate() {
        return this.template;
    }

    /**
     * Sets the generating template.
     *
     * @param template The template to set.
     */
    public void setTemplate(AbstractOption<T> template) {
        this.template = template;
    }

    /**
     * Gets the maximum number of allowed values.
     *
     * @return The maximum number of allowed values for this option.
     */
    public int getMaximumNumber() {
        return this.maximumNumber;
    }

    /**
     * Sets the maximum number of allowed values.
     *
     * @param maximumNumber The new maximum number of allowed values.
     */
    public void setMaximumNumber(int maximumNumber) {
        this.maximumNumber = maximumNumber;
    }

    /**
     * Get the values of the current non-null options in the list.
     *
     * @return A list of option values.
     */
    public List<T> getOptionValues() {
        return transform(this.value, isNotNull(), AbstractOption::getValue);
    }

    /**
     * Add a member to the values list.
     *
     * @param ao The new {@code AbstractOption} member to add.
     */
    private void addMember(AbstractOption<T> ao) {
        if (canAdd(ao)) this.value.add(ao);
    }

    /**
     * Does this list allow duplicates?
     *
     * @return True if duplicates are allowed.
     */
    public boolean getAllowDuplicates() {
        return this.allowDuplicates;
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
        return (allowDuplicates) ? true : none(value, matchKey(ao));
    }

    /**
     * Set the list option values from another list option.
     *
     * @param lo The other {@code ListOption}.
     */
    protected void setListValues(ListOption<T> lo) {
        this.setMaximumNumber(lo.getMaximumNumber());
        this.setTemplate(lo.getTemplate());
        this.setAllowDuplicates(lo.getAllowDuplicates());
    }


    // Interface Option

    /**
     * Gets the current value of this {@code ListOption}.
     *
     * @return The value.
     */
    @Override
    public List<AbstractOption<T>> getValue() {
        return this.value;
    }

    /**
     * Sets the value of this {@code ListOption}.
     *
     * @param value The value to be set.
     */
    @Override
    public void setValue(List<AbstractOption<T>> value) {
        // Fail fast: the list value may be empty, but it must not be null.
        if (value == null) {
            throw new RuntimeException("Null ListOption: " + this);
        }

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
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        switch (tag) {
        case TEMPLATE_TAG:
            xr.nextTag();
            template = readChildOption(xr);
            xr.closeTag(TEMPLATE_TAG);
            break;
        default:
            try {
                AbstractOption<T> op = readChildOption(xr);
                if (op != null) addMember(op);
            } catch (XMLStreamException xse) {
                logger.log(Level.WARNING, "Invalid option at: " + tag, xse);
                xr.closeTag(tag);
            }
            break;
        }
    }

    /**
     * Hack to suppress warning when reading a typed option.
     *
     * FIXME: Work out how to make this go away.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @return A child typed {@code AbstractOption}.
     * @exception XMLStreamException if the stream is corrupt.
     */
    @SuppressWarnings("unchecked")
    private AbstractOption<T> readChildOption(FreeColXMLReader xr)
        throws XMLStreamException {
        return (AbstractOption<T>)readOption(xr);
    }
    

    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getId());
        if (this.value != null) {
            sb.append(" [");
            for (AbstractOption<T> ao : this.value) {
                sb.append(' ').append(ao);
            }
            sb.append(" ]");
        }
        if (template != null) {
            sb.append(" template=").append(this.template);
        }
        sb.append(']');
        return sb.toString();
    }
}
