/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.util.Utils;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * A notable event in the history of a game.
 */
public class HistoryEvent extends StringTemplate {

    public static final String TAG = "historyEvent";

    public static enum HistoryEventType implements Named {
        DISCOVER_NEW_WORLD,
        DISCOVER_REGION,
        MEET_NATION,
        CITY_OF_GOLD,
        FOUND_COLONY,
        ABANDON_COLONY,
        CONQUER_COLONY,       // You conquer a colony
        COLONY_DESTROYED,
        COLONY_CONQUERED,     // Your colony is conquered
        DESTROY_SETTLEMENT,
        DESTROY_NATION,       // Native nation that is
        FOUNDING_FATHER,
        DECLARE_INDEPENDENCE,
        INDEPENDENCE,
        SPANISH_SUCCESSION,
        DECLARE_WAR,
        CEASE_FIRE,
        MAKE_PEACE,
        FORM_ALLIANCE,
        // FIXME: This is badly named, it should be NATION_WITHDRAWN
        // as it is used when a European nation permanently leaves the
        // New World
        NATION_DESTROYED;

        /**
         * Get the stem key.
         *
         * @return The stem key for this history event type.
         */
        private String getKey() {
            return "historyEventType." + getEnumKey(this);
        }

        public String getDescriptionKey() {
            return Messages.descriptionKey("model." + getKey());
        }
        
        // Implement Named

        /**
         * {@inheritDoc}
         */
        public String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    }


    /** The turn in which the event took place */
    private Turn turn;

    /** The type of event. */
    private HistoryEventType eventType;

    /** Which player gets credit for the event, if any. */
    private String playerId;

    /** Points for this event, if any. */
    private int score;


    /**
     * Trivial constructor to allow creation with Game.newInstance.
     */
    public HistoryEvent() {}

    /**
     * Create a new history event of given turn and type.
     *
     * @param turn The {@code Turn} of the event.
     * @param eventType The {@code EventType}.
     * @param player An optional {@code Player} responsible for
     *     this event.
     */
    public HistoryEvent(Turn turn, HistoryEventType eventType, Player player) {
        super(eventType.getDescriptionKey(), null, TemplateType.TEMPLATE);
        this.turn = turn;
        this.eventType = eventType;
        this.playerId = (player == null) ? null : player.getId();
        this.score = 0;
    }

    /**
     * Create a new history event by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HistoryEvent(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
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
    public final HistoryEventType getEventType() {
        return eventType;
    }

    /**
     * Given a new stance, get the appropriate event type.
     *
     * @param stance The new {@code Stance}.
     * @return The corresponding event type.
     */
    public static final HistoryEventType getEventTypeFromStance(Stance stance) {
        switch (stance) {
        case WAR:
            return HistoryEventType.DECLARE_WAR;
        case CEASE_FIRE:
            return HistoryEventType.CEASE_FIRE;
        case PEACE:
            return HistoryEventType.MAKE_PEACE;
        case ALLIANCE:
            return HistoryEventType.FORM_ALLIANCE;
        default:
            break;
        }
        return null;
    }

    /**
     * Get the id for the player that is credited with this event, if any.
     *
     * @return The credited {@code Player} id.
     */
    public final String getPlayerId() {
        return playerId;
    }

    /**
     * Set the id for the player to credit for this event.
     *
     * @param playerId The new credited {@code Player} id.
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


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        HistoryEvent o = copyInCast(other, HistoryEvent.class);
        if (o == null || !super.copyIn(o)) return false;
        this.turn = o.getTurn();
        this.eventType = o.getEventType();
        this.playerId = o.getPlayerId();
        this.score = o.getScore();
        return true;
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

        xw.writeAttribute(TURN_TAG, this.turn.getNumber());

        xw.writeAttribute(EVENT_TYPE_TAG, this.eventType);

        if (playerId != null) xw.writeAttribute(PLAYER_ID_TAG, this.playerId);

        xw.writeAttribute(SCORE_TAG, this.score);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        turn = new Turn(xr.getAttribute(TURN_TAG, 0));

        eventType = xr.getAttribute(EVENT_TYPE_TAG,
                                    HistoryEventType.class, (HistoryEventType)null);
        playerId = xr.getAttribute(PLAYER_ID_TAG, (String)null);

        score = xr.getAttribute(SCORE_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof HistoryEvent) {
            HistoryEvent other = (HistoryEvent)o;
            return turn == other.turn && eventType == other.eventType
                && score == other.score
                && Utils.equals(playerId, other.playerId)
                && super.equals(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + this.turn.hashCode();
        hash = 31 * hash + this.eventType.ordinal();
        if (this.playerId != null) hash = 31 * hash + this.playerId.hashCode();
        hash = 31 * hash + this.score;
        return hash;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append('[').append(getId())
            .append(' ').append(eventType)
            .append(" (").append(turn.getYear()).append(')');
        if (playerId != null) {
            sb.append(" playerId=").append(playerId)
                .append(" score=").append(score);
        }
        sb.append(' ').append(super.toString()).append(']');
        return sb.toString();
    }
}
