/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

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

    /** Which player gets credit for the event, if any. */
    private String playerId;

    /** Points for this event, if any. */
    private int score;


    /**
     * Create a new history event of given turn and type.
     *
     * @param turn The <code>Turn</code> of the event.
     * @param eventType The <code>EventType</code>.
     * @param player An optional <code>Player</code> responsible for
     *     this event.
     */
    public HistoryEvent(Turn turn, EventType eventType, Player player) {
        super("model.history." + eventType.toString(), TemplateType.TEMPLATE);
        this.turn = turn;
        this.eventType = eventType;
        this.playerId = (player == null) ? null : player.getId();
        this.score = 0;
    }

    /**
     * Create a new history event by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HistoryEvent(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
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
     * Get the id for the player that is credited with this event, if any.
     *
     * @return The credited <code>Player</code> id.
     */
    public final String getPlayerId() {
        return playerId;
    }

    /**
     * Set the id for the player to credit for this event.
     *
     * @param playerId The new credited <code>Player</code> id.
     */
    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    /**
     * Get the score for this event.
     *
     * @return The score.
     */
    public final int getScore() {
        return score;
    }

    /**
     * Set the score for this event.
     *
     * @param score The new score for this event.
     */
    public void setScore(int score) {
        this.score = score;
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
    private static final String PLAYER_ID_TAG = "playerId";
    private static final String SCORE_TAG = "score";
    private static final String TURN_TAG = "turn";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TURN_TAG, turn.getNumber());

        xw.writeAttribute(EVENT_TYPE_TAG, eventType);

        if (playerId != null) xw.writeAttribute(PLAYER_ID_TAG, playerId);

        xw.writeAttribute(SCORE_TAG, score);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        turn = new Turn(xr.getAttribute(TURN_TAG, 0));

        eventType = xr.getAttribute(EVENT_TYPE_TAG,
                                    EventType.class, (EventType)null);

        playerId = xr.getAttribute(PLAYER_ID_TAG, (String)null);

        score = xr.getAttribute(SCORE_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" ").append(eventType.toString())
            .append(" (").append(turn.getYear()).append(")");
        if (playerId != null) {
            sb.append(" playerId=").append(playerId)
                .append(" score=").append(score);
        }
        sb.append(" ").append(super.toString()).append("]");
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
