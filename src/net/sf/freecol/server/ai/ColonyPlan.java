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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Objects of this class describes the plan the AI has for a
 * <code>Colony</code>.
 *
 * A <code>ColonyPlan</code> contains a list of
 * {@link WorkLocationPlan}s which suggests the food and non-food
 * production of each {@link WorkLocation}, and a list of
 * {@link BuildableType}s to build.  It takes account of the available
 * tiles and building production, but does not make decisions to claim
 * tiles or change the current buildable.  It does takes account of
 * goods present in the colony, and overall colony size but not the
 * exact composition of the units involved.  However there is
 * extensive structure for making a trial assignment of workers in
 * {@link #assignWorkers}.
 *
 * {@link AIColony#rearrangeWorkers} is responsible for making
 * the real decisions.
 *
 * @see Colony
 */
public class ColonyPlan {

    private static final Logger logger = Logger.getLogger(ColonyPlan.class.getName());

    /** Require production plans to always produce an amount exceeding this. */
    private static final int LOW_PRODUCTION_THRESHOLD = 1;

    /**
     * Number of turns to require production of without exhausting the
     * input goods.
     */
    private static final int PRODUCTION_TURNOVER_TURNS = 5;

    /** The profile of the colony (a sort of general flavour). */
    private static enum ProfileType {
        OUTPOST,
        SMALL,
        MEDIUM,
        LARGE,
        CAPITAL;

        /**
         * Chooses a suitable profile type given a size of colony.
         *
         * @param size A proposed colony size.
         */
        public static ProfileType getProfileTypeFromSize(int size) {
            return (size <= 1) ? ProfileType.OUTPOST
                : (size <= 2) ? ProfileType.SMALL
                : (size <= 4) ? ProfileType.MEDIUM
                : (size <= 8) ? ProfileType.LARGE
                : ProfileType.CAPITAL;
        }
    };
    private ProfileType profileType;

    /** Private copy of the AIMain. */
    private final AIMain aiMain;

    /** The colony this AIColony manages. */
    private final Colony colony;

    /** The things to build, and their priority. */
    private static class BuildPlan {

        public final BuildableType type;
        public double weight;
        public double support;
        public double difficulty;

        public BuildPlan(BuildableType type, double weight, double support) {
            this.type = type;
            this.weight = weight;
            this.support = support;
            this.difficulty = 1.0f;
        }

        public double getValue() {
            return weight * support / difficulty;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return String.format("%s (%1.3f * %1.3f / %1.3f = %1.3f)",
                                 type.getSuffix(), weight, support,
                                 difficulty, getValue());
        }
    };
    private final List<BuildPlan> buildPlans = new ArrayList<>();

    /**
     * Comparator to sort buildable types on their priority in the
     * buildPlan map.
     */
    private static final Comparator<BuildPlan> buildPlanComparator
        = new Comparator<BuildPlan>() {
            @Override
            public int compare(BuildPlan b1, BuildPlan b2) {
                double d = b1.getValue() - b2.getValue();
                return (d > 0.0) ? -1 : (d < 0.0) ? 1 : 0;
            }
        };

    /** Plans for work locations available to this colony. */
    private final List<WorkLocationPlan> workPlans = new ArrayList<>();

    /** The goods types to produce. */
    private final List<GoodsType> produce = new ArrayList<>();

    /**
     * Lists of goods types to be produced in this colony.
     * Temporary variables that do not need to be serialized.
     */
    private final List<GoodsType> foodGoodsTypes = new ArrayList<>();
    private final List<GoodsType> libertyGoodsTypes = new ArrayList<>();
    private final List<GoodsType> immigrationGoodsTypes = new ArrayList<>();
    private final List<GoodsType> militaryGoodsTypes = new ArrayList<>();
    private final List<GoodsType> rawBuildingGoodsTypes = new ArrayList<>();
    private final List<GoodsType> buildingGoodsTypes = new ArrayList<>();
    private final List<GoodsType> rawLuxuryGoodsTypes = new ArrayList<>();
    private final List<GoodsType> luxuryGoodsTypes = new ArrayList<>();
    private final List<GoodsType> otherRawGoodsTypes = new ArrayList<>();


    /**
     * Creates a new <code>ColonyPlan</code>.
     *
     * @param aiMain The main AI-object.
     * @param colony The colony to make a <code>ColonyPlan</code> for.
     */
    public ColonyPlan(AIMain aiMain, Colony colony) {
        if (aiMain == null) throw new IllegalArgumentException("Null AIMain");
        if (colony == null) throw new IllegalArgumentException("Null colony");

        this.aiMain = aiMain;
        this.colony = colony;
        this.profileType = ProfileType
            .getProfileTypeFromSize(colony.getUnitCount());
    }


    /**
     * Gets the main AI-object.
     *
     * @return The main AI-object.
     */
    private AIMain getAIMain() {
        return aiMain;
    }

    /**
     * Gets the specification.
     *
     * @return The specification.
     */
    private Specification spec() {
        return aiMain.getGame().getSpecification();
    }


    // Public functionality.

    /**
     * Gets the preferred goods to produce.
     *
     * @return A copy of the preferred goods production list in this plan.
     */
    public List<GoodsType> getPreferredProduction() {
        return new ArrayList<>(produce);
    }

    /**
     * Gets a copy of the current list of buildable types associated
     * with this <code>ColonyPlan</code>.
     *
     * @return A copy of the of <code>BuildableType</code>s list.
     */
    public List<BuildableType> getBuildableTypes() {
        List<BuildableType> build = new ArrayList<>();
        for (BuildPlan b : buildPlans) build.add(b.type);
        return build;
    }

    /**
     * Gets the best buildable type from this plan that can currently
     * be built by the colony.
     *
     * @return The best current <code>BuildableType</code>.
     */
    public BuildableType getBestBuildableType() {
        BuildPlan bp = find(buildPlans, p -> colony.canBuild(p.type));
        return (bp == null) ? null : bp.type;
    }

    /**
     * Get a report on the build plans.
     *
     * @return A build plan report.
     */
    public String getBuildableReport() {
        LogBuilder lb = new LogBuilder(64);
        lb.add("Buildables:\n");
        for (BuildPlan b : buildPlans) lb.add(b, "\n");
        return lb.toString();
    }

    /**
     * Gets the food-producing and non-autoproducing work location
     * plans associated with this <code>ColonyPlan</code>.
     *
     * @return A list of food producing plans.
     */
    public List<WorkLocationPlan> getFoodPlans() {
        return workPlans.stream()
            .filter(wp -> wp.isFoodPlan()
                && !wp.getWorkLocation().canAutoProduce())
            .collect(Collectors.toList());
    }

    /**
     * Gets the non-food-producing/non-autoproducing work location
     * plans associated with this <code>ColonyPlan</code>.
     *
     * @return A list of non-food producing plans.
     */
    public List<WorkLocationPlan> getWorkPlans() {
        return workPlans.stream()
            .filter(wp -> !wp.isFoodPlan()
                && !wp.getWorkLocation().canAutoProduce())
            .collect(Collectors.toList());
    }

    /**
     * Refines this plan given the colony choice of what to build.
     *
     * @param build The <code>BuildableType</code> to be built (may be null).
     * @param lb A <code>LogBuilder</code> to log to.
     */
    public void refine(BuildableType build, LogBuilder lb) {
        List<GoodsType> required = new ArrayList<>();
        for (AbstractGoods ag : colony.getFullRequiredGoods(build)) {
            required.add(ag.getType());
        }
        Map<GoodsType, List<WorkLocationPlan>> suppressed = new HashMap<>();

        // Examine a copy of the work plans, but operate on the
        // original list.  Maintain a offset between the index in the
        // copied list and the original to aid reinsertion.
        //
        // Remove any work plans to make raw/building goods that are
        // not required to complete the current buildable, but take
        // care to put such plans back again if a plan is encountered
        // that makes goods that are made from a type that was removed
        // and there is less than CARGO_SIZE/2 of that type in stock.
        // Note though in such cases the position of the
        // building-goods plans in the work plans list will have moved
        // from their usual high priority to immediately before the
        // position of the manufactured goods.
        //
        // So, for example, we should suppress tool building when a
        // colony is building a warehouse, unless we find a plan to
        // make muskets and the tool stock is low.
        //
        // FIXME: generalize this further to make tools for pioneers.
        //
        List<WorkLocationPlan> plans = new ArrayList<>(workPlans);
        int offset = 0;
        for (int i = 0; i < plans.size(); i++) {
            List<WorkLocationPlan> wls;
            WorkLocationPlan wlp = plans.get(i);
            GoodsType g = wlp.getGoodsType();
            if ((rawBuildingGoodsTypes.contains(g)
                    && !required.contains(g.getOutputType()))
                || (buildingGoodsTypes.contains(g)
                    && !required.contains(g))) {
                workPlans.remove(i - offset);
                offset++;
                wls = suppressed.get(g);
                if (wls == null) wls = new ArrayList<>();
                wls.add(0, wlp); // reverses list
                suppressed.put(g, wls);
                produce.remove(g);
                lb.add(", suppress production of ", g);
            } else if (g.isRefined()
                && (rawBuildingGoodsTypes.contains(g.getInputType())
                    || buildingGoodsTypes.contains(g.getInputType()))) {
                int n = 0, idx = produce.indexOf(g);
                for (GoodsType type = g.getInputType(); type != null;
                     type = type.getInputType()) {
                    if ((wls = suppressed.get(type)) == null) break;
                    if (colony.getGoodsCount(type)
                        >= GoodsContainer.CARGO_SIZE/2) break;
                    n += wls.size();
                    while (!wls.isEmpty()) {
                        // reverses again when adding, cancelling reversal above
                        workPlans.add(i - offset, wls.remove(0));
                    }
                    produce.add(idx, type);
                    lb.add(", restore production of ", type);
                }
                offset -= n;
            }
        }
    }

    /**
     * Recreates the buildables and work location plans for this
     * colony.
     */
    public void update() {
        // Update the profile type.
        profileType = ProfileType.getProfileTypeFromSize(colony.getUnitCount());

        // Build the total map of all possible production with standard units.
        Map<GoodsType, Map<WorkLocation, Integer>> production
            = createProductionMap();

        // Set the goods type lists, and prune production of manufactured
        // goods that are missing raw materials and other non-interesting.
        updateGoodsTypeLists(production);

        // Set the preferred raw materials.  Prune production and
        // goods lists further removing the non-preferred new world
        // raw and refined materials.
        updateRawMaterials(production);

        // The buildables depend on the profile type, the goods type lists
        // and/or goods-to-produce list.
        updateBuildableTypes();

        // Make plans for each valid <goods, location> production and
        // complete the list of goods to produce.
        updatePlans(production);
    }


    // Internals

    /**
     * Creates a map of potential production of all goods types
     * from all available work locations using the default unit type.
     * Includes non-workable locations (e.g. chapel, colony-center-tile)
     * as their production can influence the choice of goods to produce.
     *
     * @return The map of potential production.
     */
    private Map<GoodsType, Map<WorkLocation, Integer>> createProductionMap() {
        Map<GoodsType, Map<WorkLocation, Integer>> production = new HashMap<>();
        for (WorkLocation wl : colony.getAvailableWorkLocations()) {
            for (GoodsType g : spec().getGoodsTypeList()) {
                int p = wl.getGenericPotential(g);
                if (p > 0) {
                    Map<WorkLocation, Integer> m = production.get(g);
                    if (m == null) {
                        m = new HashMap<>();
                        production.put(g, m);
                    }
                    m.put(wl, p);
                }
            }
        }
        return production;
    }

    /**
     * Updates the goods type lists.  The categories are:<UL>
     * <LI>food</LI>
     * <LI>liberty</LI>
     * <LI>immigration</LI>
     * <LI>military</LI>
     * <LI>raw building</LI>
     * <LI>building</LI>
     * <LI>raw luxury</LI>
     * <LI>luxury</LI>
     * <LI>raw other</LI>
     * </UL>
     *
     * Ignore raw materials which can not be refined and refined goods
     * that have no raw materials available.  Also ignore other goods
     * that do not fit these categories (e.g. trade goods).
     *
     * @param production The production map.
     */
    private void updateGoodsTypeLists(Map<GoodsType, Map<WorkLocation, Integer>> production) {
        foodGoodsTypes.clear();
        libertyGoodsTypes.clear();
        immigrationGoodsTypes.clear();
        militaryGoodsTypes.clear();
        rawBuildingGoodsTypes.clear();
        buildingGoodsTypes.clear();
        rawLuxuryGoodsTypes.clear();
        luxuryGoodsTypes.clear();
        otherRawGoodsTypes.clear();
        for (GoodsType g : new ArrayList<>(production.keySet())) {
            if (g.isFoodType()) {
                foodGoodsTypes.add(g);
            } else if (g.isLibertyType()) {
                libertyGoodsTypes.add(g);
            } else if (g.isImmigrationType()) {
                immigrationGoodsTypes.add(g);
            } else if (g.isMilitaryGoods()) {
                militaryGoodsTypes.add(g);
            } else if (g.isRawBuildingMaterial()) {
                rawBuildingGoodsTypes.add(g);
            } else if (g.isBuildingMaterial()
                && g.getInputType().isRawBuildingMaterial()) {
                buildingGoodsTypes.add(g);
            } else if (g.isNewWorldGoodsType()) {
                rawLuxuryGoodsTypes.add(g);
            } else if (g.isRefined()
                && g.getInputType().isNewWorldGoodsType()) {
                luxuryGoodsTypes.add(g);
            } else if (g.isFarmed()) {
                otherRawGoodsTypes.add(g);
            } else { // Not interested in this goods type.  Should not happen.
                logger.warning("Ignoring goods type " + g
                    + " at " + colony.getName());
                production.remove(g);
            }
        }
    }

    /**
     * Chooses the two best raw materials, updating the production
     * map and lists.
     *
     * FIXME: scale with colony size.
     *
     * @param production The production map.
     */
    private void updateRawMaterials(Map<GoodsType, Map<WorkLocation, Integer>> production) {
        Player player = colony.getOwner();
        Market market = player.getMarket();
        NationType nationType = player.getNationType();
        GoodsType primaryRawMaterial = null;
        GoodsType secondaryRawMaterial = null;
        int primaryValue = -1;
        int secondaryValue = -1;

        produce.clear();
        List<GoodsType> rawMaterials = new ArrayList<>(rawLuxuryGoodsTypes);
        rawMaterials.addAll(otherRawGoodsTypes);
        for (GoodsType g : rawMaterials) {
            int value = production.get(g).entrySet().stream()
                .mapToInt(e -> e.getValue()).sum();
            if (value <= LOW_PRODUCTION_THRESHOLD) {
                production.remove(g);
                continue;
            }
            if (market != null) {
                // If the market is available, weight by sale price of
                // the material, or if it is the raw material for a
                // refined goods type, the average of the raw and
                // refined goods prices.
                if (g.getOutputType() == null) {
                    value *= market.getSalePrice(g, 1);
                } else if (production.containsKey(g.getOutputType())) {
                    value *= (market.getSalePrice(g, 1)
                        + market.getSalePrice(g.getOutputType(), 1)) / 2;
                }
            }
            if (nationType.hasModifier(g.getId())) {
                value = (value * 12) / 10; // Bonus for national advantages
            }
            if (value > secondaryValue && secondaryRawMaterial != null) {
                production.remove(secondaryRawMaterial);
                production.remove(secondaryRawMaterial.getOutputType());
                if (rawLuxuryGoodsTypes.contains(secondaryRawMaterial)) {
                    rawLuxuryGoodsTypes.remove(secondaryRawMaterial);
                    luxuryGoodsTypes.remove(secondaryRawMaterial.getOutputType());
                } else if (otherRawGoodsTypes.contains(secondaryRawMaterial)) {
                    otherRawGoodsTypes.remove(secondaryRawMaterial);
                }
            }
            if (value > primaryValue) {
                secondaryRawMaterial = primaryRawMaterial;
                secondaryValue = primaryValue;
                primaryRawMaterial = g;
                primaryValue = value;
            } else if (value > secondaryValue) {
                secondaryRawMaterial = g;
                secondaryValue = value;
            }
        }
        if (primaryRawMaterial != null) {
            produce.add(primaryRawMaterial);
            if (primaryRawMaterial.getOutputType() != null) {
                produce.add(primaryRawMaterial.getOutputType());
            }
            if (secondaryRawMaterial != null) {
                produce.add(secondaryRawMaterial);
                if (secondaryRawMaterial.getOutputType() != null) {
                    produce.add(secondaryRawMaterial.getOutputType());
                }
            }
        }
    }


    // Relative weights of the various building categories.
    // FIXME: split out/parameterize into a "building strategy"
    //
    // BuildableTypes that improve breeding.
    private static final double BREEDING_WEIGHT    = 0.1;
    // BuildableTypes that improve building production.
    private static final double BUILDING_WEIGHT    = 0.9;
    // BuildableTypes that produce defensive units.
    private static final double DEFENCE_WEIGHT     = 0.1;
    // BuildableTypes that provide export ability.
    private static final double EXPORT_WEIGHT      = 0.6;
    // BuildableTypes that allow water to be used.
    private static final double FISH_WEIGHT        = 0.25;
    // BuildableTypes that improve the colony fortifications.
    private static final double FORTIFY_WEIGHT     = 0.3;
    // BuildableTypes that improve immigration production.
    private static final double IMMIGRATION_WEIGHT = 0.05;
    // BuildableTypes that improve liberty production.
    private static final double LIBERTY_WEIGHT     = 0.75;
    // BuildableTypes that improve military goods production.
    private static final double MILITARY_WEIGHT    = 0.4;
    // BuildableTypes that improve luxury goods production.
    private static final double PRODUCTION_WEIGHT  = 0.25;
    // BuildableTypes that improve colony storage.
    private static final double REPAIR_WEIGHT      = 0.1;
    // BuildableTypes that improve colony storage.
    private static final double STORAGE_WEIGHT     = 0.85;
    // BuildableTypes that improve education.
    private static final double TEACH_WEIGHT       = 0.2;
    // BuildableTypes that improve transport.
    private static final double TRANSPORT_WEIGHT   = 0.15;

    /**
     * Finds a build plan for this type.
     *
     * @param type The <code>BuildableType</code> to search for.
     * @return A <code>BuildPlan</code> with this type, or null if not found.
     */
    private BuildPlan findBuildPlan(BuildableType type) {
        return find(buildPlans, bp -> bp.type == type);
    }

    /**
     * Adds or improves the priority of a buildable in a list.
     *
     * @param type The <code>BuildableType</code> to use.
     * @param weight The relative weight of this class of buildable with
     *     respect to other buildable classes.
     * @param support The support for this buildable within its class.
     * @return True if this type was prioritized.
     */
    private boolean prioritize(BuildableType type,
                               double weight, double support) {
        BuildPlan bp = findBuildPlan(type);
        if (bp == null) {
            buildPlans.add(new BuildPlan(type, weight, support));
            return true;
        }
        if (bp.weight * bp.support < weight * support) {
            bp.weight = weight;
            bp.support = support;
            return true;
        }
        return false;
    }

    /**
     * Given a buildable that improves production of a goods type,
     * prioritize it.
     *
     * @param type The <code>BuildableType</code> to consider.
     * @param goodsType The <code>GoodsType</code> improved by the buildable.
     * @return True if this type was prioritized.
     */
    private boolean prioritizeProduction(BuildableType type,
                                         GoodsType goodsType) {
        Player player = colony.getOwner();
        NationType nationType = player.getNationType();
        String advantage = getAIMain().getAIPlayer(player).getAIAdvantage();
        boolean ret = false;
        double factor = 1.0;
        if (nationType.hasModifier(goodsType.getId())) {
            // Handles building, agriculture, furTrapping advantages
            factor *= 1.2;
        }
        if (goodsType.isMilitaryGoods()) {
            if ("conquest".equals(advantage)) factor = 1.2;
            ret = prioritize(type, MILITARY_WEIGHT * factor,
                1.0/*FIXME: amount present wrt amount to equip*/);
        } else if (goodsType.isBuildingMaterial()) {
            ret = prioritize(type, BUILDING_WEIGHT * factor,
                1.0/*FIXME: need for this type*/);
        } else if (goodsType.isLibertyType()) {
            if (player.isREF()) return false; // no bells for REF colonies
            ret = prioritize(type, LIBERTY_WEIGHT,
                (colony.getSoL() >= 100) ? 0.01 : 1.0);
        } else if (goodsType.isImmigrationType()) {
            if ("immigration".equals(advantage)) factor = 1.2;
            ret = prioritize(type, IMMIGRATION_WEIGHT * factor,
                1.0/*FIXME: Brewster?*/);
        } else if (produce.contains(goodsType)) {
            if ("trade".equals(advantage)) factor = 1.2;
            double f = 0.1 * colony.getTotalProductionOf(goodsType.getInputType());
            ret = prioritize(type, PRODUCTION_WEIGHT,
                f/*FIXME: improvement?*/);
        }
        return ret;
    }

    /**
     * Updates the build plans for this colony.
     */
    private void updateBuildableTypes() {
        final EuropeanAIPlayer euaip = (EuropeanAIPlayer)getAIMain()
            .getAIPlayer(colony.getOwner());
        String advantage = euaip.getAIAdvantage();
        buildPlans.clear();

        int maxLevel;
        switch (profileType) {
        case OUTPOST:
        case SMALL:   maxLevel = 1; break;
        case MEDIUM:  maxLevel = 2; break;
        case LARGE:   maxLevel = 3; break;
        case CAPITAL: maxLevel = 4; break;
        default:
            throw new IllegalStateException("Bogus profile type: "
                + profileType);
        }

        Player player = colony.getOwner();
        for (BuildingType type : spec().getBuildingTypeList()) {
            boolean expectFail = false;
            if (!colony.canBuild(type)) continue;

            // Exempt defence and export from the level check.
            if (type.hasModifier(Modifier.DEFENCE)) {
                double factor = 1.0;
                if ("conquest".equals(advantage)) factor = 1.1;
                prioritize(type, FORTIFY_WEIGHT * factor,
                    1.0/*FIXME: 0 if FF underway*/);
            }
            if (type.hasAbility(Ability.EXPORT)) {
                double factor = 1.0;
                if ("trade".equals(advantage)) factor = 1.1;
                prioritize(type, EXPORT_WEIGHT * factor,
                    1.0/*FIXME: weigh production v transport*/);
            }

            // Skip later stage buildings for smaller settlements.
            if (type.getLevel() > maxLevel) continue;

            // Scale docks by the improvement available to the food supply.
            if (type.hasAbility(Ability.PRODUCE_IN_WATER)) {
                double factor = 0.0;
                if (!colony.hasAbility(Ability.PRODUCE_IN_WATER)
                    && colony.getTile().isShore()) {
                    int landFood = 0, seaFood = 0;
                    for (Tile t : colony.getTile().getSurroundingTiles(1)) {
                        if (t.getOwningSettlement() == colony
                            || player.canClaimForSettlement(t)) {
                            for (AbstractGoods ag : t.getSortedPotential()) {
                                if (ag.getType().isFoodType()) {
                                    if (t.isLand()) {
                                        landFood += ag.getAmount();
                                    } else {
                                        seaFood += ag.getAmount();
                                    }
                                }
                            }
                        }
                    }
                    factor = (seaFood + landFood == 0) ? 0.0
                        : seaFood / (double)(seaFood + landFood);
                }
                prioritize(type, FISH_WEIGHT, factor);
            }

            if (type.hasAbility(Ability.BUILD)) {
                double factor = 1.0;
                if ("building".equals(advantage)) factor = 1.1;
                double support = 1.0;
                for (Ability a : type.getAbilities(Ability.BUILD)) {
                    List<Scope> scopes = a.getScopes();
                    if (scopes != null && !scopes.isEmpty()) support = 0.1;
                }
                prioritize(type, BUILDING_WEIGHT * factor,
                    support/*FIXME: need for the thing now buildable*/);
            }

            if (type.hasAbility(Ability.TEACH)) {
                prioritize(type, TEACH_WEIGHT,
                    1.0/*FIXME: #students, #specialists here, #wanted*/);
            }

            if (type.hasAbility(Ability.REPAIR_UNITS)) {
                double factor = 1.0;
                if ("naval".equals(advantage)) factor = 1.1;
                prioritize(type, REPAIR_WEIGHT * factor,
                    1.0/*FIXME: #units-to-repair, has-Europe etc*/);
            }

            GoodsType output = type.getProducedGoodsType();
            if (output != null) {
                if (!prioritizeProduction(type, output)) {
                    // Allow failure if this building can not build.
                    expectFail = true;
                }
            } else {
                for (GoodsType g : spec().getGoodsTypeList()) {
                    if (type.hasModifier(g.getId())) {
                        if (!prioritizeProduction(type, g)) {
                            expectFail = true;
                        }
                    }
                }
                // Hacks.  No good way to make this really generic.
                if (type.hasModifier(Modifier.WAREHOUSE_STORAGE)) {
                    double factor = 1.0;
                    if ("trade".equals(advantage)) factor = 1.1;
                    prioritize(type, STORAGE_WEIGHT * factor,
                        1.0/*FIXME: amount of goods*/);
                }
                if (type.hasModifier(Modifier.BREEDING_DIVISOR)) {
                    prioritize(type, BREEDING_WEIGHT,
                        1.0/*FIXME: horses present?*/);
                }
            }

            if (findBuildPlan(type) == null && !expectFail) {
                logger.warning("No building priority found for: " + type);
            }
        }

        double wagonNeed = 0.0;
        if (!colony.isConnectedPort()) { // Inland colonies need transportation
            int wagons = euaip.getNeededWagons(colony.getTile());
            wagonNeed = (wagons <= 0) ? 0.0 : (wagons > 3) ? 1.0
                : wagons / 3.0;
        }
        for (UnitType unitType : spec().getUnitTypeList()) {
            if (!colony.canBuild(unitType)) continue;
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                ; // FIXME: decide to build a ship
            } else if (unitType.isDefensive()) {
                if (colony.isBadlyDefended()) {
                    prioritize(unitType, DEFENCE_WEIGHT,
                        1.0/*FIXME: how badly defended?*/);
                }
            } else if (unitType.hasAbility(Ability.CARRY_GOODS)) {
                if (wagonNeed > 0.0) {
                    double factor = 1.0;
                    if ("trade".equals(advantage)) factor = 1.1;
                    prioritize(unitType, TRANSPORT_WEIGHT * factor,
                        wagonNeed/*FIXME: type.getSpace()*/);
                }
            }
        }

        // Weight by lower required goods.
        for (BuildPlan bp : buildPlans) {
            double difficulty = bp.type.getRequiredGoods().stream()
                .filter(ag -> ag.getAmount() > colony.getGoodsCount(ag.getType()))
                .mapToDouble(ag -> {
                        final GoodsType type = ag.getType();
                        return (ag.getAmount() - colony.getGoodsCount(type))
                            // Penalize building with type that can not be
                            // made locally.
                            * ((produce.contains(type.getInputType())) ? 1.0
                                : 5.0);
                    }).sum();
            bp.difficulty = Math.max(1.0f, Math.sqrt(difficulty));
        }

        Collections.sort(buildPlans, buildPlanComparator);
    }

    /**
     * Makes a plan for each type of possible production, that is
     * those work locations that can use a unit or can auto-produce.
     * Note that this will almost certainly include clashes over work
     * locations.  That gets sorted out elsewhere as ColonyPlans do
     * not examine the units present.
     *
     * With the complete list of work plans, finish creating the list
     * of goods to produce.
     *
     * Then filter out the auto-production plans as they are not
     * going to be helpful for unit allocation.
     *
     * Finally sort by desirability.
     */
    private void updatePlans(Map<GoodsType, Map<WorkLocation, Integer>> production) {
        workPlans.clear();
        for (Entry<GoodsType, Map<WorkLocation, Integer>> entry
                 : production.entrySet()) {
            GoodsType g = entry.getKey();
            // Do not make plans to produce into a full warehouse.
            if (g.isStorable()
                && colony.getGoodsCount(g) >= colony.getWarehouseCapacity()
                && !g.limitIgnored()) continue;

            for (WorkLocation wl : entry.getValue().keySet()) {
                if (wl.canBeWorked() || wl.canAutoProduce()) {
                    workPlans.add(new WorkLocationPlan(getAIMain(), wl, g));
                }
            }
        }

        // Now we have lots of plans, determine what goods to produce.
        updateProductionList(production);

        // Filter out plans that can not use a unit.
        List<WorkLocationPlan> oldPlans = new ArrayList<>(workPlans);
        workPlans.clear();
        for (WorkLocationPlan wlp : oldPlans) {
            if (wlp.getWorkLocation().canBeWorked()) workPlans.add(wlp);
        }

        // Sort the work plans by earliest presence in the produce
        // list, and then by amount.  If the type of goods produced is
        // not on the produce list, then make sure such plans sort to
        // the end, except for food plans.
        Collections.sort(workPlans, new Comparator<WorkLocationPlan>() {
                @Override
                public int compare(WorkLocationPlan w1, WorkLocationPlan w2) {
                    GoodsType g1 = w1.getGoodsType();
                    GoodsType g2 = w2.getGoodsType();
                    int i1 = produce.indexOf(g1);
                    int i2 = produce.indexOf(g2);
                    if (i1 < 0 && !g1.isFoodType()) i1 = 99999;
                    if (i2 < 0 && !g2.isFoodType()) i2 = 99999;
                    int cmp = i1 - i2;
                    if (cmp == 0) {
                        cmp = w2.getWorkLocation().getGenericPotential(g2)
                            - w1.getWorkLocation().getGenericPotential(g1);
                    }
                    return cmp;
                }
            });
    }

    /**
     * Add the other goods types to the production list.  When this is
     * called the new world goods production is already present on the
     * produce list.  Ignores food which is treated separately.
     */
    private void updateProductionList(final Map<GoodsType, Map<WorkLocation, Integer>> production) {
        final Comparator<GoodsType> productionComparator
            = new Comparator<GoodsType>() {
                @Override
                public int compare(GoodsType g1, GoodsType g2) {
                    int p1 = production.get(g1).values().stream()
                        .mapToInt(Integer::intValue).sum();
                    int p2 = production.get(g2).values().stream()
                        .mapToInt(Integer::intValue).sum();
                    return p2 - p1;
                }
            };
        List<GoodsType> toAdd = new ArrayList<>();

        // If we need liberty put it before the new world production.
        if (colony.getSoL() < 100) {
            for (GoodsType g : libertyGoodsTypes) {
                if (production.containsKey(g)) toAdd.add(g);
            }
            Collections.sort(toAdd, productionComparator);
            produce.addAll(0, toAdd);
            toAdd.clear();
        }

        // Always add raw/building materials first.
        Collections.sort(rawBuildingGoodsTypes, productionComparator);
        for (GoodsType g : buildingGoodsTypes) {
            if (production.containsKey(g)) {
                GoodsType raw = g.getInputType();
                if (colony.getGoodsCount(raw) >= GoodsContainer.CARGO_SIZE/2
                    || production.containsKey(raw)) {
                    toAdd.add(g);
                }
            }
        }
        Collections.sort(toAdd, new Comparator<GoodsType>() {
                @Override
                public int compare(GoodsType g1, GoodsType g2) {
                    int i1 = rawBuildingGoodsTypes.indexOf(g1.getInputType());
                    int i2 = rawBuildingGoodsTypes.indexOf(g2.getInputType());
                    return i1 - i2;
                }
            });
        for (int i = toAdd.size()-1; i >= 0; i--) {
            GoodsType make = toAdd.get(i);
            GoodsType raw = make.getInputType();
            if (production.containsKey(raw)) {
                if (colony.getGoodsCount(raw) >= GoodsContainer.CARGO_SIZE/2) {
                    produce.add(raw); // Add at the end, enough in stock
                    produce.add(0, make);
                } else {
                    produce.add(0, make);
                    produce.add(0, raw);
                }
            } else {
                produce.add(0, make);
            }
        }
        toAdd.clear();

        // Military goods after lucrative production.
        for (GoodsType g : militaryGoodsTypes) {
            if (production.containsKey(g)) toAdd.add(g);
        }
        Collections.sort(toAdd, productionComparator);
        produce.addAll(toAdd);
        toAdd.clear();

        // Immigration last.
        if (colony.getOwner().getEurope() != null) {
            for (GoodsType g : immigrationGoodsTypes) {
                if (production.containsKey(g)) toAdd.add(g);
            }
            Collections.sort(toAdd, productionComparator);
            produce.addAll(toAdd);
            toAdd.clear();
        }
    }

    /**
     * Tries to swap an expert unit for another doing its job.
     *
     * @param expert The expert <code>Unit</code>.
     * @param others A list of other <code>Unit</code>s to test against.
     * @param colony The <code>Colony</code> the units are working in.
     * @return The unit that was replaced by the expert, or null if none.
     */
    private Unit trySwapExpert(Unit expert, List<Unit> others, Colony colony) {
        Role oldRole = expert.getRole();
        int oldRoleCount = expert.getRoleCount();
        GoodsType work = expert.getType().getExpertProduction();
        GoodsType oldWork = expert.getWorkType();
        for (Unit other : others) {
            if (!other.isPerson()) continue;
            if (other.getWorkType() == work
                && other.getType().getExpertProduction() != work) {
                Location l1 = expert.getLocation();
                Location l2 = other.getLocation();
                other.setLocation(colony.getTile());
                expert.setLocation(l2);
                expert.changeWorkType(work);
                other.setLocation(l1);
                if (oldWork != null) other.changeWorkType(oldWork);
                Role tmpRole = other.getRole();
                int tmpRoleCount = other.getRoleCount();
                other.changeRole(oldRole, oldRoleCount);
                expert.changeRole(tmpRole, tmpRoleCount);
                return other;
            }
        }
        return null;
    }

    /**
     * Finds a plan on a list that produces a given goods type.
     *
     * @param goodsType The <code>GoodsType</code> to produce.
     * @param plans The list of <code>WorkLocationPlan</code>s to check.
     * @return The first plan found that produces the goods type, or null
     *     if none found.
     */
    private WorkLocationPlan findPlan(GoodsType goodsType,
                                      List<WorkLocationPlan> plans) {
        return find(plans, wlp -> wlp.getGoodsType() == goodsType);
    }

    /**
     * Gets the best worker to execute a work location plan.
     * - The most productive one wins (which will automatically pick a
     *   relevant expert).
     * - If they are all relevant experts, pick any.
     * - Pick the unit that can upgrade to the required expert with the most
     *     relevant experience or least irrelevant expertise.
     * - Pick a unit that can not upgrade at all.
     * - Pick an otherwise upgradeable unit with the most relevant experience
     *     or least irrelevant experience.
     * - Pick the least skillful unit.
     *
     * Public for the benefit of the test suite.
     *
     * @param wl The <code>WorkLocation</code> to work at.
     * @param goodsType The <code>GoodsType</code> to make.
     * @param workers A list of potential <code>Unit</code>s to try.
     * @return The best worker for the job.
     */
    public static Unit getBestWorker(WorkLocation wl, GoodsType goodsType,
                                     List<Unit> workers) {
        if (workers == null || workers.isEmpty()) return null;
        final Colony colony = wl.getColony();
        final GoodsType outputType = (goodsType.isStoredAs())
            ? goodsType.getStoredAs() : goodsType;

        // Avoid some nasty autodestructions by accepting singleton
        // workers that do *something*.
        if (workers.size() == 1) {
            Unit u = workers.get(0);
            if (!wl.canAdd(u)) return null;
            Location oldLoc = u.getLocation();
            GoodsType oldWork = u.getWorkType();
            u.setLocation(wl);
            u.changeWorkType(goodsType);
            int production = wl.getProductionOf(u, goodsType);
            u.setLocation(oldLoc);
            u.changeWorkType(oldWork);
            return (production > 0) ? u : null;
        }

        // Do not mutate the workers list!
        List<Unit> todo = new ArrayList<>(workers);
        List<Unit> best = new ArrayList<>();
        int bestValue = colony.getAdjustedNetProductionOf(outputType);
        Unit special = null;
        best.clear();
        for (Unit u : todo) {
            if (!wl.canAdd(u)) continue;
            Location oldLoc = u.getLocation();
            GoodsType oldWork = u.getWorkType();
            u.setLocation(wl);
            u.changeWorkType(goodsType);

            int value = colony.getAdjustedNetProductionOf(outputType);
            if (value > bestValue) {
                bestValue = value;
                best.clear();
                best.add(u);
                if (u.getType().getExpertProduction() == goodsType) {
                    special = u;
                }
            } else if (value == bestValue && !best.isEmpty()) {
                best.add(u);
                if (u.getType().getExpertProduction() == goodsType) {
                    special = u;
                }
            }

            u.setLocation(oldLoc);
            u.changeWorkType(oldWork);
        }

        switch (best.size()) {
        case 0: return null; // Not good.  No unit improves production.
        case 1: return best.get(0);
        default:todo.clear(); todo.addAll(best); break;
        }
        // Several winners including an expert implies they are all experts.
        if (special != null) return special;

        // Partition units into those that can upgrade-by-experience
        // to the relevant expert (which we favour), those that can
        // upgrade-by-experience in some way but not to the expert
        // (which we avoid), and the rest.  Within the groups, favour
        // those with the most relevant experience and the least irrelevant
        // experience.
        Specification spec = colony.getSpecification();
        UnitType expert = spec.getExpertForProducing(goodsType);
        best.clear();
        bestValue = Integer.MIN_VALUE;
        for (Unit u : todo) {
            boolean relevant = u.getWorkType() == goodsType;
            int score = (relevant) ? u.getExperience() : -u.getExperience();
            if (expert != null
                && u.getType().canBeUpgraded(expert, ChangeType.EXPERIENCE)) {
                score += 10000;
            } else if (expert != null
                && u.getType().canBeUpgraded(null, ChangeType.EXPERIENCE)) {
                score -= 10000;
            }
            if (score > bestValue) {
                best.clear();
                best.add(u);
                bestValue = score;
            } else if (score == bestValue) {
                best.add(u);
            }
        }
        switch (best.size()) {
        case 0: break;
        case 1: return best.get(0);
        default:todo.clear(); todo.addAll(best); break;
        }

        // Use the unit with the least skill, in the hope that
        // remaining experts will be called upon in due course.
        int worstSkill = Integer.MAX_VALUE;
        special = null;
        for (Unit u : todo) {
            if (u.getType().getSkill() < worstSkill) {
                special = u;
                worstSkill = u.getType().getSkill();
            }
        }
        return special;
    }

    /**
     * Equips a unit for a role, trying extra possibilities.
     *
     * @param unit The <code>Unit</code> to equip if possible.
     * @param role The <code>Role</code> for the unit to take.
     * @param colony The <code>Colony</code> storing the equipment.
     * @return True if the unit was equipped.
     */
    private static boolean fullEquipUnit(Specification spec, Unit unit,
                                         Role role, Colony colony) {
        if (role.isOffensive()) {
            for (Role r : unit.getAvailableRoles(spec.getMilitaryRoles())) {
                if (colony.equipForRole(unit, 
                                        r, r.getMaximumCount())) return true;
            }
            return false;
        }
        return colony.equipForRole(unit, role, role.getMaximumCount());
    }

    /**
     * Tries to apply a colony plan given a list of workers.
     *
     * @param workers A list of <code>Unit</code>s to assign.
     * @param preferScout Prefer to make scouts rather than soldiers.
     * @param lb A <code>LogBuilder</code> to log to.
     * @return A scratch colony with the workers in place.
     */
    public Colony assignWorkers(List<Unit> workers, boolean preferScout,
                                LogBuilder lb) {
        final GoodsType foodType = spec().getPrimaryFoodType();

        // Collect the work location plans.  Note that the plans are
        // pre-sorted in order of desirability.
        final List<GoodsType> produce = getPreferredProduction();
        List<WorkLocationPlan> foodPlans = getFoodPlans();
        List<WorkLocationPlan> workPlans = getWorkPlans();

        // Make a scratch colony to work on.
        Colony col = colony.copyColony();
        Tile tile = col.getTile();

        // Replace the given workers with those in the scratch colony.
        List<Unit> otherWorkers = new ArrayList<>(workers);
        workers.clear();
        for (Unit u : otherWorkers) workers.add(col.getCorresponding(u));

        // Move all workers to the tile.
        // Also remove equipment, which is safe because no missionaries
        // or active pioneers should be on the worker list.
        for (Unit u : workers) {
            u.setLocation(tile);
            col.equipForRole(u, spec().getDefaultRole(), 0);
        }

        // Move outdoor experts outside if possible.
        // Prefer scouts in early game if there are very few.
        Role[] outdoorRoles = {
            spec().getRoleWithAbility(Ability.IMPROVE_TERRAIN, null),
            null,
            spec().getRoleWithAbility(Ability.SPEAK_WITH_CHIEF, null)
        };
        if (preferScout) {
            Role tmp = outdoorRoles[1];
            outdoorRoles[1] = outdoorRoles[2];
            outdoorRoles[2] = tmp;
        }
        for (Role outdoorRole : outdoorRoles) {
            for (Unit u : new ArrayList<>(workers)) {
                if (workers.size() <= 1) break;
                Role role = outdoorRole;
                if (role == null) {
                    if ((role = u.getMilitaryRole()) == null) continue;
                }
                if (u.getType() == role.getExpertUnit()
                        && fullEquipUnit(spec(), u, role, col)) {
                    workers.remove(u);
                    lb.add(u.getId(), "(", u.getType().getSuffix(),
                            ") -> ", role.getSuffix(), "\n");
                }
            }
        }

        // Consider the defence situation.
        // FIXME: scan for neighbouring hostiles
        // Favour low-skill/experience units for defenders, order experts
        // in reverse order of their production on the produce-list.
        Comparator<Unit> soldierComparator = new Comparator<Unit>() {
            @Override
            public int compare(Unit u1, Unit u2) {
                int cmp = u1.getSkillLevel() - u2.getSkillLevel();
                if (cmp == 0) {
                    GoodsType g1 = u1.getType().getExpertProduction();
                    GoodsType g2 = u2.getType().getExpertProduction();
                    cmp = ((g2 == null) ? 1 : 0) - ((g1 == null) ? 1 : 0);
                    if (cmp == 0 && g1 != null) {
                        int i = produce.indexOf(g2);
                        cmp = (i < 0) ? produce.size() : i;
                        i = produce.indexOf(g1);
                        cmp -= (i < 0) ? produce.size() : i;
                    }
                }
                if (cmp == 0) {
                    cmp = u1.getExperience() - u2.getExperience();
                }
                return cmp;
            }
        };
        Collections.sort(workers, soldierComparator);
        for (Unit u : new ArrayList<>(workers)) {
            if (workers.size() <= 1) break;
            if (!col.isBadlyDefended()) break;
            Role role = u.getMilitaryRole();
            if (role != null && fullEquipUnit(spec(), u, role, col)) {
                workers.remove(u);
                lb.add(u.getId(), "(", u.getType().getSuffix(), ") -> ",
                       u.getRoleSuffix(), "\n");
            }
        }

        // Greedy assignment of other workers to plans.
        List<AbstractGoods> buildGoods = new ArrayList<>();
        BuildableType build = col.getCurrentlyBuilding();
        if (build != null) buildGoods.addAll(build.getRequiredGoods());
        List<WorkLocationPlan> wlps;
        WorkLocationPlan wlp;
        boolean done = false;
        while (!workers.isEmpty() && !done) {
            // Decide what to produce: set the work location plan to
            // try (wlp), and the list the plan came from so it can
            // be recycled if successful (wlps).
            wlps = null;
            wlp = null;
            if (col.getAdjustedNetProductionOf(foodType) > 0) {
                // Try to produce something.
                wlps = workPlans;
                while (!produce.isEmpty()) {
                    if ((wlp = findPlan(produce.get(0), workPlans)) != null) {
                        break; // Found a plan to try.
                    }
                    produce.remove(0); // Can not produce this goods type
                }
            }

            // See if a plan can be satisfied.
            Unit best;
            WorkLocation wl;
            GoodsType goodsType;
            for (;;) {
                if (wlp == null) { // Time to use a food plan.
                    if (foodPlans.isEmpty()) {
                        lb.add("    Food plans exhausted\n");
                        done = true;
                        break;
                    }
                    wlps = foodPlans;
                    wlp = wlps.get(0);
                }

                String err = null;
                goodsType = wlp.getGoodsType();
                wl = col.getCorresponding(wlp.getWorkLocation());
                best = null;
                lb.add("    ", LogBuilder.wide(2, col.getUnitCount()),
                       ": ", LogBuilder.wide(-15, goodsType.getSuffix()),
                       "@", LogBuilder.wide(25, locationDescription(wl)),
                       " => ");

                if (!wl.canBeWorked()) {
                    err = "can not be worked";
                } else if (wl.isFull()) {
                    err = "full";
                } else if ((best = ColonyPlan.getBestWorker(wl, goodsType,
                                                            workers)) == null) {
                    err = "no worker found";
                }
                if (err != null) {
                    wlps.remove(wlp); // The plan can not be worked, dump it.
                    lb.add(err, "\n");
                    break;
                }

                // Found a suitable worker, place it.
                best.setLocation(wl);

                // Did the placement break the production bonus?
                if (col.getProductionBonus() < 0) {
                    best.setLocation(tile);
                    done = true;
                    lb.add("    broke production bonus\n");
                    break;
                }

                // Is the colony going to starve because of this placement?
                if (col.getAdjustedNetProductionOf(foodType) < 0) {
                    int net = col.getAdjustedNetProductionOf(foodType);
                    int count = col.getGoodsCount(foodType);
                    if (count / -net < PRODUCTION_TURNOVER_TURNS) {
                        // Too close for comfort.  Back out the
                        // placement and try a food plan, unless this
                        // was already a food plan.
                        best.setLocation(tile);
                        wlp = null;
                        if (goodsType.isFoodType()) {
                            lb.add("    starvation (", count, "/", net, ")\n");
                            done = true;
                            break;
                        }
                        lb.add("    would starve (", count, "/", net, ")\n");
                        continue;
                    }
                    // Otherwise tolerate the food stock running down.
                    // Rely on the warehouse-exhaustion code to fire
                    // another rearrangement before units starve.
                }

                // Check if placing the worker will soon exhaust the
                // raw material.  Do not reduce raw materials below
                // what is needed for a building--- e.g. prevent
                // musket production from hogging the tools.
                GoodsType raw = goodsType.getInputType();
                int rawNeeded = buildGoods.stream()
                    .filter(ag -> ag.getType() == raw)
                    .mapToInt(AbstractGoods::getAmount).sum();
                if (raw == null
                    || col.getAdjustedNetProductionOf(raw) >= 0
                    || (((col.getGoodsCount(raw) - rawNeeded)
                            / -col.getAdjustedNetProductionOf(raw))
                        >= PRODUCTION_TURNOVER_TURNS)) {
                    // No raw material problems, the placement
                    // succeeded.  Set the work type, move the
                    // successful goods type to the end of the produce
                    // list for later reuse, remove the worker from
                    // the workers pool, but leave the successful plan
                    // on its list.
                    best.changeWorkType(goodsType);
                    workers.remove(best);
                    lb.add("    ", best.getId(), "(",
                           best.getType().getSuffix(),")\n");
                    if (!goodsType.isFoodType() && produce.remove(goodsType)) {
                        produce.add(goodsType);
                    }
                    break;
                }

                // Yes, we need more of the raw material.  Pull the
                // unit out again and see if we can make more.
                best.setLocation(tile);

                WorkLocationPlan rawWlp = findPlan(raw, workPlans);
                if (rawWlp != null) {
                    // OK, we have an alternate plan.  Put the raw
                    // material at the start of the produce list and
                    // loop trying to satisfy the alternate plan.
                    if (produce.remove(raw)) produce.add(0, raw);
                    wlp = rawWlp;
                    lb.add("    retry with ", raw.getSuffix(), "\n");
                    continue;
                }

                // No raw material available, so we have to give up on
                // both the plan and the type of production.
                // Hopefully the raw production is positive again and
                // we will succeed next time.
                wlps.remove(wlp);
                produce.remove(goodsType);
                lb.add("    needs more ", raw.getSuffix(), "\n");
                break;
            }
        }

        // Put the rest of the workers on the tile.
        for (Unit u : workers) {
            if (u.getLocation() != tile) u.setLocation(tile);
        }

        // Check for failure to assign any workers.  This happens when:
        // - there are no useful food plans
        //   - in which case look for a `harmless' place and add one worker
        // - food is low, and perhaps partly eaten by horses, and no
        //   unit can *improve* production by being added.
        //   - find a place to produce food that at least avoids
        //     starvation and add one worker.
        if (col.getUnitCount() == 0) {
            if (getFoodPlans().isEmpty()) {
locations:      for (WorkLocation wl : col.getAvailableWorkLocations()) {
                    for (Unit u : new ArrayList<>(workers)) {
                        for (GoodsType type : libertyGoodsTypes) {
                            if (wl.canAdd(u)
                                && wl.getPotentialProduction(type,
                                    u.getType()) > 0) {
                                u.setLocation(wl);
                                u.changeWorkType(type);
                                workers.remove(u);
                                break locations;
                            }
                        }
                    }
                }
            } else {
plans:          for (WorkLocationPlan w : getFoodPlans()) {
                    GoodsType goodsType = w.getGoodsType();
                    WorkLocation wl = col.getCorresponding(w.getWorkLocation());
                    for (Unit u : new ArrayList<>(workers)) {
                        GoodsType oldWork = u.getWorkType();
                        u.setLocation(wl);
                        u.changeWorkType(goodsType);
                        if (col.getAdjustedNetProductionOf(foodType) >= 0) {
                            lb.add("    Subsist with ", u, "\n");
                            workers.remove(u);
                            break plans;
                        }
                        u.setLocation(tile);
                        u.changeWorkType(oldWork);
                    }
                }
            }
        }

        // The greedy algorithm works reasonably well, but will
        // misplace experts when they are more productive at the
        // immediately required task than a lesser unit, not knowing
        // that a requirement for their speciality will subsequently
        // follow.  Do a cleanup pass to sort these out.
        List<Unit> experts = new ArrayList<>();
        List<Unit> nonExperts = new ArrayList<>();
        for (Unit u : col.getUnitList()) {
            if (u.getType().getExpertProduction() != null) {
                if (u.getType().getExpertProduction() != u.getWorkType()) {
                    experts.add(u);
                }
            } else {
                nonExperts.add(u);
            }
        }
        int expert = 0;
        while (expert < experts.size()) {
            Unit u1 = experts.get(expert);
            Unit other;
            if ((other = trySwapExpert(u1, experts, col)) != null) {
                lb.add("    Swapped ", u1.getId(), "(",
                    u1.getType().getSuffix(), ") for ", other, "\n");
                experts.remove(u1);
            } else if ((other = trySwapExpert(u1, nonExperts, col)) != null) {
                lb.add("    Swapped ", u1.getId(), "(",
                    u1.getType().getSuffix(), ") for ", other, "\n");
                experts.remove(u1);
            } else {
                expert++;
            }
        }
        for (Unit u : new ArrayList<>(workers)) {
            GoodsType work = u.getType().getExpertProduction();
            if (work != null) {
                Unit other = trySwapExpert(u, col.getUnitList(), col);
                if (other != null) {
                    lb.add("    Swapped ", u.getId(), "(",
                        u.getType().getSuffix(), ") for ", other, "\n");
                    workers.remove(u);
                    workers.add(other);
                }
            }
        }

        // Rearm what remains as far as possible.
        Collections.sort(workers, soldierComparator);
        for (Unit u : new ArrayList<>(workers)) {
            Role role = u.getMilitaryRole();
            if (role == null) continue;
            if (fullEquipUnit(spec(), u, role, col)) {
                lb.add("    ", u.getId(), "(", u.getType().getSuffix(),
                       ") -> ", u.getRoleSuffix(), "\n");
                workers.remove(u);
            } else break;
        }
        for (Unit u : col.getUnitList()) {
            if (!u.hasDefaultRole()) {
                logger.warning("assignWorkers bogus role for " + u);
                u.changeRole(spec().getDefaultRole(), 0);
            }
        }

        // Log and return the scratch colony on success.
        // Otherwise abandon this rearrangement, disposing of the
        // scratch colony and returning null.
        for (Unit u : workers) {
            lb.add("    ", u.getId(), "(", u.getType().getSuffix(),
                   ") -> UNUSED\n");
        }
        if (col.getUnitCount() <= 0) col = null;
        return col;
    }

    /**
     * Gets a concise textual description of a location associated with
     * the colony.  No i18n here, this is for debugging purposes.
     *
     * @param loc The <code>Location</code> to describe.
     * @return The text description.
     */
    private String locationDescription(Location loc) {
        String name = colony.getName() + "-";
        String desc = loc.toShortString();
        if (desc.startsWith(name)) {
            desc = desc.substring(name.length(), desc.length());
        }
        return desc;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final Tile tile = colony.getTile();
        LogBuilder lb = new LogBuilder(256);
        lb.add("ColonyPlan: ", colony,
            " ", colony.getTile(),
            "\nProfile: ", profileType, "\nPreferred production:");
        FreeColObject.logFreeColObjects(getPreferredProduction(), lb);
        lb.add(getBuildableReport(), "Food Plans:\n");
        for (WorkLocationPlan wlp : getFoodPlans()) {
            WorkLocation wl = wlp.getWorkLocation();
            lb.add(locationDescription(wl),
                ": ", wl.getGenericPotential(wlp.getGoodsType()), 
                " ", wlp.getGoodsType().getSuffix(), "\n");
        }
        lb.add("Work Plans:\n");
        for (WorkLocationPlan wlp : getWorkPlans()) {
            WorkLocation wl = wlp.getWorkLocation();
            lb.add(locationDescription(wl),
                ": ", wl.getGenericPotential(wlp.getGoodsType()),
                " ", wlp.getGoodsType().getSuffix(), "\n");
        }
        return lb.toString();
    }
}
