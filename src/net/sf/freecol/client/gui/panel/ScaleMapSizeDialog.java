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

import java.awt.Dimension;
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
import net.sf.freecol.common.model.Map;


/**
 * A dialog to allow resizing of the map.
 */
public class ScaleMapSizeDialog extends FreeColDialog<Dimension> {

    private static final int COLUMNS = 5;

    private Map oldMap;

    final JTextField inputWidth = new JTextField(Integer.toString(oldMap.getWidth()), COLUMNS);

    final JTextField inputHeight = new JTextField(Integer.toString(oldMap.getHeight()), COLUMNS);


    public ScaleMapSizeDialog(final FreeColClient freeColClient) {
        super(freeColClient);

        oldMap = freeColClient.getGame().getMap();
        /*
         * TODO: Extend this dialog. It should be possible to specify the sizes
         * using percentages.
         *
         * Add a panel containing information about the scaling (old size, new
         * size etc).
         */

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

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
                    setResponse(new Dimension(width, height));
                } catch (NumberFormatException nfe) {
                    freeColClient.getGUI().errorMessage("integerAboveZero");
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
        inputWidth.addActionListener(al);
        inputHeight.addActionListener(al);

        JLabel widthLabel = new JLabel(Messages.message("width"));
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = new JLabel(Messages.message("height"));
        heightLabel.setLabelFor(inputHeight);

        JPanel widthPanel = new JPanel(new FlowLayout());
        widthPanel.setOpaque(false);
        widthPanel.add(widthLabel);
        widthPanel.add(inputWidth);
        JPanel heightPanel = new JPanel(new FlowLayout());
        heightPanel.setOpaque(false);
        heightPanel.add(heightLabel);
        heightPanel.add(inputHeight);

        add(widthPanel);
        add(heightPanel);
        add(buttons);

        setSize(getPreferredSize());
    }

    @Override
    public void requestFocus() {
        inputWidth.requestFocus();
    }

}
