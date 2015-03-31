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

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.Utils;


/**
 * Represents an option that can be an arbitrary string.
 */
public class ModOption extends AbstractOption<FreeColModFile> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ModOption.class.getName());

    /** The value of this option. */
    private FreeColModFile value = null;


    /**
     * Creates a new <code>ModOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public ModOption(Specification specification) {
        super(specification);
    }


    /**
     * Get the choices available for this option.
     *
     * @return A list of <code>FreeColModFile</code>s.
     */
    public final List<FreeColModFile> getChoices() {
        return new ArrayList<>(Mods.getAllMods());
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public ModOption clone() {
        ModOption result = new ModOption(getSpecification());
        result.setId(this.getId());
        result.value = this.value;
        return result;
    }

    /**
     * Gets the current value of this <code>ModOption</code>.
     *
     * @return The value.
     */
    @Override
    public FreeColModFile getValue() {
        return value;
    }

    /**
     * Sets the current value of this option.
     *
     * @param value The new value.
     */
    @Override
    public void setValue(FreeColModFile value) {
        final FreeColModFile oldValue = this.value;
        this.value = value;
        setId(value.getId());
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
    protected void setValue(String valueString, String defaultValueString) throws XMLStreamException {
        String id = (valueString != null) ? valueString : defaultValueString;
        FreeColModFile fcmf = Mods.getModFile(id);
        if (fcmf == null) {
            throw new XMLStreamException("Could not find mod for: " + id);
        }
        setValue(fcmf);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ModOption) {
            ModOption mod = (ModOption)o;
            return this.value == mod.value
                && super.equals(o);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return 31 * hash + Utils.hashCode(this.value);
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId()).append("]");
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
     * @return "modOption".
     */
    public static String getXMLElementTagName() {
        return "modOption";
    }
}
