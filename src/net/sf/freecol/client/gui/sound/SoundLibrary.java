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
import java.io.FileFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.sf.freecol.common.FreeColException;

/**
 * A <i>SoundLibrary</i> stores a number of playlists.
 */
public class SoundLibrary {


    // These constants are used to index the "Playlist[]" from SoundLibrary:
    public static enum SoundEffect { ERROR,
            ATTACK,
            ILLEGAL_MOVE,
            LOAD_CARGO,
            SELL_CARGO,
            ATTACK_ARTILLERY,
            SUNK,
            ATTACK_DRAGOON,
            ATTACK_NAVAL,
            CAPTURED_BY_ARTILLERY,
            BUILDING_COMPLETE,
            MISSION_ESTABLISHED}

    
    /** This array contains the playlists. */
    private Map<String, Playlist> playlists = new HashMap<String, Playlist>();

    private static final FileFilter soundFileFilter =
        new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return false;
                } else {
                    String s = file.getName();
                    return (s.endsWith(".au") || s.endsWith(".wav") || 
                            s.endsWith(".aif") || s.endsWith(".aiff") ||
                            s.endsWith(".ogg") || s.endsWith(".mp3"));
                }
            }
        };
                

    /**
     * The constructor. Load the directory spesified by <i>file</i>.
     * 
     * @param dir A directory containing the sound-files.
     * @throws FreeColException If the file spesified is not a directory.
     */
    public SoundLibrary(File dir) throws FreeColException {
        if (!dir.isDirectory()) {
            throw new FreeColException("The file \"" + dir.getName() + "\" is not a directory.");
        }

        for (File file : dir.listFiles()) {
            if (file.getName().endsWith(".svn")) {
                continue;
            } else if (file.isDirectory()) {
                File[] files = file.listFiles(soundFileFilter);
                Arrays.sort(files);
                playlists.put(file.getName(), new Playlist(files));
            } else {
                String fileName = file.getName();
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
                playlists.put(fileName, new Playlist(file));
            }

        }
    }

    /**
     * Returns a playlist associated with a <code>Nation</code>.
     * 
     * @param string The key to look up a Playlist.
     * @return The <code>PlayList</code> specified by the ID.
     */
    public Playlist get(String string) {
        return playlists.get(string);
    }


    /**
     * Returns a playlist identified by id.
     * 
     * @param effect The sound effect.
     * @return The <code>Playlist</code> specified by the ID.
     */
    public Playlist get(SoundEffect effect) {
        return playlists.get(effect.toString().toLowerCase());
    }

}
