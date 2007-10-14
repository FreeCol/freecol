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


package net.sf.freecol.common.model;


import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public abstract class FreeColObject extends PersistentObject {


    protected static Logger logger = Logger.getLogger(FreeColObject.class.getName());


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param document The <code>Document</code>.
     * @return An XML-representation of this object.
     */    
    public Element toXMLElement(Document document) {
        return toXMLElement(null, document, false, false);
    }


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation 
     *      should be made for, or <code>null</code> if
     *      <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> 
     *      will be added to the representation if <code>showAll</code>
     *      is set to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that
     *      is only needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXML(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame) throws XMLStreamException {
        toXMLImpl(out);
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    abstract protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException;

}

