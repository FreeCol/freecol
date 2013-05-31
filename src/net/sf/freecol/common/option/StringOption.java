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
 * Represents an option that can be an arbitrary string.
 */
public class StringOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(StringOption.class.getName());

    /** The value of this option. */
    private String value;

    /** A list of choices to provide to the UI. */
    private final List<String> choices = new ArrayList<String>();


    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param id The object identifier.
     */
    public StringOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public StringOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>StringOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public StringOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the list of string choices.
     *
     * @return The list of choices.
     */
    public final List<String> getChoices() {
        return choices;
    }

    /**
     * Set the choices.
     *
     * @param newChoices The new list of choices.
     */
    public final void setChoices(final List<String> newChoices) {
        this.choices.clear();
        this.choices.addAll(newChoices);
    }


    // Interface Option.

    /**
     * {@inheritDoc}
     */
    public StringOption clone() {
        StringOption result = new StringOption(getId());
        result.setValues(this);
        result.setChoices(this.choices);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String value) {
        final String oldValue = this.value;
        this.value = value;

        if (isDefined && value != oldValue) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue((valueString != null) ? valueString : defaultValueString);
    }


    // Serialization

    private static final String CHOICE_TAG = "choice";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute(VALUE_TAG, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (String choice : choices) {
            out.writeStartElement(CHOICE_TAG);
            
            out.writeAttribute(VALUE_TAG, choice);
                
            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        choices.clear(); // Clear containers

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (CHOICE_TAG.equals(tag)) {
            choices.add(getAttribute(in, VALUE_TAG, (String)null));
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
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" value=").append(value)
            .append(" choices=[");
        if (choices != null) {
            for (String choice : choices) sb.append(" ").append(choice);
        }
        sb.append("]]");
        return sb.toString();
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
