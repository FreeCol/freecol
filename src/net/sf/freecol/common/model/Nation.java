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
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.io.FreeColXMLReader;


/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {

    public static String UNKNOWN_NATION_ID = "model.nation.unknownEnemy";

    public static final String[] EUROPEAN_NATIONS = new String[] {
        // the original game's nations
        "english", "french", "spanish", "dutch",
        // FreeCol's additions
        "portuguese", "danish", "swedish", "russian",
        // other Europeans, used to generate Monarch messages
        "german", "austrian", "prussian", "turkish"
    };

    /** The nation type, European, native, etc. */
    private NationType type;

    /** Can this nation be selected? */
    private boolean selectable;

    /** The REF nation to oppose this nation. */
    private Nation refNation;

    /** The preferred starting latitude for this nation. */
    private int preferredLatitude = 0;

    /** Whether this nation starts on the East coast by default. */
    private boolean startsOnEastCoast = true;


    /**
     * Create a new nation.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public Nation(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the nation type.
     *
     * @return The nation type.
     */
    public final NationType getType() {
        return type;
    }

    /**
     * Is this nation selectable?
     *
     * @return True if the nation is selectable.
     */
    public final boolean isSelectable() {
        return selectable;
    }

    /**
     * Get the REF nation to oppose this nation.
     *
     * @return The REF <code>Nation</code>, or null if not applicable.
     */
    public final Nation getREFNation() {
        return refNation;
    }

    /**
     * Get the preferred latitude of this nation.
     *
     * @return The preferred latitude.
     */
    public final int getPreferredLatitude() {
        return preferredLatitude;
    }

    /**
     * Does this nation start on the east coast by default?
     *
     * @return True if the nation starts on the east coast.
     */
    public final boolean startsOnEastCoast() {
        return startsOnEastCoast;
    }


    /**
     * Get a message key for the ruler of this nation.
     *
     * @return a <code>String</code> value
     */
    public final String getRulerNameKey() {
        return getId() + ".ruler";
    }


    // Serialization

    private static final String NATION_TYPE_TAG = "nation-type";
    private static final String PREFERRED_LATITUDE_TAG = "preferredLatitude";
    private static final String REF_TAG = "ref";
    private static final String SELECTABLE_TAG = "selectable";
    private static final String STARTS_ON_EAST_COAST_TAG = "startsOnEastCoast";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NATION_TYPE_TAG, type);

        writeAttribute(out, SELECTABLE_TAG, selectable);

        writeAttribute(out, PREFERRED_LATITUDE_TAG, preferredLatitude);

        writeAttribute(out, STARTS_ON_EAST_COAST_TAG, startsOnEastCoast);

        if (refNation != null) writeAttribute(out, REF_TAG, refNation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(xr);

        type = xr.getType(spec, NATION_TYPE_TAG,
                          NationType.class, (NationType)null);

        selectable = xr.getAttribute(SELECTABLE_TAG, false);

        preferredLatitude = xr.getAttribute(PREFERRED_LATITUDE_TAG, 0);

        startsOnEastCoast = xr.getAttribute(STARTS_ON_EAST_COAST_TAG, true);

        refNation = xr.getType(spec, REF_TAG, Nation.class, (Nation)null);
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "nation".
     */
    public static String getXMLElementTagName() {
        return "nation";
    }
}
