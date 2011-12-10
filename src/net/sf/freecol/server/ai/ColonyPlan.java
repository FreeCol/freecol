/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;
import net.sf.freecol.common.model.WorkLocation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Objects of this class describes the plan the AI has for a
 * <code>Colony</code>.
 *
 * A <code>ColonyPlan</code> contains a list of {@link
 * WorkLocationPlan}s which suggests the food and non-food production
 * of each {@link WorkLocation}, and a list of {@link BuildableType}s
 * to build.  It takes account of the available tiles and building
 * production, but does not make decisions to claim tiles or change
 * the current buildable.  It does takes account of goods present in
 * the colony, and overall colony size but not the exact composition
 * of the units involved.  However there is extensive structure for
 * making a trial assignment of workers in {@link assignWorkers}.
 *
 * {@link AIColony.rearrangeWorkers} is responsible for making
 * the real decisions.
 *
 * @see Colony
 */
public class ColonyPlan {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ColonyPlan.class.getName());

    // Require production plans to always produce an amount exceeding this.
    private static final int LOW_PRODUCTION_THRESHOLD = 1;

    // Number of turns to require production of without exhausting the
    // input goods.
    private static final int PRODUCTION_TURNOVER_TURNS = 5;

    // The profile of the colony (a sort of general flavour).
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

    // Private copy of the AIMain.
    private AIMain aiMain;

    // The colony this AIColony manages.
    private Colony colony;

    // The things to build, and their priority.
    private class BuildPlan {
        public BuildableType type;
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

        public String toString() {
            String t = type.toString();
            return String.format("%s (%1.3f * %1.3f / %1.3f = %1.3f)",
                t.substring(t.lastIndexOf(".")+1),
                weight, support, difficulty, getValue());
        }
    };
    private final List<BuildPlan> buildPlans = new ArrayList<BuildPlan>();

    // Comparator to sort buildable types on their priority in the
    // buildPlan map.
    private final Comparator<BuildPlan> buildPlanComparator
        = new Comparator<BuildPlan>() {
            public int compare(BuildPlan b1, BuildPlan b2) {
                double d = b1.getValue() - b2.getValue();
                return (d > 0.0) ? -1 : (d < 0.0) ? 1 : 0;
            }
        };

    // Plans for work locations available to this colony.
    private final List<WorkLocationPlan> workPlans
        = new ArrayList<WorkLocationPlan>();

    // The goods types to produce.
    private final List<GoodsType> produce = new ArrayList<GoodsType>();

    // Lists of goods types to be produced in this colony.
    // Temporary variables that do not need to be serialized.
    private final List<GoodsType> foodGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> libertyGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> immigrationGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> militaryGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> rawBuildingGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> buildingGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> rawLuxuryGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> luxuryGoodsTypes
        = new ArrayList<GoodsType>();
    private final List<GoodsType> otherRawGoodsTypes
        = new ArrayList<GoodsType>();


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
     * Creates a new <code>ColonyPlan</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an XML-representation
     *            of this object.
     */
    public ColonyPlan(AIMain aiMain, Element element) {
        this.aiMain = aiMain;
        readFromXMLElement(element);
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
     * Gets the <code>Colony</code> this <code>ColonyPlan</code> controls.
     *
     * @return The <code>Colony</code>.
     */
    private Colony getColony() {
        return colony;
    }

    /**
     * Gets the specification.
     *
     * @return The specification.
     */
    private Specification spec() {
        return aiMain.getGame().getSpecification();
    }

    /**
     * Gets the production for a work location of a specified goods type,
     * using the default unit type to avoid considering which unit is
     * to be allocated.  This is thus an approximation to what will
     * finally occur when units are assigned, but it serves for planning
     * purposes.
     *
     * @param wl The <code>WorkLocation</code> where production is to occur.
     * @param goodsType The <code>GoodsType</code> to produce.
     * @return The work location production.
     */
    private int getWorkLocationProduction(WorkLocation wl,
                                          GoodsType goodsType) {
        return wl.getPotentialProduction(spec().getDefaultUnitType(),
                                         goodsType);
    }

    // Public functionality.

    /**
     * Gets the preferred goods to produce.
     *
     * @return A copy of the preferred goods production list in this plan.
     */
    public List<GoodsType> getPreferredProduction() {
        return new ArrayList<GoodsType>(produce);
    }

    /**
     * Gets a copy of the current list of buildable types associated
     * with this <code>ColonyPlan</code>.
     *
     * @return A copy of the of <code>BuildableType</code>s list.
     */
    public List<BuildableType> getBuildableTypes() {
        List<BuildableType> build = new ArrayList<BuildableType>();
        for (BuildPlan b : buildPlans) build.add(b.type);
        return build;
    }

    /**
     * Gets the food-producing and non-autoproducing work location
     * plans associated with this <code>ColonyPlan</code>.
     *
     * @return A list of food producing plans.
     */
    public List<WorkLocationPlan> getFoodPlans() {
        List<WorkLocationPlan> plans = new ArrayList<WorkLocationPlan>();
        for (WorkLocationPlan wlp : workPlans) {
            if (wlp.getGoodsType().isFoodType()
                && !wlp.getWorkLocation().canAutoProduce()) plans.add(wlp);
        }
        return plans;
    }

    /**
     * Gets the non-food-producing/non-autoproducing work location
     * plans associated with this <code>ColonyPlan</code>.
     *
     * @return A list of nonfood producing plans.
     */
    public List<WorkLocationPlan> getWorkPlans() {
        List<WorkLocationPlan> plans = new ArrayList<WorkLocationPlan>();
        for (WorkLocationPlan wlp : workPlans) {
            if (!wlp.getGoodsType().isFoodType()
                && !wlp.getWorkLocation().canAutoProduce()) plans.add(wlp);
        }
        return plans;
    }

    /**
     * Gets the work location plans associated with this
     * <code>ColonyPlan</code> that use a tile that could be improved.
     *
     * @return A copy of the nonfood producing plan list.
     */
    public List<WorkLocationPlan> getTilePlans() {
        List<WorkLocationPlan> plans = new ArrayList<WorkLocationPlan>();
        for (WorkLocationPlan wlp : workPlans) {
            if (wlp.getWorkLocation() instanceof ColonyTile
                && !((ColonyTile)wlp.getWorkLocation()).isColonyCenterTile()
                && ((ColonyTile)wlp.getWorkLocation()).getWorkTile().getOwner()
                    == colony.getOwner()) {
                plans.add(wlp);
            }
        }
        return plans;
    }

    /**
     * Recreates the buildables and work location plans for this
     * colony.
     */
    public void update() {
        UnitType defaultUnitType = spec().getDefaultUnitType();
        GoodsType goodsType;

        // Update the profile type.
        profileType = ProfileType
            .getProfileTypeFromSize(colony.getUnitCount());

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

    /**
     * Gets the goods types required to complete a build.  The list
     * includes the prerequisite raw materials as well as the direct
     * requirements (i.e. hammers, tools).  If enough of a required
     * goods is present in the colony, then that type is not returned.
     * Take care to order types with raw materials first so that we
     * can prioritize gathering what is required before manufacturing.
     *
     * Public for the benefit of the test suite.
     *
     * @param buildable The <code>BuildableType</code> to consider.
     * @return A list of required abstract goods.
     */
    public List<AbstractGoods> getRequiredGoodsTypes(BuildableType buildable) {
        List<AbstractGoods> required = new ArrayList<AbstractGoods>();
        if (buildable != null && buildable.getGoodsRequired() != null) {
            for (AbstractGoods ag : buildable.getGoodsRequired()) {
                int amount = ag.getAmount();
                GoodsType type = ag.getType();
                while (type != null) {
                    if (amount <= colony.getGoodsCount(type)) break; // Shortcut
                    required.add(0, new AbstractGoods(type,
                            amount - colony.getGoodsCount(type)));
                    type = type.getRawMaterial();
                }
            }
        }
        return required;
    }

    /**
     * Refines this plan given the colony choice of what to build.
     *
     * @param build The <code>BuildableType</code> to be built (may be null).
     */
    public void refine(BuildableType build) {
        List<GoodsType> required = new ArrayList<GoodsType>();
        for (AbstractGoods ag : getRequiredGoodsTypes(build)) {
            required.add(ag.getType());
        }
        Map<GoodsType, List<WorkLocationPlan>> suppressed
            = new HashMap<GoodsType, List<WorkLocationPlan>>();

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
        // TODO: generalize this further to make tools for pioneers.
        //
        List<WorkLocationPlan> plans
            = new ArrayList<WorkLocationPlan>(workPlans);
        int offset = 0;
        for (int i = 0; i < plans.size(); i++) {
            List<WorkLocationPlan> wls;
            WorkLocationPlan wlp = plans.get(i);
            GoodsType g = wlp.getGoodsType();
            if ((rawBuildingGoodsTypes.contains(g)
                    && !required.contains(g.getProducedMaterial()))
                || (buildingGoodsTypes.contains(g)
                    && !required.contains(g))) {
                workPlans.remove(i - offset);
                offset++;
                wls = suppressed.get(g);
                if (wls == null) wls = new ArrayList<WorkLocationPlan>();
                wls.add(0, wlp); // reverses list
                suppressed.put(g, wls);
                produce.remove(g);
                logger.finest("At " + colony.getName()
                    + " suppress production of " + g);
            } else if (g.isRefined()
                && (rawBuildingGoodsTypes.contains(g.getRawMaterial())
                    || buildingGoodsTypes.contains(g.getRawMaterial()))) {
                int n = 0, idx = produce.indexOf(g);
                for (GoodsType type = g.getRawMaterial(); type != null;
                     type = type.getRawMaterial()) {
                    if ((wls = suppressed.get(type)) == null) break;
                    if (colony.getGoodsCount(type)
                        >= GoodsContainer.CARGO_SIZE/2) break;
                    n += wls.size();
                    while (!wls.isEmpty()) {
                        // reverses again when adding, cancelling reversal above
                        workPlans.add(i - offset, wls.remove(0));
                    }
                    produce.add(idx, type);
                    logger.finest("At " + colony.getName()
                        + " restore production of " + type);
                }
                offset -= n;
            }
        }
    }

    /**
     * Creates a map of potential production of all goods types
     * from all available work locations using the default unit type.
     * Includes non-workable locations (e.g. chapel, colony-center-tile)
     * as their production can influence the choice of goods to produce.
     *
     * @return The map of potential production.
     */
    private Map<GoodsType, Map<WorkLocation, Integer>> createProductionMap() {
        Map<GoodsType, Map<WorkLocation, Integer>> production
            = new HashMap<GoodsType, Map<WorkLocation, Integer>>();
        for (WorkLocation wl : colony.getAvailableWorkLocations()) {
            for (GoodsType g : spec().getGoodsTypeList()) {
                int p = getWorkLocationProduction(wl, g);
                if (p > 0) {
                    Map<WorkLocation, Integer> m = production.get(g);
                    if (m == null) {
                        m = new HashMap<WorkLocation, Integer>();
                        production.put(g, m);
                    }
                    m.put(wl, new Integer(p));
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
        for (GoodsType g : new ArrayList<GoodsType>(production.keySet())) {
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
                && g.getRawMaterial().isRawBuildingMaterial()) {
                buildingGoodsTypes.add(g);
            } else if (g.isNewWorldGoodsType()) {
                rawLuxuryGoodsTypes.add(g);
            } else if (g.isRefined()
                && g.getRawMaterial().isNewWorldGoodsType()) {
                luxuryGoodsTypes.add(g);
            } else if (g.isFarmed()) {
                otherRawGoodsTypes.add(g);
            } else { // Not interested in this goods type.
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
     * @param production The production map.
     */
    private void updateRawMaterials(Map<GoodsType, Map<WorkLocation, Integer>> production) {
        Market market = colony.getOwner().getMarket();
        GoodsType primaryRawMaterial = null;
        GoodsType secondaryRawMaterial = null;
        int primaryValue = -1;
        int secondaryValue = -1;

        produce.clear();
        List<GoodsType> rawMaterials
            = new ArrayList<GoodsType>(rawLuxuryGoodsTypes);
        rawMaterials.addAll(otherRawGoodsTypes);
        for (GoodsType g : rawMaterials) {
            int value = 0;
            for (Entry<WorkLocation, Integer> e
                     : production.get(g).entrySet()) {
                value += e.getValue().intValue();
            }
            if (value <= LOW_PRODUCTION_THRESHOLD) {
                production.remove(g);
                continue;
            }
            if (market != null) {
                // If the market is available, weight by sale price of
                // the material, or if it is the raw material for a
                // refined goods type, the average of the raw and
                // refined goods prices.
                if (g.getProducedMaterial() == null) {
                    value *= market.getSalePrice(g, 1);
                } else if (production.containsKey(g.getProducedMaterial())) {
                    value *= (market.getSalePrice(g, 1)
                        + market.getSalePrice(g.getProducedMaterial(), 1)) / 2;
                }
            }
            if (value > secondaryValue && secondaryRawMaterial != null) {
                production.remove(secondaryRawMaterial);
                production.remove(secondaryRawMaterial.getProducedMaterial());
                if (rawLuxuryGoodsTypes.contains(secondaryRawMaterial)) {
                    rawLuxuryGoodsTypes.remove(secondaryRawMaterial);
                    luxuryGoodsTypes.remove(secondaryRawMaterial.getProducedMaterial());
                } else if (rawMaterials.contains(otherRawGoodsTypes)) {
                    rawMaterials.remove(otherRawGoodsTypes);
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
            if (primaryRawMaterial.getProducedMaterial() != null) {
                produce.add(primaryRawMaterial.getProducedMaterial());
            }
            if (secondaryRawMaterial != null) {
                produce.add(secondaryRawMaterial);
                if (secondaryRawMaterial.getProducedMaterial() != null) {
                    produce.add(secondaryRawMaterial.getProducedMaterial());
                }
            }
        }
    }

    // Relative weights of the various building categories.
    // TODO: split out/parameterize into a `building strategy'
    //
    // BuildableTypes that improve breeding.
    private static final double BREEDING_WEIGHT    = 0.1;
    // BuildableTypes that improve building production.
    private static final double BUILDING_WEIGHT    = 1.0;
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
    private static final double STORAGE_WEIGHT     = 0.9;
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
        for (BuildPlan bp : buildPlans) {
            if (bp.type == type) return bp;
        }
        return null;
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
        boolean ret = false;
        if (goodsType.isMilitaryGoods()) {
            ret = prioritize(type, MILITARY_WEIGHT,
                1.0/*FIXME: amount present wrt amount to equip*/);
        } else if (goodsType.isBuildingMaterial()) {
            ret = prioritize(type, BUILDING_WEIGHT,
                1.0/*FIXME: need for this type*/);
        } else if (goodsType.isLibertyType()) {
            ret = prioritize(type, LIBERTY_WEIGHT,
                (colony.getSoL() >= 100) ? 0.01 : 1.0);
        } else if (goodsType.isImmigrationType()) {
            ret = prioritize(type, IMMIGRATION_WEIGHT,
                1.0/*FIXME: Brewster?*/);
        } else if (produce.contains(goodsType)) {
            double f = 0.1 * colony.getProductionOf(goodsType.getRawMaterial());
            ret = prioritize(type, PRODUCTION_WEIGHT,
                f/*FIXME: improvement?*/);
        }
        return ret;
    }
            
    /**
     * Updates the build plans for this colony.
     */
    private void updateBuildableTypes() {
        buildPlans.clear();

        int maxLevel;
        switch (profileType) {
        case OUTPOST: return; // No building in outposts
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
            if (!type.getModifierSet(Modifier.DEFENCE).isEmpty()) {
                prioritize(type, FORTIFY_WEIGHT,
                    1.0/*FIXME: 0 if FF underway*/);
            }
            if (type.hasAbility(Ability.EXPORT)) {
                prioritize(type, EXPORT_WEIGHT,
                    1.0/*FIXME: weigh production v transport*/);
            }

            // Skip later stage buildings for smaller settlements.
            if (type.getLevel() > maxLevel) continue;

            // Scale docks by the improvement available to the food supply.
            if (type.hasAbility(Ability.PRODUCE_IN_WATER)
                && !colony.hasAbility(Ability.PRODUCE_IN_WATER)
                && colony.getTile().isCoast()) {
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
                if (seaFood > 0) {
                    prioritize(type, FISH_WEIGHT,
                        seaFood / (double)(seaFood + landFood));
                }
            }

            if (type.hasAbility(Ability.BUILD)) {
                double support = 1.0;
                for (Ability a : type.getFeatureContainer().getAbilitySet(Ability.BUILD)) {
                    List<Scope> scopes = a.getScopes();
                    if (scopes != null && !scopes.isEmpty()) support = 0.1;
                }
                prioritize(type, BUILDING_WEIGHT,
                    support/*FIXME: need for the thing now buildable*/);
            }

            if (type.hasAbility(Ability.CAN_TEACH)) {
                prioritize(type, TEACH_WEIGHT,
                    1.0/*FIXME: #students, #specialists here, #wanted*/);
            }

            if (type.hasAbility(Ability.REPAIR_UNITS)) {
                prioritize(type, REPAIR_WEIGHT,
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
                    if (!type.getModifierSet(g.getId()).isEmpty()) {
                        if (!prioritizeProduction(type, g)) {
                            expectFail = true;
                        }
                    }
                }
                // Hacks.  No good way to make this really generic.
                if (!type.getModifierSet("model.modifier.warehouseStorage")
                    .isEmpty()) {
                    prioritize(type, STORAGE_WEIGHT,
                        1.0/*FIXME: amount of goods*/);
                }
                if (!type.getModifierSet("model.modifier.breedingDivisor")
                    .isEmpty()) {
                    prioritize(type, BREEDING_WEIGHT,
                        1.0/*FIXME: horses present?*/);
                }
            }

            if (findBuildPlan(type) == null && !expectFail) {
                logger.warning("No building priority found for: " + type);
            }
        }
        
        double wagonNeed = 0.0;
        if (!colony.isConnected()) { // Inland colonies need transportation
            int wagons = 0;
            for (Unit u : player.getUnits()) {
                if (u.hasAbility(Ability.CARRY_GOODS)
                    && !u.isNaval()) wagons++;
            }
            int inland = 0;
            for (Colony c : player.getColonies()) {
                if (!c.isConnected()) inland++;
            }
            if (inland > wagons) {
                wagonNeed = (double)(inland - wagons) / inland;
            }
        }
        for (UnitType unitType : spec().getUnitTypeList()) {
            if (!colony.canBuild(unitType)) continue;
            if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
                ; // TODO: decide to build a ship
            } else if (unitType.getDefence() > UnitType.DEFAULT_DEFENCE) {
                if (aiMain.getAIColony(colony).isBadlyDefended()) {
                    prioritize(unitType, DEFENCE_WEIGHT,
                        1.0/*FIXME: how badly defended?*/);
                }
            } else if (unitType.hasAbility(Ability.CARRY_GOODS)) {
                if (wagonNeed > 0.0) {
                    prioritize(unitType, TRANSPORT_WEIGHT,
                        wagonNeed/*FIXME: type.getSpace()*/);
                }
            }
        }

        // Weight by lower required goods.
        for (BuildPlan bp : buildPlans) {
            double difficulty = 0.0f;
            for (AbstractGoods ag : bp.type.getGoodsRequired()) {
                GoodsType g = ag.getType();
                int need = ag.getAmount() - colony.getGoodsCount(g);
                if (need > 0) {
                    // Penalize building with type that can not be
                    // made locally.
                    double f = (produce.contains(g.getRawMaterial())) ? 1.0
                        : 5.0;
                    difficulty += need * f;
                }                    
            }
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
        for (GoodsType g : production.keySet()) {
            // Do not make plans to produce into a full warehouse.
            if (g.isStorable()
                && colony.getGoodsCount(g) >= colony.getWarehouseCapacity()
                && !g.limitIgnored()) continue;

            for (WorkLocation wl : production.get(g).keySet()) {
                if (wl.canBeWorked() || wl.canAutoProduce()) {
                    workPlans.add(new WorkLocationPlan(getAIMain(), wl, g));
                }
            }
        }

        // Now we have lots of plans, determine what goods to produce.
        updateProductionList(production);

        // Filter out plans that can not use a unit.
        List<WorkLocationPlan> oldPlans
            = new ArrayList<WorkLocationPlan>(workPlans);
        workPlans.clear();
        for (WorkLocationPlan wlp : oldPlans) {
            if (wlp.getWorkLocation().canBeWorked()) workPlans.add(wlp);
        }

        // Sort the work plans by earliest presence in the produce
        // list, and then by amount.  If the type of goods produced is
        // not on the produce list, then make sure such plans sort to
        // the end, except for food plans.
        Collections.sort(workPlans, new Comparator<WorkLocationPlan>() {
                public int compare(WorkLocationPlan w1, WorkLocationPlan w2) {
                    GoodsType g1 = w1.getGoodsType();
                    GoodsType g2 = w2.getGoodsType();
                    int i1 = produce.indexOf(g1);
                    int i2 = produce.indexOf(g2);
                    if (i1 < 0 && !g1.isFoodType()) i1 = 99999;
                    if (i2 < 0 && !g2.isFoodType()) i2 = 99999;
                    int cmp = i1 - i2;
                    if (cmp == 0) {
                        cmp = getWorkLocationProduction(w2.getWorkLocation(), g2)
                            - getWorkLocationProduction(w1.getWorkLocation(), g1);
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
                public int compare(GoodsType g1, GoodsType g2) {
                    int p1 = 0;
                    for (Integer i : production.get(g1).values()) {
                        p1 += i.intValue();
                    }
                    int p2 = 0;
                    for (Integer i : production.get(g2).values()) {
                        p2 += i.intValue();
                    }
                    return p2 - p1;
                }
            };
        List<GoodsType> toAdd = new ArrayList<GoodsType>();
        
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
                GoodsType raw = g.getRawMaterial();
                if (colony.getGoodsCount(raw) >= GoodsContainer.CARGO_SIZE/2
                    || production.containsKey(raw)) {
                    toAdd.add(g);
                }
            }
        }
        Collections.sort(toAdd, new Comparator<GoodsType>() {
                public int compare(GoodsType g1, GoodsType g2) {
                    int i1 = rawBuildingGoodsTypes.indexOf(g1.getRawMaterial());
                    int i2 = rawBuildingGoodsTypes.indexOf(g2.getRawMaterial());
                    return i1 - i2;
                }
            });
        for (int i = toAdd.size()-1; i >= 0; i--) {
            GoodsType make = toAdd.get(i);
            GoodsType raw = make.getRawMaterial();
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
     * Equips a unit.
     *
     * @param unit The <code>Unit</code>s to equip if possible.
     * @param type The <code>EquipmentType</code> to provide.
     * @param colony The <code>Colony</code> that provides the equipment.
     * @return True if the unit was eqipped.
     */
    private boolean equipUnit(Unit unit, EquipmentType type, Colony colony) {
        if (!unit.isPerson()
            || !colony.canProvideEquipment(type)
            || !unit.canBeEquippedWith(type)) return false;
        unit.setLocation(colony.getTile());
        unit.changeEquipment(type, 1);
        colony.addEquipmentGoods(type, -1);
        return true;
    }

    /**
     * Equips any experts with implicitly out-of-colony jobs
     * (pioneers, scouts or veteran soldiers).
     *
     * @param workers A list of <code>Unit</code>s to scan.
     * @param scratch A scratch <code>Colony</code> to use to find equipment.
     */
    private void equipOutdoorExperts(List<Unit> workers, Colony scratch) {
        // TODO: make the equipment types generic
        final EquipmentType horsesEqType
            = spec().getEquipmentType("model.equipment.horses");
        final EquipmentType musketsEqType
            = spec().getEquipmentType("model.equipment.muskets");
        final EquipmentType toolsEqType
            = spec().getEquipmentType("model.equipment.tools");
        final Tile tile = scratch.getTile();

        int i = 0;
        while (i < workers.size()) {
            if (workers.size() <= 1) break;
            Unit u = workers.get(i);
            if (u.hasAbility(Ability.EXPERT_PIONEER)
                && equipUnit(u, toolsEqType, scratch)) {
                workers.remove(u);
                continue;
            }
            if (u.hasAbility(Ability.EXPERT_SOLDIER)
                && equipUnit(u, musketsEqType, scratch)) {
                equipUnit(u, horsesEqType, scratch);
                workers.remove(u);
                continue;
            }
            // Scout *after* soldier.
            // TODO: do it the other way round if there are no scouts.
            if (u.hasAbility(Ability.EXPERT_SCOUT)
                && equipUnit(u, horsesEqType, scratch)) {
                workers.remove(u);
                continue;
            }
            i++;
        }
    }

    /**
     * Tries to swap an expert unit for another doing its job.
     *
     * @param expert The expert <code>Unit</code>.
     * @param others A list of other <code>Unit</code>s to test against.
     * @return True if the expert was moved to its speciality.
     */
    private boolean trySwapExpert(Unit expert, List<Unit> others) {
        GoodsType work = expert.getType().getExpertProduction();
        GoodsType oldWork = expert.getWorkType();
        for (int i = 0; i < others.size(); i++) {
            Unit other = others.get(i);
            if (!other.isPerson()) continue;
            if (other.getWorkType() == work
                && other.getType().getExpertProduction() != work) {
                Location l1 = expert.getLocation();
                Location l2 = other.getLocation();
                other.setLocation(colony.getTile());
                expert.setLocation(l2);
                expert.setWorkType(work);
                other.setLocation(l1);
                if (oldWork != null) other.setWorkType(oldWork);
                return true;
            }
        }
        return false;
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
        for (WorkLocationPlan wlp : plans) {
            if (wlp.getGoodsType() == goodsType) return wlp;
        }
        return null;
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
    static public Unit getBestWorker(WorkLocation wl, GoodsType goodsType,
                                     List<Unit> workers) {
        if (workers == null || workers.isEmpty()) return null;
        Colony colony = wl.getColony();
        // Do not mutate the workers list!
        List<Unit> todo = new ArrayList<Unit>(workers);
        List<Unit> best = new ArrayList<Unit>();
        Unit special = null;
        GoodsType outputType = (goodsType.isStoredAs()) 
            ? goodsType.getStoredAs() : goodsType;
        int bestValue;

        best.clear();
        bestValue = colony.getNetProductionOf(outputType);
        for (Unit u : todo) {
            if (!wl.canAdd(u)) continue;
            Location oldLoc = u.getLocation();
            GoodsType oldWork = u.getWorkType();
            u.setLocation(wl);
            u.setWorkType(goodsType);

            int value = colony.getNetProductionOf(outputType);
            if (value > bestValue) {
                bestValue = value;
                best.clear();
                best.add(u);
                if (u.getType().getExpertProduction() == goodsType) special = u;
            } else if (value == bestValue && !best.isEmpty()) {
                best.add(u);
            }

            u.setLocation(oldLoc);
            u.setWorkType(oldWork);
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
     * Tries to apply a colony plan given a list of workers.
     *
     * @param colonyPlan The <code>ColonyPlan</code> to apply.
     * @param workers A list of <code>Unit</code>s to assign.
     * @return A scratch colony with the workers in place.
     */
    public Colony assignWorkers(List<Unit> workers) {
        final GoodsType foodType = spec().getPrimaryFoodType();
        final int maxUnitFood = colony.getOwner().getMaximumFoodConsumption();
        final EquipmentType horsesEqType
            = spec().getEquipmentType("model.equipment.horses");
        final EquipmentType musketsEqType
            = spec().getEquipmentType("model.equipment.muskets");

        // Collect the work location plans.  Note that the plans are
        // pre-sorted in order of desirability.
        List<GoodsType> produce = getPreferredProduction();
        List<WorkLocationPlan> foodPlans = getFoodPlans();
        List<WorkLocationPlan> workPlans = getWorkPlans();

        // Make a scratch colony to work on.
        Colony scratch = colony.getScratchColony();
        Tile tile = scratch.getTile();

        // Move all workers to the tile, removing storable equipment.
        for (Unit u : workers) {
            TypeCountMap<EquipmentType> equipment = u.getEquipment();
            Set<EquipmentType> keys = equipment.keySet();
            u.setLocation(tile);
            for (EquipmentType e : new ArrayList<EquipmentType>(keys)) {
                int n = equipment.getCount(e);
                u.changeEquipment(e, -n);
                scratch.addEquipmentGoods(e, n);
            }
        }

        // Move outdoor experts outside if possible.
        equipOutdoorExperts(workers, scratch);

        // Consider the defence situation.
        // TODO: scan for neighbouring hostiles
        Collections.sort(workers, Unit.getSkillLevelComparator());
        for (Unit u : new ArrayList<Unit>(workers)) {
            if (workers.size() <= 1
                || !colony.canProvideEquipment(musketsEqType)) break;
            
            // TODO: this `adequate defence' heuristic is still
            // quite experimental.  When ready it should be moved to
            // isBadlyDefended, but there are problems.
            // 1. The wider-AI tends to supply colonies with all sorts
            //    of unnecessary military units --- sort this out.
            // 2. isBadlyDefended is an AIColony routine, which ColonyPlan
            //    can not see.  Move to the Colony?
            float defence = colony.getTotalDefencePower();
            final float perUnit = 1.25f;
            final float offset = -2.5f;
            float wanted = perUnit * workers.size() + offset;
            //System.err.println("DEFENCE " + colony.getName() + " defence=" + defence + " workers=" + workers.size() + " wanted=" + wanted + " OK=" + (defence>=wanted));
            //for (Unit z : colony.getTile().getUnitList()) {
            //    if (z.isDefensiveUnit()) System.err.println("  " + z);
            //}
            if (defence >= wanted) break;

            if (equipUnit(u, musketsEqType, scratch)) {
                equipUnit(u, horsesEqType, scratch);
                workers.remove(u);
            }
        }

        // Greedy assignment of other workers to plans.
        List<WorkLocationPlan> wlps;
        WorkLocationPlan wlp;
        boolean colonyFull = false;
        while (!workers.isEmpty() && !colonyFull) {
            // Decide what to produce: set the work location plan to
            // try (wlp), and the list the plan came from so it can
            // be recycled if successful (wlps).
            wlps = null;
            wlp = null;
            if (scratch.getNetProductionOf(foodType) >= maxUnitFood) {
                // Enough food, try to produce something.
                while (!produce.isEmpty() && wlp == null) {
                    wlp = findPlan(produce.get(0), workPlans);
                    if (wlp == null) {
                        // No plan found, we can no longer produce
                        // this goods type.
                        produce.remove(0);
                    }
                }
            }
            if (wlp != null) {
                wlps = workPlans;
            } else { // Time to use a food plan.
                if (foodPlans.isEmpty()) break;
                wlps = foodPlans;
                wlp = wlps.get(0);
            }

            // See if a plan can be satisfied.
            Unit best = null;
            WorkLocation wl;
            GoodsType goodsType;
            for (;;) {
                goodsType = wlp.getGoodsType();
                wl = wlp.getWorkLocation();
                wl = scratch.getCorrespondingWorkLocation(wl);
                best = null;

                if (!wl.canBeWorked()
                    || wl.isFull()
                    || (best = getBestWorker(wl, goodsType, workers)) == null) {
                    // The plan can not be worked, dump it.
                    wlps.remove(wlp);
                    break;
                }

                // Found a suitable worker, place it.
                best.setLocation(wl);

                // Did the placement break the production bonus?
                if (scratch.getProductionBonus() < 0) {
                    best.setLocation(tile);
                    colonyFull = true;
                    break;
                }

                // Check if placing the worker will soon exhaust the
                // raw material.
                GoodsType raw = goodsType.getRawMaterial();
                if (raw == null
                    || scratch.getNetProductionOf(raw) >= 0
                    || ((scratch.getGoodsCount(raw)
                            / -scratch.getNetProductionOf(raw))
                        >= PRODUCTION_TURNOVER_TURNS)) {
                    // No raw material problems, the placement
                    // succeeded.  Set the work type, move the
                    // successful goods type to the end of the produce
                    // list for later reuse, remove the worker from
                    // the workers pool, but leave the successful plan
                    // on its list.
                    best.setWorkType(goodsType);
                    workers.remove(best);
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
                    continue;
                }

                // No raw material available, so we have to give up on
                // both the plan and the type of production.
                // Hopefully the raw production is positive again and
                // we will succeed next time.
                wlps.remove(wlp);
                produce.remove(goodsType);
                break;
            }
        }

        // Put the rest of the workers on the tile.
        for (Unit u : workers) {
            if (u.getLocation() != tile) u.setLocation(tile);
        }

        // The greedy algorithm works reasonably well, but will
        // misplace experts when they are more productive at the
        // immediately required task than a lesser unit, not knowing
        // that a requirement for their speciality will subsequently
        // follow.  Do a cleanup pass to sort these out.
        List<Unit> experts = new ArrayList<Unit>();
        List<Unit> nonExperts = new ArrayList<Unit>();
        for (Unit u : scratch.getUnitList()) {
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
            if (trySwapExpert(u1, experts)
                || trySwapExpert(u1, nonExperts)) {
                experts.remove(u1);
            } else {
                expert++;
            }
        }
        for (Unit u : tile.getUnitList()) {
            GoodsType work = u.getType().getExpertProduction();
            if (work != null) trySwapExpert(u, scratch.getUnitList());
        }

        // Rearm what remains as far as possible.
        workers.clear();
        workers.addAll(tile.getUnitList());
        Collections.sort(workers, Unit.getSkillLevelComparator());
        for (Unit u : workers) {
            if (equipUnit(u, musketsEqType, scratch)) {
                equipUnit(u, horsesEqType, scratch);
            }
        }

        return scratch;
    }


    public String getBuildableReport() {
        String ret = "Buildables:\n";
        for (BuildPlan b : buildPlans) ret += b.toString() + "\n";
        return ret;
    }

    /**
     * {@inherit-doc}
     */
    public String toString() {
        final Tile tile = colony.getTile();
        final StringBuilder sb = new StringBuilder();
        sb.append("ColonyPlan: " + colony.getName()
            + " " + colony.getTile().getPosition()
            + "\nProfile: " + profileType.toString()
            + "\nPreferred production:\n");
        for (GoodsType goodsType : getPreferredProduction()) {
            sb.append(goodsType.toString().substring(12) + "\n");
        }
        sb.append(getBuildableReport());
        sb.append("Food Plans:\n");
        for (WorkLocationPlan wlp : getFoodPlans()) {
            WorkLocation wl = wlp.getWorkLocation();
            String wlStr = (wl instanceof Building)
                ? ((Building)wl).getType().toString().substring(15)
                : (wl instanceof ColonyTile)
                ? tile.getDirection(((ColonyTile)wl).getWorkTile()).toString()
                : wl.getId();
            sb.append(wlStr
                + ": " + getWorkLocationProduction(wl, wlp.getGoodsType())
                + " " + wlp.getGoodsType().toString().substring(12)
                + "\n");
        }
        sb.append("Work Plans:\n");
        for (WorkLocationPlan wlp : getWorkPlans()) {
            WorkLocation wl = wlp.getWorkLocation();
            String wlStr = (wl instanceof Building)
                ? ((Building)wl).getType().toString().substring(15)
                : (wl instanceof ColonyTile)
                ? tile.getDirection(((ColonyTile)wl).getWorkTile()).toString()
                : wl.getId();
            sb.append(wlStr
                + ": " + getWorkLocationProduction(wl, wlp.getGoodsType())
                + " " + wlp.getGoodsType().toString().substring(12)
                + "\n");
        }
        return sb.toString();
    }


    // Serialization

    /**
     * Creates an XML-representation of this object.
     *
     * @param document The <code>Document</code> in which the
     *            XML-representation should be created.
     * @return The XML-representation.
     */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());
        element.setAttribute(FreeColObject.ID_ATTRIBUTE, colony.getId());
        return element;
    }

    /**
     * Updates this object from an XML-representation of a
     * <code>ColonyPlan</code>.
     *
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        String colonyId = element.getAttribute(FreeColObject.ID_ATTRIBUTE);
        colony = (Colony) getAIMain().getFreeColGameObject(colonyId);

        // TODO: serialize profile?
        profileType = ProfileType
            .getProfileTypeFromSize(colony.getUnitCount());
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "colonyPlan"
     */
    public static String getXMLElementTagName() {
        return "colonyPlan";
    }
}
