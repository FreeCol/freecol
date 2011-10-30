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

import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be an arbitrary string.
 */
public class StringOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(StringOption.class.getName());

    /**
     * The option value.
     */
    private String value;

    /**
     * A list of choices to provide to the UI.
     */
    private List<String> choices;

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public StringOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public StringOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *     should be found in an {@link OptionGroup}.
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public StringOption(String id, Specification specification) {
        super(id, specification);
    }

    public StringOption clone() {
        StringOption result = new StringOption(getId());
        result.value = value;
        result.choices = new ArrayList<String>(choices);
        result.isDefined = true;
        return result;
    }

    /**
     * Gets the current value of this <code>StringOption</code>.
     * @return The value.
     */
    public String getValue() {
        return value;
    }


    /**
     * Sets the current value of this <code>StringOption</code>.
     * @param value The value.
     */
    public void setValue(String value) {
        final String oldValue = this.value;
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
        setValue((valueString != null) ? valueString : defaultValueString);
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

        out.writeAttribute(VALUE_TAG, value);
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
            for (String choice : choices) {
                out.writeStartElement("choice");
                out.writeAttribute(VALUE_TAG, choice);
                out.writeEndElement();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        choices = new ArrayList<String>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        if ("choice".equals(in.getLocalName())) {
            choices.add(in.getAttributeValue(null, VALUE_TAG));
            in.nextTag();
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "StringOption".
     */
    public static String getXMLElementTagName() {
        return "stringOption";
    }
}
