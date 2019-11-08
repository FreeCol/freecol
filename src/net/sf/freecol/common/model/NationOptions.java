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

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * The options specific to a nation.
 */
public class NationOptions extends FreeColSpecObject {

    private static final Logger logger = Logger.getLogger(NationOptions.class.getName());

    public static final String TAG = "nationOptions";

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

    /** The type of European national advantages. */
    private Advantages nationalAdvantages;

    /** All nations in the game. */
    private final Map<Nation, NationState> nations = new HashMap<>();


    /**
     * Creates a new {@code NationOptions} instance.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public NationOptions(Specification specification) {
        super(specification);
        
        this.nationalAdvantages = FreeCol.getAdvantages();
        if (specification != null) {
            int counter = 0, maxEuropeans = FreeCol.getEuropeanCount();
            for (Nation nation : transform(specification.getNations(),
                    n -> !n.isUnknownEnemy() && !n.getType().isREF())) {
                if (nation.getType().isEuropean() && nation.isSelectable()) {
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
     * Creates a new {@code NationOptions} instance by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public NationOptions(FreeColXMLReader xr,
                         Specification specification) throws XMLStreamException {
        this(specification);
        
        readFromXML(xr);
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
     * Get the nations in the game.
     *
     * @return A map of the nations.
     */
    public final Map<Nation, NationState> getNations() {
        return this.nations;
    }

    /**
     * Set the nation map.
     *
     * @param nations The new nations map.
     */
    protected void setNations(Map<Nation, NationState> nations) {
        this.nations.clear();
        this.nations.putAll(nations);
    }

    /**
     * Get the {@code NationState} value of a particular Nation.
     *
     * @param nation The {@code Nation} to query.
     * @return The corresponding {@code NationState}.
     */
    public final NationState getNationState(Nation nation) {
        return this.nations.get(nation);
    }

    /**
     * Set the {@code NationState} value of a particular Nation.
     *
     * @param nation The {@code Nation} to set the state for.
     * @param state The {@code NationState} to set.
     */
    public final void setNationState(final Nation nation,
                                     final NationState state) {
        this.nations.put(nation, state);
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        NationOptions o = copyInCast(other, NationOptions.class);
        if (o == null || !super.copyIn(o)) return false;
        this.nationalAdvantages = o.getNationalAdvantages();
        this.setNations(o.getNations());
        return true;
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

        for (Nation nation : sort(nations.keySet())) {
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
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        String tag = xr.getLocalName();

        if (NATION_OPTION_TAG.equals(tag)) {
            Nation nation = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                       Nation.class, (Nation)null);
            NationState state = xr.getAttribute(STATE_TAG, NationState.class,
                                                (NationState)null);
            if (nation != null && state != null) {
                nations.put(nation, state);
            }
            xr.closeTag(NATION_OPTION_TAG);

        // @compat 0.11.3
        } else if (OLD_NATIONS_TAG.equals(tag)) {
            while (xr.moreTags()) {
                tag = xr.getLocalName();
                if (OLD_NATION_TAG.equals(tag)) {
                    Nation nation = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                               Nation.class, (Nation)null);
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append(NATIONAL_ADVANTAGES_TAG).append(": ")
            .append(nationalAdvantages).append('\n');
        forEachMapEntry(nations,
            e -> sb.append(' ').append(e.getKey().getId())
                   .append(' ').append(e.getValue()).append('\n'));
        return sb.toString();
    }
}
