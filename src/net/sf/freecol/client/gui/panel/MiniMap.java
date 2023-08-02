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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.util.Utils;


/**
 * This component draws a small version of the map.  It allows us to
 * see a larger part of the map and to relocate the viewport by
 * clicking on it.  Pity its not a FreeColClientHolder.
 */
public final class MiniMap extends JPanel implements MouseInputListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MiniMap.class.getName());

    public static final int MAX_TILE_SIZE = 24;
    public static final int MIN_TILE_SIZE = 4;
    public static final int SCALE_STEP = 4;

    private final FreeColClient freeColClient;

    /**
     * tileSize is the size (in pixels) that each tile will take up on
     * the mini map
     */
    private int tileSize;

    /**
     * An image to contain the painted minimap. This is necessary in order to
     * reduce rendering time when scrolling or animating.
     */
    private PrerenderedMinimap prerenderedMinimap = null;
    
    private volatile boolean dirty = true;
    
    private MinimapPainterThread minimapPainterThread = new MinimapPainterThread();
    

    /**
     * The constructor that will initialize this component.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public MiniMap(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;

        setLayout(null);

        tileSize = 4 * freeColClient.getClientOptions()
            .getInteger(ClientOptions.DEFAULT_ZOOM_LEVEL);
        
        addMouseListener(this);
        addMouseMotionListener(this);
        minimapPainterThread.start();
        minimapPainterThread.setPriority(Thread.NORM_PRIORITY - 1);
    }

    /**
     * Internal GUI accessor.
     *
     * @return The {@code GUI}.
     */
    private GUI getGUI() {
        return this.freeColClient.getGUI();
    }

    /**
     * Internal game accessor.
     *
     * @return The {@code Game}.
     */
    private Game getGame() {
        return this.freeColClient.getGame();
    }

    /**
     * Internal Map accessor.
     *
     * @return The {@code Map}.
     */
    private Map getMap() {
        Game game = getGame();
        return (game == null) ? null : game.getMap();
    }

    /**
     * Set the DEFAULT_ZOOM_LEVEL option on the basis of a tile size.
     *
     * @param tileSize The tile size to check.
     */
    private void setZoomOption(int tileSize) {
        int zoom = tileSize / 4;
        freeColClient.getClientOptions()
            .setInteger(ClientOptions.DEFAULT_ZOOM_LEVEL, zoom);
    }

    /**
     * Set the map focus from a mouse click location.
     *
     * @param e The {@code MouseEvent} containing the location.
     */
    private void setFocus(MouseEvent e) {
        if (!e.getComponent().isEnabled()) {
            return;
        }
        
        final Map map = getMap();
        if (map == null) {
            return;
        }
        
        final int x = e.getX(), y = e.getY();

        final Dimension size = getSize();
        final Point focusPoint = getGUI().getFocusMapPoint();
        final Dimension mapTileSize = getGUI().getScaledImageLibrary().scale(ImageLibrary.TILE_SIZE);
        final int mapPointX = focusPoint.x + (mapTileSize.width * (x - size.width / 2)) / tileSize;
        final int mapPointY = focusPoint.y + (mapTileSize.height * (y - size.height / 2)) / (tileSize / 2);
        
        getGUI().setFocusMapPoint(new Point(mapPointX, mapPointY));
    }

    private void recreateBufferImage() {
        final Map map = getMap();
        if (map == null) {
            this.prerenderedMinimap = null;
            return;
        }
        
        final int theTileSize = tileSize;
        final Dimension size = new Dimension(theTileSize * map.getWidth() + theTileSize / 2,
                theTileSize * map.getHeight() / 2 + theTileSize / 4);
        final BufferedImage nextPaintedMinimapImage = Utils.getGoodGraphicsDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(size.width, size.height, Transparency.OPAQUE);
        final Graphics2D g2d = nextPaintedMinimapImage.createGraphics();
        paintEntireMinimap(g2d, theTileSize, size);
        g2d.dispose();
        
        this.prerenderedMinimap = new PrerenderedMinimap(nextPaintedMinimapImage, theTileSize);
    }

    // Public API

    /**
     * Updates the cached minimap image.
     */
    public void updateCachedMinimap() {
        this.dirty = true;
        LockSupport.unpark(minimapPainterThread);
    }
    
    /**
     * Zooms in the mini map.
     */
    public void zoomIn() {
        tileSize = Math.min(tileSize + SCALE_STEP, MAX_TILE_SIZE);
        setZoomOption(tileSize);
        updateCachedMinimap();
    }

    /**
     * Zooms out the mini map.
     */
    public void zoomOut() {
        tileSize = Math.max(tileSize - SCALE_STEP, MIN_TILE_SIZE);
        setZoomOption(tileSize);
        updateCachedMinimap();
    }

    /**
     * Return true if tile size can be decreased.
     *
     * @return a {@code boolean} value
     */
    public boolean canZoomIn() {
        return tileSize < MAX_TILE_SIZE && getMap() != null;
    }

    /**
     * Return true if tile size can be increased.
     *
     * @return a {@code boolean} value
     */
    public boolean canZoomOut() {
        return tileSize > MIN_TILE_SIZE && getMap() != null;
    }
    
    /**
     * Set tile size to the given value, or the minimum or maximum
     * bound of the tile size.
     *
     * FIXME: Public for MapEditorController.createMiniMapThumbnail.
     *
     * @param size an {@code int} value
     */
    public void setTileSize(int size) {
        tileSize = Math.max(Math.min(size, MAX_TILE_SIZE), MIN_TILE_SIZE);
        setZoomOption(tileSize);
        repaint();
    }

    /**
     * Paints a representation of the mapboard onto this component.
     *
     * FIXME: Public for MapEditorController.createMiniMapThumbnail.
     *
     * @param graphics The {@code Graphics} context within which to draw.
     */
    public void paintMap(Graphics2D g2d) {
        if (getMap() == null) {
            return;
        }

        final Point focusPoint = getGUI().getFocusMapPoint();
        if (focusPoint == null) {
            return;
        }

        final Dimension size = getSize();
        g2d.setColor(ImageLibrary.getMinimapBackgroundColor());
        g2d.fillRect(0, 0, size.width, size.height);
        
        final PrerenderedMinimap thePrerenderedMinimap = prerenderedMinimap;
        if (thePrerenderedMinimap == null) {
            return;
        }

        final int theTileSize = thePrerenderedMinimap.tileSize;
        final Dimension mapTileSize = getGUI().getScaledImageLibrary().scale(ImageLibrary.TILE_SIZE);
        final int x = -focusPoint.x * theTileSize / mapTileSize.width + size.width / 2;
        final int y = -focusPoint.y * (theTileSize / 2) / mapTileSize.height + size.height / 2;
        
        g2d.drawImage(thePrerenderedMinimap.paintedMinimapImage, x, y, null);
        
        paintMarkerForVisibleAreaOnMainMap(g2d, size, theTileSize, mapTileSize);
    }

    private void paintMarkerForVisibleAreaOnMainMap(Graphics2D g2d, Dimension size, int tileSize, Dimension mapTileSize) {
        final Dimension actualMapViewDimension = getGUI().getMapViewDimension();
        if (actualMapViewDimension == null) {
            return;
        }
        
        final int whiteRectangleWidth = (actualMapViewDimension.width / mapTileSize.width + 1) * tileSize;
        final int whiteRectangleHeight = (actualMapViewDimension.height / mapTileSize.height + 1) * tileSize / 2;
        
        g2d.setColor(ImageLibrary.getMinimapBorderColor());
        g2d.drawRect(size.width / 2 - whiteRectangleWidth / 2,
                size.height / 2 - whiteRectangleHeight / 2,
                whiteRectangleWidth,
                whiteRectangleHeight);
    }
    
    /**
     * Paints a representation of the mapboard onto this component.
     *
     * FIXME: Public for MapEditorController.createMiniMapThumbnail.
     *
     * @param graphics The {@code Graphics} context within which to draw.
     */
    public void paintEntireMinimap(Graphics graphics, int tileSize, Dimension size) {
        final Graphics2D g = (Graphics2D) graphics;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);

        /* Fill the rectangle with background color */
        final int minimapWidth = size.width, minimapHeight = size.height;
        g.setColor(ImageLibrary.getMinimapBackgroundColor());
        g.fillRect(0, 0, minimapWidth, minimapHeight);

        /* Make sure the mini map won't try to display tiles off the
         * bounds of the world map */
        final Map map = getMap();

        final int tileWidth = tileSize;
        final int tileHeight = tileSize / 2;
        final int halfWidth = tileSize / 2;
        final int halfHeight = tileSize / 4;

        /* Iterate through all the squares on the mini map and paint the
         * tiles based on terrain */
        GeneralPath tilePath = new GeneralPath();
        tilePath.moveTo(halfWidth, 0);
        tilePath.lineTo(tileWidth, halfHeight);
        tilePath.lineTo(halfWidth, tileHeight);
        tilePath.lineTo(0, halfHeight);
        tilePath.closePath();
        
        GeneralPath settlementPath = new GeneralPath(tilePath);
        settlementPath.transform(AffineTransform.getScaleInstance(0.7, 0.7));
        settlementPath.transform(AffineTransform.getTranslateInstance(0.15 * tileWidth, 0.15 * tileHeight));
        
        GeneralPath unitPath = new GeneralPath(tilePath);
        unitPath.transform(AffineTransform.getScaleInstance(0.5, 0.5));
        unitPath.transform(AffineTransform.getTranslateInstance(0.25 * tileWidth, 0.25 * tileHeight));
        
        GeneralPath paintFull = new GeneralPath(tilePath);
        paintFull.transform(AffineTransform.getScaleInstance(1, 1));
        
        g.setStroke(new BasicStroke(1f));

        final ClientOptions clientOptions = freeColClient.getClientOptions();

        final List<Tile> subMap = map.subMap(
                0,
                0,
                map.getWidth(),
                map.getHeight());
        final Tile firstTile = subMap.get(0);
        
        paintEachTile(g, firstTile, tileSize, subMap, (tileG2d, tile) -> {
            if (tile.isExplored()) {
                if (clientOptions.getBoolean(ClientOptions.MINIMAP_TOGGLE_BORDERS)) {
                    g.setColor(ImageLibrary.getMinimapPoliticsColor(tile.getType()));
                    g.fill(tilePath);
                    
                    if (tile.getOwner() != null) {
                        Color nationOwner = tile.getOwner().getNationColor();
                        Color colorTransparent =
                            new Color(nationOwner.getRed(), nationOwner.getGreen(), nationOwner.getBlue(), 100);
                        g.setColor(colorTransparent);
                        g.fill(paintFull);
                    }
                } else {
                    g.setColor(ImageLibrary.getMinimapEconomicColor(tile.getType()));
                    g.fill(tilePath);
                }
                if (!tile.hasSettlement()) {
                    Unit unit = tile.getFirstUnit();
                    if (unit != null) {
                        g.setColor(Color.BLACK);
                        g.draw(unitPath);
                        g.setColor(unit.getOwner().getNationColor());
                        g.fill(unitPath);
                    }
                } else {
                    g.setColor(Color.BLACK);
                    g.draw(settlementPath);
                    g.setColor(tile.getSettlement().getOwner().getNationColor());
                    g.fill(settlementPath);
                }
                if (!freeColClient.isMapEditor()
                    && !freeColClient.getMyPlayer().canSee(tile)
                    && clientOptions.getBoolean(ClientOptions.MINIMAP_TOGGLE_FOG_OF_WAR)) {
                    Color blackTransparent = new Color(0, 0, 0, 100);
                    g.setColor(blackTransparent);
                    g.fill(paintFull);
                }

             }
        });
    }
    
    private static void paintEachTile(Graphics2D g2d, Tile firstTile, int tileSize, List<Tile> tiles, TileRenderingCallback c) {
        if (tiles.isEmpty()) {
            return;
        }
        
        final int x0 = firstTile.getX();
        final int y0 = firstTile.getY();
        
        final int width = tileSize;
        final int halfWidth = tileSize / 2;
        final int halfHeight = tileSize / 4;
        
        int xt0 = 0, yt0 = 0;
        for (Tile t : tiles) {
            final int x = t.getX();
            final int y = t.getY();
            final int xt = (x-x0) * width
                + (y&1) * halfWidth;
            final int yt = (y-y0) * halfHeight;
            g2d.translate(xt - xt0, yt - yt0);
            xt0 = xt; yt0 = yt;

            c.render(g2d, t);
        }
        g2d.translate(-xt0, -yt0);
    }
    
    // Implement MouseInputListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) { setFocus(e); }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {}

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) { setFocus(e); }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {}


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics graphics) {
        if (getMap() == null) return;
        graphics.drawImage(ImageLibrary.getMiniMapBackground(), 0, 0, null);
        paintMap((Graphics2D) graphics);
    }
    
    /**
     * A callback for rendering a single tile.
     */
    private interface TileRenderingCallback {
        
        /**
         * Should render a single tile.
         * 
         * @param tileG2d The {@code Graphics2D} that should be used when drawing
         *      the tile. The coordinates for the {@code Graphics2D} will be
         *      translated so that position (0, 0) is the upper left corner of
         *      the tile image (that is, outside of the tile diamond itself).
         * @param tile The {@code Tile} to be rendered. 
         */
        void render(Graphics2D tileG2d, Tile tile);
    }
    
    private static final class PrerenderedMinimap {
        /**
         * An image to contain the painted minimap. This is necessary in order to
         * reduce rendering time when scrolling or animating.
         */
        private final BufferedImage paintedMinimapImage;
        
        private final int tileSize;

        private PrerenderedMinimap(BufferedImage paintedMinimapImage, int tileSize) {
            this.paintedMinimapImage = paintedMinimapImage;
            this.tileSize = tileSize;
        }
    }
    
    private final class MinimapPainterThread extends Thread {
        @Override
        public void run() {
            while (true) {
                recreateBufferImage();
                dirty = false;
                while (!dirty) {
                    LockSupport.park();
                }
            }
        }
    }
}
