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
* A <i>SfxLibrary</i> stores an array of playlists for use with sound-effects.
*/
public final class SfxLibrary extends SoundLibrary {


    // These constants are used to index the "Playlist[]" from SoundLibrary:
    public static final int ERROR = 0;
    public static final int ATTACK = 1;
    public static final int ILLEGAL_MOVE = 2;
    public static final int LOAD_CARGO = 3;
    public static final int SELL_CARGO = 4;
    public static final int ARTILLERY = 5;
    public static final int HORSES = 6;
    public static final int MUSKETSHORSES = 7;
    public static final int DRAGOON = 8;
    public static final int SUNK = 9;
    public static final int ANTHEM_BASE = 10;
    public static final int ANTHEM_DUTCH = ANTHEM_BASE+0;
    public static final int ANTHEM_SPANISH = ANTHEM_BASE+1;
    public static final int ANTHEM_FRENCH = ANTHEM_BASE+2;
    public static final int ANTHEM_ENGLISH = ANTHEM_BASE+3;

    
    /**
    * The constructor.
    * @param freeColHome A directory containing the "audio"-directory.
    * @throws FreeColException If the "sfx"-directory could not be found in the path spesified by <i>freeColHome</i>.
    */
    public SfxLibrary(String freeColHome) throws FreeColException {
        super(new File((freeColHome.equals("") ? "data" + System.getProperty("file.separator"): freeColHome) + "audio" + System.getProperty("file.separator") + "sfx"));
    }


}
