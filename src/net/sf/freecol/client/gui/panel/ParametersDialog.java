/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A dialog for editing parameters.
 */
public class ParametersDialog extends FreeColOldDialog<Parameters> {
    
    final int COLUMNS = 5;

    final int DEFAULT_distToLandFromHighSeas = 4;

    final int DEFAULT_maxDistanceToEdge = 12;

    final JTextField inputD = new JTextField(Integer.toString(DEFAULT_distToLandFromHighSeas), COLUMNS);

    final JTextField inputM = new JTextField(Integer.toString(DEFAULT_maxDistanceToEdge), COLUMNS);


    public ParametersDialog(FreeColClient freeColClient) {
        super(freeColClient);
        /*
         * TODO: Extend this dialog. It should be possible
         *       to specify the sizes using percentages.
         *
         *       Add a panel containing information about
         *       the scaling (old size, new size etc).
         */        

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

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
                    setResponse(new Parameters(d, m));
                } catch (NumberFormatException nfe) {
                    getGUI().errorMessage("integerAboveZero");
                }
            }
        };
        JButton okButton = new JButton(Messages.message("ok"));
        buttons.add(okButton);

        JButton cancelButton = new JButton(Messages.message("cancel"));
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setResponse(null);
            }
        });
        buttons.add(cancelButton);
        setCancelComponent(cancelButton);

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

        add(widthPanel);
        add(heightPanel);
        add(buttons);

        setSize(getPreferredSize());
    }

    @Override
    public void requestFocus() {
        inputD.requestFocus();
    }

}
