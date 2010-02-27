/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.client.gui.action;

import java.util.ArrayList;
import java.util.List;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.TileImprovementType;

/**
 * A storage class for ImprovementActionType used to create ImprovementActions.
 * Filled by Specification.java, utilized by ActionManager.java
 */
public final class ImprovementActionType extends FreeColGameObjectType
{



    private char accelerator;
    
    private final List<String> names;
    private final List<TileImprovementType> impTypes;
    private final List<Integer> imageIDs;
    
    // ------------------------------------------------------------ constructors
    
    public ImprovementActionType() {
        names = new ArrayList<String>();
        impTypes = new ArrayList<TileImprovementType>();
        imageIDs = new ArrayList<Integer>();
    }

    // ------------------------------------------------------------ retrieval methods

    public char getAccelerator() {
        return accelerator;
    }

    public List<String> getNames() {
        return names;
    }

    public List<TileImprovementType> getImpTypes() {
        return impTypes;
    }
    
    public List<Integer> getImageIDs() {
        return imageIDs;
    }

    // ------------------------------------------------------------ API methods

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
           throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        accelerator = in.getAttributeValue(null, "accelerator").charAt(0);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            names.add(in.getAttributeValue(null, "name"));
            String t = in.getAttributeValue(null, "tileimprovement-type");
            impTypes.add(specification.getTileImprovementType(t));
            imageIDs.add(Integer.parseInt(in.getAttributeValue(null, "image-id")));
            in.nextTag(); // close this element
        }
    }   
}
