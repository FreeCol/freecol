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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;


/**
 * An improvement to make to a tile.
 */
public final class TileImprovementType extends FreeColSpecObjectType {

    public static final String TAG = "tile-improvement-type";

    /** Is this improvement natural or man-made? */
    private boolean natural;

    /** The magnitude of the improvement. */
    private int magnitude;

    /** The number of turns to build this improvement. */
    private int addWorkTurns;

    /** Any improvement that is required before this one. */
    private TileImprovementType requiredImprovementType;

    /** The role required to make this improvement. */
    private Role requiredRole;

    /** The amount of the equipment expended in making this improvement. */
    private int expendedAmount;

    /** The change to the movement cost due to this tile improvement. */
    private int movementCost = -1;

    /**
     * Can this improvement expose a resource when completed? This
     * should only apply to improvement types that change the
     * underlying tile type (e.g. clearing forests).
     */
    private int exposeResourcePercent;

    /**
     * The layer a TileItem belongs to. Items with higher zIndex
     * will be displayed above items with a lower zIndex. E.g. the
     * LostCityRumour would be displayed above the Plow improvement.
     */
    private int zIndex;

    /** The workers that can make this improvement. */
    private Set<String> allowedWorkers = null;

    /** The changes this improvement makes to a particular tile type. */
    private Map<TileType, TileTypeChange> tileTypeChanges = null;

    /** The disasters that may strike this type of tile improvement. */
    private List<RandomChoice<Disaster>> disasters = null;


    /**
     * Create a new tile improvement type.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public TileImprovementType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this tile improvement type natural?
     *
     * @return True if this is a natural tile improvement type.
     */
    public boolean isNatural() {
        return natural;
    }

    /**
     * Get the magnitude of this tile improvement type.
     *
     * @return The magnitude.
     */
    public int getMagnitude() {
        return magnitude;
    }

    /**
     * Get the number of turns to build this tile improvement type.
     *
     * @return The number of build turns.
     */
    public int getAddWorkTurns() {
        return addWorkTurns;
    }

    /**
     * Gets the required improvement type.
     *
     * @return The required improvement type if any.
     */
    public TileImprovementType getRequiredImprovementType() {
        return requiredImprovementType;
    }

    /**
     * Get the role required to perform this improvement, if any.
     *
     * @return The required {@code Role}.
     */
    public Role getRequiredRole() {
        return requiredRole;
    }

    /**
     * Gets the amount of equipment expended in building this improvement type.
     *
     * @return The expended equipment amount, if any.
     */
    public int getExpendedAmount() {
        return expendedAmount;
    }

    /**
     * Get the standard movement cost of the tile improvement type.
     *
     * @return The movement cost.
     */
    public int getMoveCost() {
        return this.movementCost;
    }

    /**
     * Possibly reduce the cost of moving due to this tile improvement
     * type.
     *
     * Only applies if movementCost is positive (see spec).  Do not
     * return zero from a movement costing routine or units get free
     * moves!
     *
     * @param originalCost The original movement cost.
     * @return The movement cost after any change.
     */
    public int getMoveCost(int originalCost) {
        return (movementCost > 0 && movementCost < originalCost)
            ? movementCost
            : originalCost;
    }

    /**
     * Gets the percent chance that this tile improvement can expose a
     * resource on the tile. This only applies to TileImprovementTypes
     * that change the underlying tile type (e.g. clearing forests).
     *
     * @return The exposure chance.
     */
    public int getExposeResourcePercent() {
        return exposeResourcePercent;
    }

    /**
     * Get the layer.
     *
     * @return The layer.
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Set the layer.
     *
     * @param newZIndex The new layer.
     */
    public void setZIndex(final int newZIndex) {
        this.zIndex = newZIndex;
    }

    /**
     * Get the set of allowed worker identifiers.
     *
     * @return The set of allowed worker identifiers.
     */
    protected Set<String> getAllowedWorkers() {
        if (this.allowedWorkers == null) this.allowedWorkers = new HashSet<>();
        return this.allowedWorkers;
    }

    /**
     * Set the set of allowed worker identifiers.
     *
     * @param allowed The new set of allowed worker identifiers.
     */
    protected void setAllowedWorkers(Set<String> allowed) {
        if (this.allowedWorkers == null) {
            this.allowedWorkers = new HashSet<>();
        } else {
            this.allowedWorkers.clear();
        }
        this.allowedWorkers.addAll(allowed);
    }

