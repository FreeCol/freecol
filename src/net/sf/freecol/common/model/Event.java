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
 *  MERCHANTLIMIT or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;


/**
 * A special game event.
 */
public class Event extends FreeColSpecObjectType {

    public static final String TAG = "event";

    /** A restriction on the scope of the event. */
    private String value;

    /** The score value of this event. */
    private int scoreValue = 0;

    /** Limits on this event. */
    private Map<String, Limit> limits = null;


    /**
     * Create a new event.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public Event(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Create a new event.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
     * @exception XMLStreamException if there a problem reading the stream.
     */
    public Event(FreeColXMLReader xr, Specification specification) throws XMLStreamException {
        super(specification);

        readFromXML(xr);
    }


    /**
     * Gets the event restriction.
     *
     * @return The restriction.
     */
    public final String getValue() {
        return value;
    }

    /**
     * Sets the event restriction.
     *
     * @param newValue The new event restriction.
     */
    public final void setValue(final String newValue) {
        this.value = newValue;
    }

    /**
     * Get the score value of this event.
     *
     * @return The score value.
     */
    public final int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the score value of this event.
     *
     * @param newScoreValue The new score value.
     */
    public final void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }

    /**
     * Get the limit map.
     *
     * @return The map of the limits.
     */
    protected Map<String, Limit> getLimits() {
        return this.limits;
    }

    /**
     * Set the limits on this event.
     *
     * @param limits A new limits map.
     */
    protected void setLimits(Map<String, Limit> limits) {
        if (this.limits == null) this.limits = new HashMap<>(); else {
            this.limits.clear();
        }
        this.limits.putAll(limits);
    }

    /**
     * Get the limits on this event.
     *
     * @return A list of limits.
     */
    public final Collection<Limit> getLimitValues() {
        return (limits == null) ? Collections.<Limit>emptyList()
            : limits.values();
    }

    /**
     * Gets a particular limit by identifier.
     *
     * @param id The object identifier.
     * @return The corresponding {@code Limit} or null if not found.
     */
    public final Limit getLimit(String id) {
        return (limits == null) ? null : limits.get(id);
    }

    /**
     * Add a limit.
     *
     * @param limit The {@code Limit} to add.
     */
    private void addLimit(Limit limit) {
        if (limits == null) limits = new HashMap<>();
        limits.put(limit.getId(), limit);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Event o = copyInCast(other, Event.class);
        if (o == null || !super.copyIn(o)) return false;
        this.value = o.getValue();
        this.scoreValue = o.getScoreValue();
        this.setLimits(o.getLimits());
        return true;
    }


    // Serialization

    private static final String SCORE_VALUE_TAG = "score-value";
    // @compat 0.11.3
    private static final String OLD_SCORE_VALUE_TAG = "scoreValue";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value);
        }

        if (scoreValue != 0) {
            xw.writeAttribute(SCORE_VALUE_TAG, scoreValue);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Limit limit : getLimitValues()) limit.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        value = xr.getAttribute(VALUE_TAG, (String)null);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_SCORE_VALUE_TAG)) {
            scoreValue = xr.getAttribute(OLD_SCORE_VALUE_TAG, 0);
        } else
        // end @compat 0.11.3
            scoreValue = xr.getAttribute(SCORE_VALUE_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            limits = null;
        }
        
        super.readChildren(xr);
    }        

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (Limit.TAG.equals(tag)) {
            addLimit(new Limit(xr, spec));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
