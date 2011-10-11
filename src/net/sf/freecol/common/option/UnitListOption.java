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

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Represents an option where the valid choice is a list of
 * AbstractUnits.
 *
 * TODO: can we derive this from ListOption?
 */
public class UnitListOption extends AbstractOption<List<AbstractUnitOption>> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(UnitListOption.class.getName());

    private List<AbstractUnitOption> value = new ArrayList<AbstractUnitOption>();

    /**
     * The maximum number of list entries. Defaults to Integer.MAX_VALUE.
     */
    private int maximumNumber = Integer.MAX_VALUE;

    /**
     * Creates a new <code>UnitListOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public UnitListOption(String id) {
        super(id);
    }

    /**
     * Creates a new  <code>UnitListOption</code>.
     * @param in The <code>XMLStreamReader</code> containing the data.
     * @exception XMLStreamException if an error occurs
     */
    public UnitListOption(XMLStreamReader in) throws XMLStreamException {
        super(NO_ID);
        readFromXML(in);
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
     * Gets the current value of this <code>UnitListOption</code>.
     * @return The value.
     */
    public List<AbstractUnitOption> getValue() {
        return value;
    }

    /**
     * Sets the value of this <code>UnitListOption</code>.
     * @param value The value to be set.
     */
    public void setValue(List<AbstractUnitOption> value) {
        final List<AbstractUnitOption> oldValue = this.value;
        this.value = value;

        if (!value.equals(oldValue) && isDefined) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
        isDefined = true;
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
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("maximumNumber", Integer.toString(maximumNumber));
        for (AbstractUnitOption option : value) {
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
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);

        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid <" + getXMLElementTagName()
                                         + "> tag : no id attribute found.");
        }

        if (getId() == NO_ID) {
            setId(id);
        }

        maximumNumber = getAttribute(in, "maximumNumber", 1);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (AbstractUnitOption.getXMLElementTagName().equals(in.getLocalName())) {
                value.add(new AbstractUnitOption(in));
                in.nextTag();
            }
        }

    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "unitListOption".
     */
    public static String getXMLElementTagName() {
        return "unitListOption";
    }
}
