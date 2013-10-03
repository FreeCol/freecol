/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.Graphics;
import java.awt.Image;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.border.BevelBorder;
import javax.swing.JLabel;
import javax.swing.JPanel;

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

    private static final Logger logger = Logger.getLogger(CornerMapControls.class.getName());

    public class MiniMapPanel extends JPanel {
        /**
         * {@inheritDoc}
         */
        @Override
        public void paintComponent(Graphics graphics) {
            if (miniMapSkin != null) {
                graphics.drawImage(miniMapSkin, 0, 0, null);
            }
            super.paintComponent(graphics);
        }
    }

    private final JLabel compassRose;

    private final MiniMapPanel miniMapPanel;

    private Image miniMapSkin;


    /**
     * The basic constructor.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public CornerMapControls(final FreeColClient freeColClient) {
        super(freeColClient, true);

        compassRose = new JLabel(ResourceManager.getImageIcon("compass.image"));
        compassRose.setFocusable(false);
        compassRose.setSize(compassRose.getPreferredSize());
        compassRose.addMouseListener(new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX() - compassRose.getWidth()/2;
                    int y = e.getY() - compassRose.getHeight()/2;
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    freeColClient.getInGameController()
                        .moveActiveUnit(Direction.angleToDirection(theta));
                }
            });

        miniMapSkin = ResourceManager.getImage("MiniMap.skin");

        miniMapPanel = new MiniMapPanel();
        miniMapPanel.setFocusable(false);
        
        /**
         * In order to make the setLocation setup work, we need to set
         * the layout to null first, then set the size of the minimap,
         * and then its location.
         */
        miniMapPanel.setLayout(null);
        miniMap.setSize(MAP_WIDTH, MAP_HEIGHT);
        // Add buttons:
        miniMapPanel.add(miniMapZoomInButton);
        miniMapPanel.add(miniMapZoomOutButton);
        miniMapPanel.add(miniMap);

        if (miniMapSkin != null) {
            miniMapPanel.setBorder(null);
            miniMapPanel.setSize(miniMapSkin.getWidth(null),
                                 miniMapSkin.getHeight(null));
            miniMapPanel.setOpaque(false);
            // TODO-LATER: The values below should be specified by a
            // skin-configuration-file.
            miniMap.setLocation(38, 75);
            miniMapZoomInButton.setLocation(4, 174);
            miniMapZoomOutButton.setLocation(264, 174);
        } else {
            int width = miniMapZoomOutButton.getWidth()
                + miniMapZoomInButton.getWidth() + 4 * GAP;
            miniMapPanel.setOpaque(true);
            miniMap.setBorder(new BevelBorder(BevelBorder.RAISED));
            miniMap.setLocation(width/2, GAP);
            miniMapZoomInButton.setLocation(GAP, 
                MAP_HEIGHT + GAP - miniMapZoomInButton.getHeight());
            miniMapZoomOutButton.setLocation(
                miniMapZoomInButton.getWidth() + MAP_WIDTH + 3 * GAP,
                MAP_HEIGHT + GAP - miniMapZoomOutButton.getHeight());
        }
    }

    /**
     * Adds the map controls to the given canvas.
     *
     * @param canvas The parent <code>Canvas</code>.
     */
    public void addToComponent(Canvas canvas) {
        if (freeColClient.getGame() == null
            || freeColClient.getGame().getMap() == null) {
            return;
        }

        final boolean rose = freeColClient.getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);

        //
        // Relocate GUI Objects
        //
        final int cw = canvas.getWidth();
        final int ch = canvas.getHeight();
        infoPanel.setLocation(cw - infoPanel.getWidth(),
                              ch - infoPanel.getHeight());
        miniMapPanel.setLocation(0, ch - miniMapPanel.getHeight());
        if (rose) {
            compassRose.setLocation(cw - compassRose.getWidth() - 20, 20);
        }

        if (!unitButtons.isEmpty()) {
            final int WIDTH = this.unitButtons.get(0).getWidth();
            final int SPACE = 5;
            int length = unitButtons.size();
            int x = miniMapPanel.getWidth() + 1 +
                ((infoPanel.getX() - miniMapPanel.getWidth() -
                  length * WIDTH - (length - 1) * SPACE - WIDTH) / 2);
            int y = canvas.getHeight() - 40;
            int step = WIDTH + SPACE;

            for (UnitButton button : unitButtons) {
                button.setLocation(x, y);
                x += step;
            }
        }

        //
        // Add the GUI Objects to the container
        //
        canvas.add(infoPanel, CONTROLS_LAYER);
        try {
            canvas.add(miniMapPanel, CONTROLS_LAYER);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Minimap add fail: "
                + miniMapPanel.getLocation(), e);
        }
        if (rose) canvas.add(compassRose, CONTROLS_LAYER);

        if (!freeColClient.isMapEditor()) {
            for (UnitButton button : unitButtons) {
                try {
                    canvas.add(button, CONTROLS_LAYER);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Button add fail: "
                        + button.getLocation(), e);
                }
                button.refreshAction();
            }
        }
    }

    /**
     * Are these map controls showing?
     *
     * @return True if the map controls are showing.
     */
    public boolean isShowing() {
        return infoPanel.getParent() != null;
    }

    /**
     * Removes the map controls from the parent canvas.
     *
     * @param canvas The parent <code>Canvas</code>.
     */
    public void removeFromComponent(Canvas canvas) {
        canvas.removeFromCanvas(infoPanel);
        canvas.removeFromCanvas(miniMapPanel);
        canvas.removeFromCanvas(compassRose);

        for (UnitButton button : unitButtons) {
            canvas.removeFromCanvas(button);
        }
    }

    public void repaint() {
        miniMapPanel.repaint();
    }
}
