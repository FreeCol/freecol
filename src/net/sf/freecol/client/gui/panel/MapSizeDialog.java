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

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColInputDialog;
import net.sf.freecol.client.gui.panel.MigPanel;

import net.miginfocom.swing.MigLayout;


/**
 * A dialog for choosing a map size.
 */
public final class MapSizeDialog extends FreeColInputDialog<Dimension> {

    private final int COLUMNS = 5;
    private final int DEFAULT_HEIGHT = 100;
    private final int DEFAULT_WIDTH = 40;

    private JTextField inputWidth
        = new JTextField(Integer.toString(DEFAULT_WIDTH), COLUMNS);
    private JTextField inputHeight
        = new JTextField(Integer.toString(DEFAULT_HEIGHT), COLUMNS);


    /**
     * Creates a dialog to choose the map size.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public MapSizeDialog(FreeColClient freeColClient) {
        super(freeColClient);

        MigPanel panel = new MigPanel(new MigLayout("wrap 2"));

        String widthText = Messages.message("width");
        JLabel widthLabel = new JLabel(widthText);
        widthLabel.setLabelFor(inputWidth);
        String heightText = Messages.message("height");
        JLabel heightLabel = new JLabel(heightText);
        heightLabel.setLabelFor(inputHeight);

        panel.add(new JLabel(Messages.message("editor.mapSize")),
                  "span, align center");
        panel.add(widthLabel, "newline 20");
        panel.add(inputWidth);
        panel.add(heightLabel);
        panel.add(inputHeight);

        initialize(panel, null, "ok", "cancel");
    }

    /**
     * {@inheritDoc}
     */
    protected Dimension getInputValue() {
        int width, height;
        try {
            width = Integer.parseInt(inputWidth.getText());
            height = Integer.parseInt(inputHeight.getText());
        } catch (NumberFormatException nfe) {
            return null;
        }
        return (width <= 0 || height <= 0) ? null
            : new Dimension(width, height);
    }
}
