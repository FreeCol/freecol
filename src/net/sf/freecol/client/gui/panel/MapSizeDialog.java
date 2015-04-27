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

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;


/**
 * A dialog for choosing a map size.
 */
public final class MapSizeDialog extends FreeColInputDialog<Dimension> {

    private static final int COLUMNS = 5;
    private static final int DEFAULT_HEIGHT = 100;
    private static final int DEFAULT_WIDTH = 40;

    private final JTextField inputWidth
        = new JTextField(Integer.toString(DEFAULT_WIDTH), COLUMNS);
    private final JTextField inputHeight
        = new JTextField(Integer.toString(DEFAULT_HEIGHT), COLUMNS);


    /**
     * Creates a dialog to choose the map size.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     */
    public MapSizeDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        JLabel widthLabel = Utility.localizedLabel("width");
        widthLabel.setLabelFor(inputWidth);
        JLabel heightLabel = Utility.localizedLabel("height");
        heightLabel.setLabelFor(inputHeight);

        MigPanel panel = new MigPanel(new MigLayout("wrap 2"));
        
        panel.add(Utility.localizedHeader("mapSizeDialog.mapSize", true),
                  "span, align center");
        panel.add(widthLabel, "newline 20");
        panel.add(inputWidth);
        panel.add(heightLabel);
        panel.add(inputHeight);

        initializeInputDialog(frame, true, panel, null, "ok", "cancel");
    }

    /**
     * {@inheritDoc}
     */
    @Override
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


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.inputWidth.requestFocus();
    }
}
