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
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be a string selected from a list of
 * possible values (choices).
 */
public class StringOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(StringOption.class.getName());

    /** The value of this option. */
    private String value;

    /** A list of choices to provide to the UI. */
    private final List<String> choices = new ArrayList<>();


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
    @Override
    public StringOption clone() {
        StringOption result = new StringOption(getId(), getSpecification());
        result.setValues(this);
        result.setChoices(this.choices);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(String value) {
        final String oldValue = this.value;
        this.value = value;

        if (isDefined && !Utils.equals(value, oldValue)) {
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
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (String choice : choices) {
            xw.writeStartElement(CHOICE_TAG);

            xw.writeAttribute(VALUE_TAG, choice);

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        choices.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (CHOICE_TAG.equals(tag)) {
            choices.add(xr.getAttribute(VALUE_TAG, (String)null));
            xr.closeTag(CHOICE_TAG);

        } else {
            super.readChild(xr);
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
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "stringOption".
     */
    public static String getXMLElementTagName() {
        return "stringOption";
    }
}
