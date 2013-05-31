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

    /** The value of this option. */
    private FreeColModFile value = null;


    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param id The object identifier.
     */
    public ModOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public ModOption(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public ModOption(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the <code>Choices</code> value.
     *
     * @return a <code>List<FreeColModFile></code> value
     */
    public final List<FreeColModFile> getChoices() {
        return new ArrayList<FreeColModFile>(Mods.getAllMods());
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    public ModOption clone() {
        ModOption result = new ModOption(getId());
        result.setValues(this);
        return result;
    }

    /**
     * Gets the current value of this <code>ModOption</code>.
     *
     * @return The value.
     */
    public FreeColModFile getValue() {
        return value;
    }

    /**
     * Sets the current value of this option.
     *
     * @param value The new value.
     */
    public void setValue(FreeColModFile value) {
        final FreeColModFile oldValue = this.value;
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
        String id = (valueString != null) ? valueString : defaultValueString;
        setValue(Mods.getModFile(id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other) {
        if (other instanceof ModOption) {
            ModOption mod = (ModOption)other;
            return (value == null) ? mod.getValue() == null
                : (mod.getValue() == null) ? false
                : value.getId().equals(mod.getValue().getId());
        }
        return false;
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (value != null) {
            out.writeAttribute(VALUE_TAG, value.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" value=").append(value.getId())
            .append("]");
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modOption".
     */
    public static String getXMLElementTagName() {
        return "modOption";
    }
}
