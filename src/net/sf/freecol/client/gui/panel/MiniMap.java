package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Insets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.event.MouseInputListener;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.FreeColClient;

import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;


/**
 * This component draws a small version of the map. It allows us
 * to see a larger part of the map and to relocate the viewport by
 * clicking on it.
 */
public final class MiniMap extends JPanel implements MouseInputListener {
    public static final String COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(MiniMap.class.getName());
    public static final int MINIMAP_ZOOMOUT = 13;
    public static final int MINIMAP_ZOOMIN = 14;

    private FreeColClient freeColClient;
    private final Map map;
    private final ImageProvider imageProvider;
    private JComponent container;
    private final JButton          miniMapZoomOutButton;
    private final JButton          miniMapZoomInButton;

    private int tileSize; //tileSize is the size (in pixels) that each tile will take up on the mini map

    /* The top left tile on the mini map represents the tile
    * (xOffset, yOffset) in the world map */
    private int xOffset, yOffset;

    


    /**
     * The constructor that will initialize this component.
     * @param map The map that is displayed on the screen.
     * @param imageProvider The ImageProvider that can provide us with images to display
     * and information about those images (such as the width of a tile image).
     * @param container The component that contains the minimap.
     */
    public MiniMap(FreeColClient freeColClient, Map map, ImageProvider imageProvider, JComponent container) {
        this.freeColClient = freeColClient;
        this.map = map;
        this.imageProvider = imageProvider;
        this.container = container;

        tileSize = 12;

        //setBackground(Color.BLACK);
        addMouseListener(this);
        addMouseMotionListener(this);
        setLayout(null);
        setSize(256, 128);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}
        


