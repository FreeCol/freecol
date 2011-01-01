/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;



public class Event extends FreeColGameObjectType {

    /**
     * A restriction on the scope of the event.
     */
    private String value;

    /**
     * The score value of this event.
     */
    private int scoreValue = 0;

    /**
     * Describe limits here.
     */
    private Map<String, Limit> limits;



    public Event(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final String newValue) {
        this.value = newValue;
    }

    /**
     * Get the <code>Limits</code> value.
     *
     * @return a <code>List<Limit></code> value
     */
    public final Collection<Limit> getLimits() {
        return limits.values();
    }

    /**
     * Return the <code>Limit</code> with the given id.
     *
     * @param id a <code>String</code> value
     * @return a <code>Limit</code> value
     */
    public final Limit getLimit(String id) {
        return limits.get(id);
    }

    /**
     * Get the <code>ScoreValue</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getScoreValue() {
        return scoreValue;
    }

    /**
     * Set the <code>ScoreValue</code> value.
     *
     * @param newScoreValue The new ScoreValue value.
     */
    public final void setScoreValue(final int newScoreValue) {
        this.scoreValue = newScoreValue;
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        if (value != null) {
            out.writeAttribute(VALUE_TAG, value);
        }
        if (scoreValue != 0) {
            out.writeAttribute("scoreValue", Integer.toString(scoreValue));
        }
    }


    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        if (limits != null) {
            for (Limit limit : limits.values()) {
                limit.toXMLImpl(out);
            }
        }
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        value = in.getAttributeValue(null, VALUE_TAG);
        scoreValue = getAttribute(in, "scoreValue", 0);
    }

    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        if (Limit.getXMLElementTagName().equals(in.getLocalName())) {
            if (limits == null) {
                limits = new HashMap<String, Limit>();
            }
            Limit limit = new Limit(getSpecification());
            limit.readFromXML(in);
            limits.put(limit.getId(), limit);
        } else {
            super.readChild(in);
        }
    }


    public static String getXMLElementTagName() {
        return "event";
    }


}