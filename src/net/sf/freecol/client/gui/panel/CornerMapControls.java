/**
 *  Copyright (C) 2002-2020   The FreeCol Team
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

        final boolean rose = getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);
        if (rose) {
            this.compassRose = new JLabel(new ImageIcon(ImageLibrary
                    .getUnscaledImage("image.skin.compass")));
            this.compassRose.setFocusable(false);
            this.compassRose.setSize(compassRose.getPreferredSize());
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
        } else {
            this.compassRose = null;
        }

        this.miniMapPanel = new MiniMapPanel();
        this.miniMapPanel.setFocusable(false);
        /**
         * In order to make the setLocation setup work, we need to set
         * the layout to null first, then set the size of the minimap,
         * and then its location.
         */
        this.miniMapPanel.setLayout(null);
        this.miniMap.setSize(MINI_MAP_WIDTH, MINI_MAP_HEIGHT);
        // Add buttons:
        this.miniMapPanel.add(this.miniMapToggleBorders);
        this.miniMapPanel.add(this.miniMapToggleFogOfWarButton);
        this.miniMapPanel.add(this.miniMapZoomInButton);
        this.miniMapPanel.add(this.miniMapZoomOutButton);
        this.miniMapPanel.add(this.miniMap);

        if ((this.miniMapSkin = ImageLibrary.getMiniMapSkin()) != null) {
            this.miniMapPanel.setBorder(null);
            this.miniMapPanel.setSize(this.miniMapSkin.getWidth(null),
                                      this.miniMapSkin.getHeight(null));
            this.miniMapPanel.setOpaque(false);
            // FIXME: LATER: The values below should be specified by a
            // skin-configuration-file.
            this.miniMap.setLocation(38, 75);
            this.miniMapToggleBorders.setLocation(4,114);
            this.miniMapToggleFogOfWarButton.setLocation(4, 144);
            this.miniMapZoomInButton.setLocation(4, 174);
            this.miniMapZoomOutButton.setLocation(264, 174);
        } else {
            int width = this.miniMapZoomOutButton.getWidth()
                + this.miniMapZoomInButton.getWidth() + 4 * GAP;
            this.miniMapPanel.setOpaque(true);
            this.miniMap.setBorder(new BevelBorder(BevelBorder.RAISED));
            this.miniMap.setLocation(width/2, GAP);
            this.miniMapZoomInButton.setLocation(GAP, 
                MINI_MAP_HEIGHT + GAP - this.miniMapZoomInButton.getHeight());
            this.miniMapZoomOutButton.setLocation(MINI_MAP_WIDTH + 3 * GAP
                | this.miniMapZoomInButton.getWidth(),
                MINI_MAP_HEIGHT + GAP - this.miniMapZoomOutButton.getHeight());
        }
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

        if (this.compassRose != null && !this.compassRose.isShowing()) {
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
        if (this.compassRose != null
            && this.compassRose.isShowing()) ret.add(this.compassRose);
        for (UnitButton ub : this.unitButtons) {
            if (ub.isShowing()) ret.add(ub);
        }
        return ret;
    }
}
