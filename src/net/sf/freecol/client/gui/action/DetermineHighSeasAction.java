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

package net.sf.freecol.client.gui.action;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

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
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.server.generator.TerrainGenerator;

/**
 * An action for determining the high seas tiles.
 */
public class DetermineHighSeasAction extends FreeColAction {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(DetermineHighSeasAction.class.getName());




    public static final String ID = "determineHighSeasAction";


    /**
     * Creates a new <code>DetermineHighSeasAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     */
    DetermineHighSeasAction(FreeColClient freeColClient) {
        super(freeColClient, "menuBar.tools.determineHighSeas", null, 0, null);
    }

    /**
     * Returns the id of this <code>Option</code>.
     * 
     * @return "determineHighSeasAction"
     */
    public String getId() {
        return ID;
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
        final Game game = freeColClient.getGame();
        final Map map = game.getMap();

        Parameters p = showParametersDialog();
        if (p != null) {
            TerrainGenerator.determineHighSeas(map, p.distToLandFromHighSeas, p.maxDistanceToEdge);
        }
    }
    
    /**
     * Displays a dialog for setting parameters.
     * @return The parameters
     */
    private Parameters showParametersDialog() {
        /*
         * TODO: Extend this dialog. It should be possible
         *       to specify the sizes using percentages.
         *       
         *       Add a panel containing information about
         *       the scaling (old size, new size etc).
         */
        final int COLUMNS = 5;
        final int DEFAULT_distToLandFromHighSeas = 4;
        final int DEFAULT_maxDistanceToEdge = 12;
        
        final Canvas canvas = getFreeColClient().getCanvas();
        final String okText = Messages.message("ok");
        final String cancelText = Messages.message("cancel");
        final String dText = Messages.message("menuBar.tools.determineHighSeas.distToLandFromHighSeas");
        final String mText = Messages.message("menuBar.tools.determineHighSeas.maxDistanceToEdge");
        
        final JTextField inputD = new JTextField(Integer.toString(DEFAULT_distToLandFromHighSeas), COLUMNS);
        final JTextField inputM = new JTextField(Integer.toString(DEFAULT_maxDistanceToEdge), COLUMNS);

        final FreeColDialog inputDialog = new FreeColDialog()  {
            public void requestFocus() {
                inputD.requestFocus();
            }
        };

        inputDialog.setLayout(new BoxLayout(inputDialog, BoxLayout.Y_AXIS));

        JPanel buttons = new JPanel();
        buttons.setOpaque(false);

        final ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    int d = Integer.parseInt(inputD.getText());
                    int m = Integer.parseInt(inputM.getText());
                    if (d <= 0 || m <= 0) {
                        throw new NumberFormatException();
                    }
                    inputDialog.setResponse(new Parameters(d, m));
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
        inputD.addActionListener(al);
        inputM.addActionListener(al);
        
        JLabel widthLabel = new JLabel(dText);
        widthLabel.setLabelFor(inputD);
        JLabel heightLabel = new JLabel(mText);
        heightLabel.setLabelFor(inputM);
        
        JPanel widthPanel = new JPanel(new FlowLayout());
        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputD);
        JPanel heightPanel = new JPanel(new FlowLayout());
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputM);       
        
        inputDialog.add(widthPanel);
        inputDialog.add(heightPanel);
        inputDialog.add(buttons);

        inputDialog.setSize(inputDialog.getPreferredSize());

        return (Parameters) canvas.showFreeColDialog(inputDialog);
    }
    
    private class Parameters {
        int distToLandFromHighSeas;
        int maxDistanceToEdge;
        
        Parameters(int distToLandFromHighSeas, int maxDistanceToEdge) {
            this.distToLandFromHighSeas = distToLandFromHighSeas;
            this.maxDistanceToEdge = maxDistanceToEdge;
        }
    }
    
    /**
     * Scales the current map into the specified size. The current
     * map is given by {@link FreeColClient#getGame()#getMap}.   
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
        
        Vector<Vector<Tile>> columns = new Vector<Vector<Tile>>(width);
        for (int i = 0; i < width; i++) {
            Vector<Tile> v = new Vector<Tile>(height);
            for (int j = 0; j < height; j++) {
                final int oldX = (i * oldWidth) / width;
                final int oldY = (j * oldHeight) / height;
                /*
                 * TODO: This tile should be based on the average as
                 *       mentioned at the top of this method.
                 */
                Tile importTile = oldMap.getTile(oldX, oldY);
                Tile t = new Tile(game, importTile.getType(), i, j);
                t.getTileItemContainer().copyFrom(importTile.getTileItemContainer());
                v.add(t);
            }
            columns.add(v);
        }

        Map map = new Map(game, columns);
        game.setMap(map);
        
        // Update river directions
        for (Tile t : map.getAllTiles()) {
            t.getTileItemContainer().updateRiver();
        }
        
        freeColClient.getGUI().setSelectedTile(new Position(0, 0));
        freeColClient.getCanvas().refresh();
    }
}
