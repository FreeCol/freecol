/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.util.Utils;


/**
 * Represents an option that can be an arbitrary string.
 */
public class ModOption extends AbstractOption<FreeColModFile> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ModOption.class.getName());

    public static final String TAG = "modOption";

    /** The value of this option. */
    private FreeColModFile value = null;


    /**
     * Creates a new {@code ModOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public ModOption(Specification specification) {
        super(specification);
    }


    /**
     * Get the choices available for this option.
     *
     * @return A list of {@code FreeColModFile}s.
     */
    public final List<FreeColModFile> getChoices() {
        return new ArrayList<>(FreeColModFile.getModsList());
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public ModOption cloneOption() {
        ModOption result = new ModOption(getSpecification());
        result.setId(this.getId());
        result.value = this.value;
        return result;
    }

    /**
     * Gets the current value of this {@code ModOption}.
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
        FreeColModFile fcmf = FreeColModFile.getFreeColModFile(id);
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ModOption) {
            ModOption other = (ModOption)o;
            return this.value == other.value
                && super.equals(other);
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append('[').append(getId()).append(']');
        return sb.toString();
    }
}
