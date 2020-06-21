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

package net.sf.freecol.client.gui.panel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLayeredPane;
import net.sf.freecol.client.ClientOptions;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;


/**
 * A collection of panels and buttons that are used to provide the
 * user with a more detailed view of certain elements on the map and
 * also to provide a means of input in case the user can't use the
 * keyboard.
 *
 * The MapControls are useless by themselves, this object needs to be
 * placed on a JComponent in order to be usable.
 */
public abstract class MapControls extends FreeColClientHolder {

    public static final int MAP_WIDTH = 220;
    public static final int MAP_HEIGHT = 128;
    public static final int GAP = 4;
    public static final int CONTROLS_LAYER = JLayeredPane.MODAL_LAYER;

    protected final InfoPanel infoPanel;
    protected final MiniMap miniMap;
    protected final UnitButton miniMapToggleBorders;
    protected final UnitButton miniMapToggleFogOfWarButton;
    protected final UnitButton miniMapZoomOutButton;
    protected final UnitButton miniMapZoomInButton;
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
     * Initialize the unit buttons.
     *
     * Initialization is deferred until we are confident we are in-game.
     */
    protected boolean initializeUnitButtons() {
        if (this.unitButtons.isEmpty()) return false;
        final ActionManager am = getFreeColClient().getActionManager();
        this.unitButtons.addAll(am.makeUnitActionButtons(getSpecification()));
        return true;
    }

    // Abstract API

    /**
     * Get the components of the map controls.
     *
     * @return A list of {@code Component}s.
     */
    public abstract List<Component> getComponents();

    /**
     * Adds the map controls to the given component.
     *
     * @param component The component to add the map controls to.
     */
    public abstract void addToComponent(Canvas component);

    /**
     * Removes the map controls from the parent canvas component.
     *
     * @param canvas {@code Canvas} parent
     */
    public abstract void removeFromComponent(Canvas canvas);

    /**
     * Are the map controls currently showing?
     *
     * @return True if visible.
     */
    public abstract boolean isShowing();


    // Simple public routines
    
    public boolean canZoomInMapControls() {
        return miniMap.canZoomIn();
    }

    public boolean canZoomOutMapControls() {
        return miniMap.canZoomOut();
    }

    public void repaint() {
        for (Component c : getComponents()) {
            c.repaint();
        }
    }

    public void toggleView() {
        miniMap.setToggleBordersOption(!getClientOptions()
            .getBoolean(ClientOptions.MINIMAP_TOGGLE_BORDERS));
        repaint();
    }
    
    public void toggleFogOfWar() {
        miniMap.setToggleFogOfWarOption(!getClientOptions()
            .getBoolean(ClientOptions.MINIMAP_TOGGLE_FOG_OF_WAR));
        repaint();
    }

    /**
     * Updates this {@code MapControls}.
     *
     * @param active The active {@code Unit} if any.
     */
    public void update(Unit active) {
        final GUI gui = getGUI();

        if (active != null) initializeUnitButtons();
        for (UnitButton ub : this.unitButtons) {
            ub.setVisible(active != null);
        }
        
        switch (gui.getViewMode()) {
        case MOVE_UNITS:
            infoPanel.update(active);
            break;
        case TERRAIN:
            infoPanel.update(gui.getSelectedTile());
            break;
        case MAP_TRANSFORM:
            infoPanel.update(getFreeColClient().getMapEditorController()
                .getMapTransform());
            break;
        case END_TURN:
            infoPanel.update();
            break;
        default:
            break;
        }
    }

    public void zoomIn() {
        miniMap.zoomIn();
        repaint();
    }

    public void zoomOut() {
        miniMap.zoomOut();
        repaint();
    }
}
