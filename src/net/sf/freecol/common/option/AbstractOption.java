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

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Specification;


/**
 * The super class of all options. GUI components making use of this
 * class can refer to its name and shortDescription properties. The
 * complete keys of these properties consist of the id of the option
 * group (if any), followed by a "."  unless the option group is null,
 * followed by the id of the option object, followed by a ".",
 * followed by "name" or "shortDescription".
 */
abstract public class AbstractOption<T> extends FreeColObject
    implements Option<T> {

    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    private String optionGroup = "";

    // Determine if the option has been defined
    // When defined an option won't change when a default value is read from an
    // XML file.
    protected boolean isDefined = false;


    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public AbstractOption(String id) {
        this(id, null);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param specification The specification this Option refers
     *     to. This may be null, since only some options need access
     *     to the specification.
     */
    public AbstractOption(Specification specification) {
        this(null, specification);
    }

    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The identifier for this option. This is used when the
     *     object should be found in an {@link OptionGroup}.
     * @param specification The specification this Option refers
     *     to. This may be null, since only some options need access
     *     to the specification.
     */
    public AbstractOption(String id, Specification specification) {
        setId(id);
        setSpecification(specification);
    }

    /**
     * Returns the string prefix that identifies the group of this
     * <code>Option</code>.
     *
     * @return The string prefix provided by the OptionGroup.
     */
    public String getGroup() {
        return optionGroup;
    }

    /**
     * Set the option group
     *
     * @param group <code>OptionGroup</code> to set
     *
     */
    public void setGroup(String group) {
        if (group == null) {
            optionGroup = "";
        } else {
            optionGroup = group;
        }
    }

    /**
     * Returns the value of this Option.
     *
     * @return the value of this Option
     */
    public abstract T getValue();

    /**
     * Sets the value of this Option.
     *
     * @param value the value of this Option
     */
    public abstract void setValue(T value);

    /**
     * Sets the value of this Option from the given string
     * representation. Both parameters must not be null at the same
     * time. This method does nothing. Override it if the Option has a
     * suitable string representation.
     *
     * @param valueString the string representation of the value of
     * this Option
     * @param defaultValueString the string representation of the
     * default value of this Option
     */
    protected void setValue(String valueString, String defaultValueString) {
        logger.warning("Unsupported method: setValue.");
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        readAttributes(in);
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        final String defaultValue = in.getAttributeValue(null, "defaultValue");
        final String value = in.getAttributeValue(null, VALUE_TAG);

        if (id == null && getId().equals(NO_ID)){
            throw new XMLStreamException("invalid " + in.getLocalName()
                                         + ": no id attribute found.");
        }
        if (defaultValue == null && value == null) {
            throw new XMLStreamException("invalid option " + id
                                         + ": no value nor default value found.");
        }

        if (getId() == null || getId() == NO_ID) {
            setId(id);
        }
        setValue(value, defaultValue);
    }


}
