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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class NationOptions extends FreeColObject{

    /**
     * The default number of European nations.
     */
    public static final int DEFAULT_NO_OF_EUROPEANS = 4;

    /**
     * National advantages for European players only. The natives will
     * always have national advantages.
     */
    public static enum Advantages { NONE, FIXED, SELECTABLE };

    /**
     * Nations may be available to all players, to AI players only, or
     * to no players.
     */
    public static enum NationState { AVAILABLE, AI_ONLY, NOT_AVAILABLE }

    /**
     * Describe nationalAdvantages here.
     */
    private Advantages nationalAdvantages;

    /**
     * All nations in the game.
     */
    private Map<Nation, NationState> nations = new HashMap<Nation, NationState>();

    /**
     * Get the <code>Nations</code> value.
     *
     * @return a <code>Map<Nation, NationState></code> value
     */
    public final Map<Nation, NationState> getNations() {
        return nations;
    }

    /**
     * Set the <code>Nations</code> value.
     *
     * @param newNations The new Nations value.
     */
    public final void setNations(final Map<Nation, NationState> newNations) {
        this.nations = newNations;
    }

    /**
     * Get the <code>NationalAdvantages</code> value.
     *
     * @return an <code>Advantages</code> value
     */
    public final Advantages getNationalAdvantages() {
        return nationalAdvantages;
    }

    /**
     * Set the <code>NationalAdvantages</code> value.
     *
     * @param newNationalAdvantages The new NationalAdvantages value.
     */
    public final void setNationalAdvantages(final Advantages newNationalAdvantages) {
        this.nationalAdvantages = newNationalAdvantages;
    }

    /**
     * Get the <code>NationState</code> value of a particular Nation.
     *
     * @param nation a <code>Nation</code> value
     * @return a <code>NationState</code> value
     */
    public final NationState getNationState(Nation nation) {
        return nations.get(nation);
    }

    /**
     * Set the <code>NationState</code> value of a particular Nation.
     *
     * @param nation a <code>Nation</code> value
     * @param state a <code>NationState</code> value
     */
    public final void setNationState(final Nation nation, final NationState state) {
        this.nations.put(nation, state);
    }

    /**
     * Describe <code>getDefaults</code> method here.
     *
     * @return a <code>NationOptions</code> value
     */
    public static final NationOptions getDefaults() {
        NationOptions result = new NationOptions();
        result.setNationalAdvantages(Advantages.SELECTABLE);
        int counter = 0;
        Map<Nation, NationState> defaultNations = new HashMap<Nation, NationState>();
        for (Nation nation : Specification.getSpecification().getNations()) {
            if (nation.getType().isREF()) {
                continue;
            } else if (nation.getType().isEuropean() && nation.isSelectable()) {
                if (counter < DEFAULT_NO_OF_EUROPEANS) {
                    defaultNations.put(nation, NationState.AVAILABLE);
                    counter++;
                } else {
                    defaultNations.put(nation, NationState.NOT_AVAILABLE);
                }
            } else {
                defaultNations.put(nation, NationState.AI_ONLY);
            }
        }
        result.setNations(defaultNations);
        return result;
    }


    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public final void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        //setId(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));

        String advantages = getAttribute(in, "nationalAdvantages", "selectable").toUpperCase(Locale.US);
        nationalAdvantages = Enum.valueOf(Advantages.class, advantages);

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("Nations")) {
                nations.clear();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals("Nation")) {
                        String nationId = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
                        Nation nation = Specification.getSpecification().getNation(nationId);
                        NationState state = Enum.valueOf(NationState.class,
                                                         in.getAttributeValue(null, "state"));
                        nations.put(nation, state);
                    }
                    in.nextTag();
                }
            }
        }
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        //out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("nationalAdvantages", nationalAdvantages.toString());
        out.writeStartElement("Nations");
        for (Map.Entry<Nation, NationState> entry : nations.entrySet()) {
            out.writeStartElement("Nation");
            out.writeAttribute(ID_ATTRIBUTE_TAG, entry.getKey().getId());
            out.writeAttribute("state", entry.getValue().toString());
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeEndElement();
    }

    public static String getXMLElementTagName() {
        return "nationOptions";
    }

    // debugging only
    public String toString() {
        StringBuilder result = new StringBuilder(); 
        result.append("nationalAdvantages: " + nationalAdvantages.toString() + "\n");
        result.append("Nations:\n");
        for (Map.Entry<Nation, NationState> entry : nations.entrySet()) {
            result.append("   " + entry.getKey().getId() + " " + entry.getValue().toString() + "\n");
        }
        return result.toString();
    }
}