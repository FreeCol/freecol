/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

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
    private List<Limit> limits;

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
    public final List<Limit> getLimits() {
        return limits;
    }

    /**
     * Set the <code>Limits</code> value.
     *
     * @param newLimits The new Limits value.
     */
    public final void setLimits(final List<Limit> newLimits) {
        this.limits = newLimits;
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
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public static String getXMLElementTagName() {
        return "event";
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        if (value != null) {
            out.writeAttribute("value", value);
        }
        if (scoreValue != 0) {
            out.writeAttribute("scoreValue", Integer.toString(scoreValue));
        }
    }


    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        writeFeatures(out);
        if (limits != null) {
            for (Limit limit : limits) {
                limit.toXMLImpl(out);
            }
        }
    }

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
        value = in.getAttributeValue(null, "value");
        scoreValue = getAttribute(in, "scoreValue", 0);
    }

    @Override
    public FreeColObject readChild(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        if (Limit.getXMLElementTagName().equals(in.getLocalName())) {
            if (limits == null) {
                limits = new ArrayList<Limit>();
            }
            Limit limit = new Limit();
            limit.readFromXML(in, specification);
            limits.add(limit);
            return limit;
        } else {
            return super.readChild(in, specification);
        }
    }



}