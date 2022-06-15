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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.PropertyList;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A collection of panels and buttons that are used to provide the
 * user with a more detailed view of certain elements on the map and
 * also to provide a means of input in case the user can't use the
 * keyboard.
 */
public final class CornerMapControls extends MapControls {

    private static final Logger logger = Logger.getLogger(CornerMapControls.class.getName());
    
    private class MiniMapPanelSkin extends JPanel {
        
        MiniMapPanelSkin() {
            setOpaque(false);
        }
        
        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (miniMapSkin != null) {
                graphics.drawImage(miniMapSkin, 0, 0, null);
            }
        }
    }

    /** The image library. */
    private final ImageLibrary lib;
    
    /** The compass rose graphic. */
    private final JLabel compassRose;

    /** The mini map has its own panel. */
    private final JPanel miniMapPanel;

    /** A skin for the mini map. */
    private Image miniMapSkin;

    private MiniMapPanelSkin miniMapPanelSkin;


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
    
        this.miniMapPanel = new JPanel();
        this.miniMapPanel.setFocusable(false);
        /**
         * In order to make the setLocation setup work, we need to set
         * the layout to null first, then set the size of the minimap,
         * and then its location.
         */
        this.miniMapPanel.setLayout(null);

        this.miniMapPanelSkin = new MiniMapPanelSkin();
        
        // Add buttons:
        this.miniMapPanel.add(this.miniMapToggleBorders);
        this.miniMapPanel.add(this.miniMapToggleFogOfWarButton);
        this.miniMapPanel.add(this.miniMapZoomInButton);
        this.miniMapPanel.add(this.miniMapZoomOutButton);
        this.miniMapPanel.add(this.miniMapPanelSkin);
        this.miniMapPanel.add(this.miniMap);
        
        updateLayoutIfNeeded();
    }

            
    // Implement MapControls
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLayoutIfNeeded() {
        super.updateLayoutIfNeeded();
        
        final BufferedImage newMinimapSkin = this.lib.getMiniMapSkin();
        if (this.miniMapSkin == newMinimapSkin) {
            // No update necessary.
            return;
        }
        this.miniMapSkin = newMinimapSkin;
        
        
        int width = this.lib.scaleInt(MINI_MAP_WIDTH),
                height = this.lib.scaleInt(MINI_MAP_HEIGHT);
        this.miniMap.setSize(width, height);
        
        if (this.miniMapSkin != null) {
            width = this.miniMapSkin.getWidth(null);
            height = this.miniMapSkin.getHeight(null);
            this.miniMapPanel.setBorder(null);
            this.miniMapPanel.setSize(width, height);
            this.miniMapPanelSkin.setSize(width, height);
            this.miniMapPanel.setOpaque(false);
        } else {
            this.miniMapPanel.setOpaque(true);
            this.miniMap.setBorder(new BevelBorder(BevelBorder.RAISED));
        }

        final PropertyList pl = ResourceManager.getPropertyList("image.skin.MiniMap.properties");
        this.miniMap.setLocation(
                this.lib.scaleInt(pl.getInt("minimap.x")),
                this.lib.scaleInt(pl.getInt("minimap.y")));
        this.miniMap.setSize(
                this.lib.scaleInt(pl.getInt("minimap.width")),
                this.lib.scaleInt(pl.getInt("minimap.height")));
        
        centerComponentOnCoordinate(miniMapToggleBorders, pl, "politicalButton");
        centerComponentOnCoordinate(miniMapToggleFogOfWarButton, pl, "fogOfWarButton");
        centerComponentOnCoordinate(miniMapZoomInButton, pl, "zoomInButton");
        centerComponentOnCoordinate(miniMapZoomOutButton, pl, "zoomOutButton");

        miniMapPanel.revalidate();
        miniMapPanel.repaint();
    }
    
    private void centerComponentOnCoordinate(JComponent component, PropertyList pl, String key) {
        final int x = this.lib.scaleInt(pl.getInt(key + ".x"));
        final int y = this.lib.scaleInt(pl.getInt(key + ".y"));
        component.setLocation(x - component.getWidth() / 2, y - component.getHeight() / 2);
    }

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
        
        ret.addAll(this.unitButtons.stream().filter(b -> !b.isShowing()).collect(Collectors.toList()));

        if (!this.unitButtons.isEmpty() && !this.getFreeColClient().isMapEditor()) {
            final int UNSCALED_SPACE_BETWEEN_BUTTONS = 5;
            final int spaceBetweenButtons = lib.scaleInt(UNSCALED_SPACE_BETWEEN_BUTTONS);
            final Dimension buttonsDimension = calculateTotalDimension(unitButtons, spaceBetweenButtons);
            
            final int totalWidth = buttonsDimension.width + this.miniMapPanel.getWidth() + this.infoPanel.getWidth();
            if (totalWidth < newSize.width) {
                final Point firstButtonPoint = calculateFirstPosition(newSize, unitButtons, spaceBetweenButtons, buttonsDimension);
                layoutUnitButtons(unitButtons, buttonsDimension, firstButtonPoint, spaceBetweenButtons);
            } else {
                final int numberInTopRow = this.unitButtons.size() / 2;
                
                final List<UnitButton> bottomRowButtons = unitButtons.subList(numberInTopRow, unitButtons.size());
                final Dimension buttonsBottomRowDimension = calculateTotalDimension(bottomRowButtons, spaceBetweenButtons);
                final Point firstButtonBottomRowPoint = calculateFirstPosition(newSize, bottomRowButtons, spaceBetweenButtons, buttonsBottomRowDimension);
                layoutUnitButtons(bottomRowButtons,buttonsDimension, firstButtonBottomRowPoint, spaceBetweenButtons);

                final List<UnitButton> topRowButtons = unitButtons.subList(0, numberInTopRow);
                final Dimension buttonsTopRowDimension = calculateTotalDimension(topRowButtons, spaceBetweenButtons);
                final Point firstButtonTopRowPoint = calculateFirstPosition(newSize, bottomRowButtons, spaceBetweenButtons, buttonsTopRowDimension);
                layoutUnitButtons(topRowButtons, buttonsDimension, new Point(firstButtonTopRowPoint.x, firstButtonBottomRowPoint.y - buttonsTopRowDimension.height - spaceBetweenButtons), spaceBetweenButtons);
            }
        }
        return ret;
    }
    
    private static Dimension calculateTotalDimension(List<UnitButton> unitButtons, int spaceBetweenButtons) {
        int width = -spaceBetweenButtons, height = 0;
        for (UnitButton ub : unitButtons) {
            if (ub.isShowing()) continue;
            height = Math.max(height, ub.getHeight());
            width += spaceBetweenButtons + ub.getWidth();
        }
        return new Dimension(width, height);
    }
    
    private Point calculateFirstPosition(Dimension newSize, List<UnitButton> unitButtons, int spaceBetweenButtons, Dimension buttonsDimension) {
        final int x = this.miniMapPanel.getWidth() + 1
                + (this.infoPanel.getX() - this.miniMapPanel.getWidth() - buttonsDimension.width) / 2;
        final int y = newSize.height - buttonsDimension.height - spaceBetweenButtons;
        return new Point(x, y);
    }

    private void layoutUnitButtons(List<UnitButton> unitButtons, final Dimension buttonsDimension, Point firstButtonPoint, final int spaceBetweenButtons) {
        int x = firstButtonPoint.x;
        final int y = firstButtonPoint.y;
        
        for (UnitButton ub : unitButtons) {
            if (ub.isShowing()) continue;
            ub.setLocation(x, y);
            x += spaceBetweenButtons + ub.getWidth();
            ub.refreshAction();
        }
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
