/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
    private Turn turn;

    /**
     * The type of event.
     */
    private EventType eventType;

    public HistoryEvent() {
        // empty constructor
    }

    public HistoryEvent(Turn turn, EventType eventType) {
        super("model.history." + eventType.toString(), TemplateType.TEMPLATE);
        this.turn = turn;
        this.eventType = eventType;
    }

    /**
     * Get the <code>int</code> value.
     *
     * @return a <code>int</code> value
     */
    public final Turn getTurn() {
        return turn;
    }

    /**
     * Set the <code>int</code> value.
     *
     * @param newInt The new int value.
     */
    public final void setTurn(final Turn newInt) {
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


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("turn", Integer.toString(turn.getNumber()));
        out.writeAttribute("eventType", eventType.toString());
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        turn = new Turn(Integer.parseInt(in.getAttributeValue(null, "turn")));
        String eventString = in.getAttributeValue(null, "eventType");
        // @compat 0.9.x
        if (eventString == null) {
            eventString = in.getAttributeValue(null, "type");
        }
        if ("".equals(getId())) {
            setId("model.history." + eventString);
        }
        // end compatibility code
        super.readChildren(in);
        eventType = Enum.valueOf(EventType.class, eventString);
    }

    /**
     * Builds a string representation of this object.
     */
    @Override
    public String toString() {
        return eventType.toString() + " (" + turn.getYear() + ") ["
            + super.toString() + "]";
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
