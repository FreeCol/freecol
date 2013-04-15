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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.util.RandomChoice;


public final class TileImprovementType extends FreeColGameObjectType {

    /** Is this improvement natural or man-made? */
    private boolean natural;

    /** The magnitude of the improvement. */
    private int magnitude;

    /** The number of turns to build this improvement. */
    private int addWorkTurns;

    /** Any improvement that is required before this one. */
    private TileImprovementType requiredImprovementType;

    /** Equipment expended in making this improvement. */
    private EquipmentType expendedEquipmentType;

    /** The amount of the equipment expended in making this improvement. */
    private int expendedAmount;

    // @compat 0.10.4
    /** The type of goods delivered by making this improvement. */
    private GoodsType deliverGoodsType = null;

    /** The amount of goods delivered by making this improvement. */
    private int deliverAmount;
    // end @compat

    /** The change to the movement cost due to this tile improvement. */
    private int movementCost = -1;

    /**
     * The layer a TileItem belongs to. Items with higher zIndex
     * will be displayed above items with a lower zIndex. E.g. the
     * LostCityRumour would be displayed above the Plow improvement.
     */
    private int zIndex;

    /**
     * Can this improvement expose a resource when completed? This
     * should only apply to improvement types that change the
     * underlying tile type (e.g. clearing forests).
     */
    private int exposeResourcePercent;

    /** The workers that can make this improvement. */
    private Set<String> allowedWorkers = null;

    /** The changes this improvement makes to a particular tile type. */
    private Map<TileType, TileTypeChange> tileTypeChanges = null;

    /** The disasters that may strike this type of tile improvement. */
    private List<RandomChoice<Disaster>> disasters = null;

    /**
     * The scopes define which TileTypes support this improvement.
     * An eligible TileType must match all scopes.
     */
    private List<Scope> scopes = null;


