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

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.model.Specification;


/**
 * Represents an option where the valid choice is a list of
 * FreeColModFiles.
 */
public class ModListOption extends ListOption<FreeColModFile> {

    /**
     * Creates a new <code>ModListOption</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public ModListOption(Specification specification) {
        super(specification);

        setAllowDuplicates(false);
    }

    /**
     * Creates a new <code>ModListOption</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public ModListOption(String id, Specification specification) {
        super(id, specification);

        setAllowDuplicates(false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public ModListOption clone() {
        ModListOption ret = new ModListOption(getId(), getSpecification());
        ret.setValues(this);
        return ret;
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modListOption".
     */
    public static String getXMLElementTagName() {
        return "modListOption";
    }
}
