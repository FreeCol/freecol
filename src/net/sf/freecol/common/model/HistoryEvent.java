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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class HistoryEvent extends FreeColObject {

    public static enum Type {
        DISCOVER_NEW_WORLD,
        DISCOVER_REGION,
        MEET_NATION,
        CITY_OF_GOLD,
        FOUND_COLONY,
        ABANDON_COLONY,
        CONQUER_COLONY,
        COLONY_DESTROYED,
        COLONY_CONQUERED,
        DESTROY_SETTLEMENT,
        // TODO: when exactly is a European nation destroyed?
        DESTROY_NATION,
        NATION_DESTROYED,
        FOUNDING_FATHER,
        DECLARE_INDEPENDENCE,
        INDEPENDENCE,
        SPANISH_SUCCESSION
    }
            

    /**
     * The turn in which the event took place
     */
    private int turn;

    /**
     * The type of event.
     */
    private Type type;

    /**
     * The details of the event.
     */
    private String[] strings = new String[0];

    public HistoryEvent() {
        // empty constructor
        setId("");
    }

    public HistoryEvent(int turn, Type type, String... strings) {
        setId("");
        this.turn = turn;
        this.type = type;
        this.strings = strings;
    }        

    /**
     * Get the <code>int</code> value.
     *
     * @return a <code>int</code> value
     */
    public final int getTurn() {
        return turn;
    }

    /**
     * Set the <code>int</code> value.
     *
     * @param newint The new int value.
     */
    public final void setTurn(final int newInt) {
        this.turn = newInt;
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>Type</code> value
     */
    public final Type getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final Type newType) {
        this.type = newType;
    }

    /**
     * Get the <code>Strings</code> value.
     *
     * @return a <code>String[]</code> value
     */
    public final String[] getStrings() {
        return strings;
    }

    /**
     * Set the <code>Strings</code> value.
     *
     * @param newStrings The new Strings value.
     */
    public final void setStrings(final String[] newStrings) {
        this.strings = newStrings;
    }

    public String toString() {
        String result = type.toString() + " [";
        for (String string : strings) {
            result += " " + string;
        }
        return result + "]";
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
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("turn", Integer.toString(turn));
        out.writeAttribute("type", type.toString());

        toArrayElement("strings", strings, out);

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        turn = Integer.parseInt(in.getAttributeValue(null, "turn"));
        type = Enum.valueOf(Type.class, in.getAttributeValue(null, "type"));

        strings = readFromArrayElement("strings", in, new String[0]);

        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "historyEvent";
    }

}