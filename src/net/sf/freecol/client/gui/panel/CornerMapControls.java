/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JLabel;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.common.model.Map.Direction;
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
public final class CornerMapControls extends MapControls {

    private final JLabel compassRose;

    /**
     * The basic constructor.
     * @param freeColClient The main controller object for the client
     * @param gui
     */
    public CornerMapControls(final FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, true);
        compassRose = new JLabel(ResourceManager.getImageIcon("compass.image"));
        compassRose.setFocusable(false);
        compassRose.setSize(compassRose.getPreferredSize());
        compassRose.addMouseListener(new MouseAdapter() {
                @Override
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

    public boolean isShowing() {
        return getInfoPanel().getParent() != null;
    }

    /**
     * Adds the map controls to the given component.
     * @param component The component to add the map controls to.
     */
    public void addToComponent(Canvas component) {
        if (getFreeColClient().getGame() == null
            || getFreeColClient().getGame().getMap() == null) {
            return;
        }

        //
        // Relocate GUI Objects
        //

        MiniMap miniMap = getMiniMap();
        InfoPanel infoPanel = getInfoPanel();

        infoPanel.setLocation(component.getWidth() - infoPanel.getWidth(), component.getHeight() - infoPanel.getHeight());
        miniMap.setLocation(0, component.getHeight() - miniMap.getHeight());
        compassRose.setLocation(component.getWidth() - compassRose.getWidth() - 20, 20);

        List<UnitButton> unitButtons = getUnitButtons();
        if (!unitButtons.isEmpty()) {
            final int WIDTH = unitButtons.get(0).getWidth();
            final int SPACE = 5;
            int length = unitButtons.size();
            int x = miniMap.getWidth() + 1 +
                ((infoPanel.getX() - miniMap.getWidth() -
                  length * WIDTH - (length - 1) * SPACE - WIDTH) / 2);
            int y = component.getHeight() - 40;
            int step = WIDTH + SPACE;

            for (UnitButton button : unitButtons) {
                button.setLocation(x, y);
                x += step;
            }
        }

        //
        // Add the GUI Objects to the container
        //
        component.add(infoPanel, CONTROLS_LAYER, false);
        component.add(miniMap, CONTROLS_LAYER, false);
        if (getFreeColClient().getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE)) {
            component.add(compassRose, CONTROLS_LAYER, false);
        }

        if (!getFreeColClient().isMapEditor()) {
            for (UnitButton button : unitButtons) {
                component.add(button, CONTROLS_LAYER, false);
                button.refreshAction();
            }
        }
    }

    /**
     * Removes the map controls from the parent canvas component.
     *
     * @param canvas <code>Canvas</code> parent
     */
    public void removeFromComponent(Canvas canvas) {
        canvas.remove(getInfoPanel(), false);
        canvas.remove(getMiniMap(), false);
        canvas.remove(compassRose, false);

        for (UnitButton button : getUnitButtons()) {
            canvas.remove(button, false);
        }
    }

}
