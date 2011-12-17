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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.model.Specification;

/**
 * Represents an option where the valid choice is a list of
 * FreeColModFiles.
 *
 */
public class ModListOption extends ListOption<FreeColModFile> {

    /**
     * Creates a new <code>ModListOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public ModListOption(String id) {
        super(id);
    }

    /**
     * Creates a new <code>ModListOption</code>.
     *
     * @param specification The specification this option belongs
     *     to. May be null.
     */
    public ModListOption(Specification specification) {
        super(specification);
    }

    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXMLImpl(out, getXMLElementTagName());
    }

   /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "modListOption".
     */
    public static String getXMLElementTagName() {
        return "modListOption";
    }
}
