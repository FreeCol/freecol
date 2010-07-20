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

import net.sf.freecol.common.FreeColException;


/**
* A <i>MusicLibrary</i> stores an array of playlists for use with music.
*/
public final class MusicLibrary extends SoundLibrary {
    

    // These constants are used to index the "Playlist[]" from SoundLibrary:
    public static final int SILENCE = 0;
    public static final int INTRO = 1;


    /**
    * The constructor.
    * @param freeColHome A directory containing the "music"-directory.
    * @throws FreeColException If the "music"-directory could not be found in the path specified by <i>freeColHome</i>.
    */
    public MusicLibrary(String freeColHome) throws FreeColException {
        super(new File(new File(("".equals(freeColHome) ? "data" : freeColHome), "audio"), "music"));
    }


}
