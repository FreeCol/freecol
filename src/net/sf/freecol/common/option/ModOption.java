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
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option that can be an arbitrary string.
 */
public class ModOption extends AbstractOption<FreeColModFile> {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(ModOption.class.getName());

    /**
     * The option value.
     */
    private FreeColModFile value;

    /**
     * A list of choices to provide to the UI.
     */
    private List<FreeColModFile> choices =
        new ArrayList<FreeColModFile>(Mods.getAllMods());

    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public ModOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public ModOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *     should be found in an {@link OptionGroup}.
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public ModOption(String id, Specification specification) {
        super(id, specification);
    }

    public ModOption clone() {
        ModOption result = new ModOption(getId());
        result.setValues(this);
        result.choices = new ArrayList<FreeColModFile>(choices);
        return result;
    }

    /**
     * Gets the current value of this <code>ModOption</code>.
     * @return The value.
     */
    public FreeColModFile getValue() {
        return value;
    }


    /**
     * Sets the current value of this <code>ModOption</code>.
     * @param value The value.
     */
    public void setValue(FreeColModFile value) {
        final FreeColModFile oldValue = this.value;
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
        String id = ((valueString != null) ? valueString : defaultValueString);
        setValue(Mods.getModFile(id));
    }

    /**
     * Get the <code>Choices</code> value.
     *
     * @return a <code>List<FreeColModFile></code> value
     */
    public final List<FreeColModFile> getChoices() {
        return choices;
    }

    /**
     * Set the <code>Choices</code> value.
     *
     * @param newChoices The new Choices value.
     */
    public final void setChoices(final List<FreeColModFile> newChoices) {
        this.choices = newChoices;
    }

    /**
     * Returns whether <code>null</code> is an acceptable value for
     * this Option. This method always returns <code>true</code>.
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
        if (value != null) {
            out.writeAttribute(VALUE_TAG, value.getId());
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modOption".
     */
    public static String getXMLElementTagName() {
        return "modOption";
    }

    public String toString() {
        String result = "";
        if (choices != null) {
            for (FreeColModFile choice : choices) {
                result += ", " + choice.getId();
            }
            if (result.length() > 0) {
                result = result.substring(2);
            }
        }
        return getXMLElementTagName() + " [value=" + value
            + ", choices=[" + result + "]]";
    }
}
