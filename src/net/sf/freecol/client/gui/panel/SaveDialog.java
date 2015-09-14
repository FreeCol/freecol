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

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ChoiceItem;


/**
 * A dialog for choosing a file to save.
 */
public final class SaveDialog extends FreeColDialog<File> {

    /**
     * We need a magic cookie to use for the cancel response, as
     * the JFileChooser does not tolerate setValue(null).
     */
    private static final File cancelFile = new File(".");


    /**
     * Creates a dialog to choose a file to load.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param directory The directory to display when choosing the file.
     * @param fileFilters The available file filters in the dialog.
     * @param defaultName Name of the default save game file.
     */
    public SaveDialog(FreeColClient freeColClient, JFrame frame,
            File directory, FileFilter[] fileFilters, String defaultName) {
        super(freeColClient, frame);

        final JFileChooser fileChooser = new JFileChooser(directory);
        if (fileFilters.length > 0) {
            for (FileFilter fileFilter : fileFilters) {
                fileChooser.addChoosableFileFilter(fileFilter);
            }
            fileChooser.setFileFilter(fileFilters[0]);
            fileChooser.setAcceptAllFileFilterUsed(false);
        }
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileHidingEnabled(false);
        fileChooser.setSelectedFile(new File(defaultName));
        fileChooser.addActionListener((ActionEvent ae) ->
                setValue((JFileChooser.APPROVE_SELECTION
                        .equals(ae.getActionCommand()))
                    ? fileChooser.getSelectedFile() : cancelFile));
        
        List<ChoiceItem<File>> c = choices();
        initializeDialog(frame, DialogType.QUESTION, true, fileChooser, null, c);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public File getResponse() {
        if (responded()) {
            File value = (File)getValue();
            return (value == cancelFile) ? null : value;
        }
        return null;
    }
}
