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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * Represents a building in a colony.
 */
public class Building extends WorkLocation
    implements Named, Consumer {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Building.class.getName());

    public static final String UNIT_CHANGE = "UNIT_CHANGE";

    /** The type of building. */
    protected BuildingType buildingType;


    /**
     * Constructor for ServerBuilding.
     *
     * @param game The enclosing <code>Game</code>.
     * @param colony The <code>Colony</code> in which this building is located.
     * @param type The <code>BuildingType</code> of building.
     */
    protected Building(Game game, Colony colony, BuildingType type) {
        super(game);

        this.colony = colony;
        this.buildingType = type;
        // set production type to default value
        updateProductionType();
    }

    /**
     * Create a new <code>Building</code> with the given identifier.
     * The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing <code>Game</code>.
     * @param id The object identifier.
     */
    public Building(Game game, String id) {
        super(game, id);
    }


    /**
     * Gets the type of this building.
     *
     * @return The building type.
     */
    public BuildingType getType() {
        return buildingType;
    }

    /**
     * Changes the type of the Building.  The type of a building may
     * change when it is upgraded or damaged.
     *
     * -til: If this is a defensive building.
     *
     * @see #upgrade
     * @see #downgrade
     * @param newBuildingType The new <code>BuildingType</code>.
     * @return A list of units present that need to be removed.
     */
    private List<Unit> setType(final BuildingType newBuildingType) {
        // remove features from current type
        final Colony colony = getColony();
        colony.removeFeatures(buildingType);
        List<Unit> eject = new ArrayList<>();

        if (newBuildingType != null) {
            buildingType = newBuildingType;

            // change default production type
            updateProductionType();

            // add new features and abilities from new type
            colony.addFeatures(buildingType);

            // Colonists which can't work here must be put outside
            for (Unit unit : getUnitList()) {
                if (!canAddType(unit.getType())) eject.add(unit);
            }
        }

        // Colonists exceding units limit must be put outside
        int extra = getUnitCount() - getUnitCapacity() - eject.size();
        for (Unit unit : getUnitList()) {
            if (extra <= 0) break;
            if (!eject.contains(unit)) {
                eject.add(unit);
                extra -= 1;
            }
        }

        return eject;
    }

    /**
     * Gets the level of this building.
     * Delegates to type.
     *
     * @return The building level.
     */
    public int getLevel() {
        return getType().getLevel();
    }

    /**
     * Does this building have a higher level?
     *
     * @return True if this <code>Building</code> can have a higher level.
     */
    public boolean canBuildNext() {
        return getColony().canBuild(getType().getUpgradesTo());
    }

    /**
     * Can this building can be damaged?
     *
     * @return True if this building can be damaged.
     */
    public boolean canBeDamaged() {
        return !getType().isAutomaticBuild()
            && !getColony().isAutomaticBuild(getType());
    }

    /**
     * Downgrade this building.
     *
     * -til: If this is a defensive building.
     *
     * @return A list of units to eject (usually empty) if the
     *     building was downgraded, or null on failure.
     */
    public List<Unit> downgrade() {
        if (!canBeDamaged()) return null;
        List<Unit> ret = setType(getType().getUpgradesFrom());
        getColony().invalidateCache();
        return ret;
    }

    /**
     * Upgrade this building to next level.
     *
     * -til: If this is a defensive building.
     *
     * @return A list of units to eject (usually empty) if the
     *     building was upgraded, or null on failure.
     */
    public List<Unit> upgrade() {
        if (!canBuildNext()) return null;
        List<Unit> ret = setType(getType().getUpgradesTo());
        getColony().invalidateCache();
        return ret;
    }

    /**
     * Can a particular type of unit be added to this building?
     *
     * @param unitType The <code>UnitType</code> to check.
     * @return True if unit type can be added to this building.
     */
    public boolean canAddType(UnitType unitType) {
        return canBeWorked() && getType().canAdd(unitType);
    }

    /**
     * Convenience function to extract a goods amount from a list of
     * available goods.
     *
     * @param type The <code>GoodsType</code> to extract the amount for.
     * @param available The list of available goods to query.
     * @return The goods amount, or zero if none found.
     */
    private int getAvailable(GoodsType type, List<AbstractGoods> available) {
        return AbstractGoods.getCount(type, available);
    }

    /**
     * Gets the production information for this building taking account
     * of the available input and output goods.
     *
     * @param inputs The input goods available.
     * @param outputs The output goods already available in the colony,
     *     necessary in order to avoid excess production.
     * @return The production information.
     * @see ProductionCache#update
     */
    public ProductionInfo getAdjustedProductionInfo(List<AbstractGoods> inputs,
                                                    List<AbstractGoods> outputs) {
        ProductionInfo result = new ProductionInfo();
        if (!hasOutputs()) return result;
        final Specification spec = getSpecification();
        final Turn turn = getGame().getTurn();
        final boolean avoidOverflow
            = hasAbility(Ability.AVOID_EXCESS_PRODUCTION);
        final int capacity = getColony().getWarehouseCapacity();
        // Calculate two production ratios, the minimum (and actual)
        // possible multiplier between the nominal input and output
        // goods and the amount actually consumed and produced, and
        // the maximum possible ratio that would apply but for
        // circumstances such as limited input availability.
        double maximumRatio = 0.0, minimumRatio = Double.MAX_VALUE;

        // First, calculate the nominal production ratios.
        if (canAutoProduce()) {
            // Autoproducers are special
            for (AbstractGoods output : getOutputs()) {
                if (output.getAmount() <= 0) continue;
                final GoodsType goodsType = output.getType();
                int available = getColony().getGoodsCount(goodsType);
                if (available >= capacity) {
                    minimumRatio = maximumRatio = 0.0;
                } else {
                    int divisor = (int)getType().applyModifiers(0f, turn,
                        Modifier.BREEDING_DIVISOR);
                    int factor = (int)getType().applyModifiers(0f, turn,
                        Modifier.BREEDING_FACTOR);
                    int production = (available < goodsType.getBreedingNumber()
                        || divisor <= 0) ? 0
                        // Deliberate use of integer division
                        : ((available - 1) / divisor + 1) * factor;
                    double newRatio = (double)production / output.getAmount();
                    minimumRatio = Math.min(minimumRatio, newRatio);
                    maximumRatio = Math.max(maximumRatio, newRatio);
                }
            }
        } else {
            for (AbstractGoods output : getOutputs()) {
                final GoodsType goodsType = output.getType();
                float production = getUnitList().stream()
                    .mapToInt(u -> getUnitProduction(u, goodsType)).sum();
                // Unattended production always applies for buildings!
                production += getBaseProduction(null, goodsType, null);
                production = applyModifiers(production, turn,
                    getProductionModifiers(goodsType, null));
                production = (int)Math.floor(production);
                // Beware!  If we ever unify this code with ColonyTile,
                // ColonyTiles have outputs with zero amount.
                double newRatio = production / output.getAmount();
                minimumRatio = Math.min(minimumRatio, newRatio);
                maximumRatio = Math.max(maximumRatio, newRatio);
            }
        }

        // Then reduce the minimum ratio if some input is in short supply.
        for (AbstractGoods input : getInputs()) {
            long required = (long)Math.floor(input.getAmount() * minimumRatio);
            long available = getAvailable(input.getType(), inputs);
            // Do not allow auto-production to go negative.
            if (canAutoProduce()) available = Math.max(0, available);
            // Experts in factory level buildings may produce a
            // certain amount of goods even when no input is available.
            // Factories have the EXPERTS_USE_CONNECTIONS ability.
            if (available < required
                && hasAbility(Ability.EXPERTS_USE_CONNECTIONS)
                && spec.getBoolean(GameOptions.EXPERTS_HAVE_CONNECTIONS)) {
                long minimumGoodsInput = 4 // FIXME: magic number
                    * (int)getUnitList().stream()
                        .filter(u -> u.getType() == getExpertUnitType())
                        .count();
                if (minimumGoodsInput > available) {
                    available = minimumGoodsInput;
                }
            }
            // Scale production by limitations on availability.
            if (available < required) {
                minimumRatio *= (double)available / required;
                //maximumRatio = Math.max(maximumRatio, minimumRatio);
            }
        }

        // Check whether there is space enough to store the goods
        // produced in order to avoid excess production.
        if (avoidOverflow) {
            for (AbstractGoods output : getOutputs()) {
                double production = output.getAmount() * minimumRatio;
                if (production <= 0) continue;
                double headroom = (double)capacity
                    - getAvailable(output.getType(), outputs);
                // Clamp production at warehouse capacity
                if (production > headroom) {
                    minimumRatio = Math.min(minimumRatio,
                        headroom / output.getAmount());
                }
                production = output.getAmount() * maximumRatio;
                if (production > headroom) {
                    maximumRatio = Math.min(maximumRatio, 
                        headroom / output.getAmount());
                }
            }
        }

        final double epsilon = 0.0001;
        for (AbstractGoods input : getInputs()) {
            GoodsType type = input.getType();
            // maximize consumption
            int consumption = (int)Math.floor(input.getAmount()
                * minimumRatio + epsilon);
            int maximumConsumption = (int)Math.floor(input.getAmount()
                * maximumRatio);
            result.addConsumption(new AbstractGoods(type, consumption));
            if (consumption < maximumConsumption) {
                result.addMaximumConsumption(new AbstractGoods(type, maximumConsumption));
            }
        }
        for (AbstractGoods output : getOutputs()) {
            GoodsType type = output.getType();
            // minimize production, but add a magic little something
            // to counter rounding errors
            int production = (int)Math.floor(output.getAmount() * minimumRatio
                + epsilon);
            int maximumProduction = (int)Math.floor(output.getAmount()
                * maximumRatio);
            result.addProduction(new AbstractGoods(type, production));
            if (production < maximumProduction) {
                result.addMaximumProduction(new AbstractGoods(type, maximumProduction));
            }
        }
        return result;
    }

    /**
     * Evaluate this work location for a given player.
     *
     * @param player The <code>Player</code> to evaluate for.
     * @return A value for the player.
     */
    @Override
    public int evaluateFor(Player player) {
        return super.evaluateFor(player)
            + getType().getRequiredGoods().stream()
                .mapToInt(ag -> ag.evaluateFor(player)).sum();
    }
        

    // Interface Location
    // Inherits:
    //   FreeColObject.getId
    //   WorkLocation.getTile
    //   UnitLocation.getLocationLabelFor
    //   UnitLocation.contains
    //   UnitLocation.canAdd
    //   WorkLocation.remove
    //   UnitLocation.getUnitCount
    //   final UnitLocation.getUnitIterator
    //   UnitLocation.getUnitList
    //   UnitLocation.getGoodsContainer
    //   final WorkLocation.getSettlement
    //   final WorkLocation.getColony
    //   final WorkLocation.getRank

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.template("model.building.locationLabel")
            .addNamed("%location%", this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Location up() {
        return getColony();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toShortString() {
        return getColony().getName() + "-" + getType().getSuffix();
    }


    // Interface UnitLocation
    // Inherits:
    //   UnitLocation.getSpaceTaken
    //   UnitLocation.moveToFront
    //   UnitLocation.clearUnitList
    //   UnitLocation.equipForRole
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoAddReason(Locatable locatable) {
        NoAddReason reason = super.getNoAddReason(locatable);
        if (reason == NoAddReason.NONE) {
            reason = getType().getNoAddReason(((Unit) locatable).getType());
            if (reason == NoAddReason.NONE) reason = getNoWorkReason();
        }
        return reason;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getUnitCapacity() {
        return getType().getWorkPlaces();
    }


    // Interface WorkLocation
    // Inherits:
    //   WorkLocation.getClaimTemplate: buildings do not need to be claimed.

    /**
     * {@inheritDoc}
     */
    @Override
    public StringTemplate getLabel() {
        return (buildingType == null) ? null
            : StringTemplate.key(buildingType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCurrent() {
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public NoAddReason getNoWorkReason() {
        return NoAddReason.NONE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canAutoProduce() {
        return hasAbility(Ability.AUTO_PRODUCTION);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canProduce(GoodsType goodsType, UnitType unitType) {
        final BuildingType type = getType();
        return type != null && type.canProduce(goodsType, unitType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getBaseProduction(ProductionType productionType,
                                 GoodsType goodsType, UnitType unitType) {
        final BuildingType type = getType();
        return (type == null) ? 0
            : getType().getBaseProduction(productionType, goodsType, unitType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Modifier> getProductionModifiers(GoodsType goodsType,
                                                 UnitType unitType) {
        final BuildingType type = getType();
        final String id = (goodsType == null) ? null : goodsType.getId();
        final Colony colony = getColony();
        final Player owner = getOwner();
        final Turn turn = getGame().getTurn();

        List<Modifier> mods = new ArrayList<>();
        if (unitType == null) { // Add only the building-specific bonuses
            mods.addAll(colony.getModifiers(id, type, turn));
            if (owner != null) {
                mods.addAll(owner.getModifiers(id, type, turn));
            }

        } else { // If a unit is present add unit specific bonuses.
            mods.addAll(this.getModifiers(id, unitType, turn));
            mods.addAll(colony.getProductionModifiers(goodsType));
            mods.addAll(unitType.getModifiers(id, goodsType, turn));
            if (owner != null) {
                mods.addAll(owner.getModifiers(id, unitType, turn));
            }
        }
        Collections.sort(mods);
        return mods;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
        return (buildingType == null) ? Collections.<ProductionType>emptyList()
            : getType().getAvailableProductionTypes(unattended);
    }


    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        return getInputs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return getType().getPriority();
    }


    // Interface Named

    /**
     * {@inheritDoc}
     */
    @Override
    public String getNameKey() {
        return getType().getNameKey();
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Ability> getAbilities(String id, FreeColGameObjectType type,
                                     Turn turn) {
        // Buildings have no abilities independent of their type (for now).
        return getType().getAbilities(id, type, turn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<Modifier> getModifiers(String id, FreeColGameObjectType fcgot,
                                      Turn turn) {
        // Buildings have no modifiers independent of type
        return getType().getModifiers(id, fcgot, turn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof Building) {
            Building building = (Building)other;
            cmp = getType().compareTo(building.getType());
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
    }


    // Serialization

    private static final String BUILDING_TYPE_TAG = "buildingType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(BUILDING_TYPE_TAG, buildingType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        buildingType = xr.getType(spec, BUILDING_TYPE_TAG,
                                  BuildingType.class, (BuildingType)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        sb.append("[").append(getId())
            .append(" ").append((buildingType == null) ? ""
                : lastPart(buildingType.getId(), "."))
            .append("/").append(getColony().getName())
            .append("]");
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
     * @return "building".
     */
    public static String getXMLElementTagName() {
        return "building";
    }
}
