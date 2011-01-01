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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public final class TileImprovementType extends FreeColGameObjectType {

    private boolean natural;
    private int magnitude;
    private int addWorkTurns;

    private TileImprovementType requiredImprovementType;

    private Set<String> allowedWorkers = new HashSet<String>();
    private EquipmentType expendedEquipmentType;
    private int expendedAmount;
    private GoodsType deliverGoodsType;
    private int deliverAmount;

    private Map<TileType, TileType> tileTypeChange = new HashMap<TileType, TileType>();

    private int movementCost;
    private float movementCostFactor;

    /**
     * The layer a TileItem belongs to. Items with higher zIndex
     * will be displayed above items with a lower zIndex. E.g. the
     * LostCityRumour will be displayed above the Plow improvement.
     */
    private int zIndex;

    // Does this improvement expose a resource when completed
    // (should only apply to clearing forests).
    private int exposeResourcePercent;

    /**
     * The scopes define which TileTypes support this improvement. An
     * eligible TileType must match all scopes.
     */
    private List<Scope> scopes = new ArrayList<Scope>();

    // ------------------------------------------------------------ constructors

    public TileImprovementType(String id, Specification specification) {
        super(id, specification);
        setModifierIndex(Modifier.IMPROVEMENT_PRODUCTION_INDEX);
    }

    // ------------------------------------------------------------ retrieval methods

    public boolean isNatural() {
        return natural;
    }

    public int getMagnitude() {
        return magnitude;
    }

    public int getAddWorkTurns() {
        return addWorkTurns;
    }

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getZIndex() {
        return zIndex;
    }

    /**
     * Set the <code>ZIndex</code> value.
     *
     * @param newZIndex The new ZIndex value.
     */
    public void setZIndex(final int newZIndex) {
        this.zIndex = newZIndex;
    }

    public TileImprovementType getRequiredImprovementType() {
        return requiredImprovementType;
    }

    public EquipmentType getExpendedEquipmentType() {
        return expendedEquipmentType;
    }

    public int getExpendedAmount() {
        return expendedAmount;
    }

    public GoodsType getDeliverGoodsType() {
        return deliverGoodsType;
    }

    public int getDeliverAmount() {
        return deliverAmount;
    }

    /**
     * Get the <code>Scopes</code> value.
     *
     * @return a <code>List<Scope></code> value
     */
    public List<Scope> getScopes() {
        return scopes;
    }

    /**
     * Return an ID of an appropriate action.
     *
     * @return a <code>String</code> value
     */
    public String getShortId() {
        int index = getId().lastIndexOf('.') + 1;
        return getId().substring(index);
    }


    public boolean isWorkerTypeAllowed(UnitType unitType) {
        return allowedWorkers.isEmpty() || allowedWorkers.contains(unitType.getId());
    }

    /**
     * Check if a given <code>Unit</code> can perform this TileImprovement.
     * @return true if Worker UnitType is allowed and expended Goods are available
     */
    public boolean isWorkerAllowed(Unit unit) {
        if (!isWorkerTypeAllowed(unit.getType())) {
            return false;
        }
        return (unit.getEquipment().getCount(expendedEquipmentType) >= expendedAmount);
    }

    /**
     * This will check if in principle this type of improvement can be used on
     * this kind of tile, disregarding the current state of an actual tile.
     *
     * If you want to find out if an improvement is allowed for a tile, call
     * {@link #isTileAllowed(Tile)}.
     *
     * @param tileType The type of terrain
     * @return true if improvement is possible
     */
    public boolean isTileTypeAllowed(TileType tileType) {
        for (Scope scope : scopes) {
            if (!scope.appliesTo(tileType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a given <code>Tile</code> is valid for this TileImprovement.
     *
     * @return true if Tile TileType is valid and required Improvement (if any)
     *         is present.
     */
    public boolean isTileAllowed(Tile tile) {
        if (!isTileTypeAllowed(tile.getType())) {
            return false;
        }
        if (requiredImprovementType != null && tile.findTileImprovementType(requiredImprovementType) == null) {
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
        Set<Modifier> modifierSet = getFeatureContainer().getModifierSet(goodsType.getId());
        if (modifierSet == null || modifierSet.isEmpty()) {
            return null;
        } else {
            if (modifierSet.size() > 1) {
                logger.warning("Only one Modifier for " + goodsType.getId() + " expected!");
            }
            return modifierSet.iterator().next();
        }
    }

    public TileType getChange(TileType tileType) {
        return tileTypeChange.get(tileType);
    }

    public boolean changeContainsTarget(TileType tileType) {
        return tileTypeChange.containsValue(tileType);
    }

    /**
     * Returns a value for use in AI decision making.
     * @param tileType The <code>TileType</code> to be considered. A <code>null</code> entry
     *        denotes no interest in a TileImprovementType that changes TileTypes
     * @param goodsType A preferred <code>GoodsType</code> or <code>null</code>
     * @return Sum of all bonuses with a triple bonus for the preferred GoodsType
     */
    public int getValue(TileType tileType, GoodsType goodsType) {
        int value = 0;
        if (goodsType.isFarmed()) {
            TileType newTileType = getChange(tileType);
            // 2 main types TileImprovementTypes - Changing of TileType and Simple Bonus
            if (newTileType != null) {
                int change = newTileType.getProductionOf(goodsType, null)
                    - tileType.getProductionOf(goodsType, null);
                if (change > 0) {
                    value += change * 3;
                }
            } else if (tileType.getProductionOf(goodsType, null) > 0) {
                // Calculate bonuses from TileImprovementType
                for (Modifier modifier : getFeatureContainer().getModifiers()) {
                    float change = modifier.applyTo(1);
                    if (modifier.getId().equals(goodsType.getId())) {
                        if (change > 1) {
                            value += change * 3;
                        }
                    }
                }
            }
        }
        return value;
    }

    /**
     * Performs reduction of the movement-cost.
     * @param moveCost Original movement cost
     * @return The movement cost after any change
     */
    public int getMovementCost(int moveCost) {
        int cost = moveCost;
        if (movementCostFactor >= 0) {
            float cost2 = cost * movementCostFactor;
            cost = (int)cost2;
            if (cost < cost2) {
                cost++;
            }
        }
        if (movementCost > 0) {
            // Only >0 values are meaningful (see spec).
            // Do not return zero from a movement costing routine or
            // units get free moves!
            if (movementCost < cost) {
                cost = movementCost;
            }
        }
        return cost;
    }

    /**
     * Gets the percent chance that this tile improvement can expose
     * a resource on the tile.
     *
     * @return The exposure chance.
     */
    public int getExposeResourcePercent() {
        return exposeResourcePercent;
    }

    // ------------------------------------------------------------ API methods

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        natural = getAttribute(in, "natural", false);
        addWorkTurns = getAttribute(in, "add-work-turns", 0);
        movementCost = getAttribute(in, "movement-cost", 0);
        movementCostFactor = -1;
        magnitude = getAttribute(in, "magnitude", 1);

        requiredImprovementType = getSpecification().getType(in, "required-improvement",
                                                             TileImprovementType.class, null);

        zIndex = getAttribute(in, "zIndex", 0);
        exposeResourcePercent = getAttribute(in, "exposeResourcePercent", 0);

        expendedEquipmentType = getSpecification().getType(in, "expended-equipment-type",
                                                           EquipmentType.class, null);
        expendedAmount = getAttribute(in, "expended-amount", 0);
        deliverGoodsType = getSpecification().getType(in, "deliver-goods-type", GoodsType.class, null);
        deliverAmount = getAttribute(in, "deliver-amount", 0);
    }

    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("scope".equals(childName)) {
            scopes.add(new Scope(in));
        } else if ("worker".equals(childName)) {
            allowedWorkers.add(in.getAttributeValue(null, ID_ATTRIBUTE_TAG));
            in.nextTag(); // close this element
        } else if ("change".equals(childName)) {
            tileTypeChange.put(getSpecification().getTileType(in.getAttributeValue(null, "from")),
                               getSpecification().getTileType(in.getAttributeValue(null, "to")));
            in.nextTag(); // close this element
        } else {
            super.readChild(in);
        }
    }


    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("natural", Boolean.toString(natural));
        out.writeAttribute("add-work-turns", Integer.toString(addWorkTurns));
        out.writeAttribute("movement-cost", Integer.toString(movementCost));
        out.writeAttribute("magnitude", Integer.toString(magnitude));
        out.writeAttribute("zIndex", Integer.toString(zIndex));
        out.writeAttribute("exposeResourcePercent",
                           Integer.toString(exposeResourcePercent));
        if (requiredImprovementType != null) {
            out.writeAttribute("required-improvement", requiredImprovementType.getId());
        }
        if (expendedEquipmentType != null) {
            out.writeAttribute("expended-equipment-type", expendedEquipmentType.getId());
            out.writeAttribute("expended-amount", Integer.toString(expendedAmount));
        }
        if (deliverGoodsType != null) {
            out.writeAttribute("deliver-goods-type", deliverGoodsType.getId());
            out.writeAttribute("deliver-amount", Integer.toString(deliverAmount));
        }
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);
        if (scopes != null) {
            for (Scope scope : scopes) {
                scope.toXMLImpl(out);
            }
        }
        if (allowedWorkers != null) {
            for (String id : allowedWorkers) {
                out.writeStartElement("worker");
                out.writeAttribute(ID_ATTRIBUTE_TAG, id);
                out.writeEndElement();
            }
        }
        if (tileTypeChange != null) {
            for (Map.Entry<TileType, TileType> entry : tileTypeChange.entrySet()) {
                out.writeStartElement("change");
                out.writeAttribute("from", entry.getKey().getId());
                out.writeAttribute("to", entry.getValue().getId());
                out.writeEndElement();
            }
        }
    }

    public static String getXMLElementTagName() {
        return "tileimprovement-type";
    }


}
