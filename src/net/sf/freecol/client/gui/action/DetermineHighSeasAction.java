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
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.server.generator.TerrainGenerator;

/**
 * An action for determining the high seas tiles.
 */
public class DetermineHighSeasAction extends FreeColAction {

    public static final String id = "determineHighSeasAction";


    /**
     * Creates a new <code>DetermineHighSeasAction</code>.
     *
     * @param freeColClient The main controller object for the client.
     * @param gui 
     */
    DetermineHighSeasAction(FreeColClient freeColClient, GUI gui) {
        super(freeColClient, gui, id);
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

        final Canvas canvas = gui.getCanvas();
        final JTextField inputD = new JTextField(Integer.toString(DEFAULT_distToLandFromHighSeas), COLUMNS);
        final JTextField inputM = new JTextField(Integer.toString(DEFAULT_maxDistanceToEdge), COLUMNS);

        final FreeColDialog<Parameters> inputDialog = new FreeColDialog<Parameters>(freeColClient, gui)  {
            @Override
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
                    gui.errorMessage("integerAboveZero");
                }
            }
        };
        JButton okButton = new JButton(Messages.message("ok"));
        buttons.add(okButton);

        JButton cancelButton = new JButton(Messages.message("cancel"));
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

        JLabel widthLabel = new JLabel(Messages.message("menuBar.tools.determineHighSeas.distToLandFromHighSeas"));
        widthLabel.setLabelFor(inputD);
        JLabel heightLabel = new JLabel(Messages.message("menuBar.tools.determineHighSeas.maxDistanceToEdge"));
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

        return canvas.showFreeColDialog(inputDialog);
    }

    private class Parameters {
        int distToLandFromHighSeas;
        int maxDistanceToEdge;

        Parameters(int distToLandFromHighSeas, int maxDistanceToEdge) {
            this.distToLandFromHighSeas = distToLandFromHighSeas;
            this.maxDistanceToEdge = maxDistanceToEdge;
        }
    }

}
