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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is an integer and the choices are
 * represented by strings. In general, these strings are localized by looking up
 * the key of the choice, which consists of the id of the AbstractObject
 * followed by a "." followed by the value of the option string. The automatic
 * localization can be suppressed with the doNotLocalize parameter, however.
 * There are two reasons to do this: either the option strings should not be
 * localized at all (because they are language names, for example), or the
 * option strings have already been localized (because they do not use the
 * default keys, for example).
 */
public class SelectOption extends IntegerOption {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(SelectOption.class.getName());

    /** Use localized labels? */
    protected boolean localizedLabels = false;

    /** A map of the valid values. */
    private final Map<Integer, String> itemValues
        = new LinkedHashMap<Integer, String>();


    /**
     * Creates a new <code>SelectOption</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
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


    // Serialization

    private static final String LABEL_TAG = "label";
    private static final String LOCALIZED_LABELS_TAG = "localizedLabels";


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

        writeAttribute(out, LOCALIZED_LABELS_TAG, localizedLabels);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Map.Entry<Integer, String> entry : itemValues.entrySet()) {
            out.writeStartElement(getXMLItemElementTagName());

            writeAttribute(out, VALUE_TAG, entry.getKey());

            writeAttribute(out, LABEL_TAG, entry.getValue());

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        localizedLabels = getAttribute(in, LOCALIZED_LABELS_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        itemValues.clear(); // Clear containers

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (getXMLItemElementTagName().equals(tag)) {
            String label = getAttribute(in, LABEL_TAG, (String)null);
            int value = getAttribute(in, VALUE_TAG, INFINITY);
            if (label == null) {
                logger.warning("Missing " + LABEL_TAG + " for " + tag);
            } else if (value == INFINITY) {
                logger.warning("Missing " + VALUE_TAG + " for " + tag);
            } else {
                itemValues.put(value, label);
            }
            in.nextTag();

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "selectOption".
     */
    public static String getXMLElementTagName() {
        return "selectOption";
    }
}