    /**
     * Add an allowed worker identifier.
     *
     * @param id The worker identifier to add.
     */
    private void addAllowedWorker(String id) {
        getAllowedWorkers().add(id);
    }

    /**
     * Is a particular unit type allowed to build this improvement?
     *
     * @param unitType The {@code UnitType} to check.
     * @return True if the {@code UnitType} can build this improvement.
     */
    private boolean isWorkerTypeAllowed(UnitType unitType) {
        return allowedWorkers == null || allowedWorkers.isEmpty()
            || allowedWorkers.contains(unitType.getId());
    }

    /**
     * Is a particular unit allowed to build this improvement?
     *
     * Checks both the unit type and the available equipment.
     *
     * @param unit The {@code Unit} to check.
     * @return True if the {@code Unit} can build this improvement.
     */
    public boolean isWorkerAllowed(Unit unit) {
        return isWorkerTypeAllowed(unit.getType())
            && (requiredRole == null || unit.getRole() == requiredRole);
    }

    /**
     * This will check if in principle this type of improvement can be
     * used on this kind of tile, disregarding the current state of an
     * actual tile.
     *
     * If you want to find out if an improvement is allowed for a tile, call
     * {@link Tile#isImprovementAllowed(TileImprovement)}.
     *
     * @param tileType The {@code TileType} to check.
     * @return True if improvement is possible.
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        return all(getScopes(), s -> s.appliesTo(tileType));
    }

    public int getBonus(GoodsType goodsType) {
        Modifier result = getProductionModifier(goodsType);
        if (result == null) {
            return 0;
        } else {
            return (int) result.getValue();
        }
    }

    public Modifier getProductionModifier(GoodsType goodsType) {
        return first(getModifiers(goodsType.getId()));
    }

    /**
     * Get the tile type change map.
     *
     * @return The map of tile type to changes.
     */
    protected Map<TileType, TileTypeChange> getTileTypeChanges() {
        if (this.tileTypeChanges == null) this.tileTypeChanges = new HashMap<>();
        return this.tileTypeChanges;
    }

    /**
     * Set the tile type change map.
     *
     * @param changes A new map of tile type to changes.
     */
    protected void setTileTypeChanges(Map<TileType, TileTypeChange> changes) {
        if (this.tileTypeChanges == null) {
            this.tileTypeChanges = new HashMap<>();
        } else {
            this.tileTypeChanges.clear();
        }
        this.tileTypeChanges.putAll(changes);
    }

    /**
     * Does this tile improvement change the underlying tile type.
     *
     * @return True if this tile improvement changes the tile type.
     */
    public boolean isChangeType() {
        return tileTypeChanges != null && !tileTypeChanges.isEmpty();
    }

    /**
     * Gets the goods produced by applying this TileImprovementType
     * to a Tile with the given TileType.
     *
     * @param from The original {@code TileType}.
     * @return The {@code AbstractGoods} produced.
     */
    public AbstractGoods getProduction(TileType from) {
        if (tileTypeChanges == null) return null;
        TileTypeChange change = tileTypeChanges.get(from);
        return (change == null) ? null : change.getProduction();
    }

    /**
     * Gets the destination type of a tile type change (or null).
     *
     * @param tileType The {@code TileType} that is to change.
     * @return The resulting {@code TileType}.
     */
    public TileType getChange(TileType tileType) {
        if (tileTypeChanges == null) return null;
        TileTypeChange change = tileTypeChanges.get(tileType);
        return (change == null) ? null : change.getTo();
    }

    /**
     * Can this tile improvement type change a tile type to the given
     * tile type.
     *
     * @param tileType The required {@code TileType}.
     * @return True if the required {@code TileType} can be changed to.
     */
    public boolean changeContainsTarget(TileType tileType) {
        return (tileTypeChanges == null) ? false
            : any(tileTypeChanges.values(),
                  matchKey(tileType, TileTypeChange::getTo));
    }

    /**
     * Add a tile type change.
     *
     * @param change The {@code TileTypeChange} to add.
     */
    private void addChange(TileTypeChange change) {
        if (tileTypeChanges == null) tileTypeChanges = new HashMap<>();
        tileTypeChanges.put(change.getFrom(), change);
    }

