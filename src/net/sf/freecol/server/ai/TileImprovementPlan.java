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

package net.sf.freecol.server.ai;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.model.Constants.*;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;
import net.sf.freecol.common.util.Utils;


/**
 * Represents a plan to improve a {@code Tile} in some way.
 * For instance by plowing or by building a road.
 *
 * @see Tile
 */
public class TileImprovementPlan extends ValuedAIObject {

    private static final Logger logger = Logger.getLogger(TileImprovementPlan.class.getName());

    public static final String TAG = "tileImprovementPlan";

    /** The type of improvement, from TileImprovementTypes. */
    private TileImprovementType type;

    /** The {@code Tile} to be improved. */
    private Tile target;

    /**
     * The pioneer which should make the improvement (if a
     * {@code Unit} has been assigned).
     */
    private AIUnit pioneer = null;


    /**
     * Creates a new uninitialized {@code TileImprovementPlan}.
     *
     * @param aiMain The main AI-object.
     * @param id The object identifier.
     */
    public TileImprovementPlan(AIMain aiMain, String id) {
        super(aiMain, id);

        this.type = null;
        this.target = null;
        this.pioneer = null;
        this.initialized = false;
    }

    /**
     * Creates a new {@code TileImprovementPlan} and initializes its
     * critical fields.
     *
     * @param aiMain The main AI-object.
     * @param target The target {@code Tile} for the improvement.
     * @param type The type of improvement.
     * @param value The value identifying the importance of
     *     this {@code TileImprovementPlan} - a higher value
     *     signals a higher importance.
     */
    public TileImprovementPlan(AIMain aiMain, Tile target,
                               TileImprovementType type, int value) {
        super(aiMain, TAG + ":" + aiMain.getNextId());

        this.target = target;
        this.type = type;
        this.pioneer = null;
        setValue(value);
        setInitialized();
    }

    /**
     * Creates a new {@code TileImprovementPlan} from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param xr The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *     during parsing.
     */
    public TileImprovementPlan(AIMain aiMain,
                               FreeColXMLReader xr) throws XMLStreamException {
        super(aiMain, xr);

        setInitialized();
    }


    /**
     * {@inheritDoc}
     */
    public final void setInitialized() {
        this.initialized = getType() != null && getTarget() != null;
    }
        
    /**
     * Gets the pioneer who have been assigned to making the
     * improvement described by this object.
     *
     * @return The pioneer which should make the improvement, if
     *     such a {@code AIUnit} has been assigned, and
     *     {@code null} if nobody has been assigned this
     *     mission.
     */
    public final AIUnit getPioneer() {
        return pioneer;
    }

    /**
     * Sets the pioneer who have been assigned to making the
     * improvement described by this object.
     *
     * @param pioneer The pioneer which should make the improvement, if
     *     such a {@code Unit} has been assigned, and
     *     {@code null} if nobody has been assigned this
     *     mission.
     */
    public final void setPioneer(AIUnit pioneer) {
        this.pioneer = pioneer;
    }

    /**
     * Gets the {@code TileImprovementType} of this plan.
     *
     * @return The type of the improvement.
     */
    public final TileImprovementType getType() {
        return type;
    }

    /**
     * Sets the type of this {@code TileImprovementPlan}.
     *
     * @param type The {@code TileImprovementType}.
     * @see #getType
     */
    public final void setType(TileImprovementType type) {
        this.type = type;
    }

    /**
     * Gets the target of this {@code TileImprovementPlan}.
     *
     * @return The {@code Tile} where
     *     {@link #getPioneer pioneer} should make the
     *     given {@link #getType improvement}.
     */
    public final Tile getTarget() {
        return target;
    }

    /**
     * Gets the 'most effective' TileImprovementType allowed for a
     * given tile and goods type.  Useful for AI in deciding the
     * improvements to prioritize.
     *
     * @param tile The {@code Tile} that will be improved.
     * @param goodsType The {@code GoodsType} to be prioritized.
     * @return The best {@code TileImprovementType} available to be done.
     */
    public static TileImprovementType getBestTileImprovementType(Tile tile,
        GoodsType goodsType) {
        final Specification spec = tile.getSpecification();
        final Predicate<TileImprovementType> goodTIPred = it ->
            !it.isNatural()
                && it.isTileTypeAllowed(tile.getType())
                // FIXME: For now, disable any exotic non-Col1
                // improvement types that expend more than one parcel
                // of tools (e.g. plantForest), because
                // PioneeringMission assumes this does not happen.
                && it.getExpendedAmount() <= 1
                && tile.getTileImprovement(it) == null;
        final Comparator<TileImprovementType> bestTIComp
            = cachingIntComparator(it ->
                it.getImprovementValue(tile, goodsType));
        TileImprovementType best = maximize(spec.getTileImprovementTypeList(),
                                            goodTIPred, bestTIComp);
        return (best == null || best.getImprovementValue(tile, goodsType) <= 0)
            ? null
            : best;
    }