        // Add buttons:
        miniMapZoomOutButton = new JButton("-");
        miniMapZoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                zoomOut();
            }
        });

        miniMapZoomInButton = new JButton("+");
        miniMapZoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                zoomIn();
            }
        });

        miniMapZoomOutButton.setFocusable(false);
        miniMapZoomInButton.setFocusable(false);
        miniMapZoomOutButton.setOpaque(true);
        miniMapZoomInButton.setOpaque(true);
        miniMapZoomOutButton.setSize(50, 20);
        miniMapZoomInButton.setSize(50, 20);
        miniMapZoomOutButton.setActionCommand(String.valueOf(MINIMAP_ZOOMOUT));
        miniMapZoomInButton.setActionCommand(String.valueOf(MINIMAP_ZOOMIN));

        int bh;
        int bw;
        if (getBorder() != null) {
            Insets insets = getBorder().getBorderInsets(this);
            bh = getHeight() - Math.max(miniMapZoomOutButton.getHeight(), miniMapZoomInButton.getHeight()) - insets.bottom;
            bw = insets.left;
        } else {
            bh = getHeight() - Math.max(miniMapZoomOutButton.getHeight(), miniMapZoomInButton.getHeight());
            bw = 0;
        }

        miniMapZoomOutButton.setLocation(bw, bh);
        miniMapZoomInButton.setLocation(bw + miniMapZoomOutButton.getWidth(), bh);
        add(miniMapZoomInButton);
        add(miniMapZoomOutButton);
    }

    
    


    public void setContainer(JComponent container) {
        this.container = container;
    }


    /**
     * Zooms in the mini map
     */
    public void zoomIn() {
        tileSize += 4;
        miniMapZoomOutButton.setEnabled(true);
        repaint();
    }


    /**
     * Zooms out the mini map
     */
    public void zoomOut() {
        if (tileSize > 4) {
            tileSize -= 4;
        }
        
        if (tileSize <= 4) {
            miniMapZoomOutButton.setEnabled(false);
        }

        repaint();
    }


    /**
     * Paints this component.
     * @param graphics The Graphics context in which to draw this component.
     */
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        /* xSize and ySize represent how many tiles can be represented on the
           mini map at the current zoom level */
        int xSize = getWidth() / tileSize;
        int ySize = (getHeight() / tileSize) * 4;

        /* If the mini map is too zoomed out, then zoom it in */
        while (xSize > map.getWidth() || ySize > map.getHeight()) {
            tileSize += 4;

            /* Update with the new values */
            xSize = getWidth() / tileSize;
            ySize = (getHeight() / tileSize) * 4;
        }

        /* Center the mini map correctly based on the map's focus */

        xOffset = freeColClient.getGUI().getFocus().getX() - (xSize / 2);
        yOffset = freeColClient.getGUI().getFocus().getY() - (ySize / 2);

        /* Make sure the mini map won't try to display tiles off the
         * bounds of the world map */
        if (xOffset < 0) {
            xOffset = 0;
        } else if (xOffset + xSize > map.getWidth()) {
            xOffset = map.getWidth() - xSize;
        }
        if (yOffset < 0) {
            yOffset = 0;
        } else if (yOffset + ySize > map.getHeight()) {
            yOffset = map.getHeight() - ySize;
        }

        /* Fill the rectangle with solid black */
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        /* Iterate through all the squares on the mini map and paint the
         * tiles based on terrain */

        /* Points that will be used to draw the diamond for each tile */
        int[] xPoints = new int[4];
        int[] yPoints = new int[4];

        int xPixels = 0;
        for (int x = 0; xPixels <= 256 + tileSize; x++, xPixels += tileSize) {
            int yPixels = 0;
            for (int y = 0; yPixels <= 128 + tileSize; y++, yPixels += tileSize / 4) {
                /* Check the terrain to find out which color to use */
                g.setColor(Color.BLACK); //Default

                Tile tile = map.getTileOrNull(x + xOffset, y + yOffset);
                Settlement settlement = tile != null ? tile.getSettlement() : null;
                int units = tile != null ? tile.getUnitCount() : 0;

                if (settlement != null) {
                    //There's a Settlement on this tile
                    g.setColor(settlement.getOwner().getColor());
                } else if (units > 0) {
                    //There are units on this tile.
                    g.setColor(tile.getFirstUnit().getOwner().getColor());
                } else {
                    int type = tile != null ? tile.getType() : 0;
                    switch (type) {
                        case 1:
                        case 2:
                        case 3:
                        case 4:
                            g.setColor(new Color(0.0f, 0.75f, 0.0f));
                            break;

                        case 5:
                        case 6:
                            g.setColor(new Color(0.0f, 0.75f, 0.25f));
                            break;

                        case 7:
                            g.setColor(new Color(0.25f, 0.75f, 0.0f));
                            break;

                        case 8:
                            g.setColor(new Color(0.50f, 0.75f, 0.50f));
                            break;

                        case 9:
                            g.setColor(Color.WHITE);
                            break;

                        case 10: //Blue ocean
                            g.setColor(Color.BLUE);
                            break;

                        case 11: //Darker blue high seas
                            g.setColor(new Color(0.0f, 0.0f, 0.8f));
                            break;

                        case 0:
                        default:
                            g.setColor(Color.BLACK);
                            break;
                    }
                }

                /* Due to the coordinate system, if the y value of the tile is odd,
                 * it needs to be shifted to the right a half-tile's width
                 */
                if (((y + yOffset) % 2) == 0) {
                    xPoints[0] = x * tileSize - tileSize / 2;
                    xPoints[1] = xPoints[3] = x * tileSize;
                    xPoints[2] = x * tileSize + tileSize / 2;
                } else {
                    xPoints[0] = x * tileSize;
                    xPoints[1] = xPoints[3] = x * tileSize + tileSize / 2;
                    xPoints[2] = x * tileSize + tileSize;
                }
                yPoints[0] = yPoints[2] = y * tileSize / 4;
                yPoints[1] = y * tileSize / 4 - tileSize / 4;
                yPoints[3] = y * tileSize / 4 + tileSize / 4;

                //Draw it
                g.fillPolygon(xPoints, yPoints, 4);
            }
        }

        /* Defines where to draw the white rectangle on the mini map.
         * miniRectX/Y are the center of the rectangle.
         * Use miniRectWidth/Height / 2 to get the upper left corner.
         * x/yTiles are the number of tiles that fit on the large map */
        int miniRectX = (freeColClient.getGUI().getFocus().getX() - xOffset) * tileSize;
        int miniRectY = (freeColClient.getGUI().getFocus().getY() - yOffset) * tileSize / 4;
        int miniRectWidth = (container.getWidth() / imageProvider.getTerrainImageWidth(0) + 1) * tileSize;
        int miniRectHeight = (container.getHeight() / imageProvider.getTerrainImageHeight(0) + 1) * tileSize / 2;
        if (miniRectX + miniRectWidth / 2 > getWidth())
            miniRectX = getWidth() - miniRectWidth / 2 - 1;
        else if (miniRectX - miniRectWidth / 2 < 0) miniRectX = miniRectWidth / 2;
        if (miniRectY + miniRectHeight / 2 > getHeight())
            miniRectY = getHeight() - miniRectHeight / 2 - 1;
        else if (miniRectY - miniRectHeight / 2 < 0) miniRectY = miniRectHeight / 2;

        g.setColor(Color.WHITE);
        g.drawRect(miniRectX - miniRectWidth / 2, miniRectY - miniRectHeight / 2, miniRectWidth, miniRectHeight);
    }


    public void mouseClicked(MouseEvent e) {

    }


    /* Used to keep track of the initial values of xOffset
     * and yOffset for more accurate dragging */
    private int initialX, initialY;

    /* If the user clicks on the mini map, refocus the map
     * to center on the tile that he clicked on */
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled()) {
            return;
        }

        int x = e.getX();
        int y = e.getY();
        initialX = xOffset;
        initialY = yOffset;
        int tileX = (int) (x / tileSize) + xOffset;
        int tileY = (int) (y / tileSize * 4) + yOffset;
        freeColClient.getGUI().setFocus(tileX, tileY);
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

        if (!e.getComponent().isEnabled()) {
            return;
        }

        int x = e.getX();
        int y = e.getY();
        int tileX = (int) (x / tileSize) + initialX;
        int tileY = (int) (y / tileSize * 4) + initialY;
        freeColClient.getGUI().setFocus(tileX, tileY);
    }


    public void mouseMoved(MouseEvent e) {

    }
}
