/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

/**
 * A Null-Object implementation of {@link Location}.
 *
 * Used as a temporary placeholder for {@link Locatable} objects (like Goods)
 * when their original container is destroyed but they have not yet been
 * assigned a new one (e.g., during looting).
 *
 * This class is intentionally minimal and side-effect free.
 */
public final class LootLocation extends FreeColGameObject implements Location {

    /** Singleton instance to avoid identity mismatches. */
    public static final LootLocation INSTANCE = new LootLocation();

    /** A stable ID string. */
    public static final String STR_ID = "model.location.loot";

    /** Private constructor to enforce singleton. */
    private LootLocation() {
        super(null, STR_ID);  // null Game is acceptable for special objects
    }

    // --- FreeColObject / FreeColGameObject XML methods ---
    @Override
    public String getXMLTagName() {
        return "lootLocation";
    }

    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // No attributes to write
    }

    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        // No children to write
    }

    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // Nothing to read
    }

    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Nothing to read
    }

    // --- Location interface implementation ---

    @Override
    public Tile getTile() {
        return null; // Loot is not on a tile
    }

    @Override
    public StringTemplate getLocationLabel() {
        return StringTemplate.key("model.location.loot.label");
    }

    @Override
    public StringTemplate getLocationLabelFor(Player player) {
        return getLocationLabel();
    }

    @Override
    public boolean add(Locatable locatable) {
        return true; // Accept anything, but do not store it
    }

    @Override
    public boolean remove(Locatable locatable) {
        return true;
    }

    @Override
    public boolean contains(Locatable locatable) {
        return false;
    }

    @Override
    public boolean canAdd(Locatable locatable) {
        return true;
    }

    @Override
    public int getUnitCount() {
        return 0;
    }

    @Override
    public Stream<Unit> getUnits() {
        return Stream.empty();
    }

    @Override
    public List<Unit> getUnitList() {
        return Collections.emptyList();
    }

    @Override
    public GoodsContainer getGoodsContainer() {
        return null; // Cannot store goods permanently
    }

    @Override
    public Settlement getSettlement() {
        return null;
    }

    @Override
    public Colony getColony() {
        return null;
    }

    @Override
    public IndianSettlement getIndianSettlement() {
        return null;
    }

    @Override
    public Location up() {
        return this; // No parent location
    }

    @Override
    public int getRank() {
        return LOCATION_RANK_NOWHERE;
    }

    @Override
    public String toShortString() {
        return "Looting...";
    }

    @Override
    public String toString() {
        return "LootLocation";
    }
}
