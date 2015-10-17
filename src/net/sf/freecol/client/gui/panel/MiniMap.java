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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This component draws a small version of the map.  It allows us to
 * see a larger part of the map and to relocate the viewport by
 * clicking on it.
 */
public final class MiniMap extends JPanel implements MouseInputListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MiniMap.class.getName());

    public static final int MAX_TILE_SIZE = 24;
    public static final int MIN_TILE_SIZE = 4;
    public static final int SCALE_STEP = 4;

    private final FreeColClient freeColClient;

    private final GUI gui;

    private int tileSize; //tileSize is the size (in pixels) that each tile will take up on the mini map

    /**
     * The top left tile on the mini map represents the tile.
     * (firstColumn, firstRow) in the world map
     */
    private int firstColumn, firstRow;

    /**
     * Used for adjusting the position of the mapboard image.
     * @see #paintMap
     */
    private int adjustX = 0, adjustY = 0;


    /**
     * The constructor that will initialize this component.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MiniMap(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.gui = freeColClient.getGUI();

        setLayout(null);

        tileSize = 4 * (freeColClient.getClientOptions().getInteger(ClientOptions.DEFAULT_MINIMAP_ZOOM) + 1);

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    /**
     * Zooms in the mini map.
     */
    public void zoomIn() {
        tileSize = Math.min(tileSize + SCALE_STEP, MAX_TILE_SIZE);
        setZoomOption(tileSize);
        repaint();
    }

    /**
     * Zooms out the mini map.
     */
    public void zoomOut() {
        tileSize = Math.max(tileSize - SCALE_STEP, MIN_TILE_SIZE);
        setZoomOption(tileSize);
        repaint();
    }

    /**
     * Set tile size to the given value, or the minimum or maximum
     * bound of the tile size.
     *
     * @param size an <code>int</code> value
     */
    public void setTileSize(int size) {
        tileSize = Math.max(Math.min(size, MAX_TILE_SIZE), MIN_TILE_SIZE);
        setZoomOption(tileSize);
        repaint();
    }

    /**
     * Return true if tile size can be decreased.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canZoomIn() {
        return (freeColClient.getGame() != null
                && freeColClient.getGame().getMap() != null
                && tileSize < MAX_TILE_SIZE);
    }

    /**
     * Return true if tile size can be increased.
     *
     * @return a <code>boolean</code> value
     */
    public boolean canZoomOut() {
        return (freeColClient.getGame() != null
                && freeColClient.getGame().getMap() != null
                && tileSize > MIN_TILE_SIZE);
    }

    private void setZoomOption(int tileSize) {
        int zoom = tileSize / 4 - 1;
        freeColClient.getClientOptions()
            .setInteger(ClientOptions.DEFAULT_MINIMAP_ZOOM, zoom);
    }

    public void setToggleBordersOption(boolean toggle) {
        freeColClient.getClientOptions()
            .setBoolean(ClientOptions.MINIMAP_TOGGLE_BORDERS, toggle);
    }
    
    public void setToggleFogOfWarOption(boolean toggle) {
        freeColClient.getClientOptions()
            .setBoolean(ClientOptions.MINIMAP_TOGGLE_FOG_OF_WAR, toggle);
    }
    
    /**
     * Paints this component.
     * @param graphics The <code>Graphics</code> context in which
     *                 to draw this component.
     */
    @Override
    public void paintComponent(Graphics graphics) {
        if (freeColClient.getGame() == null
            || freeColClient.getGame().getMap() == null) {
            return;
        }
        graphics.drawImage(ResourceManager.getImage("image.background.MiniMap"),
            0, 0, null);
        paintMap(graphics);
     }

    private Color getMinimapEconomicColor(TileType type) {
        return ResourceManager.getColor("color.economic.MiniMap." + type.getId());
    }

    private Color getMinimapPoliticsColor(TileType type) {
        return ResourceManager.getColor("color.politics.MiniMap." + type.getId());
    }

    /**
     * Paints a representation of the mapboard onto this component.
     * @param graphics The <code>Graphics</code> context in which
     *                 to draw this component.
     */
    public void paintMap(Graphics graphics) {

        int width = getWidth();
        int height = getHeight();
        final Graphics2D g = (Graphics2D) graphics;
        final AffineTransform originTransform = g.getTransform();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);

        /* Fill the rectangle with background color */
        g.setColor(ResourceManager.getColor("color.background.MiniMap"));
        g.fillRect(0, 0, width, height);

        if (gui.getFocus() == null) {
            return;
        }

        /* xSize and ySize represent how many tiles can be represented on the
           mini map at the current zoom level */
        int xSize = width / tileSize;
        int ySize = (height / tileSize) * 4;

        /* Center the mini map correctly based on the map's focus */
        firstColumn = gui.getFocus().getX() - (xSize / 2);
        firstRow = gui.getFocus().getY() - (ySize / 2);

        /* Make sure the mini map won't try to display tiles off the
         * bounds of the world map */

        final Map map = freeColClient.getGame().getMap();

        if (firstColumn < 0) {
            firstColumn = 0;
        } else if (firstColumn + xSize + 1 > map.getWidth()) {
            firstColumn = map.getWidth() - xSize - 1;
        }
        if (firstRow < 0) {
            firstRow = 0;
        } else if (firstRow + ySize + 1> map.getHeight()) {
            firstRow = map.getHeight() - ySize - 1;
        }


        if (map.getWidth() <= xSize) {
            firstColumn = 0;
            adjustX = ((xSize - map.getWidth()) * tileSize)/2;
            width = map.getWidth() * tileSize;
        } else {
            adjustX = 0;
        }

        if (map.getHeight() <= ySize) {
            firstRow = 0;
            adjustY = ((ySize - map.getHeight()) * tileSize)/MIN_TILE_SIZE;
            height = map.getHeight() * (tileSize/4);
        } else {
            adjustY = 0;
        }

        int lastRow = Math.min(firstRow + ySize, map.getHeight() - 1);
        int lastColumn = Math.min(firstColumn + xSize, map.getWidth() - 1);
        int tileWidth = tileSize;
        int tileHeight = tileSize/2;
        int halfWidth = tileSize/2;
        int halfHeight = tileSize/4;

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

        AffineTransform baseTransform = g.getTransform();
        AffineTransform rowTransform = null;

        final ImageLibrary library = gui.getImageLibrary();
        final ClientOptions clientOptions = freeColClient.getClientOptions();

        // Row per row; start with the top modified row
        for (int row = firstRow; row <= lastRow; row++) {
            rowTransform = g.getTransform();
            if ((row & 1) == 1) {
                g.translate(halfWidth, 0);
            }

            // Column per column; start at the left side to display the tiles.
            for (int column = firstColumn; column <= lastColumn; column++) {
                Tile tile = map.getTile(column, row);
                if (tile.isExplored()) {
                    if (clientOptions.getBoolean(ClientOptions.MINIMAP_TOGGLE_BORDERS)) {
                        g.setColor(getMinimapPoliticsColor(tile.getType()));
                        g.fill(tilePath);
                        
                        if (tile.getOwner() != null) {
                            Color nationOwner = tile.getOwner().getNationColor();
                            Color colorTransparent =
                                    new Color(nationOwner.getRed(), nationOwner.getGreen(), nationOwner.getBlue(), 100);
                            g.setColor(colorTransparent);
                            g.fill(paintFull);
                        }
                    } else {
                        g.setColor(getMinimapEconomicColor(tile.getType()));
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
                g.translate(tileWidth, 0);
            }
            g.setTransform(rowTransform);
            g.translate(0, halfHeight);
        }
        g.setTransform(baseTransform);

        /* Defines where to draw the white rectangle on the mini map.
         * miniRectX/Y are the center of the rectangle.
         * Use miniRectWidth/Height / 2 to get the upper left corner.
         * x/yTiles are the number of tiles that fit on the large map */
        if (getParent() != null) {
            int miniRectX = (gui.getFocus().getX() - firstColumn) * tileSize;
            int miniRectY = (gui.getFocus().getY() - firstRow) * tileSize / 4;
            Dimension mapTileSize = library.scaleDimension(ImageLibrary.TILE_SIZE);
            int miniRectWidth = (getParent().getWidth() / mapTileSize.width + 1) * tileSize;
            int miniRectHeight = (getParent().getHeight() / mapTileSize.height + 1) * tileSize / 2;
            if (miniRectX + miniRectWidth / 2 > width) {
                miniRectX = width - miniRectWidth / 2 - 1;
            } else if (miniRectX - miniRectWidth / 2 < 0) {
                miniRectX = miniRectWidth / 2;
            }
            if (miniRectY + miniRectHeight / 2 > height) {
                miniRectY = height - miniRectHeight / 2 - 1;
            } else if (miniRectY - miniRectHeight / 2 < 0) {
                miniRectY = miniRectHeight / 2;
            }

            g.setColor(ResourceManager.getColor("color.border.MiniMap"));
            // Use Math max and min to prevent the rect from being larger than the minimap.
            int miniRectMaxX = Math.max(miniRectX - miniRectWidth / 2, 0);
            int miniRectMaxY = Math.max(miniRectY - miniRectHeight / 2, 0);
            int miniRectMinWidth = Math.min(miniRectWidth, width - 1);
            int miniRectMinHeight = Math.min(miniRectHeight, height - 1);
            // Prevent the rect from overlapping the bigger adjust rect
            if(miniRectMaxX + miniRectMinWidth > width - 1) {
                miniRectMaxX = width - miniRectMinWidth - 1;
            }
            if(miniRectMaxY + miniRectMinHeight > height - 1) {
                miniRectMaxY = height - miniRectMinHeight - 1;
            }
            // Draw the rect.
            g.drawRect(miniRectMaxX, miniRectMaxY, miniRectMinWidth, miniRectMinHeight);
            // Draw an additional rect, if the whole map is shown on the minimap
            if (adjustX > 0 && adjustY > 0) {
                g.setColor(ResourceManager.getColor("color.border.MiniMap"));
                g.drawRect(0, 0, width - 1, height - 1);
            }
        }
        g.setTransform(originTransform);
    }


    private void focus(int x, int y) {
        int tileX, tileY;

        // When focusing out on the minimap, the last available focus out takes a larger jump than previous ones.
        // This if statement adjusts for the last larger jump in focus out.
        if (adjustX > 0 && adjustY > 0) {
            tileX = ((x - adjustX) / tileSize) + firstColumn + adjustX / MIN_TILE_SIZE;
            tileY = ((y - adjustY) / tileSize * MIN_TILE_SIZE) + firstRow + adjustY;
        } else {
            tileX = ((x - adjustX) / tileSize) + firstColumn;
            tileY = ((y - adjustY) / tileSize * 4) + firstRow ;
        }

        gui.setFocus(freeColClient.getGame().getMap().getTile(tileX,tileY));
    }

    private void focus(MouseEvent e) {
        if (e.getComponent().isEnabled()) {
            focus(e.getX(), e.getY());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    /**
     * If the user clicks on the mini map, refocus the map
     * to center on the tile that he clicked on
     * @param e a <code>MouseEvent</code> value
     */
    @Override
    public void mousePressed(MouseEvent e) {
        focus(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        focus(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

}
