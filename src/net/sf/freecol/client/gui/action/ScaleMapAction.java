/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

package net.sf.freecol.client.gui.action;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Tile;

/**
 * An action for scaling a map. This action is a part of the map editor.
 */
public class ScaleMapAction extends FreeColAction {

    public static final String id = "scaleMapAction";


    /**
     * Creates a new <code>ScaleMapAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    ScaleMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }

    /**
     * Checks if this action should be enabled.
     * 
     * @return <code>false</code> if there is no active map.
     */
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled()
            && freeColClient.isMapEditor()
            && freeColClient.getGame() != null
            && freeColClient.getGame().getMap() != null; 
    }
    
    /**
     * Applies this action.
     * 
     * @param e The <code>ActionEvent</code>.
     */
    public void actionPerformed(ActionEvent e) {
        MapSize ms = showMapSizeDialog();
        if (ms != null) {
            scaleMapTo(ms.width, ms.height);
        }
    }
    
    /**
     * Displays a dialog for choosing the new map size.
     * @return The size of the new map.
     */
    private MapSize showMapSizeDialog() {
        /*
         * TODO: Extend this dialog. It should be possible
         *       to specify the sizes using percentages.
         *       
         *       Add a panel containing information about
         *       the scaling (old size, new size etc).
         */
        final int COLUMNS = 5;

        final Game game = freeColClient.getGame();
        final Map oldMap = game.getMap();
        
        final Canvas canvas = getFreeColClient().getCanvas();
        final String okText = Messages.message("ok");
        final String cancelText = Messages.message("cancel");
        final String widthText = Messages.message("width");
        final String heightText = Messages.message("height");
        
        final JTextField inputWidth = new JTextField(Integer.toString(oldMap.getWidth()), COLUMNS);
        final JTextField inputHeight = new JTextField(Integer.toString(oldMap.getHeight()), COLUMNS);

        final FreeColDialog<MapSize> inputDialog = new FreeColDialog<MapSize>(canvas) {
            public void requestFocus() {
                inputWidth.requestFocus();
            }
        };

        inputDialog.setLayout(new BoxLayout(inputDialog, BoxLayout.Y_AXIS));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        final ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    int width = Integer.parseInt(inputWidth.getText());
                    int height = Integer.parseInt(inputHeight.getText());
                    if (width <= 0 || height <= 0) {
                        throw new NumberFormatException();
                    }
                    inputDialog.setResponse(new MapSize(width, height));
                } catch (NumberFormatException nfe) {
                    canvas.errorMessage("integerAboveZero");
                }
            }
        };
        JButton okButton = new JButton(okText);
        buttons.add(okButton);
        
        JButton cancelButton = new JButton(cancelText);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                inputDialog.setResponse(null);
            }
        });
        buttons.add(cancelButton);
        inputDialog.setCancelComponent(cancelButton);
        
        okButton.addActionListener(al);
        inputWidth.addActionListener(al);
        inputHeight.addActionListener(al);
        
        JLabel widthLabel = new JLabel(widthText);
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = new JLabel(heightText);
        heightLabel.setLabelFor(inputHeight);
        
        JPanel widthPanel = new JPanel(new FlowLayout());
        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputWidth);
        JPanel heightPanel = new JPanel(new FlowLayout());
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputHeight);       
        
        inputDialog.add(widthPanel);
        inputDialog.add(heightPanel);
        inputDialog.add(buttons);

        inputDialog.setSize(inputDialog.getPreferredSize());

        return canvas.showFreeColDialog(inputDialog);
    }
    
    private class MapSize {
        int width;
        int height;
        
        MapSize(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
    
    /**
     * Scales the current map into the specified size. The current
     * map is given by freeColClient.getGame().getMap().
     * 
     * @param width The width of the resulting map.
     * @param height The height of the resulting map.
     */
    private void scaleMapTo(final int width, final int height) {
        /*
         * This implementation uses a simple linear scaling, and
         * the isometric shape is not taken into account.
         * 
         * TODO: Find a better method for choosing a group of
         *       adjacent tiles. This group can then be merged into
         *       a common tile by using the average value (for
         *       example: are there a majority of ocean tiles?).
         */
        
        final Game game = freeColClient.getGame();
        final Map oldMap = game.getMap();

        final int oldWidth = oldMap.getWidth();
        final int oldHeight = oldMap.getHeight();
        
        Tile[][] tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                final int oldX = (x * oldWidth) / width;
                final int oldY = (y * oldHeight) / height;
                /*
                 * TODO: This tile should be based on the average as
                 *       mentioned at the top of this method.
                 */
                Tile importTile = oldMap.getTile(oldX, oldY);
                Tile t = new Tile(game, importTile.getType(), x, y);
                if (importTile.getMoveToEurope() != null) {
                    t.setMoveToEurope(importTile.getMoveToEurope());
                }
                if (t.getTileItemContainer() != null) {
                    t.getTileItemContainer().copyFrom(importTile.getTileItemContainer());
                }
                tiles[x][y] = t;
            }
        }

        Map map = new Map(game, tiles);
        game.setMap(map);
        
        /* Commented because it doesn't appear to do anything valuable
        // Update river directions
        for (Tile t : map.getAllTiles()) {
            t.getTileItemContainer().updateRiver();
        }*/
        
        freeColClient.getGUI().setSelectedTile(map.getTile(0, 0), false);
        freeColClient.getCanvas().refresh();
    }
}
