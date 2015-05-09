/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The options specific to a nation.
 */
public class NationOptions extends FreeColObject {

    private static final Logger logger = Logger.getLogger(NationOptions.class.getName());

    /** Type of national advantages for European players. */
    public static enum Advantages implements Named {
        NONE,
        FIXED,
        SELECTABLE;

        /**
         * Get a message key for this Advantages.
         *
         * @return A message key.
         */
        private String getKey() {
            return "advantages." + getEnumKey(this);
        }

        public final String getShortDescriptionKey() {
            return Messages.shortDescriptionKey("model." + getKey());
        }

        // Implement Named

        /**
         * {@inheritDoc}
         */
        @Override
        public final String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    };

    /**
     * Nations may be available to all players, to AI players only, or
     * to no players.
     */
    public static enum NationState implements Named {
        AVAILABLE,
        AI_ONLY,
        NOT_AVAILABLE;

        /**
         * Get a message key for a nation state.
         *
         * @return A message key.
         */
        private String getKey() {
            return "nationState." + getEnumKey(this);
        }

        public final String getShortDescriptionKey() {
            return Messages.shortDescriptionKey("model." + getKey());
        }

        // Implement Named

        /**
         * {@inheritDoc}
         */
        @Override
        public final String getNameKey() {
            return Messages.nameKey("model." + getKey());
        }
    }

    /** The specification to refer to. */
    private final Specification specification;

    /** The type of European national advantages. */
    private Advantages nationalAdvantages;

    /** All nations in the game. */
    private final Map<Nation, NationState> nations = new HashMap<>();


    /**
     * Creates a new <code>NationOptions</code> instance.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public NationOptions(Specification specification) {
        this.specification = specification;
        this.nationalAdvantages = FreeCol.getAdvantages();
        if (specification != null) {
            int counter = 0, maxEuropeans = FreeCol.getEuropeanCount();
            for (Nation nation : specification.getNations()) {
                if (nation.isUnknownEnemy() || nation.getType().isREF()) {
                    continue;
                } else if (nation.getType().isEuropean()
                    && nation.isSelectable()) {
                    if (counter < maxEuropeans) {
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
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param specification The <code>Specification</code> to refer to.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NationOptions(FreeColXMLReader xr,
                         Specification specification) throws XMLStreamException {
        this(specification);
        
        readFromXML(xr);
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
    private static final String NATION_OPTION_TAG = "nationOption";
    private static final String STATE_TAG = "state";
    // @compat 0.11.3
    private static final String OLD_NATION_TAG = "Nation";
    private static final String OLD_NATIONS_TAG = "Nations";
    // end @compat 0.11.3


    /**
     * {@inheritDoc}
     */
    @Override
    public void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // The nation options do not use the FreeColObject attributes, so
        // no: super.writeAttributes(out);

        xw.writeAttribute(NATIONAL_ADVANTAGES_TAG, nationalAdvantages);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Nation nation : getSortedCopy(nations.keySet())) {
            xw.writeStartElement(NATION_OPTION_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, nation);

            xw.writeAttribute(STATE_TAG, nations.get(nation));
            
            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // The nation options do not use the FreeColObject attributes, so
        // no: super.readAttributes(in);

        nationalAdvantages = xr.getAttribute(NATIONAL_ADVANTAGES_TAG,
            Advantages.class, Advantages.SELECTABLE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        nations.clear();

        super.readChildren(xr);
    }

    /**
     * Defend against an invalid nation tag.  This can happen when
     * using classic mode (no extra Europeans) but loading a map that
     * contains the extra Europeans.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @return A suitable <code>Nation</code> or null on error.
     */
    private Nation readNation(FreeColXMLReader xr) {
        try {
            return xr.getType(specification, ID_ATTRIBUTE_TAG,
                              Nation.class, (Nation)null);
        } catch (IllegalArgumentException iae) {
            logger.warning("Bad nation tag: " + xr.readId());
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        String tag = xr.getLocalName();

        if (NATION_OPTION_TAG.equals(tag)) {
            Nation nation = readNation(xr);
            NationState state = xr.getAttribute(STATE_TAG,
                NationState.class, (NationState)null);
            if (nation != null && state != null) {
                nations.put(nation, state);
            }
            xr.closeTag(NATION_OPTION_TAG);

        // @compat 0.11.3
        } else if (OLD_NATIONS_TAG.equals(tag)) {
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                tag = xr.getLocalName();
                if (OLD_NATION_TAG.equals(tag)) {
                    Nation nation = readNation(xr);
                    NationState state = xr.getAttribute(STATE_TAG,
                        NationState.class, (NationState)null);
                    if (nation != null && state != null) {
                        nations.put(nation, state);
                    }
                    xr.closeTag(OLD_NATION_TAG);

                } else {
                    throw new XMLStreamException("Bogus " + OLD_NATION_TAG
                        + " tag: " + tag);
                }
            }
        // end @compat 0.11.3

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(NATIONAL_ADVANTAGES_TAG).append(": ")
            .append(nationalAdvantages).append("\n");
        for (Map.Entry<Nation, NationState> entry : nations.entrySet()) {
            sb.append(" ").append(entry.getKey().getId())
                .append(" ").append(entry.getValue())
                .append("\n");
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "nationOptions".
     */
    public static String getXMLElementTagName() {
        return "nationOptions";
    }
}
