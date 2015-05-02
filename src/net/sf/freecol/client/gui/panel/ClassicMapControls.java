/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.action.ActionManager;
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
public final class ClassicMapControls extends MapControls {

    private final JPanel panel;
    private final Font arrowFont;
    private final ActionManager am;


    /**
     * The basic constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ClassicMapControls(final FreeColClient freeColClient) {
        super(freeColClient, false);

        am = freeColClient.getActionManager();
        arrowFont = FontLibrary.createFont(FontLibrary.FontType.SIMPLE,
            FontLibrary.FontSize.SMALL, Font.BOLD);

        panel = new MigPanel();
        panel.setLayout(new MigLayout("wrap 3"));
        panel.add(miniMap, "span, width " + MAP_WIDTH
                           + ", height " + MAP_HEIGHT);

        panel.add(miniMapZoomInButton, "newline 10");
        panel.add(miniMapZoomOutButton, "skip");

        panel.add(makeButton("NW", ResourceManager.getString("arrow.NW")),
                  "newline 20");
        panel.add(makeButton("N",  ResourceManager.getString("arrow.N")));
        panel.add(makeButton("NE", ResourceManager.getString("arrow.NE")));
        panel.add(makeButton("W",  ResourceManager.getString("arrow.W")));
        panel.add(makeButton("E",  ResourceManager.getString("arrow.E")),
                  "skip");
        panel.add(makeButton("SW", ResourceManager.getString("arrow.SW")));
        panel.add(makeButton("S",  ResourceManager.getString("arrow.S")));
        panel.add(makeButton("SE", ResourceManager.getString("arrow.SE")),
                  "wrap 20");

        for (UnitButton button : unitButtons) {
            panel.add(button);
        }

        panel.add(infoPanel, "newline push, span, width "
            + infoPanel.getWidth() + ", height " + infoPanel.getHeight());
    }

    /**
     * Adds the map controls to the given component.
     * @param component The component to add the map controls to.
     */
    @Override
    public void addToComponent(Canvas component) {
        if (freeColClient.getGame() == null
            || freeColClient.getGame().getMap() == null) {
            return;
        }
        int width = (int) panel.getPreferredSize().getWidth();
        panel.setSize(width, component.getHeight());
        panel.setLocation(component.getWidth() - width, 0);
        component.add(panel, CONTROLS_LAYER);
    }

    @Override
    public boolean isShowing() {
        return panel.getParent() != null;
    }

    /**
     * Removes the map controls from the parent canvas component.
     *
     * @param canvas <code>Canvas</code> parent
     */
    @Override
    public void removeFromComponent(Canvas canvas) {
        canvas.removeFromCanvas(panel);
    }

    /**
     * Repaint
     */
    @Override
    public void repaint() {
        panel.repaint();
    }

    private JButton makeButton(String direction, String arrow) {
        JButton button
            = new JButton(am.getFreeColAction("moveAction." + direction));
        button.setFont(arrowFont);
        button.setText(arrow);
        return button;
    }
}
