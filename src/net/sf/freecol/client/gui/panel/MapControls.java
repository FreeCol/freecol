/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ViewMode;
import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.BuildColonyAction;
import net.sf.freecol.client.gui.action.DisbandUnitAction;
import net.sf.freecol.client.gui.action.UnitStateChangeAction;
import net.sf.freecol.client.gui.action.WaitAction;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.MapTransform;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A collection of panels and buttons that are used to provide
 * the user with a more detailed view of certain elements on the
 * map and also to provide a means of input in case the user
 * can't use the keyboard.
 *
 * The MapControls are useless by themselves, this object needs to
 * be placed on a JComponent in order to be usable.
 */
public final class MapControls {

    private final FreeColClient freeColClient;

    private final InfoPanel        infoPanel;
    private final MiniMap          miniMap;
    private final UnitButton[]     unitButton;
    private final JLabel compassRose;

    private static final int CONTROLS_LAYER = JLayeredPane.MODAL_LAYER;

    /**
     * The basic constructor.
     * @param freeColClient The main controller object for the client
     */
    public MapControls(final FreeColClient freeColClient) {
        this.freeColClient = freeColClient;

        //
        // Create GUI Objects
        //

        infoPanel = new InfoPanel(freeColClient);
        miniMap = new MiniMap(freeColClient);
        compassRose = new JLabel(ResourceManager.getImageIcon("compass.image"));

        final ActionManager am = freeColClient.getActionManager();

        List<UnitButton> ubList = new ArrayList<UnitButton>();
        ubList.add(new UnitButton(am.getFreeColAction(WaitAction.id)));
        ubList.add(new UnitButton(am.getAction(UnitState.SKIPPED)));
        ubList.add(new UnitButton(am.getAction(UnitState.SENTRY)));
        ubList.add(new UnitButton(am.getAction(UnitState.FORTIFYING)));
        for (TileImprovementType type : freeColClient.getGame().getSpecification()
                 .getTileImprovementTypeList()) {
            if (!type.isNatural()) {
                ubList.add(new UnitButton(am.getFreeColAction(type.getShortId() + "Action")));
            }
        }
        ubList.add(new UnitButton(am.getFreeColAction(BuildColonyAction.id)));
        ubList.add(new UnitButton(am.getFreeColAction(DisbandUnitAction.id)));
        unitButton = (ubList.toArray(new UnitButton[0]));

        //
        // Don't allow them to gain focus
        //
        infoPanel.setFocusable(false);
        miniMap.setFocusable(false);
        compassRose.setFocusable(false);

        for(int i=0; i<unitButton.length; i++) {
            unitButton[i].setFocusable(false);
        }

        compassRose.setSize(compassRose.getPreferredSize());
        compassRose.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX() - compassRose.getWidth()/2;
                    int y = e.getY() - compassRose.getHeight()/2;
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    Direction direction = Direction.values()[(int) Math.floor(theta / (Math.PI/4))];
                    freeColClient.getInGameController().moveActiveUnit(direction);
                }
            });

    }

    /**
     * Updates this <code>InfoPanel</code>.
     *
     * @param mapTransform The current MapTransform.
     */
    public void update(MapTransform mapTransform) {
        if (infoPanel != null) {
            infoPanel.update(mapTransform);
        }
    }

    /**
     * Adds the map controls to the given component.
     * @param component The component to add the map controls to.
     */
    public void addToComponent(Canvas component) {
        if (freeColClient.getGame() == null
            || freeColClient.getGame().getMap() == null) {
            return;
        }

        //
        // Relocate GUI Objects
        //

        infoPanel.setLocation(component.getWidth() - infoPanel.getWidth(), component.getHeight() - infoPanel.getHeight());
        miniMap.setLocation(0, component.getHeight() - miniMap.getHeight());
        compassRose.setLocation(component.getWidth() - compassRose.getWidth() - 20, 20);

        final int SPACE = unitButton[0].getWidth() + 5;
        for(int i=0; i<unitButton.length; i++) {
            unitButton[i].setLocation(miniMap.getWidth() +
                                      (infoPanel.getX() - miniMap.getWidth() -
                                       unitButton.length*SPACE)/2 +
                                      i*SPACE,
                                      component.getHeight() - 40);
        }

        //
        // Add the GUI Objects to the container
        //
        component.add(infoPanel, CONTROLS_LAYER, false);
        component.add(miniMap, CONTROLS_LAYER, false);
        if (freeColClient.getClientOptions().getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE)) {
            component.add(compassRose, CONTROLS_LAYER, false);
        }

        if (!freeColClient.isMapEditor()) {
            for(int i=0; i<unitButton.length; i++) {
                component.add(unitButton[i], CONTROLS_LAYER, false);
                Action a = unitButton[i].getAction();
                unitButton[i].setAction(null);
                unitButton[i].setAction(a);
            }
        }
    }

    /**
     * Returns the width of the InfoPanel.
     *
     * @return an <code>int</code> value
     */
    public int getInfoPanelWidth() {
        return infoPanel.getWidth();
    }

    /**
     * Returns the height of the InfoPanel.
     *
     * @return an <code>int</code> value
     */
    public int getInfoPanelHeight() {
        return infoPanel.getHeight();
    }


    /**
     * Removes the map controls from the parent component.
     */
    public void removeFromComponent(Canvas canvas) {
        canvas.remove(infoPanel, false);
        canvas.remove(miniMap, false);
        canvas.remove(compassRose, false);

        for(int i=0; i<unitButton.length; i++) {
            canvas.remove(unitButton[i], false);
        }
    }


    public boolean isShowing() {
        return infoPanel.getParent() != null;
    }


    /**
     * Zooms in the mini map.
     */
    public void zoomIn() {
        miniMap.zoomIn();
    }


    /**
     * Zooms out the mini map.
     */
    public void zoomOut() {
        miniMap.zoomOut();
    }

    public boolean canZoomIn() {
        return miniMap.canZoomIn();
    }

    public boolean canZoomOut() {
        return miniMap.canZoomOut();
    }

    /**
     *
     * @param newColor
     */
    public void changeBackgroundColor(Color newColor) {
    	miniMap.setBackgroundColor(newColor);
    }

    /**
     * Updates this <code>MapControls</code>.
     */
    public void update() {
        GUI gui = freeColClient.getGUI();
        int viewMode = gui.getViewMode().getView();
        switch (viewMode) {
        case ViewMode.MOVE_UNITS_MODE:
            infoPanel.update(gui.getActiveUnit());
            break;
        case ViewMode.VIEW_TERRAIN_MODE:
            if (gui.getSelectedTile() != null) {
                Tile selectedTile = freeColClient.getGame().getMap().getTile(gui.getSelectedTile());
                if (infoPanel.getTile() != selectedTile) {
                    infoPanel.update(selectedTile);
                }
            }
            break;
        }
    }
}
