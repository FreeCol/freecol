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

import java.awt.Color;

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {

    public static String UNKNOWN_NATION_ID = "model.nation.unknownEnemy";

    public static Color UNKNOWN_NATION_COLOR = Color.BLACK;

    // @compat 0.10.x
    // Colours moved back into the spec at 0.11.  We have to tolerate
    // old specs that lack them while 0.10.x is supported.
    /** A map of default nation colours. */
    private final static Map<String, Color> defaultColors
        = new HashMap<String, Color>();
    static {
        defaultColors.put("model.nation.dutch",         new Color(0xff9d3c));
        defaultColors.put("model.nation.french",        new Color(0x0000ff));
        defaultColors.put("model.nation.english",       new Color(0xff0000));
        defaultColors.put("model.nation.spanish",       new Color(0xffff00));
        defaultColors.put("model.nation.inca",          new Color(0xf4f0c4));
        defaultColors.put("model.nation.aztec",         new Color(0xc4a020));
        defaultColors.put("model.nation.arawak",        new Color(0x6888c0));
        defaultColors.put("model.nation.cherokee",      new Color(0x6c3c18));
        defaultColors.put("model.nation.iroquois",      new Color(0x74a44c));
        defaultColors.put("model.nation.sioux",         new Color(0xc0ac84));
        defaultColors.put("model.nation.apache",        new Color(0x900000));
        defaultColors.put("model.nation.tupi",          new Color(0x045c04));
        defaultColors.put("model.nation.dutchREF",      new Color(0xcc5500));
        defaultColors.put("model.nation.frenchREF",     new Color(0x6050dc));
        defaultColors.put("model.nation.englishREF",    new Color(0xde3163));
        defaultColors.put("model.nation.spanishREF",    new Color(0xffdf00));
        defaultColors.put("model.nation.portuguese",    new Color(0x00ff00));
        defaultColors.put("model.nation.swedish",       new Color(0x00bfff));
        defaultColors.put("model.nation.danish",        new Color(0xff00bf));
        defaultColors.put("model.nation.russian",       new Color(0xffffff));
        defaultColors.put("model.nation.portugueseREF", new Color(0xbfff00));
        defaultColors.put("model.nation.swedishREF",    new Color(0x367588));
        defaultColors.put("model.nation.danishREF",     new Color(0x91006d));
        defaultColors.put("model.nation.russianREF",    new Color(0xbebebe));
    }
    // end @compat 0.10.x

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

    /** The color of this nation. */
    private Color color;


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

    /**
     * Get the nation color.
     *
     * @return The color for this nation.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Set the nation color.
     *
     * @param color The new nation color.
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * Enforce the default color for this nation.
     * Call this if getColor() is returning null, which should only happen
     * if a colorless old specification is in force.
     *
     * @return The default color for this nation.
     */
    public Color forceDefaultColor() {
        Color ret = defaultColors.get(getId());
        setColor(ret);
        return ret;
    }


    // Serialization

    private static final String COLOR_TAG = "color";
    private static final String NATION_TYPE_TAG = "nation-type";
    private static final String PREFERRED_LATITUDE_TAG = "preferredLatitude";
    private static final String REF_TAG = "ref";
    private static final String SELECTABLE_TAG = "selectable";
    private static final String STARTS_ON_EAST_COAST_TAG = "startsOnEastCoast";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NATION_TYPE_TAG, type);

        xw.writeAttribute(SELECTABLE_TAG, selectable);

        xw.writeAttribute(PREFERRED_LATITUDE_TAG, preferredLatitude);

        xw.writeAttribute(STARTS_ON_EAST_COAST_TAG, startsOnEastCoast);

        if (refNation != null) xw.writeAttribute(REF_TAG, refNation);

        if (color != null) xw.writeAttribute(COLOR_TAG, color.getRGB());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        type = xr.getType(spec, NATION_TYPE_TAG,
                          NationType.class, (NationType)null);

        selectable = xr.getAttribute(SELECTABLE_TAG, false);

        preferredLatitude = xr.getAttribute(PREFERRED_LATITUDE_TAG, 0);

        startsOnEastCoast = xr.getAttribute(STARTS_ON_EAST_COAST_TAG, true);

        refNation = xr.getType(spec, REF_TAG, Nation.class, (Nation)null);

        int rgb = xr.getAttribute(COLOR_TAG, UNDEFINED);
        if (rgb != UNDEFINED) setColor(new Color(rgb));
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
