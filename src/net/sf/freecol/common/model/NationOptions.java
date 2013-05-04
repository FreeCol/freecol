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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;


public class NationOptions extends FreeColObject {

    /** Type of national advantages for European players. */
    public static enum Advantages {
        NONE,
        FIXED,
        SELECTABLE;

        public String getKey() {
            return "playerOptions." + this.toString();
        }
    };

    /**
     * Nations may be available to all players, to AI players only, or
     * to no players.
     */
    public static enum NationState { AVAILABLE, AI_ONLY, NOT_AVAILABLE }

    /** The default number of European nations. */
    public static final int DEFAULT_NO_OF_EUROPEANS = 4;

    /** The specification to refer to. */
    private Specification specification;

    /** The type of European national advantages. */
    private Advantages nationalAdvantages;

    /** All nations in the game. */
    private final Map<Nation, NationState> nations
        = new HashMap<Nation, NationState>();


    /**
     * Creates a new <code>NationOptions</code> instance.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public NationOptions(Specification specification) {
        this.specification = specification;
        this.nationalAdvantages = FreeCol.getAdvantages();
        if (specification != null) {
            int counter = 0;
            for (Nation nation : specification.getNations()) {
                if (nation.getType().isREF()) {
                    continue;
                } else if (nation.getType().isEuropean()
                    && nation.isSelectable()) {
                    if (counter < DEFAULT_NO_OF_EUROPEANS) {
                        nations.put(nation, NationState.AVAILABLE);
                        counter++;
                    } else {
                        nations.put(nation, NationState.NOT_AVAILABLE);
                    }
                } else {
                    nations.put(nation, NationState.AI_ONLY);
                }
            }
        }
    }

    /**
     * Creates a new <code>NationOptions</code> instance by reading a stream.
     *
     * @param in The <code>XMLStreamReader</code> to read from.
     * @param specification The <code>Specification</code> to refer to.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NationOptions(XMLStreamReader in,
                         Specification specification) throws XMLStreamException {
        this(specification);
        
        readFromXML(in);
    }


    /**
     * Get the nations in the game.
     *
     * @return A map of the nations.
     */
    public final Map<Nation, NationState> getNations() {
        return nations;
    }

    /**
     * Get the national advantages.
     *
     * @return The national advantages.
     */
    public final Advantages getNationalAdvantages() {
        return nationalAdvantages;
    }

    /**
     * Get the <code>NationState</code> value of a particular Nation.
     *
     * @param nation The <code>Nation</code> to query.
     * @return The corresponding <code>NationState</code>.
     */
    public final NationState getNationState(Nation nation) {
        return nations.get(nation);
    }

    /**
     * Set the <code>NationState</code> value of a particular Nation.
     *
     * @param nation The <code>Nation</code> to set the state for.
     * @param state The <code>NationState</code> to set.
     */
    public final void setNationState(final Nation nation,
                                     final NationState state) {
        this.nations.put(nation, state);
    }


    // Serialization
    // Note: NATION/S_TAG is capitalized to avoid collision with Nation.java.

    private static final String NATIONAL_ADVANTAGES_TAG = "nationalAdvantages";
    private static final String NATION_TAG = "Nation";
    private static final String NATIONS_TAG = "Nations";
    private static final String STATE_TAG = "state";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        // The nation options do not use the FreeColObject attributes, so
        // no: super.writeAttributes(out);

        writeAttribute(out, NATIONAL_ADVANTAGES_TAG, nationalAdvantages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        out.writeStartElement(NATIONS_TAG);

        List<Nation> sorted = getSortedCopy(nations.keySet());
        for (Nation nation : sorted) {
            out.writeStartElement(NATION_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, nation);

            writeAttribute(out, STATE_TAG, nations.get(nation));
            
            out.writeEndElement();
        }

        out.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        // The nation options do not use the FreeColObject attributes, so
        // no: super.readAttributes(in);

        nationalAdvantages = getAttribute(in, NATIONAL_ADVANTAGES_TAG,
                                          Advantages.class,
                                          Advantages.SELECTABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        nations.clear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String tag = in.getLocalName();

        if (NATIONS_TAG.equals(tag)) {
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                tag = in.getLocalName();
                if (NATION_TAG.equals(tag)) {

                    Nation nation = specification.getType(in, ID_ATTRIBUTE_TAG,
                        Nation.class, (Nation)null);
                    if (nation == null) {
                        logger.warning("Invalid nation id: " + readId(in));
                    }

                    NationState state = getAttribute(in, STATE_TAG,
                        NationState.class, (NationState)null);
                    if (state == null) {
                        logger.warning("Invalid state tag: "
                            + in.getAttributeValue(null, STATE_TAG));
                    }

                    if (nation != null && state != null) {
                        nations.put(nation, state);
                    }
                    closeTag(in, NATION_TAG);

                } else {
                    logger.warning("Invalid " + NATION_TAG + " tag: " + tag);
                }
            }

        } else {
            super.readChild(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(NATIONAL_ADVANTAGES_TAG).append(": ")
            .append(nationalAdvantages.toString()).append("\n")
            .append(NATIONS_TAG).append(":\n");
        for (Map.Entry<Nation, NationState> entry : nations.entrySet()) {
            sb.append("   ").append(entry.getKey().getId())
                .append(" ").append(entry.getValue().toString())
                .append("\n");
        }
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "nationOptions".
     */
    public static String getXMLElementTagName() {
        return "nationOptions";
    }
}
