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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Specification;

/**
 * Represents a List of Options.
 *
 */
public class ListOption<T> extends AbstractOption<List<AbstractOption<T>>> {

    private static Logger logger = Logger.getLogger(ListOption.class.getName());

    /**
     * A list of Options.
     */
    private List<AbstractOption<T>> value = new ArrayList<AbstractOption<T>>();

    /**
     * The AbstractOption used to generate new values.
     */
    private AbstractOption<T> template;

    /**
     * The maximum number of list entries. Defaults to Integer.MAX_VALUE.
     */
    private int maximumNumber = Integer.MAX_VALUE;


    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public ListOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>ListOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public ListOption(Specification specification) {
        super(specification);
    }

    public ListOption<T> clone() {
        ListOption<T> result = new ListOption<T>(getId());
        return result;
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
     * Gets the current value of this <code>ListOption</code>.
     * @return The value.
     */
    public List<AbstractOption<T>> getValue() {
        return value;
    }

    public List<T> getOptionValues() {
        List<T> result = new ArrayList<T>();
        for (AbstractOption<T> option : value) {
            if (option != null) {
                result.add(option.getValue());
            }
        }
        return result;
    }

    /**
     * Sets the value of this <code>ListOption</code>.
     * @param value The value to be set.
     */
    public void setValue(List<AbstractOption<T>> value) {
        // fail fast: the list value may be empty, but it must not be
        // null
        if (value == null) {
            throw new IllegalArgumentException("ListOption value must not be 'null'!");
        }
        final List<AbstractOption<T>> oldValue = this.value;
        this.value = value;

        if (!value.equals(oldValue) && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }

    public AbstractOption<T> getTemplate() {
        return template;
    }

    public void setTemplate(AbstractOption<T> template) {
        this.template = template;
    }

    /**
     * Returns whether <code>null</code> is an acceptable value for
     * this AbstractOption. This method always returns <code>true</code>.
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
        toXMLImpl(out, getXMLElementTagName());
    }

    protected void toXMLImpl(XMLStreamWriter out, String tag)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(tag);

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("maximumNumber", Integer.toString(maximumNumber));
        if (template != null) {
            out.writeStartElement("template");
            template.toXML(out);
            out.writeEndElement();
        }
        for (AbstractOption option : value) {
            option.toXML(out);
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    @SuppressWarnings("unchecked")
    public void readFromXML(XMLStreamReader in)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        maximumNumber = getAttribute(in, "maximumNumber", 1);

        value.clear();

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("template".equals(in.getLocalName())) {
                in.nextTag();
                template = readOption(in);
                in.nextTag();
            } else if ("optionValue".equals(in.getLocalName())) {
                // @compat 0.10.4
                String modId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                logger.finest("found old-style mod value: " + modId);
                ModOption modOption = new ModOption(modId);
                modOption.setValue(net.sf.freecol.common.io.Mods.getModFile(modId));
                value.add((AbstractOption<T>) modOption);
                // end @compat
            } else {
                value.add(readOption(in));
            }
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitListOption".
     */
    public static String getXMLElementTagName() {
        return "listOption";
    }
}
