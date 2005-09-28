
package net.sf.freecol.client.gui.sound;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import net.sf.freecol.common.FreeColException;

/**
* A <i>SoundLibrary</i> stores an array of playlists.
*/
public class SoundLibrary {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** This array contains the playlists. */
    private Playlist[] playlists;



    /**
    * The constructor. Load the directory spesified by <i>file</i>.
    * @param file A directory containing the sound-files.
    * @throws FreeColException If the file spesified is not a directory.
    */
    public SoundLibrary(File file) throws FreeColException  {
        if (!file.isDirectory())
            throw new FreeColException("The file \"" + file.getName() + "\" is not a directory.");

        HashMap playlists = new HashMap();
        int greatestNumber = -1;

        String[] files = file.list();

        for (int i = 0; i < files.length; i++) {
            File leafFile = new File(file.getAbsolutePath(), files[i]);
            int fileNumber = getNumber(leafFile.getName());

            if (fileNumber == -1)
                continue;

            if (fileNumber > greatestNumber)
                greatestNumber = fileNumber;

            if (leafFile.isDirectory()) {
                ArrayList arrayList = new ArrayList();
                playlists.put(new Integer(fileNumber), new Playlist(((File[]) (loadFiles(leafFile.getAbsolutePath(), arrayList)).toArray(new File[0]))));
            } else {
                ArrayList arrayList = new ArrayList();
                addFile(leafFile, arrayList);
                playlists.put(new Integer(fileNumber), new Playlist(((File[]) arrayList.toArray(new File[0]))));
            }

        }

        this.playlists = new Playlist[greatestNumber + 1];

        for (int i = 0; i < greatestNumber + 1; i++)
            if (playlists.containsKey(new Integer(i)))
                this.playlists[i] = (Playlist) playlists.get(new Integer(i));
    }



    /**
    * Get a number from a filename like this:
    *
    * "01Attack.wav" returns 1.
    * "Build12.wav" returns 12.
    *
    * @return Mentioned above.
    */
    private int getNumber(String filename) {
        StringBuffer stringBuffer = new StringBuffer();

        for (int i=0; i < filename.length(); i++)
            if (Character.isDigit(filename.charAt(i)))
                stringBuffer.append(filename.charAt(i));

        try {
            return Integer.parseInt(stringBuffer.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }



    /**
    * Go recursive through the files/directories from the file/directory spesified by name
    * and add the files to an <i>ArrayList</i>.
    * @param name A file/directory containing sound/sound-files.
    * @param arrayList The arrayList you should add the sound files to.
    * @return The <i>ArrayList</i> with the files added.
    */
    private ArrayList loadFiles(String name, ArrayList arrayList) {
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
    * Adds a file to an <i>ArrayList</i>. It only adds files that ends with ".au", ".mid", ".aif",
    * ".aiff", ".rmf" or ".wav"; other files are ignored.
    *
    * @param file The file to add.
    * @param arrayList The arrayList the file is added to.
    */
    private void addFile(File file, ArrayList arrayList) {
        String s = file.getName();

        // Only accept sound file-types.
        if (s.endsWith(".au") || s.endsWith(".rmf") ||
            s.endsWith(".mid") || s.endsWith(".wav") ||
            s.endsWith(".aif") || s.endsWith(".aiff"))
        {

            arrayList.add(file);
        }
    }



    /**
    * Returns a playlist identified by id.
    */
    public Playlist get(int id) {
        if (id >= 0 && id < playlists.length)
            return playlists[id];
        else
            return null;
    }
}
