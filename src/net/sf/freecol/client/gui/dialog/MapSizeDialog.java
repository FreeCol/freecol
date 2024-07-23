/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.Dimension;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.panel.FreeColButton;
import net.sf.freecol.client.gui.panel.FreeColButton.ButtonStyle;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;


/**
 * A dialog for choosing a map size.
 */
public final class MapSizeDialog {

    private static final int COLUMNS = 5;
    private static final int DEFAULT_WIDTH = 40;
    private static final int DEFAULT_HEIGHT = 100;

    
    public static FreeColDialog<Dimension> create() {
        final FreeColDialog<Dimension> dialog = new FreeColDialog<Dimension>(api -> {
            final JPanel content = new JPanel(new MigLayout("fill"));
            content.add(Utility.localizedHeaderLabel("mapSizeDialog.mapSize", SwingConstants.CENTER, Utility.FONTSPEC_SUBTITLE), "span");
            
            final JTextField inputWidth = new JTextField(Integer.toString(DEFAULT_WIDTH), COLUMNS);
            final JTextField inputHeight = new JTextField(Integer.toString(DEFAULT_HEIGHT), COLUMNS);
            
            final JLabel widthLabel = Utility.localizedLabel("width");
            widthLabel.setLabelFor(inputWidth);
            final JLabel heightLabel = Utility.localizedLabel("height");
            heightLabel.setLabelFor(inputHeight);
            
            content.add(widthLabel, "newline unrel");
            content.add(inputWidth);
            content.add(heightLabel, "newline");
            content.add(inputHeight);

            final ActionListener al = ae -> {
                api.setValue(determineResult(inputWidth, inputHeight));
            };
            inputWidth.addActionListener(al);
            inputHeight.addActionListener(al);
            
            final JButton okButton = new FreeColButton(Messages.message("ok")).withButtonStyle(ButtonStyle.IMPORTANT);
            okButton.addActionListener(al);
            content.add(okButton, "newline unrel, span, split 2, tag ok");
            
            final JButton cancelButton = new FreeColButton(Messages.message("cancel"));
            cancelButton.addActionListener(ae -> {
                api.setValue(null);
            });
            content.add(cancelButton, "tag cancel");
            
            api.setInitialFocusComponent(okButton);
            
            return content;
        });
        return dialog;
    }

    private static Dimension determineResult(JTextField inputWidth, JTextField inputHeight) {
        int width, height;
        try {
            width = Integer.parseInt(inputWidth.getText());
            height = Integer.parseInt(inputHeight.getText());
        } catch (NumberFormatException nfe) {
            return null;
        }
        return (width <= 0 || height <= 0) ? null : new Dimension(width, height);
    }
}
