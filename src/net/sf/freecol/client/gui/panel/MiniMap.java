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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.event.MouseInputListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.MiniMapZoomInAction;
import net.sf.freecol.client.gui.action.MiniMapZoomOutAction;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.resources.ResourceManager;



/**
 * This component draws a small version of the map. It allows us
 * to see a larger part of the map and to relocate the viewport by
 * clicking on it.
 */
public final class MiniMap extends JPanel implements MouseInputListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MiniMap.class.getName());
    public static final int MINIMAP_ZOOMOUT = 13;
    public static final int MINIMAP_ZOOMIN = 14;
    
    private static final int MAP_WIDTH = 220;
    private static final int MAP_HEIGHT = 128;

    private int mapX;
    private int mapY;
    
    private boolean scaleMap = false;

    private FreeColClient freeColClient;
    private final JButton          miniMapZoomOutButton;
    private final JButton          miniMapZoomInButton;
    private Color backgroundColor;

    private int tileSize; //tileSize is the size (in pixels) that each tile will take up on the mini map

    /**
    * The top left tile on the mini map represents the tile.
    * (xOffset, yOffset) in the world map
    */
    private int xOffset, yOffset;

    /**
    * Used for adjusting the position of the mapboard image.
    * @see #paintMap
    */
    private int adjustX = 0, adjustY = 0;

    float scaledFactorX = 1, scaledFactorY = 1;
    int scaledOffsetX = 0, scaledOffsetY = 0;



    /**
     * The constructor that will initialize this component.
     * 
     * @param freeColClient The main controller object for the client
     */
    public MiniMap(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        backgroundColor = Color.BLACK;

        tileSize = 4 * (freeColClient.getClientOptions().getInteger(ClientOptions.DEFAULT_MINIMAP_ZOOM) + 1);

        addMouseListener(this);
        addMouseMotionListener(this);
        setLayout(null);

        //boolean usingSkin;
        Image skin = ResourceManager.getImage("MiniMap.skin");
        if (skin == null) {
            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            } catch (Exception e) {}
            setSize(MAP_WIDTH, MAP_HEIGHT);
            setOpaque(true);
            //usingSkin = false;
            mapX = 0;
            mapY = 0;
        } else {
            setBorder(null);
            setSize(skin.getWidth(null), skin.getHeight(null));
            setOpaque(false);
            //usingSkin = true;
            // TODO-LATER: The values below should be specified by a skin-configuration-file:
            mapX = 38;
            mapY = 75;
        }

        // Add buttons:
        miniMapZoomOutButton = new UnitButton(freeColClient.getActionManager().getFreeColAction(MiniMapZoomOutAction.id));
        miniMapZoomInButton = new UnitButton(freeColClient.getActionManager().getFreeColAction(MiniMapZoomInAction.id));

        miniMapZoomOutButton.setFocusable(false);
        miniMapZoomInButton.setFocusable(false);

        int bh = mapY + MAP_HEIGHT - Math.max(miniMapZoomOutButton.getHeight(), miniMapZoomInButton.getHeight());
        int bw = mapX;
        if (getBorder() != null) {
            Insets insets = getBorder().getBorderInsets(this);
            bh -= insets.bottom;
            bw += insets.left;
        }

        // TODO-LATER: The values below should be specified by a skin-configuration-file:
        miniMapZoomInButton.setLocation(4, 174);
        miniMapZoomOutButton.setLocation(264, 174);

        add(miniMapZoomInButton);
        add(miniMapZoomOutButton);        
    }

    /**
     * Zooms in the mini map.
     */
    public void zoomIn() {
        if (scaleMap) {
            scaleMap = false;
        } else { 
            tileSize += 4;
            if (tileSize >= 24) {
                tileSize = 24;
            }
        }

        repaint();
    }


    /**
     * Zooms out the mini map.
     */
    public void zoomOut() {
        if (tileSize > 8) {
            tileSize -= 4;
        } else if (tileSize == 4) {
            scaleMap = true;
        } else {
            tileSize = 4;
        }

        repaint();
    }
    
    public boolean canZoomIn() {
        return tileSize < 20;
    }
    
    public boolean canZoomOut() {
        if (freeColClient.getGame() == null
                || freeColClient.getGame().getMap() == null) {
            return false;
        }
        final int realMapWidth = freeColClient.getGame().getMap().getWidth();
        final int realMapHeight = freeColClient.getGame().getMap().getHeight();
        return tileSize > 4 || (!scaleMap 
                && (realMapWidth * 4 > MAP_WIDTH || realMapHeight > MAP_HEIGHT));
    }


    /**
     * Paints this component.
     * @param graphics The <code>Graphics</code> context in which 
     *                 to draw this component.
     */
    public void paintComponent(Graphics graphics) {
        if (freeColClient.getGame() == null
                || freeColClient.getGame().getMap() == null) {
            return;
        }        
        Image skin = ResourceManager.getImage("MiniMap.skin");
        
    	Color newBackground = ResourceManager.getColor("miniMapBackground.color");
    	this.setBackgroundColor(newBackground);
        
        scaledFactorX = 1;
        scaledFactorY = 1;
        scaledOffsetX = 0;
        scaledOffsetY = 1;
        
        if (skin == null) {
            paintMap(graphics, 0, 0, getWidth(), getHeight());
        } else {
            if (!scaleMap) {
                paintMap(graphics, mapX, mapY, MAP_WIDTH, MAP_HEIGHT);
            } else {
                graphics.setColor(backgroundColor);
                graphics.fillRect(mapX, mapY, MAP_WIDTH, MAP_HEIGHT);
                
                final int realMapWidth = freeColClient.getGame().getMap().getWidth();
                final int realMapHeight = freeColClient.getGame().getMap().getHeight();
                BufferedImage bi = new BufferedImage(realMapWidth * 4, realMapHeight, BufferedImage.TYPE_INT_ARGB);
                paintMap(bi.createGraphics(), 0, 0, realMapWidth * 4, realMapHeight);
                
                int scaledWidth = MAP_WIDTH;
                int scaledHeight = MAP_HEIGHT;
                if (realMapWidth * 4 > realMapHeight * 2) {
                    scaledHeight = (MAP_WIDTH * realMapHeight) / (realMapWidth * 4);
                } else {
                    scaledWidth = (MAP_HEIGHT * realMapWidth * 4) / (realMapHeight); 
                }
                scaledOffsetX = (MAP_WIDTH - scaledWidth) / 2;
                scaledOffsetY = (MAP_HEIGHT - scaledHeight) / 2;
                
                scaledFactorX = (realMapWidth * 4) / ((float) scaledWidth);
                scaledFactorY = realMapHeight / ((float) scaledHeight);
                
                final int scalingHint = (freeColClient.getClientOptions().getBoolean(ClientOptions.SMOOTH_MINIMAP_RENDERING)) 
                        ? Image.SCALE_SMOOTH : Image.SCALE_FAST;
                Image image = bi.getScaledInstance(scaledWidth, scaledHeight, scalingHint);                
                graphics.drawImage(image, mapX + scaledOffsetX, mapY + scaledOffsetY, null);
            }
            paintSkin(graphics, skin);
        }
    }

    /**
     * Paints the skin onto this component.
     * 
     * @param graphics The <code>Graphics</code> to draw the skin on.
     * @param skin The skin.
     */
    private void paintSkin(Graphics graphics, Image skin) {
        graphics.drawImage(skin, 0, 0, null);
    }

    private Color getMinimapColor(TileType type) {
        return ResourceManager.getColor(type.getId() + ".color");
    }


    /**
    * Paints a representation of the mapboard onto this component.
    * @param graphics The <code>Graphics</code> context in which 
    *                 to draw this component.
    * @param x The x-position of the upperleft corner of the map.
    * @param y The y-position of the upperleft corner of the map.
    * @param width The width of the map.
    * @param height The height of the map.
    */
    private void paintMap(Graphics graphics, int x, int y, int width, int height) {
        final Graphics2D g = (Graphics2D) graphics;
        final Map map = freeColClient.getGame().getMap();
        final ImageLibrary imageProvider = freeColClient.getImageLibrary();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);
 	  	
        /* Fill the rectangle with solid black */
        g.setColor(Color.BLACK);
        g.fillRect(x, y, width, height);

        if (freeColClient.getGUI() == null || freeColClient.getGUI().getFocus() == null) {
            return;
        }
        /* xSize and ySize represent how many tiles can be represented on the
           mini map at the current zoom level */
        int xSize = width / tileSize;
        int ySize = (height / tileSize) * 4;

        /* Center the mini map correctly based on the map's focus */
        xOffset = freeColClient.getGUI().getFocus().getX() - (xSize / 2);
        yOffset = freeColClient.getGUI().getFocus().getY() - (ySize / 2);

        /* Make sure the mini map won't try to display tiles off the
         * bounds of the world map */

        if (xOffset < 0) {
            xOffset = 0;
        } else if (xOffset + xSize + 1 > map.getWidth()) {
            xOffset = map.getWidth() - xSize - 1;
        }
        if (yOffset < 0) {
            yOffset = 0;
        } else if (yOffset + ySize + 1> map.getHeight()) {
            yOffset = map.getHeight() - ySize - 1;
        }


        if (map.getWidth() <= xSize) {
            xOffset = 0;
            adjustX = ((xSize - map.getWidth()) * tileSize)/2;
            width = map.getWidth() * tileSize;
            x += adjustX;
        } else {
            adjustX = 0;
        }

        if (map.getHeight() <= ySize) {
            yOffset = 0;
            adjustY = ((ySize - map.getHeight()) * tileSize)/8;
            height = map.getHeight() * (tileSize/4);
            y += adjustY;
        } else {
            adjustY = 0;
        }

        /* Iterate through all the squares on the mini map and paint the
         * tiles based on terrain */

        /* Points that will be used to draw the diamond for each tile */
        int[] xPoints = new int[4] ;
        int[] yPoints = new int[4] ;

        int xPixels = 0;
        for (int tileX = 0; xPixels <= width + tileSize; tileX++, xPixels += tileSize) {
            int yPixels = 0;
            for (int tileY = 0; yPixels <= height + tileSize; tileY++, yPixels += tileSize/4) {
                /* Check the terrain to find out which color to use */
                Tile tile = map.getTile(tileX + xOffset, tileY + yOffset);
                if (tile == null) {
                    continue;
                }
                Settlement settlement = tile.getSettlement();
                int units = tile.getUnitCount();
                g.setColor(Color.BLACK); //Default
                if (tile.isExplored()) {
                    g.setColor(getMinimapColor(tile.getType()));
                }
                if (tileSize == 4) {
                    int extra = (((tileY + yOffset) % 2) == 0) ? 0 : 2;
                    g.drawLine(x+extra+ 4*tileX, y+tileY, x+2+extra+4*tileX, y+tileY);
                    g.drawLine(x+extra+1+4*tileX, y+1+tileY, x+extra+1+4*tileX, y+1+tileY);

                    if (settlement != null) {
                        g.setColor(settlement.getOwner().getColor());
                        g.drawLine(x+extra+4*tileX+1, y+tileY, x+extra+4*tileX+1, y+tileY);
                    } else if (units > 0) {
                        g.setColor(tile.getFirstUnit().getOwner().getColor());
                        g.drawLine(x+extra+4*tileX+1, y+tileY, x+extra+4*tileX+1, y+tileY);
                    }
                } else {
                    /* Due to the coordinate system, if the y value of the tile is odd,
                    * it needs to be shifted to the right a half-tile's width
                    */
                    if (((tileY + yOffset) % 2) == 0) {
                        xPoints[0] = x + tileX * tileSize - tileSize / 2;
                        xPoints[1] = x + tileX * tileSize;
                        xPoints[2] = x + tileX * tileSize + tileSize / 2;
                        xPoints[3] = xPoints[1];
                    } else {
                        xPoints[0] = x + tileX * tileSize;
                        xPoints[1] = x + tileX * tileSize + tileSize / 2;
                        xPoints[2] = x + tileX * tileSize + tileSize;
                        xPoints[3] = xPoints[1];
                    }
                    yPoints[0] = y + tileY * tileSize / 4;
                    yPoints[1] = y + tileY * tileSize / 4 - tileSize / 4;
                    yPoints[3] = y + tileY * tileSize / 4 + tileSize / 4;
                    yPoints[2] = yPoints[0];

                    //Draw it
                    g.fillPolygon(xPoints, yPoints, 4);

                    if (settlement != null) {
                        xPoints[0] += tileSize / 8;
                        xPoints[2] -= tileSize / 8;
                        yPoints[1] += tileSize / 16;
                        yPoints[3] -= tileSize / 16;
                        g.setColor(settlement.getOwner().getColor());
                        g.fillPolygon(xPoints, yPoints, 4);
                        g.setColor(Color.BLACK);
                        g.drawPolygon(xPoints, yPoints, 4);
                    } else if (units > 0) {
                        xPoints[0] += tileSize / 4;
                        xPoints[2] -= tileSize / 4;
                        yPoints[1] += tileSize / 8;
                        yPoints[3] -= tileSize / 8;
                        g.setColor(tile.getFirstUnit().getOwner().getColor());
                        g.fillPolygon(xPoints, yPoints, 4);
                        g.setColor(Color.BLACK);
                        g.drawPolygon(xPoints, yPoints, 4);
                    }
                }
            }
        }

        /* Defines where to draw the white rectangle on the mini map.
         * miniRectX/Y are the center of the rectangle.
         * Use miniRectWidth/Height / 2 to get the upper left corner.
         * x/yTiles are the number of tiles that fit on the large map */
        TileType tileType = FreeCol.getSpecification().getTileTypeList().get(0);
        int miniRectX = (freeColClient.getGUI().getFocus().getX() - xOffset) * tileSize;
        int miniRectY = (freeColClient.getGUI().getFocus().getY() - yOffset) * tileSize / 4;
        int miniRectWidth = (getParent().getWidth() / imageProvider.getTerrainImageWidth(tileType) + 1) * tileSize;
        int miniRectHeight = (getParent().getHeight() / imageProvider.getTerrainImageHeight(tileType) + 1) * tileSize / 2;
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

        miniRectX += x;
        miniRectY += y;

        g.setColor(Color.WHITE);
        /* Use Math max and min to prevent the rect from being larger than the minimap. */
        int miniRectMaxX = Math.max(miniRectX - miniRectWidth / 2, x);
        int miniRectMaxY = Math.max(miniRectY - miniRectHeight / 2, y);
        int miniRectMinWidth = Math.min(miniRectWidth, width - 1);
        int miniRectMinHeight = Math.min(miniRectHeight, height - 1);
        /* Prevent the rect from overlapping the bigger adjust rect */
        if(miniRectMaxX + miniRectMinWidth > x + width - 1) {
        	miniRectMaxX = x + width - miniRectMinWidth - 1;
        }
        if(miniRectMaxY + miniRectMinHeight > y + height - 1) {
        	miniRectMaxY = y + height - miniRectMinHeight - 1;
        }
        /* Draw the white rect. */
        g.drawRect(miniRectMaxX, miniRectMaxY, miniRectMinWidth, miniRectMinHeight);
        if(scaleMap) {
        	g.drawRect(miniRectX - miniRectWidth / 2 + 1, miniRectY - miniRectHeight / 2 + 1, miniRectWidth - 2, miniRectHeight - 2);
        }
		/* Draw an additional white rect, if the whole map is shown on the minimap */
        if (adjustX > 0 && adjustY > 0) {
            g.setColor(Color.WHITE);
            g.drawRect(x, y, width - 1, height - 1);
        }
    }


    public void mouseClicked(MouseEvent e) {

    }


    /* Used to keep track of the initial values of xOffset
     * and yOffset for more accurate dragging */
    private int initialX, initialY;

    /* If the user clicks on the mini map, refocus the map
     * to center on the tile that he clicked on */
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled() || !isInMap(e.getX(), e.getY())) {
            return;
        }

        initialX = xOffset;
        initialY = yOffset;
        
        int x = (int) ((e.getX() - mapX - scaledOffsetX) * scaledFactorX);
        int y = (int) ((e.getY() - mapY - scaledOffsetY) * scaledFactorY);

        int tileX = ((x - adjustX) / tileSize) + initialX;
        int tileY = ((y - adjustY) / tileSize * 4) + initialY;

        freeColClient.getGUI().setFocus(tileX, tileY);
    }

    
    private boolean isInMap(int x, int y) {
        return x >= mapX && x < mapX+MAP_WIDTH && y >= mapY && y < mapY+MAP_HEIGHT;
    }


    public void mouseReleased(MouseEvent e) {

    }


    public void mouseEntered(MouseEvent e) {

    }


    public void mouseExited(MouseEvent e) {

    }


    public void mouseDragged(MouseEvent e) {
        /*
          If the user drags the mouse, then continue
          to refocus the screen
        */

        if (!e.getComponent().isEnabled() || !isInMap(e.getX(), e.getY())) {
            return;
        }
        
        int x = (int) ((e.getX() - mapX - scaledOffsetX) * scaledFactorX);
        int y = (int) ((e.getY() - mapY - scaledOffsetY) * scaledFactorY);

        int tileX = ((x - adjustX) / tileSize) + initialX;
        int tileY = ((y - adjustY) / tileSize * 4) + initialY;

        freeColClient.getGUI().setFocus(tileX, tileY);
    }


    public void mouseMoved(MouseEvent e) {

    }

	public Color getBackgroundColor() {
		return backgroundColor;
	}

	public void setBackgroundColor(Color backgroundColor) {
		this.backgroundColor = backgroundColor;
	}
}
