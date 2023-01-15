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

package net.sf.freecol.client.gui.mapviewer;

import java.awt.event.ActionListener;
import java.util.List;

import net.sf.freecol.client.gui.GUI.ViewMode;
import net.sf.freecol.client.gui.SwingGUI;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

/**
 * Internal state for the {@link MapViewer}.
 * 
 * Methods in this class should only be used by {@link SwingGUI},
 * {@link net.sf.freecol.client.gui.Canvas} or {@link MapViewer}.
 */
public final class MapViewerState {

    /** The cursor for the selected tile. */
    private TerrainCursor cursor;

    /** A path for a current goto order. */
    private PathNode gotoPath = null;

    /** A path for the active unit. */
    private PathNode unitPath = null;

    /** The view mode in use. */
    private ViewMode viewMode = ViewMode.MOVE_UNITS;
    
    /** The selected tile, for ViewMode.TERRAIN. */
    private Tile selectedTile;
    
    /** The active unit, for ViewMode.MOVE_UNITS. */
    private Unit activeUnit;
    
    private boolean rangedAttackMode = false;

    /** The chat message area. */
    private final ChatDisplay chatDisplay;
    
    private final UnitAnimator unitAnimator;

    
    MapViewerState(ChatDisplay chatDisplay, UnitAnimator unitAnimator, ActionListener al) {
        this.cursor = new TerrainCursor();
        this.cursor.addActionListener(al);
        this.chatDisplay = chatDisplay;
        this.unitAnimator = unitAnimator;
    }

    
    public void setRangedAttackMode(boolean rangedAttackMode) {
        this.rangedAttackMode = rangedAttackMode;
    }
    
    public boolean isRangedAttackMode() {
        return rangedAttackMode;
    }
    
    /**
     * Sets if the cursor should be blinking.
     * 
     * @param cursorBlinking The cursor will start blinking if set to {@code true},
     *      and stop if set to {@code false}
     */
    public void setCursorBlinking(boolean cursorBlinking) {
        if (cursorBlinking) {
            cursor.startBlinking();
        } else {
            cursor.stopBlinking();
        }
    }
    
    /**
     * Add a chat message.
     *
     * @param message The chat message.
     */
    public void displayChat(GUIMessage message) {
        this.chatDisplay.addMessage(message);
    }
    
    /**
     * Gets the unit that should be displayed on the given tile.
     *
     * Used mostly by displayMap, but public for SwingGUI.clickAt.
     *
     * @param unitTile The {@code Tile} to check.
     * @return The {@code Unit} to display or null if none found.
     */
    public Unit findUnitInFront(Tile unitTile) {
        Unit result;

        if (unitTile == null || unitTile.isEmpty()) {
            result = null;

        } else if (this.activeUnit != null
            && this.activeUnit.getTile() == unitTile
            && !unitAnimator.isOutForAnimation(this.activeUnit)) {
            result = this.activeUnit;
        } else if (unitTile.hasSettlement()) {
            result = null;
        } else if (this.activeUnit != null
            && this.activeUnit.isOffensiveUnit()) {
            result = unitTile.getDefendingUnit(this.activeUnit);
        } else {
            // Find the unit with the most moves left, preferring active units.
            result = null;
            List<Unit> units = unitTile.getUnitList();
            int bestScore = -1;
            while (!units.isEmpty()) {
                Unit u = units.remove(0);
                if (unitAnimator.isOutForAnimation(u)) continue;
                boolean active = u.getState() == Unit.UnitState.ACTIVE;
                int score = u.getMovesLeft() + ((active) ? 10000 : 0);
                if (bestScore < score) {
                    bestScore = score;
                    result = u;
                }
            }
        }
        return result;
    }
    
    // Path wrangling, path variables are in MapViewer for displayMap
    
    /**
     * Change the goto path.
     * 
     * @param gotoPath The new goto {@code PathNode}.
     * @return True if the goto path was changed.
     */
    public boolean changeGotoPath(PathNode gotoPath) {
        if (this.gotoPath == gotoPath) return false;
        this.gotoPath = gotoPath;
        this.rangedAttackMode = false;
        return true;
    }

    /**
     * Get the current goto path.
     *
     * @return The goto {@code PathNode}.
     */
    public PathNode getGotoPath() {
        return this.gotoPath;
    }

    /**
     * Set the current active unit path.
     *
     * @param path The new {@code PathNode}.
     */
    public void setUnitPath(PathNode path) {
        this.unitPath = path;
        this.rangedAttackMode = false;
    }
    
 // View Mode and associates

    /**
     * Get the view mode.
     *
     * @return The view mode.
     */
    public ViewMode getViewMode() {
        return this.viewMode;
    }

    /**
     * Set the view mode.
     *
     * @param vm The new {@code ViewMode}.
     */
    public void setViewMode(ViewMode vm) {
        this.viewMode = vm;
        this.rangedAttackMode = false;
    }
    
    /**
     * Gets the active unit.
     *
     * @return The {@code Unit}.
     */
    public Unit getActiveUnit() {
        return this.activeUnit;
    }

    /**
     * Sets the active unit.
     *
     * @param activeUnit The new active {@code Unit}.
     */
    public void setActiveUnit(Unit activeUnit) {
        this.activeUnit = activeUnit;
        this.rangedAttackMode = false;
    }

    /**
     * Gets the selected tile.
     *
     * @return The {@code Tile} selected.
     */
    public Tile getSelectedTile() {
        return this.selectedTile;
    }

    /**
     * Sets the selected tile.
     *
     * @param tile The new selected {@code Tile}.
     */
    public void setSelectedTile(Tile tile) {
        this.selectedTile = tile;
        this.rangedAttackMode = false;
    }

    /**
     * Get the tile with a cursor on it.
     *
     * @return The {@code Tile} found.
     */
    public Tile getCursorTile() {
        Tile ret = null;
        switch (this.viewMode) {
        case MOVE_UNITS:
            if (this.activeUnit != null) ret = this.activeUnit.getTile();
            break;
        case TERRAIN:
            ret = this.selectedTile;
            break;
        default:
            break;
        }
        return ret;
    }

    public UnitAnimator getUnitAnimator() {
        return unitAnimator;
    }
    
    ChatDisplay getChatDisplay() {
        return chatDisplay;
    }
    
    PathNode getUnitPath() {
        return unitPath;
    }

    TerrainCursor getCursor() {
        return cursor;
    }
}
