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

package net.sf.freecol.client.gui.option;

import java.awt.event.ActionEvent;
import java.awt.Color;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.option.FileOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.FileOption} in order to enable values
 * to be both seen and changed.
 */
public final class FileOptionUI extends OptionUI<FileOption>  {

    private final JPanel panel = new JPanel();
    private final JTextField fileField;

    /**
    * Creates a new <code>FileOptionUI</code> for the given
    * <code>FileOption</code>.
    *
    * @param option The <code>FileOption</code> to make a user interface for.
    * @param editable boolean whether user can modify the setting
    */
    public FileOptionUI(final GUI gui, final FileOption option, boolean editable) {
        super(option, editable);

        panel.add(getJLabel());

        File file = option.getValue();
        fileField = new JTextField((file == null) ? null : file.getAbsolutePath(), 20);
        fileField.setToolTipText((file == null) ? null : file.getAbsolutePath());
        fileField.setDisabledTextColor(Color.BLACK);
        panel.add(fileField);

        JButton browse = Utility.localizedButton("browse");
        if (editable) {
            browse.addActionListener((ActionEvent ae) -> {
                    File f = gui.showLoadSaveFileDialog();
                    if (f != null) setValue(f);
                });
        }
        panel.add(browse);

        JButton remove = Utility.localizedButton("remove");
        if (editable) {
            remove.addActionListener((ActionEvent ae) -> {
                    setValue(null);
                });
        }
        panel.add(remove);

        browse.setEnabled(editable);
        remove.setEnabled(editable);
        fileField.setEnabled(false);
        getJLabel().setLabelFor(fileField);
        /*
        fileField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent arg0) {
                editUpdate();
            }
            public void insertUpdate(DocumentEvent arg0) {
                editUpdate();
            }
            public void removeUpdate(DocumentEvent arg0) {
                editUpdate();
            }
            private void editUpdate() {
            }
        });
        */

    }

    /**
     * Sets the value of this UI's component.
     */
    public void setValue(File f) {
        getOption().setValue(f);
        reset();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getComponent() {
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        File f = (fileField.getText() == null
            || fileField.getText().isEmpty()) ? null
            : new File(fileField.getText());
        getOption().setValue(f);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        File file = getOption().getValue();
        String text = (file == null) ? "" : file.getAbsolutePath();
        fileField.setText(text);
        fileField.setToolTipText(text);
    }
}
