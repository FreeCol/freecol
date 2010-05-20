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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;


/**
* Used for grouping objects of {@link Option}s.
*/
public class OptionGroup extends AbstractOption {

    private static Logger logger = Logger.getLogger(OptionGroup.class.getName());

    private ArrayList<Option> options;


    /**
     * Creates a new <code>OptionGroup</code>.
     */
    public OptionGroup() {
        this(NO_ID);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     */
    public OptionGroup(String id) {
        super(id);
        options = new ArrayList<Option>();
    }
    
    /**
     * Creates a new  <code>OptionGroup</code>.
     * @param in The <code>XMLStreamReader</code> containing the data. 
     */
     public OptionGroup(XMLStreamReader in) throws XMLStreamException {
         this(NO_ID);
         readFromXML(in);
     }

    /**
    * Adds the given <code>Option</code>.
    * @param option The <code>Option</code> that should be
    *               added to this <code>OptionGroup</code>.
    */
    public void add(Option option) {
        options.add(option);
    }


    /**
    * Removes all of the <code>Option</code>s from this <code>OptionGroup</code>.
    */
    public void removeAll() {
        options.clear();
    }


    /**
    * Returns an <code>Iterator</code> for the <code>Option</code>s.
    * @return The <code>Iterator</code>.
    */
    public Iterator<Option> iterator() {
        return options.iterator();
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
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        Iterator<Option> oi = options.iterator();
        while (oi.hasNext()) {
            (oi.next()).toXML(out);
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        final String id = in.getAttributeValue(null, "id");
        if(id != null){
            setId(id);
        }
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            AbstractOption option = null;
            String optionType = in.getLocalName();
            if (IntegerOption.getXMLElementTagName().equals(optionType) || "integer-option".equals(optionType)) {
                option = new IntegerOption(in);
            } else if (BooleanOption.getXMLElementTagName().equals(optionType) || "boolean-option".equals(optionType)) {
                option = new BooleanOption(in);
            } else if (RangeOption.getXMLElementTagName().equals(optionType) || "range-option".equals(optionType)) {
                option = new RangeOption(in);
            } else if (SelectOption.getXMLElementTagName().equals(optionType) || "select-option".equals(optionType)) {
                option = new SelectOption(in);
            } else if (LanguageOption.getXMLElementTagName().equals(optionType) || "language-option".equals(optionType)) {
                option = new LanguageOption(in);
            } else if (FileOption.getXMLElementTagName().equals(optionType) || "file-option".equals(optionType)) {
                option = new FileOption(in);
            } else if (PercentageOption.getXMLElementTagName().equals(optionType)) {
                option = new PercentageOption(in);
            } else if (AudioMixerOption.getXMLElementTagName().equals(optionType)) {
                option = new AudioMixerOption(in);
            } else {
                logger.finest("Parsing of " + optionType + " is not implemented yet");
                in.nextTag();
            }

            if (option != null) {
                add(option);
                option.setGroup(this.getId());
            }
        }
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "optionGroup".
    */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }
    
    /**
     * Returns the name of this <code>Option</code>.
     * 
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.message(getId() + ".name");
    }
    
    /**
     * Gives a short description of this <code>Option</code>. Can for
     * instance be used as a tooltip text.
     * 
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.message(getId() + ".shortDescription");
    }

}
