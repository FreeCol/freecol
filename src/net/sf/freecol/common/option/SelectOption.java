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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is an integer and the
 * choices are represented by strings.  In general, these strings are
 * localized by looking up the key of the choice, which consists of
 * the identifier of the AbstractObject followed by a "." followed by
 * the value of the option string.  The automatic localization can be
 * suppressed with the doNotLocalize parameter, however.  There are
 * two reasons to do this: either the option strings should not be
 * localized at all (because they are language names, for example), or
 * the option strings have already been localized (because they do not
 * use the default keys, for example).
 */
public class SelectOption extends IntegerOption {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(SelectOption.class.getName());

    /** Use localized labels? */
    protected boolean localizedLabels = false;

    /** A map of the valid values. */
    private final Map<Integer, String> itemValues
        = new LinkedHashMap<>();


    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public SelectOption(Specification specification) {
        super(specification);
    }


    /**
     * Gets the range values of this <code>RangeOption</code>.
     *
     * @return The value.
     */
    public Map<Integer, String> getItemValues() {
        return itemValues;
    }

    /**
     * Add a new key,value pair to this option.
     *
     * @param key The key to add.
     * @param value The value to add.
     */
    public void addItemValue(Integer key, String value) {
        itemValues.put(key, value);
    }

    /**
     * Clear the item values for this option.
     *
     * Required by ClientOptions.fixClientOptions.
     */
    public void clearItemValues() {
        itemValues.clear();
    }
    
    /**
     * Whether the labels of this option need to be localized.  This is
     * not the case when the labels are just numeric values.
     *
     * @return True if localization is required.
     */
    public boolean localizeLabels() {
        return localizedLabels;
    }

    /**
     * Gets the tag name of the item element.
     *
     * Should be overridden by subclasses to ensure read/writeChildren work.
     *
     * @return "selectValue".
     */
    public String getXMLItemElementTagName() {
        return "selectValue";
    }

    // Interface Option
    
    /**
     * {@inheritDoc}
     */
    public void setValue(Integer value) {
        Set<Integer> keys = getItemValues().keySet();
        if (keys.isEmpty()) return; // May not have been read yet

        Integer fallback = null;
        for (Integer i : keys) {
            if (i == value) { // Found a valid selection
                super.setValue(value);
                return;
            }
            if (fallback == null) fallback = i;
        }
        logger.warning(getXMLElementTagName() + ".setValue invalid value: "
            + value + ", using fallback: " + fallback);
        super.setValue(fallback);
    }


    // Serialization

    private static final String LABEL_TAG = "label";
    private static final String LOCALIZED_LABELS_TAG = "localizedLabels";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(LOCALIZED_LABELS_TAG, localizedLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Map.Entry<Integer, String> entry : itemValues.entrySet()) {
            xw.writeStartElement(getXMLItemElementTagName());

            xw.writeAttribute(VALUE_TAG, entry.getKey());

            xw.writeAttribute(LABEL_TAG, entry.getValue());

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        localizedLabels = xr.getAttribute(LOCALIZED_LABELS_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // We can not set the value until we have read the select options
        // so as to be able to check its validity.
        String value = xr.getAttribute(VALUE_TAG, (String)null);
        String defaultValue = xr.getAttribute(DEFAULT_VALUE_TAG, (String)null);

        // Clear containers.
        clearItemValues();
        
        super.readChildren(xr);

        // Now we can correctly set the value.
        setValue(value, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (getXMLItemElementTagName().equals(tag)) {
            addItemValue(xr.getAttribute(VALUE_TAG, INFINITY),
                         xr.getAttribute(LABEL_TAG, (String)null));
            xr.closeTag(tag);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append("[").append(getId())
            .append(" value=").append(getValue())
            .append(" localized=").append(localizedLabels)
            .append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "selectOption".
     */
    public static String getXMLElementTagName() {
        return "selectOption";
    }
}
