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


package net.sf.freecol.client.gui.option;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.FileOption;

/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.FileOption}. In order to enable values
 * to be both seen and changed.
 */
public final class FileOptionUI extends OptionUI<FileOption>  {

    private JPanel panel = new JPanel();
    private final JTextField fileField;

    /**
    * Creates a new <code>FileOptionUI</code> for the given
    * <code>FileOption</code>.
    *
    * @param option The <code>FileOption</code> to make a user interface for.
    * @param editable boolean whether user can modify the setting
    */
    public FileOptionUI(final GUI gui, final FileOption option, boolean editable) {
        super(gui, option, editable);

        panel.add(getLabel());

        File file = option.getValue();
        fileField = new JTextField((file == null) ? null : file.getAbsolutePath(), 10);
        panel.add(fileField);

        JButton browse = new JButton(Messages.message("file.browse"));
        if (editable) {
            browse.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   File file = gui.showLoadDialog(FreeCol.getSaveDirectory());

                   if (file == null) {
                       return;
                   }

                   if (!file.isFile()) {
                       gui.errorMessage("fileNotFound");
                       return;
                   }

                   fileField.setText(file.getAbsolutePath());
               }
            });
        }
        panel.add(browse);

        JButton remove = new JButton(Messages.message("option.remove"));
        if (editable) {
            remove.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   fileField.setText("");
               }
            });
        }
        panel.add(remove);

        browse.setEnabled(editable);
        remove.setEnabled(editable);
        fileField.setEnabled(false);
        getLabel().setLabelFor(fileField);
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
        if (f == null) {
            fileField.setText("");
        } else {
            fileField.setText(f.getAbsolutePath());
        }
    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        if (fileField.getText().equals("")) {
            getOption().setValue(null);
        } else {
            getOption().setValue(new File(fileField.getText()));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        File file = getOption().getValue();
        fileField.setText((file == null) ? null : file.getAbsolutePath());
    }

}