    /**
     * Create a new tile improvement type.
     *
     * @param id The object id.
     * @param specification The enclosing <code>Specification</code>.
     */
    public TileImprovementType(String id, Specification specification) {
        super(id, specification);

        setModifierIndex(Modifier.IMPROVEMENT_PRODUCTION_INDEX);
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
     * Gets the expended equipment type to build this improvement type.
     *
     * @return The expended equipment, if any.
     */
    public EquipmentType getExpendedEquipmentType() {
        return expendedEquipmentType;
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
     * Get the scopes applicable to this improvement.
     *
     * @return A list of <code>Scope</code>s.
     */
    public List<Scope> getScopes() {
        if (scopes == null) return Collections.emptyList();
        return scopes;
    }

    /**
     * Get a weighted list of natural disasters than can strike this
     * tile improvement type.
     *
     * @return A random choice list of <code>Disaster</code>s.
     */
    public List<RandomChoice<Disaster>> getDisasters() {
        if (disasters == null) return Collections.emptyList();
        return disasters;
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
     * Gets an identifier for the action of building this improvement.
     *
     * @return a <code>String</code> value
     */
    public String getShortId() {
        int index = getId().lastIndexOf('.') + 1;
        return getId().substring(index);
    }

    /**
     * Is a particular unit type allowed to build this improvement?
     *
     * @param unitType The <code>UnitType</code> to check.
     * @return True if the <code>UnitType</code> can build this improvement.
     */
    public boolean isWorkerTypeAllowed(UnitType unitType) {
        return allowedWorkers == null || allowedWorkers.isEmpty()
            || allowedWorkers.contains(unitType.getId());
    }

    /**
     * Is a particular unit allowed to build this improvement?
     *
     * Checks both the unit type and the available equipment.
     *
     * @param unit The <code>Unit</code> to check.
     * @return True if the <code>Unit</code> can build this improvement.
     */
    public boolean isWorkerAllowed(Unit unit) {
        return isWorkerTypeAllowed(unit.getType())
            && (unit.getEquipment().getCount(expendedEquipmentType)
                >= expendedAmount);
    }

    /**
     * This will check if in principle this type of improvement can be
     * used on this kind of tile, disregarding the current state of an
     * actual tile.
     *
     * If you want to find out if an improvement is allowed for a tile, call
     * {@link #isTileAllowed(Tile)}.
     *
     * @param tileType The <code>TileType</code> to check.
     * @return True if improvement is possible.
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        for (Scope scope : getScopes()) {
            if (!scope.appliesTo(tileType)) return false;
        }
        return true;
    }

    /**
     * Check if a given tile is valid for this tile improvement.
     *
     * @param tile The <code>Tile</code> to check.
     * @return True if the tile can be improved with this improvement.
     */
    public boolean isTileAllowed(Tile tile) {
        if (!isTileTypeAllowed(tile.getType())) return false;
        if (requiredImprovementType != null
            && tile.findTileImprovementType(requiredImprovementType) == null) {
            return false;
        }
        TileImprovement ti = tile.findTileImprovementType(this);
        return ti == null || !ti.isComplete();
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
        Set<Modifier> modifierSet = getModifierSet(goodsType.getId());
        if (modifierSet == null || modifierSet.isEmpty()) {
            return null;
        } else {
            if (modifierSet.size() > 1) {
                logger.warning("Only one Modifier for " + goodsType.getId()
                    + " expected!");
            }
            return modifierSet.iterator().next();
        }
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
     * @param from The original <code>TileType</code>.
     * @return The <code>AbstractGoods</code> produced.
     */
    public AbstractGoods getProduction(TileType from) {
        if (tileTypeChanges == null) return null;
        TileTypeChange change = tileTypeChanges.get(from);
        return (change == null) ? null : change.getProduction();
    }

    /**
     * Gets the destination type of a tile type change (or null).
     *
     * @param tileType The <code>TileType</code> that is to change.
     * @return The resulting <code>TileType</code>.
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
     * @param tileType The required <code>TileType</code>.
     * @return True if the required <code>TileType</code> can be changed to.
     */
    public boolean changeContainsTarget(TileType tileType) {
        if (tileTypeChanges == null) return false;
        for (TileTypeChange change : tileTypeChanges.values()) {
            if (change.getTo() == tileType) return true;
        }
        return false;
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
     * Gets the increase in production of the given GoodsType
     * this tile improvement type would yield at a specified tile.
     *
     * @param tile The <code>Tile</code> to be considered.
     * @param goodsType A preferred <code>GoodsType</code> or <code>null</code>
     * @return The increase in production
     */
    public int getImprovementValue(Tile tile, GoodsType goodsType) {
        int value = 0;
        if (goodsType.isFarmed()) {
            TileType newTileType = getChange(tile.getType());
            if (newTileType == null) { // simple bonus
                int production = tile.potential(goodsType, null);
                if (production > 0) {
                    float chg = applyModifier(production, goodsType.getId());
                    value = (int)(chg - production);
                }
            } else { // tile type change
                int chg = newTileType.getProductionOf(goodsType, null)
                    - tile.getType().getProductionOf(goodsType, null);
                value = chg;
            }
        }
        return value;
    }


    // Serialization
    private static final String ADD_WORK_TURNS_TAG = "add-work-turns";
    private static final String CHANGE_TAG = "change";
    private static final String DELIVER_AMOUNT_TAG = "deliver-amount";
    private static final String DELIVER_GOODS_TYPE_TAG = "deliver-goods-type";
    private static final String DISASTER_TAG = "disaster";
    private static final String EXPENDED_AMOUNT_TAG = "expended-amount";
    private static final String EXPENDED_EQUIPMENT_TYPE_TAG = "expended-equipment-type";
    private static final String EXPOSE_RESOURCE_PERCENT_TAG = "exposeResourcePercent";
    private static final String FROM_TAG = "from";
    private static final String MAGNITUDE_TAG = "magnitude";
    private static final String MOVEMENT_COST_TAG = "movement-cost";
    private static final String NATURAL_TAG = "natural";
    private static final String PROBABILITY_TAG = "probability";
    private static final String REQUIRED_IMPROVEMENT_TAG = "required-improvement";
    private static final String TO_TAG = "to";
    private static final String WORKER_TAG = "worker";
    private static final String ZINDEX_TAG = "zIndex";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NATURAL_TAG, natural);

        writeAttribute(out, MAGNITUDE_TAG, magnitude);

        writeAttribute(out, ADD_WORK_TURNS_TAG, addWorkTurns);

        if (requiredImprovementType != null) {
            writeAttribute(out, REQUIRED_IMPROVEMENT_TAG,
                           requiredImprovementType);
        }

        if (expendedEquipmentType != null) {
            writeAttribute(out, EXPENDED_EQUIPMENT_TYPE_TAG, 
                           expendedEquipmentType);

            writeAttribute(out, EXPENDED_AMOUNT_TAG, expendedAmount);
        }

        writeAttribute(out, MOVEMENT_COST_TAG, movementCost);

        writeAttribute(out, ZINDEX_TAG, zIndex);

        writeAttribute(out, EXPOSE_RESOURCE_PERCENT_TAG, exposeResourcePercent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Scope scope : getScopes()) scope.toXML(out);

        if (allowedWorkers != null) {
            for (String id : allowedWorkers) {
                out.writeStartElement(WORKER_TAG);

                writeAttribute(out, ID_ATTRIBUTE_TAG, id);

                out.writeEndElement();
            }
        }

        if (tileTypeChanges != null) {
            for (TileTypeChange change : tileTypeChanges.values()) {
                change.toXML(out);
            }
        }

        for (RandomChoice<Disaster> choice : getDisasters()) {
            out.writeStartElement(DISASTER_TAG);

            writeAttribute(out, ID_ATTRIBUTE_TAG, choice.getObject().getId());

            writeAttribute(out, PROBABILITY_TAG, choice.getProbability());

            out.writeEndElement();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        natural = getAttribute(in, NATURAL_TAG, false);

        magnitude = getAttribute(in, MAGNITUDE_TAG, 1);

        addWorkTurns = getAttribute(in, ADD_WORK_TURNS_TAG, 0);

        requiredImprovementType = spec.getType(in, REQUIRED_IMPROVEMENT_TAG,
            TileImprovementType.class, (TileImprovementType)null);

        expendedEquipmentType = spec.getType(in, EXPENDED_EQUIPMENT_TYPE_TAG,
            EquipmentType.class, (EquipmentType)null);

        expendedAmount = getAttribute(in, EXPENDED_AMOUNT_TAG, 0);

        // @compat 0.10.4
        deliverGoodsType = spec.getType(in, DELIVER_GOODS_TYPE_TAG,
            GoodsType.class, (GoodsType)null);

        deliverAmount = getAttribute(in, DELIVER_AMOUNT_TAG, 0);
        // end @compat

        movementCost = getAttribute(in, MOVEMENT_COST_TAG, 0);

        zIndex = getAttribute(in, ZINDEX_TAG, 0);

        exposeResourcePercent = getAttribute(in, EXPOSE_RESOURCE_PERCENT_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            scopes = null;
            allowedWorkers = null;
            tileTypeChanges = null;
            disasters = null;
        }

        super.readChildren(in);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (CHANGE_TAG.equals(tag)) {
            TileTypeChange change = new TileTypeChange();
            if (deliverGoodsType == null) {
                change.readFromXML(in, spec);
            } else {
                // @compat 0.10.4
                TileType from = spec.getType(in, FROM_TAG,
                                             TileType.class, (TileType)null);
                TileType to = spec.getType(in, TO_TAG,
                                           TileType.class, (TileType)null);
                change.setFrom(from);
                change.setTo(to);
                change.setProduction(new AbstractGoods(deliverGoodsType,
                                                       deliverAmount));
                in.nextTag(); // close this element
                // end @compat
            }
            if (tileTypeChanges == null) {
                tileTypeChanges = new HashMap<TileType, TileTypeChange>();
            }
            tileTypeChanges.put(change.getFrom(), change);

        } else if (DISASTER_TAG.equals(tag)) {
            Disaster disaster = spec.getType(in, ID_ATTRIBUTE_TAG,
                                             Disaster.class, (Disaster)null);
            int probability = getAttribute(in, PROBABILITY_TAG, 100);
            if (disasters == null) {
                disasters = new ArrayList<RandomChoice<Disaster>>();
            }
            disasters.add(new RandomChoice<Disaster>(disaster, probability));
            in.nextTag(); // close this element

        } else if (WORKER_TAG.equals(tag)) {
            String id = readId(in);
            if (id != null) {
                if (allowedWorkers == null) {
                    allowedWorkers = new HashSet<String>();
                }
                allowedWorkers.add(id);
            }
            in.nextTag(); // close this element

        } else if (Scope.getXMLElementTagName().equals(tag)) {
            Scope scope = new Scope(in);
            if (scope != null) {
                if (scopes == null) scopes = new ArrayList<Scope>();
                scopes.add(scope);
            }

        } else {
            super.readChild(in);
        }
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "tileimprovement-type".
     */
    public static String getXMLElementTagName() {
        return "tileimprovement-type";
    }
}
