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

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Specification.TypeSelector;


/**
 * Represents an option that can be an arbitrary string.
 */
public class StringOption extends AbstractOption<String> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(StringOption.class.getName());

    public static final String NONE = "none";

    /**
     * The option value.
     */
    private String value;

    /**
     * Whether to add "none" to the list of choices to be generated.
     */
    private boolean addNone;

    /**
     * Which choices to generate.
     */
    private TypeSelector generateChoices;

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
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public StringOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXMLImpl(in);
    }

    public StringOption clone() {
        StringOption result = new StringOption(getId());
        result.value = value;
        result.addNone = addNone;
        result.generateChoices = generateChoices;
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
     * Get the <code>AddNone</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean addNone() {
        return addNone;
    }

    /**
     * Set the <code>AddNone</code> value.
     *
     * @param newAddNone The new AddNone value.
     */
    public final void setAddNone(final boolean newAddNone) {
        this.addNone = newAddNone;
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
     * Get the <code>GenerateChoices</code> value.
     *
     * @return a <code>Generate</code> value
     */
    public final TypeSelector getGenerateChoices() {
        return generateChoices;
    }

    /**
     * Set the <code>GenerateChoices</code> value.
     *
     * @param newGenerateChoices The new GenerateChoices value.
     */
    public final void setGenerateChoices(final TypeSelector newGenerateChoices) {
        this.generateChoices = newGenerateChoices;
    }

    /**
     * Generate the choices to provide to the UI based on the
     * generateChoices value.
     *
     * @param specification the Specification that defines the game
     * objects whose IDs will be generated
     */
    public void generateChoices(Specification specification) {
        if (generateChoices == null) {
            if (choices == null || choices.isEmpty()) {
                choices = new ArrayList<String>();
                choices.add(getValue());
            }
        } else {
            List<FreeColGameObjectType> objects =
                specification.getTypes(generateChoices);
            choices = new ArrayList<String>(objects.size() + (addNone ? 1 : 0));
            if (addNone) {
                choices.add(StringOption.NONE);
            }
            for (FreeColObject object : objects) {
                choices.add(object.getId());
            }
        }
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
        if (generateChoices != null) {
            out.writeAttribute("generate", generateChoices.toString());
        }
        if (addNone) {
            out.writeAttribute("addNone", Boolean.toString(addNone));
        }
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
        addNone = getAttribute(in, "addNone", false);
        String generate = in.getAttributeValue(null, "generate");
        if (generate != null) {
            generateChoices = TypeSelector.valueOf(generate);
        }
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
