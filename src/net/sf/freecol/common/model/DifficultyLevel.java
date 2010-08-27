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

import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.StringOption;

/**
 * Represents a difficulty level.
 */
// TODO: couldn't we just use an OptionGroup?
public class DifficultyLevel extends FreeColGameObjectType {

    private final Map<String, AbstractOption> levelOptions =
        new LinkedHashMap<String, AbstractOption>();


    public DifficultyLevel(String id, Specification specification) {
        super(id, specification);
    }

    public AbstractOption getOption(String Id) throws IllegalArgumentException {
        if (Id == null) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID 'null'.");
        } else if (!levelOptions.containsKey(Id)) {
            throw new IllegalArgumentException("Trying to retrieve AbstractOption" + " with ID '" + Id
                    + "' returned 'null'.");
        } else {
            return levelOptions.get(Id);
        }
    }

    public Map<String, AbstractOption> getOptions() {
        return levelOptions;
    }
    
    public void readFromXML(XMLStreamReader in) throws XMLStreamException {

        final String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        
        if (id == null){
            throw new XMLStreamException("invalid <" + getXMLElementTagName() +
                                         "> tag : no id attribute found.");
        }

        setId(id);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String optionType = in.getLocalName();
            if (IntegerOption.getXMLElementTagName().equals(optionType) ||
                "integer-option".equals(optionType)) {
                IntegerOption option = new IntegerOption(in);
                addOption(option);
            } else if (BooleanOption.getXMLElementTagName().equals(optionType) ||
                       "boolean-option".equals(optionType)) {
                BooleanOption option = new BooleanOption(in);
                addOption(option);
            } else if (StringOption.getXMLElementTagName().equals(optionType) ||
                       "string-option".equals(optionType)) {
                StringOption option = new StringOption(in);
                addOption(option);
            } else {
                logger.finest("Parsing of " + optionType + " is not implemented yet");
                in.nextTag();
            }
        }

    }

    private void addOption(AbstractOption option) {
        levelOptions.put(option.getId(), option);
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out);
    }

    public void toXML(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());

        for (AbstractOption option : levelOptions.values()) {
            option.toXML(out);
        }

        out.writeEndElement();

    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "difficultyLevel";
    }
    

}
