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

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.common.i18n.Messages;


/**
 * A dialog for editing parameters.
 */
public class ParametersDialog extends FreeColDialog<Parameters> {
    
    private static final int COLUMNS = 5;

    private static final int DEFAULT_distToLandFromHighSeas = 4;

    private static final int DEFAULT_maxDistanceToEdge = 12;

    private final JTextField inputD;

    private final JTextField inputM;


    /**
     * Create a new parameters dialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public ParametersDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);
        /*
         * FIXME: Extend this dialog. It should be possible
         *        to specify the sizes using percentages.
         *
         *        Add a panel containing information about
         *        the scaling (old size, new size etc).
         */        

        MigPanel panel = new MigPanel(new MigLayout("wrap 1, center"));
        JPanel widthPanel = new JPanel(new FlowLayout());
        JPanel heightPanel = new JPanel(new FlowLayout());
        String str;
        
        str = Integer.toString(DEFAULT_distToLandFromHighSeas);
        inputD = new JTextField(str, COLUMNS);
        str = Integer.toString(DEFAULT_maxDistanceToEdge);
        inputM = new JTextField(str, COLUMNS);

        str = Messages.message("parametersDialog.determineHighSeas.distToLandFromHighSeas");
        JLabel widthLabel = new JLabel(str);
        widthLabel.setLabelFor(inputD);
        str = Messages.message("parametersDialog.determineHighSeas.maxDistanceToEdge");
        JLabel heightLabel = new JLabel(str);
        heightLabel.setLabelFor(inputM);

        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputD);
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputM);

        panel.add(widthPanel);
        panel.add(heightPanel);
        panel.setSize(panel.getPreferredSize());
        
        final ActionListener al = (ActionEvent ae) -> {
            ParametersDialog.this.checkFields();
        };
        inputD.addActionListener(al);
        inputM.addActionListener(al);

        final Parameters fake = null;
        List<ChoiceItem<Parameters>> c = choices();
        c.add(new ChoiceItem<>(Messages.message("ok"), fake).okOption());
        c.add(new ChoiceItem<>(Messages.message("cancel"), fake)
            .cancelOption().defaultOption());
        initializeDialog(frame, DialogType.QUESTION, true, panel, null, c);
    }


    /**
     * Force the text fields to contain non-negative integers.
     */
    private void checkFields() {
        try {
            int d = Integer.parseInt(inputD.getText());
            if (d <= 0) throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
            inputD.setText(Integer.toString(DEFAULT_distToLandFromHighSeas));
        }
        try {
            int m = Integer.parseInt(inputM.getText());
            if (m <= 0) throw new NumberFormatException();
        } catch (NumberFormatException nfe) {
            inputM.setText(Integer.toString(DEFAULT_maxDistanceToEdge));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Parameters getResponse() {
        Object value = getValue();
        if (options.get(0).equals(value)) {
            checkFields();
            return new Parameters(Integer.parseInt(inputD.getText()),
                                  Integer.parseInt(inputM.getText()));
        }
        return null;
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.inputD.requestFocus();
    }
}
