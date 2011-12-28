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


package net.sf.freecol.client.gui.panel;


import javax.swing.JPanel;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;

import net.miginfocom.swing.MigLayout;

/**
 * A collection of panels and buttons that are used to provide
 * the user with a more detailed view of certain elements on the
 * map and also to provide a means of input in case the user
 * can't use the keyboard.
 *
 * The MapControls are useless by themselves, this object needs to
 * be placed on a JComponent in order to be usable.
 */
public final class ClassicMapControls extends MapControls {

    private JPanel panel;

    /**
     * The basic constructor.
     * @param freeColClient The main controller object for the client
     * @param gui
     */
    public ClassicMapControls(final FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, false);
        panel = new JPanel();
        panel.setLayout(new MigLayout("wrap 4"));

        panel.add(miniMap, "span, width " + miniMap.getWidth()
                  + ", height " + miniMap.getHeight());
        panel.add(infoPanel, "span, width " + infoPanel.getWidth()
                  + ", height " + infoPanel.getHeight());

        for (UnitButton button : unitButton) {
            panel.add(button);
        }
        panel.setSize(panel.getPreferredSize());
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
        panel.setLocation(component.getWidth() - panel.getWidth(), 0);
        component.add(panel, CONTROLS_LAYER, false);
    }

    /**
     * Removes the map controls from the parent canvas component.
     *
     * @param canvas <code>Canvas</code> parent
     */
    public void removeFromComponent(Canvas canvas) {
        canvas.remove(panel, false);
    }

}
