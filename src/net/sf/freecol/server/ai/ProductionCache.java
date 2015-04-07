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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitTypeChange;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.model.UnitTypeChange.ChangeType;


/**
 * The production cache is intended to record all possible
 * combinations of units producing goods in a colony's work
 * locations. These entries are sorted, allowing fast retrieval of the
 * most efficient way to produce a given type of goods.
 */
public class ProductionCache {

    private final Colony colony;

    /**
     * The units available in the colony.
     */
    private final Set<Unit> units;

    /**
     * The available colony tiles.
     */
    private final Set<ColonyTile> colonyTiles;

    /**
     * Sorted entries per goods type.
     */
    private final Map<GoodsType, List<Entry>> entries;

    /**
     * The assigned entries.
     */
    private final List<Entry> assigned = new ArrayList<>();

    /**
     * The reserved entries.
     */
    private final List<Entry> reserved = new ArrayList<>();

    /**
     * Compares entries by production.
     */
    private static final Comparator<Entry> defaultComparator =
        new CacheEntryComparator();

    /**
     * Compares entries by market value of production.
     */
    private static final Comparator<Entry> marketValueComparator =
        new CacheEntryComparator() {
            @Override
            public int compareProduction(Entry entry1, Entry entry2) {
                int production = entry2.getProduction() - entry1.getProduction();
                Market market = entry1.getUnit().getOwner().getMarket();
                if (market != null) {
                    production = market.getSalePrice(entry2.getGoodsType(), entry2.getProduction())
                        - market.getSalePrice(entry1.getGoodsType(), entry1.getProduction());
                }
                return production;
            }
        };

    /**
     * The number of units available.
     */
    private int unitCount;

    /**
     * The number of Units in various buildings.
     */
    private final TypeCountMap<BuildingType> unitCounts = new TypeCountMap<>();


    public ProductionCache(Colony colony) {
        this.colony = colony;
        this.units = new HashSet<>(colony.getUnitList());
        this.unitCount = units.size();
        this.colonyTiles = new HashSet<>();
        // this assumes all colonists can be added to any tile
        Unit someUnit = colony.getUnitList().get(0);
        for (ColonyTile colonyTile : colony.getColonyTiles()) {
            if (colonyTile.canAdd(someUnit)) {
                colonyTiles.add(colonyTile);
            }
        }
        this.entries = new HashMap<>();
    }

    private List<Entry> createEntries(GoodsType goodsType) {
        // FIXME: OO/generic
        List<Entry> result = new ArrayList<>();
        if (goodsType.isFarmed()) {
            for (ColonyTile colonyTile : colonyTiles) {
                Tile tile = colonyTile.getWorkTile();
                if (tile.getPotentialProduction(goodsType, null) > 0
                    || (tile.hasResource()
                        && !tile.getTileItemContainer().getResource().getType()
                        .getModifiers(goodsType.getId()).isEmpty())) {
                    for (Unit unit : units) {
                        result.add(new Entry(goodsType, colonyTile, unit));
                    }
                }
            }
        } else {
            for (WorkLocation wl : colony.getWorkLocationsForProducing(goodsType)) {
                if (!(wl instanceof Building)) continue;
                Building building = (Building)wl;
                if (building.getType().getWorkPlaces() > 0) {
                    for (Unit unit : units) {
                        result.add(new Entry(goodsType, building, unit));
                    }
                }
            }
        }
        Collections.sort(result, defaultComparator);
        entries.put(goodsType, result);
        return result;
    }

    public Set<Unit> getUnits() {
        return units;
    }

    public int getUnitCount() {
        return unitCount;
    }

    public int getUnitCount(BuildingType buildingType) {
        return unitCounts.getCount(buildingType);
    }

    public int decrementUnitCount(BuildingType buildingType) {
        Integer result = unitCounts.incrementCount(buildingType, -1);
        return (result == null) ? 0 : result;
    }

    public List<Entry> getAssigned() {
        return assigned;
    }

    public List<Entry> getReserved() {
        return reserved;
    }


    public List<Entry> getEntries(GoodsType goodsType) {
        List<Entry> result = entries.get(goodsType);
        if (result == null) {
            result = createEntries(goodsType);
        }
        return result;
    }

    public List<Entry> getEntries(List<GoodsType> goodsTypes) {
        return getEntries(goodsTypes, false);
    }

    public List<Entry> getEntries(List<GoodsType> goodsTypes, boolean useMarketValues) {
        List<Entry> result = new ArrayList<>();
        for (GoodsType goodsType : goodsTypes) {
            result.addAll(getEntries(goodsType));
        }
        if (useMarketValues) {
            Collections.sort(result, marketValueComparator);
        } else {
            Collections.sort(result, defaultComparator);
        }
        return result;
    }



