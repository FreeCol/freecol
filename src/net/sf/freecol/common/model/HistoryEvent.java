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

import org.w3c.dom.Element;


/**
 * A notable event in the history of a game.
 */
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


    /** The turn in which the event took place */
    private Turn turn;

    /** The type of event. */
    private EventType eventType;


    /**
     * Create a new history event of given turn and type.
     *
     * @param turn The <code>Turn</code> of the event.
     * @param eventType The <code>EventType</code>.
     */
    public HistoryEvent(Turn turn, EventType eventType) {
        super("model.history." + eventType.toString(), TemplateType.TEMPLATE);
        this.turn = turn;
        this.eventType = eventType;
    }

    /**
     * Create a new history event by reading a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HistoryEvent(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }

    /**
     * Create a new history event by reading a element.
     *
     * @param element The <code>Element</code> to read from.
     */
    public HistoryEvent(Element element) {
        readFromXMLElement(element);
    }


    /**
     * Get the turn of this history event.
     *
     * @return The turn.
     */
    public final Turn getTurn() {
        return turn;
    }

    /**
     * Get the type of this history event.
     *
     * @return The event type.
     */
    public final EventType getEventType() {
        return eventType;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public HistoryEvent add(String key, String value) {
        return (HistoryEvent) super.add(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HistoryEvent addName(String key, String value) {
        return (HistoryEvent) super.addName(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HistoryEvent addAmount(String key, Number amount) {
        return (HistoryEvent) super.addAmount(key, amount);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HistoryEvent addStringTemplate(String key, StringTemplate template) {
        return (HistoryEvent) super.addStringTemplate(key, template);
    }


    // Serialization

    private static final String EVENT_TYPE_TAG = "eventType";
    private static final String TURN_TAG = "turn";
    // @compat 0.9.x
    private static final String TYPE_TAG = "type";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, TURN_TAG, turn.getNumber());

        writeAttribute(out, EVENT_TYPE_TAG, eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        turn = new Turn(getAttribute(in, "turn", 0));

        eventType = getAttribute(in, EVENT_TYPE_TAG,
                                 EventType.class, (EventType)null);
        // @compat 0.9.x
        if (eventType == null) {
            eventType = getAttribute(in, TYPE_TAG,
                                     EventType.class, (EventType)null);
            if ("".equals(getId())) {
                setId("model.history." + eventType.toString());
            }
        }
        // end @compat
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" ").append(eventType.toString())
            .append(" (").append(turn.getYear()).append(")")
            .append(super.toString()).append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "historyEvent".
     */
    public static String getXMLElementTagName() {
        return "historyEvent";
    }
}
