/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Unit;


/**
 * A collection of panels and buttons that are used to provide the
 * user with a more detailed view of certain elements on the map and
 * also to provide a means of input in case the user can't use the
 * keyboard.
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

    /** The image library. */
    private final ImageLibrary lib;
    
    /** The compass rose graphic. */
    private final JLabel compassRose;

    /** The mini map has its own panel. */
    private final MiniMapPanel miniMapPanel;

    /** A skin for the mini map. */
    private final Image miniMapSkin;


    /**
     * The basic constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public CornerMapControls(final FreeColClient freeColClient) {
        super(freeColClient, true);

        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        this.compassRose = this.lib.getCompassRose();
        this.compassRose.setFocusable(false);
        this.compassRose.setSize(this.compassRose.getPreferredSize());
        this.compassRose.addMouseListener(new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    Unit unit = getGUI().getActiveUnit();
                    if (unit == null) return;
                    int x = e.getX() - compassRose.getWidth()/2;
                    int y = e.getY() - compassRose.getHeight()/2;
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    igc().moveUnit(unit, Direction.angleToDirection(theta));
                }
            });
    
        this.miniMapPanel = new MiniMapPanel();
        this.miniMapPanel.setFocusable(false);
        /**
         * In order to make the setLocation setup work, we need to set
         * the layout to null first, then set the size of the minimap,
         * and then its location.
         */
        this.miniMapPanel.setLayout(null);
        int width = this.lib.scaleInt(MINI_MAP_WIDTH),
            height = this.lib.scaleInt(MINI_MAP_HEIGHT);
        this.miniMap.setSize(width, height);
                             
        // Add buttons:
        this.miniMapPanel.add(this.miniMapToggleBorders);
        this.miniMapPanel.add(this.miniMapToggleFogOfWarButton);
        this.miniMapPanel.add(this.miniMapZoomInButton);
        this.miniMapPanel.add(this.miniMapZoomOutButton);
        this.miniMapPanel.add(this.miniMap);

        if ((this.miniMapSkin = this.lib.getMiniMapSkin()) != null) {
            width = this.miniMapSkin.getWidth(null);
            height = this.miniMapSkin.getHeight(null);
            this.miniMapPanel.setBorder(null);
            this.miniMapPanel.setSize(width, height);
            this.miniMapPanel.setOpaque(false);
        } else {
            this.miniMapPanel.setOpaque(true);
            this.miniMap.setBorder(new BevelBorder(BevelBorder.RAISED));
        }
        int x = GAP;
        int y = height - this.miniMapZoomInButton.getHeight() - 2 * GAP;
        this.miniMapZoomInButton.setLocation(x, y);
        y -= this.miniMapZoomInButton.getHeight() -  2 * GAP;
        this.miniMapToggleFogOfWarButton.setLocation(x, y);
        y -= this.miniMapToggleFogOfWarButton.getHeight() - 2 * GAP;
        this.miniMapToggleBorders.setLocation(x, y);
        x += this.miniMapZoomInButton.getWidth() + GAP;
        y = height - 2 * GAP - this.miniMap.getHeight();
        this.miniMap.setLocation(x, y);
        x += this.miniMap.getWidth() + GAP;
        y = height - this.miniMapZoomOutButton.getHeight() - 2 * GAP;
        this.miniMapZoomOutButton.setLocation(x, y);
    }

            
    // Implement MapControls

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Component> getComponentsToAdd(Dimension newSize) {
        List<Component> ret = new ArrayList<>();
        if (getGame() == null) return ret;
        
        final int cw = newSize.width;
        final int ch = newSize.height;

        if (!this.infoPanel.isShowing()) {
            this.infoPanel.setLocation(cw - this.infoPanel.getWidth(),
                                       ch - this.infoPanel.getHeight());
            this.infoPanel.refresh();
            ret.add(this.infoPanel);
        }
        
        if (!this.miniMapPanel.isShowing()) {
            this.miniMapPanel.setLocation(0, ch - this.miniMapPanel.getHeight());
            ret.add(this.miniMapPanel);
        }

        final boolean rose = getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);
        if (rose && !this.compassRose.isShowing()) {
            this.compassRose.setLocation(cw - this.compassRose.getWidth() - 20, 20);
            ret.add(this.compassRose);
        }

        if (!this.unitButtons.isEmpty()
            && !this.getFreeColClient().isMapEditor()) {
            final int SPACE = 5;
            int width = -SPACE, height = 0;
            for (UnitButton ub : this.unitButtons) {
                if (ub.isShowing()) continue;
                height = Math.max(height, ub.getHeight());
                width += SPACE + ub.getWidth();
            }
            int x = this.miniMapPanel.getWidth() + 1
                + (this.infoPanel.getX() - this.miniMapPanel.getWidth() - width) / 2;
            int y = ch - height - SPACE;
            logger.info("Unitbuttons at " + x + "," + y
                + " spaced " + SPACE + " in " + cw + "," + ch);
            for (UnitButton ub : this.unitButtons) {
                if (ub.isShowing()) continue;
                ub.setLocation(x, y);
                x += SPACE + ub.getWidth();
                ub.refreshAction();
                ret.add(ub);
            }
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Component> getComponentsPresent() {
        List<Component> ret = new ArrayList<>();
        if (this.infoPanel.isShowing()) ret.add(this.infoPanel);
        if (this.miniMapPanel.isShowing()) ret.add(this.miniMapPanel);
        final boolean rose = getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);
        if (rose && this.compassRose.isShowing()) ret.add(this.compassRose);
        for (UnitButton ub : this.unitButtons) {
            if (ub.isShowing()) ret.add(ub);
        }
        return ret;
    }
}
