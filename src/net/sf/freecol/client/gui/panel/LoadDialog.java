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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;

import java.io.File;

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.FreeColDialog;



/**
 * A dialog for choosing a file to load.
 */
public final class LoadDialog extends FreeColDialog<File> {

    /** The selected file from the file chooser. */
    private File selected = null;


    /**
     * Creates a dialog to choose a file to load.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param directory The directory to display when choosing the file.
     * @param fileFilters The available file filters in the dialog.
     */
    public LoadDialog(FreeColClient freeColClient, File directory,
                      FileFilter[] fileFilters) {
        super(freeColClient);

        JFileChooser fileChooser = new JFileChooser(directory);
        if (fileFilters.length > 0) {
            for (FileFilter fileFilter : fileFilters) {
                fileChooser.addChoosableFileFilter(fileFilter);
            }
            fileChooser.setFileFilter(fileFilters[0]);
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileHidingEnabled(false);
        fileChooser.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    final String cmd = event.getActionCommand();
                    setValue((JFileChooser.APPROVE_SELECTION.equals(cmd))
                        ? ((JFileChooser)event.getSource())
                            .getSelectedFile()
                        : null);
                }
            });

        List<ChoiceItem<File>> c = choices();
        initialize(DialogType.QUESTION, true, fileChooser, null, c);
    }

    /**
     * {@inheritDoc}
     */
    public File getResponse() {
        return (File)getValue();
    }
}