    /**
     * Gets the increase in production of the given GoodsType
     * this tile improvement type would yield at a specified tile.
     *
     * @param tile The {@code Tile} to be considered.
     * @param goodsType An optional preferred {@code GoodsType}.
     * @return The increase in production
     */
    public int getImprovementValue(Tile tile, GoodsType goodsType) {
        final UnitType colonistType
            = getSpecification().getDefaultUnitType();
        int value = 0;
        if (goodsType.isFarmed()) {
            final int oldProduction = tile.getType()
                .getPotentialProduction(goodsType, colonistType);
            TileType tt = getChange(tile.getType());
            if (tt == null) { // simple bonus
                int production = tile.getPotentialProduction(goodsType, colonistType);
                if (production > 0) {
                    float chg = apply(production, null, goodsType.getId());
                    value = (int)(chg - production);
                }
            } else { // tile type change
                int chg = tt.getPotentialProduction(goodsType, colonistType)
                    - oldProduction;
                value = chg;
            }
        }
        return value;
    }

    /**
     * Get the list of disasters for this tile type.
     *
     * @return The disaster list.
     */
    protected List<RandomChoice<Disaster>> getDisasters() {
        if (this.disasters == null) this.disasters = new ArrayList<>();
        return this.disasters;
    }

    /**
     * Set the list of disasters for this tile type.
     *
     * @param disasters The new list of {@code Disaster}s.
     */
    protected void setDisasters(List<RandomChoice<Disaster>> disasters) {
        if (this.disasters == null) {
            this.disasters = new ArrayList<>();
        } else {
            this.disasters.clear();
        }
        this.disasters.addAll(disasters);
    }

    /**
     * Get a weighted list of natural disasters than can strike this
     * tile improvement type.
     *
     * @return A stream of {@code Disaster} choices.
     */
    public Stream<RandomChoice<Disaster>> getDisasterChoices() {
        return (disasters == null)
            ? Stream.<RandomChoice<Disaster>>empty()
            : disasters.stream();
    }

    /**
     * Add a disaster.
     *
     * @param disaster The {@code Disaster} to add.
     * @param probability The probability of the disaster.
     */
    private void addDisaster(Disaster disaster, int probability) {
        if (disasters == null) disasters = new ArrayList<>();
        disasters.add(new RandomChoice<>(disaster, probability));
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        TileImprovementType o = copyInCast(other, TileImprovementType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.natural = o.isNatural();
        this.magnitude = o.getMagnitude();
        this.addWorkTurns = o.getAddWorkTurns();
        this.requiredImprovementType = o.getRequiredImprovementType();
        this.requiredRole = o.getRequiredRole();
        this.expendedAmount = o.getExpendedAmount();
        this.movementCost = o.getMoveCost();
        this.exposeResourcePercent = o.getExposeResourcePercent();
        this.zIndex = o.getZIndex();
        this.setAllowedWorkers(o.getAllowedWorkers());
        this.setTileTypeChanges(o.getTileTypeChanges());
        this.setDisasters(o.getDisasters());
        return true;
    }


    // Serialization

    private static final String ADD_WORK_TURNS_TAG = "add-work-turns";
    private static final String DISASTER_TAG = "disaster";
    private static final String EXPENDED_AMOUNT_TAG = "expended-amount";
    private static final String EXPOSE_RESOURCE_PERCENT_TAG = "expose-resource-percent";
    private static final String FROM_TAG = "from";
    private static final String MAGNITUDE_TAG = "magnitude";
    private static final String MOVEMENT_COST_TAG = "movement-cost";
    private static final String NATURAL_TAG = "natural";
    private static final String PROBABILITY_TAG = "probability";
    private static final String REQUIRED_IMPROVEMENT_TAG = "required-improvement";
    private static final String REQUIRED_ROLE_TAG = "required-role";
    private static final String TILE_TYPE_CHANGE_TAG = "tile-type-change";
    private static final String TO_TAG = "to";
    private static final String WORKER_TAG = "worker";
    private static final String ZINDEX_TAG = "zIndex";
    // @compat 0.11.0
    private static final String OLD_EXPENDED_EQUIPMENT_TYPE_TAG = "expended-equipment-type";
    // end @compat 0.11.0
    // @compat 0.11.3
    private static final String OLD_EXPOSE_RESOURCE_PERCENT_TAG = "exposeResourcePercent";
    // end @compat 0.11.3
    // @compat 0.11.6
    private static final String OLD_CHANGE_TAG = "change";
    // end @compat 0.11.6


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NATURAL_TAG, natural);

