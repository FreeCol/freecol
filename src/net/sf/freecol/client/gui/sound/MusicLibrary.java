
package net.sf.freecol.client.gui.sound;

import java.io.File;

import net.sf.freecol.common.FreeColException;


/**
* A <i>MusicLibrary</i> stores an array of playlists for use with music.
*/
public final class MusicLibrary extends SoundLibrary {
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // These constants are used to index the "Playlist[]" from SoundLibrary:
    public static final int SILENCE = 0;
    public static final int INTRO = 1;


    /**
    * The constructor.
    * @param freeColHome A directory containing the "music"-directory.
    * @throws FreeColException If the "music"-directory could not be found in the path spesified by <i>freeColHome</i>.
    */
    public MusicLibrary(String freeColHome) throws FreeColException {
        super(new File((freeColHome.equals("") ? "data" + System.getProperty("file.separator"): freeColHome) + "audio" + System.getProperty("file.separator") + "music"));
    }


}
