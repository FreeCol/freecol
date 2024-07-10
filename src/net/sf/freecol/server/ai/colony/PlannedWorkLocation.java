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

package net.sf.freecol.server.ai.colony;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Tile;

/**
 * A location for a worker inside a colony. This can be either a tile or a building.
 */
public class PlannedWorkLocation {

    private Tile tile;
    private BuildingType buildingType;
    
    
    public PlannedWorkLocation(Tile tile) {
        this.tile = tile;
    }
    
    public PlannedWorkLocation(BuildingType buildingType) {
        this.buildingType = buildingType;
    }
    
    
    public Tile getTile() {
        return tile;
    }
    
    public BuildingType getBuildingType() {
        return buildingType;
    }
        
    @Override
    public String toString() {
        if (tile != null) {
            return tile.getX() + "," + tile.getY();
        } else {
            return Utils.compressIdForLogging(buildingType.getId());
        }
    }
}
