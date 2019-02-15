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

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLayeredPane;
import net.sf.freecol.client.ClientOptions;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.FreeColClientHolder;
import net.sf.freecol.client.control.MapTransform;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.FortifyAction;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.action.MiniMapToggleViewAction;
import net.sf.freecol.client.gui.action.MiniMapToggleFogOfWarAction;
import net.sf.freecol.client.gui.action.MiniMapZoomInAction;
import net.sf.freecol.client.gui.action.MiniMapZoomOutAction;
import net.sf.freecol.client.gui.action.SentryAction;
import net.sf.freecol.client.gui.action.SkipUnitAction;
import net.sf.freecol.client.gui.action.WaitAction;

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
    protected final List<UnitButton> unitButtons;


    /**
     * The basic constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param useSkin Use a skin or not in the info panel.
     */
    protected MapControls(final FreeColClient freeColClient, boolean useSkin) {
        super(freeColClient);

        infoPanel = new InfoPanel(getFreeColClient(), useSkin);
        miniMap = new MiniMap(getFreeColClient());
        final ActionManager am = getFreeColClient().getActionManager();
        unitButtons = new ArrayList<>();

        final Game game = getGame();
        if (game != null) {
            unitButtons.add(new UnitButton(am, WaitAction.id));
            unitButtons.add(new UnitButton(am, SkipUnitAction.id));
            unitButtons.add(new UnitButton(am, SentryAction.id));
            unitButtons.add(new UnitButton(am, FortifyAction.id));
            
            final Specification spec = game.getSpecification();
            if (spec != null) {
                for (TileImprovementType type : spec.getTileImprovementTypeList()) {
                    FreeColAction action = am.getFreeColAction(type.getSuffix()
                                                               + "Action");
                    if (action != null && action.hasOrderButtons()
                        && !type.isNatural()) {
                        unitButtons.add(new UnitButton(am, type.getSuffix() + "Action"));
                    }
                }
            }
            unitButtons.add(new UnitButton(am, BuildColonyAction.id));
            unitButtons.add(new UnitButton(am, DisbandUnitAction.id));
        }
        miniMapToggleBorders = new UnitButton(am, MiniMapToggleViewAction.id);
        miniMapToggleFogOfWarButton = new UnitButton(am, MiniMapToggleFogOfWarAction.id);
        miniMapZoomOutButton = new UnitButton(am, MiniMapZoomOutAction.id);
        miniMapZoomInButton = new UnitButton(am, MiniMapZoomInAction.id);

        miniMapToggleBorders.setFocusable(false);
        miniMapToggleFogOfWarButton.setFocusable(false);
        miniMapZoomOutButton.setFocusable(false);
        miniMapZoomInButton.setFocusable(false);

        //
        // Don't allow them to gain focus
        //
        infoPanel.setFocusable(false);

        for (UnitButton button : unitButtons) {
            button.setFocusable(false);
        }
    }


    /**
     * Adds the map controls to the given component.
     *
     * @param component The component to add the map controls to.
     */
    public abstract void addToComponent(Canvas component);

    public boolean canZoomInMapControls() {
        return miniMap != null && miniMap.canZoomIn();
    }

    public boolean canZoomOutMapControls() {
        return miniMap != null && miniMap.canZoomOut();
    }

    public abstract boolean isShowing();

    /**
     * Removes the map controls from the parent canvas component.
     *
     * @param canvas {@code Canvas} parent
     */
    public abstract void removeFromComponent(Canvas canvas);

    public abstract void repaint();
    
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
     */
    public void update() {
        final GUI gui = getGUI();
        Unit unit = gui.getActiveUnit();

        switch (gui.getViewMode()) {
        case MOVE_UNITS:
            infoPanel.update(unit);
            break;
        case TERRAIN:
            infoPanel.update(gui.getSelectedTile());
            break;
        case MAP_TRANSFORM:
            infoPanel.update(getFreeColClient().getMapEditorController()
                .getMapTransform());
            break;
        case END_TURN:
            infoPanel.update((Unit)null);
            break;
        default:
            break;
        }
        for (UnitButton ub : unitButtons) {
            ub.setVisible(unit != null);
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
