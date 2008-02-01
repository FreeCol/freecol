/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.client.gui.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.freecol.common.FreeColException;

/**
 * A <i>SoundLibrary</i> stores an array of playlists.
 */
public class SoundLibrary {



    /** This array contains the playlists. */
    private Playlist[] playlists;


    /**
     * The constructor. Load the directory spesified by <i>file</i>.
     * 
     * @param file A directory containing the sound-files.
     * @throws FreeColException If the file spesified is not a directory.
     */
    public SoundLibrary(File file) throws FreeColException {
        if (!file.isDirectory())
            throw new FreeColException("The file \"" + file.getName() + "\" is not a directory.");

        HashMap<Integer, Playlist> playlists = new HashMap<Integer, Playlist>();
        int greatestNumber = -1;

        String[] files = file.list();

        for (int i = 0; i < files.length; i++) {
            File leafFile = new File(file.getAbsolutePath(), files[i]);
            if (leafFile.getAbsolutePath().endsWith(".svn"))
                continue;
            int fileNumber = getNumber(leafFile.getName());

            if (fileNumber == -1)
                continue;

            if (fileNumber > greatestNumber)
                greatestNumber = fileNumber;

            if (leafFile.isDirectory()) {
                ArrayList<File> arrayList = new ArrayList<File>();
                playlists.put(new Integer(fileNumber), new Playlist(loadFiles(leafFile.getAbsolutePath(), arrayList)
                        .toArray(new File[0])));
            } else {
                ArrayList<File> arrayList = new ArrayList<File>();
                addFile(leafFile, arrayList);
                playlists.put(fileNumber, new Playlist(arrayList.toArray(new File[0])));
            }

        }

        this.playlists = new Playlist[greatestNumber + 1];

        for (int i = 0; i < greatestNumber + 1; i++)
            if (playlists.containsKey(new Integer(i)))
                this.playlists[i] = playlists.get(i);
    }

    /**
     * Get a number from a filename like this:
     * 
     * "01Attack.wav" returns 1. "Build12.wav" returns 12.
     * 
     * @param filename The filename to get the number from.
     * @return Mentioned above.
     */
    private int getNumber(String filename) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int i = 0; i < filename.length(); i++)
            if (Character.isDigit(filename.charAt(i)))
                stringBuffer.append(filename.charAt(i));

        try {
            return Integer.parseInt(stringBuffer.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Go recursive through the files/directories from the file/directory
     * spesified by name and add the files to an <i>ArrayList</i>.
     * 
     * @param name A file/directory containing sound/sound-files.
     * @param arrayList The arrayList you should add the sound files to.
     * @return The <i>ArrayList</i> with the files added.
     */
    private ArrayList<File> loadFiles(String name, ArrayList<File> arrayList) {
        try {
            File file = new File(name);

            if (file != null && file.isDirectory()) {
                String[] files = file.list();
                for (int i = 0; i < files.length; i++) {
                    File leafFile = new File(file.getAbsolutePath(), files[i]);
                    if (leafFile.isDirectory()) {
                        loadFiles(leafFile.getAbsolutePath(), arrayList);
                    } else {
                        addFile(leafFile, arrayList);
                    }
                }
            } else if (file != null && file.exists()) {
                addFile(file, arrayList);
            }

        } catch (SecurityException ex) {
            // TODO: Log the exception: "SecurityException while loading files."
        } catch (Exception ex) {
            // TODO: Log the exception: "Error while loading files."
        }

        return arrayList;
    }

    /**
     * Adds a file to an <i>ArrayList</i>. It only adds files that ends with
     * ".au", ".mid", ".aif", ".aiff", ".rmf" or ".wav"; other files are
     * ignored.
     * 
     * @param file The file to add.
     * @param arrayList The arrayList the file is added to.
     */
    private void addFile(File file, ArrayList<File> arrayList) {
        String s = file.getName();

        // Only accept sound file-types.
        if (s.endsWith(".au") || s.endsWith(".rmf") || s.endsWith(".mid") || s.endsWith(".wav") || s.endsWith(".aif")
                || s.endsWith(".aiff")) {

            arrayList.add(file);
        }
    }

    /**
     * Returns a playlist identified by id.
     * 
     * @param id The ID.
     * @return The <code>PlayList</code> specified by the ID.
     */
    public Playlist get(int id) {
        if (id >= 0 && id < playlists.length)
            return playlists[id];
        else
            return null;
    }
}