    /**
     * Assigns an entry. All conflicting entries, i.e. entries that
     * refer to the same unit or colony tile, are removed from the
     * cache.
     *
     * @param entry an <code>Entry</code> value
     */
    public void assign(Entry entry) {
        ColonyTile colonyTile = null;
        Building building = null;
        if (entry.getWorkLocation() instanceof ColonyTile) {
            colonyTile = (ColonyTile) entry.getWorkLocation();
            colonyTiles.remove(colonyTile);
        } else if (entry.getWorkLocation() instanceof Building) {
            building = (Building) entry.getWorkLocation();
            unitCounts.incrementCount(building.getType(), 1);
        }
        Unit unit = null;
        if (!entry.isOtherExpert()) {
            unit = entry.getUnit();
            units.remove(unit);
            assigned.add(entry);
            removeEntries(unit, colonyTile, reserved);
        } else {
            if (colonyTile == null) {
                if (unitCounts.getCount(building.getType()) == 1) {
                    // only add building once
                    reserved.addAll(entries.get(entry.getGoodsType()));
                }
            } else {
                reserved.addAll(removeEntries(null, colonyTile, entries.get(entry.getGoodsType())));
            }
        }
        // if work location is a colony tile, remove it from all other
        // lists, because it only supports a single unit
        for (List<Entry> entryList : entries.values()) {
            removeEntries(unit, colonyTile, entryList);
        }
        unitCount--;
    }

    /*
    private void removeEntries(Unit unit, WorkLocation workLocation) {
        units.remove(unit);
        if (workLocation instanceof ColonyTile) {
            colonyTiles.remove((ColonyTile) workLocation);
        }
        for (List<Entry> entryList : entries.values()) {
            removeEntries(unit, workLocation, entryList);
        }
        removeEntries(unit, null, reserved);
    }
    */

    /**
     * Removes all entries that refer to the unit or work location
     * given from the given list of entries and returns them.
     *
     * @param unit a <code>Unit</code>
     * @param workLocation a <code>WorkLocation</code>
     * @param entryList a <code>List</code> of <code>Entry</code>s
     * @return the <code>Entry</code>s removed
     */
    public static List<Entry> removeEntries(Unit unit, WorkLocation workLocation, List<Entry> entryList) {
        Iterator<Entry> entryIterator = entryList.iterator();
        List<Entry> removedEntries = new ArrayList<>();
        while (entryIterator.hasNext()) {
            Entry entry = entryIterator.next();
            if (entry.getUnit() == unit
                || entry.getWorkLocation() == workLocation) {
                removedEntries.add(entry);
                entryIterator.remove();
            }
        }
        return removedEntries;
    }


    /**
     * An Entry in the production cache represents a single unit
     * producing goods in a certain work location. It records
     * information on the type and amount of goods produced, as well
     * as on whether the unit is an expert for producing this type of
     * goods, or can be upgraded to one.
     *
     */
    public static class Entry {
        private final GoodsType goodsType;
        private final WorkLocation workLocation;
        private final Unit unit;
        private final int production;
        private boolean isExpert = false;
        private boolean isOtherExpert = false;
        private boolean unitUpgrades = false;
        private boolean unitUpgradesToExpert = false;

        public Entry(GoodsType g, WorkLocation w, Unit u) {
            goodsType = g;
            workLocation = w;
            unit = u;
            production = w.getProductionOf(u, g);
            GoodsType expertProduction = unit.getType().getExpertProduction();
            if (expertProduction != null) {
                if (expertProduction == goodsType) {
                    isExpert = true;
                } else {
                    isOtherExpert = true;
                }
            } else {
                for (UnitTypeChange change : unit.getType().getTypeChanges()) {
                    if (change.asResultOf(ChangeType.EXPERIENCE)) {
                        unitUpgrades = true;
                        if (change.getNewUnitType().getExpertProduction() == goodsType) {
                            unitUpgradesToExpert = true;
                            break;
                        }
                    }
                }
            }
        }

        /**
         * Returns the type of goods produced.
         *
         * @return a <code>GoodsType</code> value
         */
        public GoodsType getGoodsType() {
            return goodsType;
        }

        /**
         * Returns the work location where goods are produced.
         *
         * @return a <code>WorkLocation</code> value
         */
        public WorkLocation getWorkLocation() {
            return workLocation;
        }

        /**
         * Returns a unit producing goods in this work location.
         *
         * @return an <code>Unit</code> value
         */
        public Unit getUnit() {
            return unit;
        }

        /**
         * Returns the amount of goods produced.
         *
         * @return an <code>int</code> value
         */
        public int getProduction() {
            return production;
        }

        /**
         * Returns true if the unit is an expert for producing the
         * type of goods selected.
         *
         * @return a <code>boolean</code> value
         */
        public boolean isExpert() {
            return isExpert;
        }

        /**
         * Returns true if the unit is an expert for producing a type
         * of goods other than the one selected.
         *
         * @return a <code>boolean</code> value
         */
        public boolean isOtherExpert() {
            return isOtherExpert;
        }

        /**
         * Returns true if the unit can be upgraded through experience.
         *
         * @return a <code>boolean</code> value
         */
        public boolean unitUpgrades() {
            return unitUpgrades;
        }

        /**
         * Returns true if the unit can be upgraded to an expert for
         * producing the type of goods selected through experience.
         *
         * @return a <code>boolean</code> value
         */
        public boolean unitUpgradesToExpert() {
            return unitUpgradesToExpert;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(64);
            sb.append("Cache entry: ").append(unit.toString());
            if (goodsType != null) {
                sb.append(" ").append(goodsType.getSuffix());
            }
            if (workLocation instanceof ColonyTile) {
                sb.append(workLocation.getTile().getType().getSuffix());
            } else if (workLocation instanceof Building) {
                sb.append(((Building)workLocation).getType().getSuffix());
            }
            sb.append("(").append(workLocation.getId()).append(") ");
            return sb.toString();
        }
    }
}