        xw.writeAttribute(MAGNITUDE_TAG, magnitude);

        xw.writeAttribute(ADD_WORK_TURNS_TAG, addWorkTurns);

        if (requiredImprovementType != null) {
            xw.writeAttribute(REQUIRED_IMPROVEMENT_TAG,
                           requiredImprovementType);
        }

        if (requiredRole != null) {
            xw.writeAttribute(REQUIRED_ROLE_TAG, requiredRole);
        }

        if (expendedAmount != 0) {
            xw.writeAttribute(EXPENDED_AMOUNT_TAG, expendedAmount);
        }

        xw.writeAttribute(MOVEMENT_COST_TAG, movementCost);

        xw.writeAttribute(ZINDEX_TAG, zIndex);

        xw.writeAttribute(EXPOSE_RESOURCE_PERCENT_TAG, exposeResourcePercent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (allowedWorkers != null) {
            for (String id : allowedWorkers) {
                xw.writeStartElement(WORKER_TAG);

                xw.writeAttribute(ID_ATTRIBUTE_TAG, id);

                xw.writeEndElement();
            }
        }

        if (tileTypeChanges != null) {
            for (Entry<TileType, TileTypeChange> e
                     : mapEntriesByValue(tileTypeChanges)) {
                e.getValue().toXML(xw);
            }
        }

        for (RandomChoice<Disaster> choice : iterable(getDisasterChoices())) {
            xw.writeStartElement(DISASTER_TAG);

            xw.writeAttribute(ID_ATTRIBUTE_TAG, choice.getObject().getId());

            xw.writeAttribute(PROBABILITY_TAG, choice.getProbability());

            xw.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        natural = xr.getAttribute(NATURAL_TAG, false);

        magnitude = xr.getAttribute(MAGNITUDE_TAG, 1);

        addWorkTurns = xr.getAttribute(ADD_WORK_TURNS_TAG, 0);

        requiredImprovementType = xr.getType(spec, REQUIRED_IMPROVEMENT_TAG,
            TileImprovementType.class, (TileImprovementType)null);

        requiredRole = xr.getType(spec, REQUIRED_ROLE_TAG,
            Role.class, (Role)null);
        // @compat 0.11.x
        if (xr.hasAttribute(OLD_EXPENDED_EQUIPMENT_TYPE_TAG)) {
            requiredRole = spec.getRole("model.role.pioneer");
        }
        // end @compat 0.11.x

        expendedAmount = xr.getAttribute(EXPENDED_AMOUNT_TAG, 0);

        movementCost = xr.getAttribute(MOVEMENT_COST_TAG, 0);

        zIndex = xr.getAttribute(ZINDEX_TAG, 0);

        // @compat 0.11.3
        if (xr.hasAttribute(OLD_EXPOSE_RESOURCE_PERCENT_TAG)) {
            exposeResourcePercent = xr.getAttribute(OLD_EXPOSE_RESOURCE_PERCENT_TAG, 0);
        } else
        // end @compat 0.11.3
            exposeResourcePercent = xr.getAttribute(EXPOSE_RESOURCE_PERCENT_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            allowedWorkers = null;
            tileTypeChanges = null;
            disasters = null;
        }

        super.readChildren(xr);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (TILE_TYPE_CHANGE_TAG.equals(tag)
            // @compat 0.11.6
            || OLD_CHANGE_TAG.equals(tag)
            // end @compat 0.11.6
            ) {
            addChange(new TileTypeChange(xr, spec));

        } else if (DISASTER_TAG.equals(tag)) {
            Disaster disaster = xr.getType(spec, ID_ATTRIBUTE_TAG,
                                           Disaster.class, (Disaster)null);
            int probability = xr.getAttribute(PROBABILITY_TAG, 100);
            addDisaster(disaster, probability);
            xr.closeTag(DISASTER_TAG);

        } else if (WORKER_TAG.equals(tag)) {
            addAllowedWorker(xr.readId());
            xr.closeTag(WORKER_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
