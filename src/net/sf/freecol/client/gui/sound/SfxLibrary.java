
package net.sf.freecol.client.gui.sound;

import java.io.File;

import net.sf.freecol.common.FreeColException;

/**
* A <i>SfxLibrary</i> stores an array of playlists for use with sound-effects.
*/
public final class SfxLibrary extends SoundLibrary {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    // These constants are used to index the "Playlist[]" from SoundLibrary:
    public static final int ERROR = 0;
    public static final int ATTACK = 1;
    public static final int ILLEGAL_MOVE = 2;
    public static final int LOAD_CARGO = 3;
    public static final int SELL_CARGO = 4;
    public static final int ARTILLERY = 5;

    
    /**
    * The constructor.
    * @param freeColHome A directory containing the "audio"-directory.
    * @throws FreeColException If the "sfx"-directory could not be found in the path spesified by <i>freeColHome</i>.
    */
    public SfxLibrary(String freeColHome) throws FreeColException {
        super(new File((freeColHome.equals("") ? "data" + System.getProperty("file.separator"): freeColHome) + "audio" + System.getProperty("file.separator") + "sfx"));
    }


}
