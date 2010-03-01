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


public class HistoryEvent extends StringTemplate {

    public static enum EventType {
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
    private EventType eventType;

    public HistoryEvent() {
        // empty constructor
    }

    public HistoryEvent(int turn, EventType eventType) {
        super("model.history." + eventType.toString(), TemplateType.TEMPLATE);
        this.turn = turn;
        this.eventType = eventType;
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
     * @param newInt The new int value.
     */
    public final void setTurn(final int newInt) {
        this.turn = newInt;
    }

    /**
     * Get the <code>EventType</code> value.
     *
     * @return a <code>EventType</code> value
     */
    public final EventType getEventType() {
        return eventType;
    }

    /**
     * Set the <code>EventType</code> value.
     *
     * @param newEventType The new EventType value.
     */
    public final void setEventType(final EventType newEventType) {
        this.eventType = newEventType;
    }

    /**
     * Add a new key and replacement to the HistoryEvent. This is
     * only possible if the HistoryEvent is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>HistoryEvent</code> value
     */
    public HistoryEvent add(String key, String value) {
        super.add(key, value);
        return this;
    }

    /**
     * Add a new key and replacement to the HistoryEvent. The
     * replacement must be a proper name. This is only possible if the
     * HistoryEvent is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>HistoryEvent</code> value
     */
    public HistoryEvent addName(String key, String value) {
        super.addName(key, value);
        return this;
    }

    /**
     * Add a key and an integer value to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param amount an <code>int</code> value
     * @return a <code>HistoryEvent</code> value
     */
    public HistoryEvent addAmount(String key, int amount) {
        super.addAmount(key, amount);
        return this;
    }

    /**
     * Add a key and a StringTemplate to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param template a <code>StringTemplate</code> value
     * @return a <code>HistoryEvent</code> value
     */
    public HistoryEvent addStringTemplate(String key, StringTemplate template) {
        super.addStringTemplate(key, template);
	return this;
    }


    public String toString() {
        return eventType.toString() + " (" + Turn.getYear(turn) + ") ["
            + super.toString() + "]";
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
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("turn", Integer.toString(turn));
        out.writeAttribute("eventType", eventType.toString());
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        turn = Integer.parseInt(in.getAttributeValue(null, "turn"));
        String eventString = in.getAttributeValue(null, "eventType");
        // TODO: remove compatibility code
        if (eventString == null) {
            eventString = in.getAttributeValue(null, "type");
        }
        if ("".equals(getId())) {
            setId("model.history." + eventString);
        }
        // end compatibility code
        eventType = Enum.valueOf(EventType.class, eventString);
        super.readChildren(in);
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