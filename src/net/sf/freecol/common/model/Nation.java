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

import java.awt.Color;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColSpecObjectType {

    public static final String TAG = "nation";

    /** The unknown enemy id. */
    public static final String UNKNOWN_NATION_ID = "model.nation.unknownEnemy";

    /** The last resort unknown nation color. */
    public static final Color UNKNOWN_NATION_COLOR = Color.BLACK;

    /**
     * A list of European nation names, where model.nation.X.name exists.
     * Used by getNonPlayerNation().
     */
    private static final List<String> EUROPEAN_NATIONS = makeUnmodifiableList(
        // Original Col1 nations
        "dutch", "english", "french", "spanish",
        // FreeCol's additions
        "danish", "portuguese", "swedish", "russian",
        // other European non-player nations
        "austrian", "german", "prussian", "turkish");


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
     * @param specification The {@code Specification} to refer to.
     */
    public Nation(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this the unknown enemy nation?
     *
     * @return True if this is the unknown enemy.
     */
    public final boolean isUnknownEnemy() {
        return UNKNOWN_NATION_ID.equals(getId());
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
     * Set the nation type.  Needed to when "no advantages" is selected.
     *
     * @param type The new {@code NationType}.
     */
    public final void setType(NationType type) {
        this.type = type;
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
     * @return The REF {@code Nation}, or null if not applicable.
     */
    public final Nation getREFNation() {
        return refNation;
    }

    /**
     * Get the rebel nation to oppose this REF nation.
     *
     * @return The rebel {@code Nation}, or null if not applicable.
     */
    public final Nation getRebelNation() {
        return find(getSpecification().getEuropeanNations(),
                    matchKey(this, Nation::getREFNation));
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
    public final boolean getStartsOnEastCoast() {
        return startsOnEastCoast;
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
     * Get a message key for the ruler of this nation.
     *
     * @return The name key for the ruler.
     */
    public final String getRulerNameKey() {
        return Messages.rulerKey(getId());
    }

    /**
     * Get the name of the nation monarch.
     *
     * @return The ruler name.
     */
    public String getRulerName() {
        return Messages.message(getRulerNameKey());
    }

    /**
     * Get a random player name key that is not in use by an active player.
     *
     * @param game The current {@code Game}.
     * @param random A pseudo-random number source.
     * @return A player name key, or an empty string on failure.
     */
    public static String getRandomNonPlayerNationNameKey(Game game,
                                                         Random random) {
        int nations = EUROPEAN_NATIONS.size();
        int start = randomInt(logger, "Random nation", random, nations);
        for (int index = 0; index < nations; index++) {
            String nationId = "model.nation."
                + EUROPEAN_NATIONS.get((start + index) % nations);
            if (game.getPlayerByNationId(nationId) == null) {
                return Messages.nameKey(nationId);
            }
        }
        // this should never happen
        return "";
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Nation o = copyInCast(other, Nation.class);
        if (o == null || !super.copyIn(o)) return false;
        this.type = o.getType();
        this.selectable = o.isSelectable();
        this.refNation = o.getREFNation();
        this.preferredLatitude = o.getPreferredLatitude();
        this.startsOnEastCoast = o.getStartsOnEastCoast();
        this.color = o.getColor();
        return true;
    }

    
    // Serialization

    private static final String COLOR_TAG = "color";
    private static final String NATION_TYPE_TAG = "nation-type";
    private static final String PREFERRED_LATITUDE_TAG = "preferred-latitude";
    private static final String REF_TAG = "ref";
    private static final String SELECTABLE_TAG = "selectable";
    private static final String STARTS_ON_EAST_COAST_TAG = "starts-on-east-coast";
    // @compat 0.11.3
    private static final String OLD_PREFERRED_LATITUDE_TAG = "preferredLatitude";
    private static final String OLD_STARTS_ON_EAST_COAST_TAG = "startsOnEastCoast";
    // end @compat 0.11.3


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

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_PREFERRED_LATITUDE_TAG)) {
            preferredLatitude = xr.getAttribute(OLD_PREFERRED_LATITUDE_TAG, 0);
        } else
        // end @compat 0.11.3
            preferredLatitude = xr.getAttribute(PREFERRED_LATITUDE_TAG, 0);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_STARTS_ON_EAST_COAST_TAG)) {
            startsOnEastCoast = xr.getAttribute(OLD_STARTS_ON_EAST_COAST_TAG, true);
        } else
        // end @compat 0.11.3
            startsOnEastCoast = xr.getAttribute(STARTS_ON_EAST_COAST_TAG, true);

        refNation = xr.getType(spec, REF_TAG, Nation.class, (Nation)null);

        int rgb = xr.getAttribute(COLOR_TAG, UNDEFINED);
        if (rgb != UNDEFINED) setColor(new Color(rgb));
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
