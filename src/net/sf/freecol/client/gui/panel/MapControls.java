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

package net.sf.freecol.client.gui.panel;

import static net.sf.freecol.common.util.StringUtils.lastPart;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Introspector;


/**
 * A collection of panels and buttons that are used to provide the
 * user with a more detailed view of certain elements on the map and
 * also to provide a means of input in case the user can't use the
 * keyboard.
 */
public abstract class MapControls extends FreeColClientHolder {

    private static final Logger logger = Logger.getLogger(MapControls.class.getName());
    public static final int MINI_MAP_WIDTH = 220;
    public static final int MINI_MAP_HEIGHT = 128;
    public static final int GAP = 4;

    /** The info panel, showing current active unit et al. */
    protected final InfoPanel infoPanel;

    /** The mini map, showing the whole of map context. */
    protected final MiniMap miniMap;

    /** Special purpose buttons for the mini map. */
    protected final UnitButton miniMapToggleBorders,
        miniMapToggleFogOfWarButton, miniMapZoomOutButton, miniMapZoomInButton;

    /** The buttons to control unit actions. */
    protected final List<UnitButton> unitButtons = new ArrayList<>();


    /**
     * The basic constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param useSkin Use a skin or not in the info panel.
     */
    protected MapControls(final FreeColClient freeColClient, boolean useSkin) {
        super(freeColClient);

        this.infoPanel = new InfoPanel(freeColClient, useSkin);
        this.infoPanel.setFocusable(false);

        this.miniMap = new MiniMap(freeColClient);

        final ActionManager am = freeColClient.getActionManager();
        List<UnitButton> miniButtons = am.makeMiniMapButtons();
        for (UnitButton ub : miniButtons) ub.setFocusable(false);
        // Pop off the first four special cases
        this.miniMapToggleBorders = miniButtons.remove(0);
        this.miniMapToggleFogOfWarButton = miniButtons.remove(0);
        this.miniMapZoomOutButton = miniButtons.remove(0);
        this.miniMapZoomInButton = miniButtons.remove(0);
    }


    /**
     * Updates the layout with possibly a new skin and/or size.
     */
    public void updateLayoutIfNeeded() {
        infoPanel.updateLayoutIfNeeded();
    }
    
    /**
     * Initialize the unit buttons.
     *
     * Initialization is deferred until in-game and the action manager is
     * available.
     *
     * @return True if initialization occurs.
     */
    protected boolean initializeUnitButtons() {
        if (!this.unitButtons.isEmpty()) return false;
        final ActionManager am = getFreeColClient().getActionManager();
        this.unitButtons.addAll(am.makeUnitActionButtons(getSpecification()));
        return true;
    }


    // Abstract API

    /**
     * Prepare and return a list of map controls components to add to
     * the canvas.
     *
     * @param size The {@code Dimension} of the canvas.
     * @return A list of {@code Component}s to add to the canvas.
     */
    public abstract List<Component> getComponentsToAdd(Dimension size);

    /**
     * Prepare and return a list of map controls components to remove
     * from the canvas.
     *
     * @return A list of {@code Component}s to remove from the canvas.
     */
    public abstract List<Component> getComponentsPresent();
    

    // Simple public routines
    
    public boolean canZoomInMapControls() {
        return this.miniMap.canZoomIn();
    }

    public boolean canZoomOutMapControls() {
        return this.miniMap.canZoomOut();
    }

    public void repaint() {
        for (Component c : getComponentsPresent()) {
            c.repaint();
        }
    }

    /**
     * Updates this {@code MapControls}.
     *
     * @param viewMode The gui {@code ViewMode}.
     * @param active The active {@code Unit} if any.
     * @param tile The selected {@code Tile} if any.
     */
    public void update(GUI.ViewMode viewMode, Unit active, Tile tile) {
        updateLayoutIfNeeded();
        
        if (active != null) {
            boolean iub = initializeUnitButtons();
            if (iub) logger.fine("Unit buttons initialized.");
        }
        for (UnitButton ub : this.unitButtons) {
            ub.setVisible(active != null);
        }
        
        switch (viewMode) {
        case MOVE_UNITS:
            this.infoPanel.update(active);
            break;
        case TERRAIN:
            this.infoPanel.update(tile);
            break;
        case MAP_TRANSFORM:
            this.infoPanel.update(getFreeColClient().getMapEditorController()
                .getMapTransform());
            break;
        case END_TURN:
            this.infoPanel.update();
            break;
        default:
            break;
        }
    }
    
    /**
     * Updates the minimap and repaints it.
     */
    public void updateMinimap() {
        this.miniMap.updateCachedMinimap();
        repaint();
    }

    public void zoomIn() {
        this.miniMap.zoomIn();
        repaint();
    }

    public void zoomOut() {
        this.miniMap.zoomOut();
        repaint();
    }

    public void clear() {
        unitButtons.clear();
    } 
}
