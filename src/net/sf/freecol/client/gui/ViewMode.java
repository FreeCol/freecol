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

package net.sf.freecol.client.gui;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * This class controls the type of view currently being used
 */
public class ViewMode {
    public static final int MOVE_UNITS_MODE = 0;

    public static final int VIEW_TERRAIN_MODE = 1;

    private static final Logger logger = Logger.getLogger(MapViewer.class.getName());

    private int currentMode;

    private Unit savedActiveUnit;

    private MapViewer mapViewer;


    public ViewMode(MapViewer gui) {
        this.mapViewer = gui;
    }

    public void toggleViewMode() {
        logger.warning("Changing view");
        changeViewMode(1 - currentMode);
    }

    public void changeViewMode(int newViewMode) {

        if (newViewMode == currentMode) {
            logger.warning("Trying to change to the same view mode");
            return;
        }

        currentMode = newViewMode;

        switch (currentMode) {
        case ViewMode.MOVE_UNITS_MODE:
            if (mapViewer.getActiveUnit() == null) {
                mapViewer.setActiveUnit(savedActiveUnit);
            }
            savedActiveUnit = null;
            logger.warning("Change view to Move Units Mode");
            break;
        case ViewMode.VIEW_TERRAIN_MODE:
            savedActiveUnit = mapViewer.getActiveUnit();
            mapViewer.setActiveUnit(null);
            logger.warning("Change view to View Terrain Mode");
            break;
        }
    }

    public int getView() {
        return currentMode;
    }

    public boolean displayTileCursor(Tile tile) {
        if (currentMode == ViewMode.VIEW_TERRAIN_MODE) {

            Tile selectedTile = mapViewer.getSelectedTile();
            if (selectedTile == null || tile == null) {
                return false;
            } else if (selectedTile.equals(tile)) {
                TerrainCursor cursor = mapViewer.getCursor();
                cursor.setTile(tile);
                return true;
            }
        }

        return false;
    }

    public boolean displayUnitCursor(Unit unit) {
        if (currentMode == ViewMode.MOVE_UNITS_MODE) {

            TerrainCursor cursor = mapViewer.getCursor();

            if ((unit == mapViewer.getActiveUnit()) && (cursor.isActive() || (unit.getMovesLeft() == 0))) {
                cursor.setTile(unit.getTile());
                return true;
            }
        }
        return false;
    }
}
