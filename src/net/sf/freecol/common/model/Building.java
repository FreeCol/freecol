/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.CollectionUtils.any;
import static net.sf.freecol.common.util.CollectionUtils.concat;
import static net.sf.freecol.common.util.CollectionUtils.sum;
import static net.sf.freecol.common.util.CollectionUtils.toList;
import static net.sf.freecol.common.util.CollectionUtils.transform;
import static net.sf.freecol.common.util.StringUtils.lastPart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.production.BuildingProductionCalculator;
import net.sf.freecol.common.model.production.WorkerAssignment;


/**
 * Represents a building in a colony.
 */
public class Building extends WorkLocation
    implements Named, Consumer {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(Building.class.getName());

    private final static double EPSILON = 0.0001;

    public static final String TAG = "building";
    
    public static final String UNIT_CHANGE = "UNIT_CHANGE";


    /** The type of building. */
    protected BuildingType buildingType;


    /**
     * Constructor for ServerBuilding.
     *
     * @param game The enclosing {@code Game}.
     * @param colony The {@code Colony} in which this building is located.
     * @param type The {@code BuildingType} of building.
     */
    protected Building(Game game, Colony colony, BuildingType type) {
        super(game);

        this.colony = colony;
        this.buildingType = type;
        // set production type to default value
        updateProductionType();
    }

    /**
     * Create a new {@code Building} with the given identifier.
     * The object should later be initialized by calling
     * {@link #readFromXML(FreeColXMLReader)}.
     *
     * @param game The enclosing {@code Game}.
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
     * @param newBuildingType The new {@code BuildingType}.
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
            eject.addAll(transform(getUnits(),
                                   u -> !canAddType(u.getType())));
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
     * Does this building have a higher level?
     *
     * @return True if this {@code Building} can have a higher level.
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
     * @param unitType The {@code UnitType} to check.
     * @return True if unit type can be added to this building.
     */
    public boolean canAddType(UnitType unitType) {
        return canBeWorked() && getType().canAdd(unitType);
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
    public ProductionInfo getAdjustedProductionInfo(List<AbstractGoods> inputs, List<AbstractGoods> outputs) {
        final BuildingProductionCalculator pc = new BuildingProductionCalculator(getOwner(), getColony().getFeatureContainer(), getColony().getProductionBonus());
        final List<WorkerAssignment> workerAssignments = getUnits()
                .map(u -> new WorkerAssignment(u.getType(), getProductionType()))
                .collect(Collectors.toList());
        final Turn turn = getGame().getTurn();
        final int warehouseCapacity = getColony().getWarehouseCapacity();
        return pc.getAdjustedProductionInfo(buildingType, turn, workerAssignments, inputs, outputs, warehouseCapacity);
    }

    /**
     * Evaluate this work location for a given player.
     *
     * @param player The {@code Player} to evaluate for.
     * @return A value for the player.
     */
    @Override
    public int evaluateFor(Player player) {
        return super.evaluateFor(player)
            + sum(getType().getRequiredGoods(), ag -> ag.evaluateFor(player));
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

    @Override
    public boolean goodSuggestionCheck(UnitType better, Unit unit, GoodsType goodsType) {
        // Make sure the type can be added.
        if (this.canAddType(better)) {
            Colony colony = getColony();
            BuildableType bt;
            // Assume work is worth doing if a unit is already
            // there, or if the building has been upgraded, or if
            // the goods are required for the current building job.
            if (this.getLevel() > 1 || unit != null) {
                return true;
            } else if (colony.getTotalProductionOf(goodsType) == 0
                    && (bt = colony.getCurrentlyBuilding()) != null
                    && any(bt.getRequiredGoods(), AbstractGoods.matches(goodsType))) {
                return true;
            }
        }
        return false;
    }

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
    public Tile getWorkTile() {
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public int getLevel() {
        return getType().getLevel(); // Delegate to type
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
    public Stream<Modifier> getProductionModifiers(GoodsType goodsType,
                                                   UnitType unitType) {
        final BuildingType type = getType();
        final String id = (goodsType == null) ? null : goodsType.getId();
        final Colony colony = getColony();
        final Player owner = getOwner();
        final Turn turn = getGame().getTurn();

        return (unitType != null)
            // With a unit, unit specific bonuses apply
            ? concat(this.getModifiers(id, unitType, turn),
                     colony.getProductionModifiers(goodsType, unitType, this),
                     getType().getCompetenceModifiers(id, unitType, turn),
                     owner.getModifiers(id, unitType, turn))
            // With no unit, only the building-specific bonuses 
            : concat(colony.getModifiers(id, type, turn),
                     owner.getModifiers(id, type, turn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ProductionType> getAvailableProductionTypes(boolean unattended) {
        return (buildingType == null) ? Collections.<ProductionType>emptyList()
            : getType().getAvailableProductionTypes(unattended);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getCompetenceFactor() {
        return getType().getCompetenceFactor();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getRebelFactor() {
        return getType().getRebelFactor();
    }


    // Interface Consumer

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AbstractGoods> getConsumedGoods() {
        return toList(getInputs());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPriority() {
        return getType().getPriority();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getConsumptionModifiers(String id) {
        return getModifiers(id);
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
    public Stream<Ability> getAbilities(String id, FreeColSpecObjectType type,
                                        Turn turn) {
        // Buildings have no abilities independent of their type (for now).
        return getType().getAbilities(id, type, turn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Stream<Modifier> getModifiers(String id, FreeColSpecObjectType fcgot,
                                         Turn turn) {
        // Buildings have no modifiers independent of type
        return getType().getModifiers(id, fcgot, turn);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Building o = copyInCast(other, Building.class);
        if (o == null || !super.copyIn(o)) return false;
        this.buildingType = o.getType();
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FreeColObject getDisplayObject() {
        return getType();
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
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(32);
        Colony c =  getColony();
        sb.append('[').append(getId())
            .append(' ').append((buildingType == null) ? ""
                : lastPart(buildingType.getId(), "."))
            .append('/').append((c == null) ? "NO-COLONY" : c.getName())
            .append(']');
        return sb.toString();
    }
}
