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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.ToIntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.LogBuilder;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The {@code WorkLocation} is a place in a {@link Colony} where
 * {@code Units} can work.  The unit capacity of a WorkLocation
 * is likely to be limited.  ColonyTiles can only hold a single
 * worker, and Buildings can hold no more than three workers, for
 * example.  WorkLocations do not store any Goods.  They take any
 * Goods they consume from the Colony, and put all Goods they produce
 * there, too.  Although the WorkLocation implements {@link Ownable},
 * its owner can not be changed directly, as it is always owned by the
 * owner of the Colony.
 */
public abstract class WorkLocation extends UnitLocation
    implements Ownable {

    private static final Logger logger = Logger.getLogger(WorkLocation.class.getName());

    public static final List<AbstractGoods> EMPTY_LIST
        = Collections.<AbstractGoods>emptyList();

    /** Container class to suggest a better use of a unit. */
    public static class Suggestion {

        public static final Comparator<Suggestion> descendingAmountComparator
            = Comparator.comparingInt(Suggestion::getAmount).reversed()
                .thenComparing(Suggestion::getGoodsType,
                               GoodsType.goodsTypeComparator)
                .thenComparing(Suggestion::getNewUnitType);
                    
        public final WorkLocation workLocation;
        public final UnitType oldType;
        public final UnitType newType;
        public final GoodsType goodsType;
        public final int amount;


        /**
         * Suggest that work done by (optional) {@code oldType}
         * would be better done by {@code newType} because it
         * could produce {@code amount} more
         * {@code goodsType}.
         *
         * @param workLocation The {@code WorkLocation} to add
         *     a unit to.
         * @param oldType The optional {@code UnitType} currently
         *     doing the work.
         * @param newType A new {@code UnitType} to do the work.
         * @param goodsType The {@code GoodsType} to produce.
         * @param amount The extra goods that would be produced if the
         *     suggestion is taken.
         */
        public Suggestion(WorkLocation workLocation, UnitType oldType,
                          UnitType newType, GoodsType goodsType, int amount) {
            this.workLocation = workLocation;
            this.oldType = oldType;
            this.newType = newType;
            this.goodsType = goodsType;
            this.amount = amount;
        }

        public UnitType getNewUnitType() {
            return this.newType;
        }
        
        public GoodsType getGoodsType() {
            return this.goodsType;
        }
        
        public int getAmount() {
            return this.amount;
        }
    };

    /** The colony that contains this work location. */
    protected Colony colony;

    /** The production type of this WorkLocation. */
    private ProductionType productionType;


    /**
     * Constructor for ServerWorkLocation.
     *
     * @param game The enclosing {@code Game}.
     */
    protected WorkLocation(Game game) {
        super(game);
    }

    /**
     * Creates a new {@code WorkLocation} with the given identifier.
     *
     * The object should be initialized later.
     *
     * @param game The enclosing {@code Game}.
     * @param id The object identifier.
     */
    public WorkLocation(Game game, String id) {
        super(game, id);
    }


    public void setColony(Colony colony) {
        this.colony = colony;
    }

    /**
     * Get the production type.
     *
     * @return The {@code ProductionType} for this work location.
     */
    public final ProductionType getProductionType() {
        return this.productionType;
    }

    /**
     * Set the prodution type.
     *
     * @param newProductionType The new {@code ProductionType} value.
     */
    public final void setProductionType(final ProductionType newProductionType) {
        if (!Utils.equals(newProductionType, this.productionType)) {
            this.productionType = newProductionType;
            getColony().invalidateCache();
            logger.fine("Production type at " + this
                + " is now: " + newProductionType);
        }
    }

    /**
     * Get the current work type of any unit present.
     *
     * This assumes that all units in a work location are doing the same
     * work, which is true for now.
     *
     * @return The current {@code GoodsType} being produced, or
     *     null if none.
     */
    public GoodsType getCurrentWorkType() {
        Unit unit = getFirstUnit();
        return (unit != null && unit.getType() != null) ? unit.getWorkType()
            : null;
    }

    /**
     * Update production type on the basis of the current work
     * location type (which might change due to an upgrade) and the
     * work type of units present.
     */
    public void updateProductionType() {
        setProductionType(getBestProductionType(isEmpty(),
                getCurrentWorkType()));
    }

    /**
     * Get the best available production type at this work location.
     *
     * @param unattended Whether to require unattended production.
     * @param workType An optional work type to require.
     * @return The best available {@code ProductionType} given the
     *     argument constraints.
     */
    public ProductionType getBestProductionType(boolean unattended,
                                                GoodsType workType) {
        return ProductionType.getBestProductionType(workType,
            getAvailableProductionTypes(unattended));
    }

    /**
     * Gets the best occupation for a given unit at this work location.
     *
     * @param unit The {@code Unit} to find an
     *     {@code Occupation} for.
     * @param userMode If a user requested this, favour the current
     *     work type, if not favour goods that the unit requires.
     * @return An {@code Occupation} for the given unit, or
     *     null if none found.
     */
    public Occupation getOccupation(Unit unit, boolean userMode) {
        LogBuilder lb = new LogBuilder((getColony().getOccupationTrace()) ? 64 : 0);
        lb.add(getColony().getName(), "/", this, ".getOccupation(", unit, ")");

        Occupation best = new Occupation(null, null, null);
        int bestAmount = 0;
        for (Collection<GoodsType> types
                 : getColony().getWorkTypeChoices(unit, userMode)) {
            lb.add("\n  ");
            logFreeColObjects(types, lb);
            bestAmount = best.improve(unit, this, bestAmount, types, lb);
            if (best.workType != null) {
                lb.add("\n  => ", best);
                break;
            }
        }
        if (best.workType == null) lb.add("\n  FAILED");
        lb.log(logger, Level.WARNING);
        return (best.workType == null) ? null : best;
    }

    /**
     * Gets the best occupation for a given unit type at this work location.
     *
     * @param unitType An optional {@code UnitType} to find an
     *     {@code Occupation} for.  If null, use the default unit type.
     * @return An {@code Occupation} for the given unit, or
     *     null if none found.
     */
    public Occupation getOccupation(UnitType unitType) {
        final Specification spec = getSpecification();
        if (unitType == null) {
            unitType = spec.getDefaultUnitType(getOwner().getNationType());
        }
        
        LogBuilder lb = new LogBuilder((getColony().getOccupationTrace()) ? 64 : 0);
        lb.add(getColony().getName(), "/", this, ".getOccupation(",
               unitType.getSuffix(), ")");

        Collection<GoodsType> types = spec.getGoodsTypeList();
        Occupation best = new Occupation(null, null, null);
        lb.add("\n  ");
        logFreeColObjects(types, lb);
        int bestAmount = best.improve(unitType, this, 0, types, lb);
        if (best.workType != null) {
            lb.add("\n  => ", best, "/", bestAmount);
        } else {
            lb.add("\n  FAILED");
        }
        lb.log(logger, Level.WARNING);
        return (best.workType == null) ? null : best;
    }
        
    /**
     * Get the best work type for a unit at this work location, favouring
     * the existing work.
     *
     * @param unit The {@code Unit} to find a work type for.
     * @return The best work {@code GoodsType} for the unit, or null
     *     if none found.
     */
    public GoodsType getWorkFor(Unit unit) {
        Occupation occupation = getOccupation(unit, true);
        return (occupation == null) ? null : occupation.workType;
    }

    /**
     * Install a unit at the best occupation for it at this work location.
     *
     * @param unit The {@code Unit} to install.
     * @return True if the installation succeeds.
     */
    public boolean setWorkFor(Unit unit) {
        Occupation occupation = getOccupation(unit, false);
        return occupation != null && occupation.install(unit);
    }

    /**
     * Is it a good idea to produce a goods type at this work location
     * using a better unit type?
     *
     * @param unit The {@code Unit} that is doing the job at
     *     present, which may be null if none is at work.
     * @param productionType The {@code ProductionType} to use.
     * @param goodsType The {@code GoodsType} to produce.
     * @return A {@code Suggestion} for a better worker, or null if
     *     improvement is not worthwhile.
     */
    private Suggestion getSuggestion(Unit unit, ProductionType productionType,
                                     GoodsType goodsType) {
        // Check first if there is space.
        if (productionType == null
            || goodsType == null
            || ((unit == null || !contains(unit)) && isFull())) return null;

        final Specification spec = getSpecification();
        final Player owner = getOwner();
        final UnitType expert = spec.getExpertForProducing(goodsType);
        
        // Require there be a better unit to do this work, and that it
        // would actually improve production.
        final UnitType better = (expert != null) ? expert
            : spec.getDefaultUnitType(owner);
        if (unit != null && better == unit.getType()) return null;
        int delta = getPotentialProduction(goodsType, better);
        if (unit != null) {
            delta -= getPotentialProduction(goodsType, unit.getType());
        }
        // Do we have a chance of satisfying the inputs?
        final ToIntFunction<AbstractGoods> prod = ag ->
            getColony().getNetProductionOf(ag.getType());
        delta = Math.min(delta, min(productionType.getInputs(), prod));
        if (delta <= 0) return null;

        // Is the production actually a good idea?  Not if we are independent
        // and have maximized liberty, or for immigration.
        if (owner.getPlayerType() == Player.PlayerType.INDEPENDENT
            && ((goodsType.isLibertyType() && getColony().getSoL() >= 100)
                || goodsType.isImmigrationType())) 
            return null;
        
        final Boolean ok = goodSuggestionCheck(better,unit, goodsType);
        return (!ok) ? null
            : new Suggestion(this, (unit == null) ? null : unit.getType(),
                             better, goodsType, delta);
    }


    /**
     * Determines whether a {@code WorkLocation} is a good suggestion
     *
     * @param unitType The type of {@code Unit} to check.
     * @param unit The Unit itself.
     * @param goodsType The {@code GoodsType} to check.
     *
     * @return True or false, depending on whether the suggestion is valid.
     */
    protected boolean goodSuggestionCheck(UnitType unitType, Unit unit, GoodsType goodsType) {
        return goodSuggestionCheck(unitType, unit, goodsType, this);
    }

    /**
     * Determines whether a {@code WorkLocation} is a good suggestion
     *
     * @param unitType The type of {@code Unit} to check.
     * @param unit The Unit itself.
     * @param goodsType The {@code GoodsType} to check.
     * @param workLocation The {@code WorkLocation} to check. This can
     *     be overloaded with a default parameter of "this" to call
     *     the WorkLocation instance currently in in reference.
     * @return True or false, depending on whether the suggestion is valid.
     */
    protected boolean goodSuggestionCheck(UnitType unitType, Unit unit,
                                          GoodsType goodsType,
                                          WorkLocation workLocation) {
        return false;
    }

    /**
     * Get a map of suggestions for better or additional units.
     *
     * @return A mapping of either existing units or null (denoting
     *     adding a unit) to a {@code Suggestion}.
     */
    public java.util.Map<Unit, Suggestion> getSuggestions() {
        java.util.Map<Unit, Suggestion> result = new HashMap<>();
        if (!canBeWorked() || canTeach()) return result;
        
        Occupation occ = getOccupation(null);
        GoodsType work;
        Suggestion sug;
        // Check if the existing non-student units can be improved.
        for (Unit u : transform(getUnits(), isNull(Unit::getTeacher))) {
            if ((work = u.getWorkType()) == null) {
                if (occ != null) work = occ.workType;
            }
            if ((sug = getSuggestion(u, getProductionType(), work)) != null) {
                result.put(u, sug);
            }
        }
        // Check for a suggestion for an extra worker if there is space.
        if (occ != null && (work = occ.workType) != null
            && !isFull()
            && (sug = getSuggestion(null, occ.productionType, work)) != null) {
            result.put(null, sug);
        }
        return result;
    }
            
    /**
     * Get the {@code AbstractGoods} consumed by this work location.
     *
     * @return A stream of {@code AbstractGoods} consumed.
     */
    public Stream<AbstractGoods> getInputs() {
        return (productionType == null) ? Stream.<AbstractGoods>empty()
            : productionType.getInputs();
    }

    /**
     * Get the {@code AbstractGoods} produced by this work location.
     *
     * @return A stream of {@code AbstractGoods} produced.
     */
    public Stream<AbstractGoods> getOutputs() {
        return (productionType == null) ? Stream.<AbstractGoods>empty()
            : productionType.getOutputs();
    }

    /**
     * Does this work location produce a given type of goods?
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return True if this {@code WorkLocation} produces the
     *     given {@code GoodsType}.
     */
    public boolean produces(GoodsType goodsType) {
        return any(getOutputs(), AbstractGoods.matches(goodsType));
    }

    /**
     * Does this work location have any inputs.
     *
     * @return True if there are any inputs.
     */
    public boolean hasInputs() {
        return any(getInputs());
    }

    /**
     * Does this work location have any outputs.
     *
     * @return True if there are any outputs.
     */
    public boolean hasOutputs() {
        return this.productionType != null
            && any(this.productionType.getOutputs());
    }

    /**
     * Checks if this work location can actually be worked.
     *
     * @return True if the work location can be worked.
     */
    public boolean canBeWorked() {
        return getNoWorkReason() == NoAddReason.NONE;
    }

    /**
     * Does this work location have teaching capability?
     *
     * @return True if this is a teaching location.
     * @see Ability#TEACH
     */
    public boolean canTeach() {
        return hasAbility(Ability.TEACH);
    }

    /**
     * Gets the ProductionInfo for this WorkLocation from the Colony's
     * cache.
     *
     * @return The work location {@code ProductionInfo}.
     */
    public ProductionInfo getProductionInfo() {
        return getColony().getProductionInfo(this);
    }

    /**
     * Gets the production at this work location.
     *
     * @return The work location production.
     */
    public List<AbstractGoods> getProduction() {
        ProductionInfo info = getProductionInfo();
        return (info == null) ? EMPTY_LIST : info.getProduction();
    }

    /**
     * Gets the total production of a specified goods type at this
     * work location.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The amount of production.
     */
    public int getTotalProductionOf(GoodsType goodsType) {
        if (goodsType == null) {
            throw new RuntimeException("Null GoodsType: " + this);
        }
        return AbstractGoods.getCount(goodsType, getProduction());
    }

    /**
     * Gets the maximum production of this work location for a given
     * goods type, assuming the current workers and input goods.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The maximum production of the goods at this work location.
     */
    public int getMaximumProductionOf(GoodsType goodsType) {
        ProductionInfo info = getProductionInfo();
        if (info == null) return 0;
        List<AbstractGoods> production = info.getMaximumProduction();
        if (production != null) {
            AbstractGoods ag = find(production, AbstractGoods.matches(goodsType));
            if (ag != null) return ag.getAmount();
        }
        return getTotalProductionOf(goodsType);
    }

    /**
     * Gets the unit type that is the expert for this work location
     * using its first output for which an expert type can be found.
     *
     * @return The expert {@code UnitType} or null if none found.
     */
    public UnitType getExpertUnitType() {
        final Specification spec = getSpecification();
        ProductionType pt = getBestProductionType(false, null);
        return (pt == null) ? null
            : find(map(pt.getOutputs(),
                       ag -> spec.getExpertForProducing(ag.getType())),
                   isNotNull());
    }

    /**
     * Get the potential production of a given goods type using the
     * default unit.  This is useful for planning.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The potential production.
     */
    public int getGenericPotential(GoodsType goodsType) {
        return getPotentialProduction(goodsType,
            getSpecification().getDefaultUnitType(getOwner()));
    }

    /**
     * Gets the productivity of a unit working in this work location,
     * considering *only* the contribution of the unit, exclusive of
     * that of the work location.
     *
     * Used below, only public for the test suite.
     *
     * @param unit The {@code Unit} to check.
     * @param goodsType The {@code GoodsType} to check the production of.
     * @return The maximum return from this unit.
     */
    public int getUnitProduction(Unit unit, GoodsType goodsType) {
        if (unit == null || unit.getWorkType() != goodsType) return 0;
        final UnitType unitType = unit.getType();
        final Turn turn = getGame().getTurn();
        return Math.max(0,
                        (int)applyModifiers(getBaseProduction(getProductionType(), goodsType, unitType),
                                            turn,
                                            getProductionModifiers(goodsType, unitType)));
    }

    /**
     * Gets the production of a unit of the given type of goods.
     *
     * @param unit The unit to do the work.
     * @param goodsType The type of goods to get the production of.
     * @return The production of the given type of goods.
     */
    public int getProductionOf(Unit unit, GoodsType goodsType) {
        if (unit == null) throw new RuntimeException("Null unit: " + this);
        return (!produces(goodsType)) ? 0
            : Math.max(0, getPotentialProduction(goodsType, unit.getType()));
    }

    /**
     * Get the potential production of a given goods type at this
     * location.  An optional unit type to do the production may be
     * specified, however if null the unattended production will be
     * calculated.
     *
     * Usually if a unit type is specified and a unit can not be added
     * to the work location, zero production is returned.  However,
     * this routine is intended to be used for planning purposes, so
     * some exceptions are allowed --- the calculation proceeds:
     *
     *   - for unclaimed tiles
     *   - when the location is currently full of units
     *
     * which are conditions that an AI might plausibly be able and
     * willing to change (a case could be made for including the
     * OCCUPIED_BY_ENEMY condition).
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code UnitType} to do the work.
     * @return The potential production with the given goods type and
     *     unit type.
     */
    public int getPotentialProduction(GoodsType goodsType,
                                      UnitType unitType) {
        if (!canProduce(goodsType, unitType)) return 0;

        if (unitType != null) {
            switch (getNoWorkReason()) {
            case NONE: case ALREADY_PRESENT: case CLAIM_REQUIRED:
                break;
            case CAPACITY_EXCEEDED:
                if (getUnitCapacity() > 0) break; // Could work after reorg!
                // Fall through
            case WRONG_TYPE: case OWNED_BY_ENEMY: case ANOTHER_COLONY:
            case COLONY_CENTER: case MISSING_ABILITY: case MISSING_SKILL:
            case MINIMUM_SKILL: case MAXIMUM_SKILL:
            case OCCUPIED_BY_ENEMY: // Arguable!
            default:
                // Non-transient or inapplicable conditions.  Production
                // is impossible.
                return 0;
            }
        }

        int amount = getBaseProduction(null, goodsType, unitType);
        amount = (int)applyModifiers(amount, getGame().getTurn(),
                                     getProductionModifiers(goodsType, unitType));
        return (amount < 0) ? 0 : amount;
    }

    /**
     * Helper to get the production deficit for a type of goods.
     *
     * @param goodsType The {@code GoodsType} to check.
     * @return The production deficit as {@code AbstractGoods}, or null
     *     if not in deficit.
     */
    public AbstractGoods getProductionDeficit(GoodsType goodsType) {
        ProductionInfo pi = getProductionInfo();
        return (pi == null) ? null
            : find(pi.getProductionDeficit(), AbstractGoods.matches(goodsType));
    }
 
   
    // Interface Location
    // Inherits:
    //   FreeColObject.getId
    //   UnitLocation.getLocationLabel
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   UnitLocation.getUnitCount
    //   UnitLocation.getGoodsContainer

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        return (getOwner() == player) ? getLocationLabel()
            : getColony().getLocationLabelFor(player);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Tile getTile() {
        return getSettlement().getTile();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(final Locatable locatable) {
        NoAddReason reason = getNoAddReason(locatable);
        switch (reason) {
        case NONE:
            break;
        case ALREADY_PRESENT:
            return true;
        default:
            throw new IllegalStateException("Can not add " + locatable
                + " to " + this + " because " + reason);
        }
        Unit unit = (Unit)locatable;
        if (!super.add(unit)) return false;

        unit.setState(Unit.UnitState.IN_COLONY);
        unit.setMovesLeft(0);

        // Choose a sensible work type, which should update production type.
        setWorkFor(unit);

        getColony().invalidateCache();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean remove(final Locatable locatable) {
        if (!(locatable instanceof Unit)) {
            throw new IllegalStateException("Not a unit: " + locatable);
        }
        Unit unit = (Unit)locatable;
        if (!contains(unit)) return true;
        if (!super.remove(unit)) return false;

        unit.setState(Unit.UnitState.ACTIVE);
        unit.setMovesLeft(0);

        // Switch to unattended production if possible.
        if (isEmpty()) updateProductionType();

        getColony().invalidateCache();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Settlement getSettlement() {
        return this.colony;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Colony getColony() {
        return this.colony;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int getRank() {
        return Location.rankOf(getTile());
    }

    
    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.getUnitCapacity
    //   UnitLocation.equipForRole

    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        return (locatable instanceof Unit && ((Unit)locatable).isPerson())
            ? super.getNoAddReason(locatable)
            : NoAddReason.WRONG_TYPE;
    }


    // Abstract and overrideable routines to be implemented by
    // WorkLocation subclasses.

    /**
     * Get a description of the work location, with any expected extra
     * detail.
     *
     * @return A label {@code StringTemplate} for this work location.
     */
    public abstract StringTemplate getLabel();

    /**
     * Is this work location available?
     *
     * @return True if the work location is either current or can be claimed.
     */
    public abstract boolean isAvailable();

    /**
     * Is this a current work location of this colony?
     *
     * @return True if the work location is current.
     */
    public abstract boolean isCurrent();

    /**
     * Checks if this work location is available to the colony to be
     * worked.
     *
     * @return The reason why/not the work location can be worked.
     */
    public abstract NoAddReason getNoWorkReason();

    /**
     * Get an associated tile, if any.
     *
     * @return The underlying {@code Tile} which is worked, if any.
     */
    public abstract Tile getWorkTile();
    
    /**
     * Get the "level" of this work location.
     *
     * Note: only meaningful for buildings at present.
     *
     * @return The work location level.
     */
    public abstract int getLevel();
        
    /**
     * Can this work location can produce goods without workers?
     *
     * @return True if this work location can produce goods without
     *     workers.
     */
    public abstract boolean canAutoProduce();

    /**
     * Can this work location produce a given goods type with
     * an optional unit.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType}, if null the
     *     unattended production is considered.
     * @return True if this location can produce the goods.
     */
    public abstract boolean canProduce(GoodsType goodsType,
                                       UnitType unitType);

    /**
     * Gets the base production of a given goods type optionally using
     * a unit of a given type in this work location.  That is, the
     * production exclusive of any modifiers.  If no unit type is
     * specified, the unattended production is calculated.
     *
     * @param productionType An optional {@code ProductionType} to use,
     *     if null the best available one is used.
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType An optional {@code UnitType} to produce
     *     the goods.
     * @return The amount of goods potentially produced.
     */
    public abstract int getBaseProduction(ProductionType productionType,
                                          GoodsType goodsType,
                                          UnitType unitType);

    /**
     * Gets the production modifiers for the given type of goods and
     * unit type.  If no unit is specified the unattended production
     * is calculated.
     *
     * @param goodsType The {@code GoodsType} to produce.
     * @param unitType The optional {@code UnitType} to produce them.
     * @return A stream of the applicable modifiers.
     */
    public abstract Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                            UnitType unitType);

    /**
     * Get the production types available for this work location.
     *
     * @param unattended If true, get unattended production types.
     * @return A list of suitable {@code ProductionType}s.
     */
    public abstract List<ProductionType> getAvailableProductionTypes(boolean unattended);

    /**
     * Get a work location specific factor to multiply any goods-specific
     * unit production bonuses by.
     *
     * @return The competence factor.
     */
    public abstract float getCompetenceFactor();

    /**
     * Get a work location specific factor to multiply the colony
     * rebel bonus by.
     *
     * @return The rebel factor.
     */
    public abstract float getRebelFactor();
      
    /**
     * Evaluate this work location for a given player.
     * To be overridden by subclasses.
     *
     * @param player The {@code Player} to evaluate for.
     * @return A value for the player.
     */
    public int evaluateFor(Player player) {
        int result = 0;
        for (Unit u : getUnitList()) {
            int v = u.evaluateFor(player);
            if (v == Integer.MIN_VALUE) return Integer.MIN_VALUE;
            result += v;
        }
        return result;
    }

    /**
     * Gets a template describing whether this work location can/needs-to
     * be claimed.  To be overridden by classes where this is meaningful.
     *
     * This is a default null implementation.
     *
     * @return A suitable template.
     */
    public StringTemplate getClaimTemplate() {
        return StringTemplate.name("");
    }


    // Interface Ownable

    /**
     * Gets the owner of this {@code Ownable}.
     *
     * @return The {@code Player} controlling this
     *         {@link Ownable}.
     */
    @Override
    public Player getOwner() {
        return getColony().getOwner();
    }

    /**
     * Sets the owner of this {@code Ownable}.  Do not call this
     * method, ever.  The owner of this WorkLocation is the owner
     * of the Colony, you must set the owner of the Colony instead.
     *
     * @param p The {@code Player} that should take ownership
     *     of this {@link Ownable}.
     * @exception UnsupportedOperationException is always thrown by
     *     this method.
     */
    @Override
    public void setOwner(Player p) {
        throw new UnsupportedOperationException();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        WorkLocation o = copyInCast(other, WorkLocation.class);
        if (o == null || !super.copyIn(o)) return false;
        this.colony = getGame().updateRef(o.getColony());
        this.productionType = o.getProductionType();
        return true;
    }


    // Serialization

    private static final String COLONY_TAG = "colony";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(COLONY_TAG, this.colony);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (this.productionType != null) this.productionType.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.colony = xr.findFreeColGameObject(getGame(), COLONY_TAG,
            Colony.class, (Colony)null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (ProductionType.TAG.equals(tag)) {
            this.productionType = new ProductionType(xr, spec);

        } else {
            super.readChild(xr);
        }
    }

    // getXMLTagName left to subclasses
}