    /**
     * Updates this tile improvement plan to the best available for its
     * tile and the specified goods type.
     *
     * @param goodsType The {@code GoodsType} to be prioritized.
     * @return True if the plan is still viable.
     */
    public boolean update(GoodsType goodsType) {
        TileImprovementType type = getBestTileImprovementType(target, goodsType);
        if (type == null) return false;
        setType(type);
        setValue(type.getImprovementValue(target, goodsType));
        return true;
    }

    /**
     * Is this improvement complete?
     *
     * @return True if the tile improvement has been completed.
     */
    public boolean isComplete() {
        return target != null && target.hasTileImprovement(getType());
    }

    /**
     * Weeds out a broken or obsolete tile improvement plan.
     *
     * @return True if the plan survives this check.
     */
    public boolean validate() {
        if (type == null) {
            logger.warning("Removing typeless TileImprovementPlan");
            dispose();
            return false;
        }
        if (target == null) {
            logger.warning("Removing targetless TileImprovementPlan");
            dispose();
            return false;
        }
        if (getPioneer() != null
            && (getPioneer().getUnit() == null
                || getPioneer().getUnit().isDisposed())) {
            logger.warning("Clearing broken pioneer for TileImprovementPlan");
            setPioneer(null);
        }
        return true;
    }


    // Override AIObject

    /**
     * Disposes this {@code TileImprovementPlan}.
     *
     * If a pioneer has been assigned to making this improvement, then
     * abort its mission.
     */
    @Override
    public void dispose() {
        this.pioneer = null;
        super.dispose();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrityType checkIntegrity(boolean fix, LogBuilder lb) {
        IntegrityType result = super.checkIntegrity(fix, lb);
        if (pioneer != null) {
            result = result.combine(pioneer.checkIntegrity(fix, lb));
        }
        if (type == null) {
            lb.add("\n  Tile improvement plan without type: ", getId());
            result = result.fail();
        } else if (target == null) {
            lb.add("\n  Tile improvement plan without target: ", getId());
            result = result.fail();
        }
        return result;
    }


    // Serialization

    private static final String PIONEER_TAG = "pioneer";
    private static final String TARGET_TAG = "target";
    private static final String TYPE_TAG = "type";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw) throws XMLStreamException {
        if (validate()) toXML(xw, TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TYPE_TAG, type);

        xw.writeAttribute(TARGET_TAG, target);

        // Only write the pioneer if it is in good condition
        if (pioneer != null
            && pioneer.checkIntegrity(false) == IntegrityType.INTEGRITY_GOOD) {
            xw.writeAttribute(PIONEER_TAG, pioneer);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final AIMain aiMain = getAIMain();
        final Specification spec = getSpecification();
        
        type = xr.getType(spec, TYPE_TAG, 
                          TileImprovementType.class, (TileImprovementType)null);

        pioneer = (xr.hasAttribute(PIONEER_TAG))
            ? xr.makeAIObject(aiMain, PIONEER_TAG,
                              AIUnit.class, (AIUnit)null, true)
            : null;

        target = xr.getAttribute(getGame(), TARGET_TAG,
                                 Tile.class, (Tile)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TileImprovementPlan)) return false;
        TileImprovementPlan other = (TileImprovementPlan)o;
        return Utils.equals(this.type, other.type)
            && Utils.equals(this.target, other.target)
            && Utils.equals(this.pioneer, other.pioneer)
            && super.equals(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 37 * hash + Utils.hashCode(this.type);
        hash = 37 * hash + Utils.hashCode(this.target);
        return 37 * hash + Utils.hashCode(this.pioneer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append('[').append(getId())
            .append(' ').append((type == null) ? "null" : type.getSuffix())
            .append(" at ").append((target == null) ? "null"
                : target.toShortString())
            .append('/').append(getValue())
            .append(']');
        return sb.toString();
    }
}
