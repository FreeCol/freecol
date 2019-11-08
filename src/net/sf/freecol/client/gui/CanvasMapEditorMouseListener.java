/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import net.sf.freecol.client.gui.GUI.ViewMode;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel.TileTypeTransform;
import net.sf.freecol.client.gui.dialog.RiverStyleDialog;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.server.generator.TerrainGenerator;


/**
 * Listens to the mouse being moved at the level of the Canvas.
 */
public final class CanvasMapEditorMouseListener extends AbstractCanvasListener
    implements MouseListener, MouseMotionListener {

    private static final Logger logger = Logger.getLogger(CanvasMapEditorMouseListener.class.getName());

    private Point endPoint;
    private Point startPoint;


    /**
     * The constructor to use.
     *
     * @param freeColClient The enclosing {@code FreeColClient}.
     * @param canvas The component this object gets created for.
     */
    public CanvasMapEditorMouseListener(FreeColClient freeColClient,
                                        Canvas canvas) {
        super(freeColClient, canvas);
    }


    /**
     * This method can be called to make sure the map is loaded.
     *
     * There is no point executing mouse events if the map is not loaded.
     *
     * @return The associated game {@code Map}.
     */
    private Map getMap() {
        return (getGame() == null) ? null : getGame().getMap();
    }

    /**
     * Draw a box on screen.
     *
     * @param component The {@code JComponent} to draw on.
     * @param startPoint The starting {@code Point}.
     * @param endPoint The end {@code Point}.
     */
    private void drawBox(JComponent component,
                         Point startPoint, Point endPoint) {
        if (startPoint == null || endPoint == null
            || startPoint.distance(endPoint) == 0
            || getFreeColClient().getMapEditorController() == null)
            return;

        Graphics2D graphics = (Graphics2D)component.getGraphics();
        graphics.setColor(Color.WHITE);
        int x = Math.min(startPoint.x, endPoint.x);
        int y = Math.min(startPoint.y, endPoint.y);
        int width = Math.abs(startPoint.x - endPoint.x);
        int height = Math.abs(startPoint.y - endPoint.y);
        graphics.drawRect(x, y, width, height);
    }


    // Implement MouseListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (getMap() == null) return;

        try {
            if (e.getClickCount() > 1) {
                Tile t = canvas.convertToMapTile(e.getX(), e.getY());
                if (t != null) getGUI().showTilePanel(t);
            } else {
                canvas.requestFocus();
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mouseClicked!", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (getMap() == null || !e.getComponent().isEnabled()) return;

        try {
            if (e.getButton() == MouseEvent.BUTTON1) {
                Tile tile = canvas.convertToMapTile(e.getX(), e.getY());
                if (tile != null) getGUI().changeView(tile);
                startPoint = endPoint = null;

            } else if (e.getButton() == MouseEvent.BUTTON2) {
                startPoint = endPoint = e.getPoint();
                JComponent component = (JComponent)e.getSource();
                drawBox(component, startPoint, endPoint);

            } else if (e.getButton() == MouseEvent.BUTTON3
                || e.isPopupTrigger()) {
                startPoint = endPoint = e.getPoint();
                Tile tile = canvas.convertToMapTile(e.getX(), e.getY());
                if (tile != null) {
                    if (tile.getIndianSettlement() != null) {
                        canvas.showEditSettlementDialog(tile.getIndianSettlement());
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Error in mousePressed!", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (getMap() == null
            || e.getButton() == MouseEvent.BUTTON1
            || getGUI().getFocus() == null) return;
        final JComponent component = (JComponent)e.getSource();
        final MapEditorController controller
            = getFreeColClient().getMapEditorController();
        final boolean isTransformActive = controller.getMapTransform() != null;

        endPoint = e.getPoint();
        if (startPoint == null) startPoint = endPoint;
        drawBox(component, startPoint, endPoint);
        Tile start = canvas.convertToMapTile(startPoint.x, startPoint.y);
        Tile end = (startPoint == endPoint) ? start
            : canvas.convertToMapTile(endPoint.x, endPoint.y);

        // edit 2 more conditions in if statement.  we need to
        // check for coordinator of X and Y if (x,y) outside of
        // map then dont focus to that else setfocus to that
        // position no option selected, just center map
        if (!isTransformActive && end.getX() >= 0 && end.getY() >= 0) {
            getGUI().setFocus(end);
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
        getGUI().refresh();
        canvas.requestFocus();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseEntered(MouseEvent e) {} // Ignore for now.

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseExited(MouseEvent e) {} // Ignore for now.


    // Implement MouseMotionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        if (getMap() == null) return;
        final JComponent component = (JComponent)e.getSource();

        drawBox(component, startPoint, endPoint);
        endPoint = e.getPoint();
        drawBox(component, startPoint, endPoint);

        performDragScrollIfActive(e);

        getGUI().refresh();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (getMap() == null) return;

        performAutoScrollIfActive(e, true);
    }
}
