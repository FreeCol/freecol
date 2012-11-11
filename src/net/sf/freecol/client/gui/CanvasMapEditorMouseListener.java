/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.client.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.MapEditorController;
import net.sf.freecol.client.gui.panel.RiverStylePanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.TileTypeTransform;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementStyle;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.server.generator.TerrainGenerator;

/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMapEditorMouseListener extends AbstractCanvasListener
    implements MouseListener, MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMapEditorMouseListener.class.getName());

    private final Canvas canvas;

    private Point oldPoint;
    private Point startPoint;

    private GUI gui;

    /**
     * The constructor to use.
     *
     * @param canvas The component this object gets created for.
     * @param gui The GUI that holds information such as screen resolution.
     */
    public CanvasMapEditorMouseListener(FreeColClient freeColClient, GUI gui, Canvas canvas) {
        super(freeColClient, gui.getMapViewer());
        this.gui = gui;
        this.canvas = canvas;
    }


    /**
     * This method can be called to make sure the map is loaded
     * There is no point executing mouse events if the map is not loaded
     */
    private Map getMap() {
        Map map = null;
        if (freeColClient.getGame() != null)
            map = freeColClient.getGame().getMap();
        return map;
    }

    /**
     * Invoked when a mouse button was clicked.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseClicked(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
        try {
            if (e.getClickCount() > 1) {
                Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
                canvas.showColonyPanel(tile);
            } else {
                canvas.requestFocus();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseClicked!", ex);
        }
    }

    /**
     * Invoked when the mouse enters the component.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseEntered(MouseEvent e) {
        // Ignore for now.
    }

    /**
     * Invoked when the mouse exits the component.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseExited(MouseEvent e) {
        // Ignore for now.
    }


    /**
     * Invoked when a mouse button was pressed.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mousePressed(MouseEvent e) {
        if (!e.getComponent().isEnabled()) {
            return;
        }
        if (getMap() == null) {
            return;
        }
        try {
            if (e.getButton() == MouseEvent.BUTTON3 || e.isPopupTrigger()) {
                Tile tile = mapViewer.convertToMapTile(e.getX(), e.getY());
                if (tile != null) {
                    if (tile.hasRiver()) {
                        TileImprovement river = tile.getRiver();
                        String style = canvas.showRiverStyleDialog();
                         if (style == null) {
                             tile.getTileItemContainer().removeTileItem(river);
                         } else if (style.equals(RiverStylePanel.CANCEL)) {
                             // user canceled
                         } else {
                             river.setStyle(TileImprovementStyle.getInstance(style));
                        }
                    }
                    if (tile.getIndianSettlement() != null) {
                        canvas.showEditSettlementDialog(tile.getIndianSettlement());
                    }
                } else {
                    gui.setSelectedTile(null, true);
                }
            } else if (e.getButton() == MouseEvent.BUTTON1) {
                startPoint = e.getPoint();
                oldPoint = e.getPoint();
                JComponent component = (JComponent)e.getSource();
                drawBox(component, startPoint, oldPoint);
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mousePressed!", ex);
        }
    }

    /**
     * Invoked when a mouse button was released.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseReleased(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
        JComponent component = (JComponent)e.getSource();

        MapEditorController controller = freeColClient
            .getMapEditorController();
        boolean isTransformActive = controller.getMapTransform() != null;

        if(startPoint == null){
        	startPoint = e.getPoint();
        }
        if(oldPoint == null){
        	oldPoint = e.getPoint();
        }
        drawBox(component, startPoint, oldPoint);
        if (gui.getFocus() != null) {
            Tile start = mapViewer.convertToMapTile(startPoint.x, startPoint.y);
            Tile end = start;
            //Optimization, only check if the points are different
            if(startPoint.x != oldPoint.x || startPoint.y != oldPoint.y){
            	end = mapViewer.convertToMapTile(oldPoint.x, oldPoint.y);
            }

            // no option selected, just center map
            if(!isTransformActive){
            	gui.setFocus(end);
            	return;
            }

            // find the area to transform
            int min_x, max_x, min_y, max_y;
            if (start.getX() < end.getX()) {
                min_x = start.getX();
                max_x = end.getX();
            } else {
                min_x = end.getX();
                max_x = start.getX();
            }
            if (start.getY() < end.getY()) {
                min_y = start.getY();
                max_y = end.getY();
            } else {
                min_y = end.getY();
                max_y = start.getY();
            }

            // apply transformation to all tiles in the area
            Tile t = null;
            for (int x = min_x; x <= max_x; x++) {
                for (int y = min_y; y <= max_y; y++) {
                    t = getMap().getTile(x, y);
                    if (t != null) {
                        controller.transform(t);
                    }
                }
            }
            if (controller.getMapTransform() instanceof TileTypeTransform) {
                for (int x = min_x - 2; x <= max_x + 2; x++) {
                    for (int y = min_y - 2; y <= max_y + 2; y++) {
                        t = getMap().getTile(x, y);
                        if (t != null && t.getType().isWater()) {
                            TerrainGenerator.encodeStyle(t);
                        }
                    }
                }
            }
            gui.refresh();
            canvas.requestFocus();
        }
    }

    /**
     * Invoked when the mouse has been moved.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseMoved(MouseEvent e) {
        if (getMap() == null) {
            return;
        }
        if (e.getY() < AUTO_SCROLLSPACE){
        	return; // handle this in the menu bar
        }
        performAutoScrollIfActive(e);
    }


    /**
     * Invoked when the mouse has been dragged.
     *
     * @param e The MouseEvent that holds all the information.
     */
    public void mouseDragged(MouseEvent e) {
        if (getMap() == null) {
            return;
        }

        JComponent component = (JComponent)e.getSource();
        drawBox(component, startPoint, oldPoint);
        oldPoint = e.getPoint();
        drawBox(component, startPoint, oldPoint);

        performDragScrollIfActive(e);

        gui.refresh();
    }

    private void drawBox(JComponent component, Point startPoint, Point endPoint) {
        if(startPoint == null || endPoint == null){
        	return;
        }
        if(startPoint.distance(endPoint) == 0){
        	return;
        }

        // only bother to draw if a transformation is active
        MapEditorController controller = freeColClient.getMapEditorController();
        if(controller.getMapTransform() == null){
        	return;
        }

    	Graphics2D graphics = (Graphics2D) component.getGraphics ();
        graphics.setColor(Color.WHITE);
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(startPoint.x - endPoint.x);
        int height = Math.abs(startPoint.y - endPoint.y);
        graphics.drawRect(x, y, width, height);
    }


}
