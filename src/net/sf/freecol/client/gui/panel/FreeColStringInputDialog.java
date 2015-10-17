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

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.FreeColClient;


/**
 * A simple input dialog to collect a string.
 */
public final class FreeColStringInputDialog extends FreeColInputDialog<String> {

    /** The text field for the user to set. */
    private final JTextField textField;


    /**
     * Creates a dialog to input a string field.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param modal True if this dialog should be modal.
     * @param text Text that explains the action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     */
    public FreeColStringInputDialog(FreeColClient freeColClient, JFrame frame,
            boolean modal, String text, String defaultValue,
            String okKey, String cancelKey) {
        super(freeColClient, frame);

        textField = new JTextField(defaultValue);
        textField.setOpaque(false);
        JPanel panel = new JPanel(new BorderLayout()) {
                @Override
                public void requestFocus() {
                    textField.requestFocus();
                }
            };
        panel.setOpaque(false);

        panel.add(Utility.getDefaultTextArea(text));
        panel.add(textField, BorderLayout.SOUTH);

        initializeInputDialog(frame, modal, panel, null, okKey, cancelKey);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getInputValue() {
        return textField.getText();
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        this.textField.requestFocus();
    }
}
