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

package net.sf.freecol.common.sound;

import java.io.File;
import java.util.Arrays;


/**
 * Represent a set of sounds that will be presented to a SoundPlayer in a
 * certain order as defined by the Playlists playmodes.
 */
public final class Playlist {
    
    /**
    * This const represents a way in which successive sounds can be played.
    */
    public static final int PLAY_ALL = 0,   // play all sounds in the Playlist once
                            REPEAT_ALL = 1, // play all sounds in the Playlist until the end of times
                            PLAY_ONE = 2,   // play one sound in the Playlist once
                            REPEAT_ONE = 3; // play one sound in the Playlist until the end of times

    /**
    * This const represents a way in which a sound will be picked from the list.
    */
    public static final int FORWARDS = 0,   // order of the sounds is determined by order in Playlist
                            BACKWARDS = 1,  // order of the sounds is determined by reverse order in Playlist
                            SHUFFLE = 2;    // order of the sounds is randomly determined (each sound has the same chance of being picked, but eventually all sounds will be played as much as any other sound)

    private final File[] soundFiles;
    private int num; // '-1' means we haven't started yet, '-2' means this Playlist is exhausted
    private int repeatMode;
    private int pickMode;
    private final int[] playedSounds;



    /**
    * The constructor to use. All songs will be played once, in order.
    * @param soundFiles The sounds that will make up this Playlist. The order of the sounds is important.
    */
    public Playlist(File... soundFiles) {
        if (soundFiles.length == 0) {
            throw new IllegalArgumentException("It's not possible to create an empty Playlist.");
        }

        this.soundFiles = soundFiles;
        repeatMode = REPEAT_ALL;
        pickMode = FORWARDS;
        num = -1;
        playedSounds = null;
    }



    /**
    * The constructor to use.
    * @param soundFiles The sounds that will make up this Playlist. The order of the sounds may be important.
    * @param repeatMode Determines how, which and if songs will be repeated. Should be one of {PLAY_ALL, REPEAT_ALL, PLAY_ONE, REPEAT_ONE}.
    * @param pickMode The way in which sounds will be picked from the list. Should be one of {FORWARDS, BACKWARDS, SHUFFLE}.
    */
    public Playlist(File[] soundFiles, int repeatMode, int pickMode) {
        if (soundFiles.length == 0) {
            throw new IllegalArgumentException("It's not possible to create an empty Playlist.");
        }

        this.soundFiles = soundFiles;
        this.repeatMode = repeatMode;
        this.pickMode = pickMode;
        num = -1;

        if (pickMode == SHUFFLE) {
            playedSounds = new int[soundFiles.length];
            for (int i = 0; i < playedSounds.length; i++) {
                playedSounds[i] = Integer.MAX_VALUE;
            }
        }
        else {
            playedSounds = null;
        }
    }



    /** 
     * Sets the repeat-mode for this playlist.
     * @param repeatMode The method this <code>PlayList</code>
     *      should be repeated.
     */
    public void setRepeatMode(int repeatMode) {
        this.repeatMode = repeatMode;
    }



    /** 
     * Sets the pick-mode for this playlist. 
     * @param pickMode The method to be used for picking
     *      the songs. 
     */
    public void setPickMode(int pickMode) {
        this.pickMode = pickMode;
    }



    /**
    * Returns the next sound file on this Playlist or null if there is no such sound.
    * @return The next sound file on this Playlist or null if there is no such sound.
    */
    public File next() {
        if (num == -2) {
            // Playlist exhausted.
        }
        else if (num == -1) {
            // Let's pick a first sound.
            if (pickMode == FORWARDS) {
                num = 0;
            }
            else if (pickMode == BACKWARDS) {
                num = soundFiles.length - 1;
            }
            else { // SHUFFLE mode
                num = (int)(Math.random() * soundFiles.length);
                playedSounds[0] = num;
            }
        }
        else if (repeatMode == PLAY_ONE) {
            num = -2;
        }
        else if (repeatMode == REPEAT_ONE) {
            // num stays the same
        }
        else if (pickMode == SHUFFLE) {
            if (playedSounds[playedSounds.length - 1] != Integer.MAX_VALUE) {
                if (repeatMode == PLAY_ALL) {
                    num = -2;
                }
                else { // (repeatMode == REPEAT_ALL)
                    for (int i = 1; i < playedSounds.length; i++) {
                        playedSounds[i] = Integer.MAX_VALUE;
                    }
                    num = (int)(Math.random() * soundFiles.length);
                    playedSounds[0] = num;
                }
            }
            else {
                int i = 0;
                for (; i < playedSounds.length; i++) {
                    if (playedSounds[i] == Integer.MAX_VALUE) {
                        break;
                    }
                }

                int tmp = (int)(Math.random() * (soundFiles.length - i));
                for (int j = 0; j < i; j++) {
                    if (tmp < playedSounds[j]) {
                        num = tmp;
                        break;
                    }
                    else {
                        tmp++;
                    }
                }

                playedSounds[i] = num;
                Arrays.sort(playedSounds);
            }
        } else {
            switch (repeatMode) {
            case PLAY_ALL:
                if (pickMode == FORWARDS) {
                    num++;
                    if (num == soundFiles.length) {
                        num = -2;
                    }
                } else { // (pickMode == BACKWARDS)
                    num--;
                    if (num == -1) {
                        num = -2;
                    }
                }
                break;
            case REPEAT_ALL:
                if (pickMode == FORWARDS) {
                    num++;
                    if (num == soundFiles.length) {
                        num = 0;
                    }
                } else { // (pickMode == BACKWARDS)
                    num--;
                    if (num == -1) {
                        num = soundFiles.length - 1;
                    }
                }
                break;
            default:
                break;
            }
        }
        
        if ((num >= 0) && (num < soundFiles.length)) {
            return soundFiles[num];
        } else {
            return null;
        }
    }

    

    /**
    * Returns <i>false</i> if this Playlist is exhausted, <i>true</i> otherwise.
    * @return <i>false</i> if this Playlist is exhausted, <i>true</i> otherwise.
    */
    public boolean hasNext() {
        if (repeatMode == PLAY_ALL) {
            if (num == -1) {
                return true;
            }
            else if (num == -2) {
                return false;
            }
            else {
                if (((pickMode == FORWARDS) && (num == soundFiles.length - 1))
                        || ((pickMode == BACKWARDS) && (num == 0))
                        || ((pickMode == SHUFFLE) && (playedSounds[playedSounds.length - 1] != Integer.MAX_VALUE))) {
                    return false;
                }
                else {
                    return true;
                }
            }
        }
        else if (repeatMode == PLAY_ONE) {
            if (num == -1) {
                return true;
            }
            else {
                return false;
            }
        }
        else {
            return true;
        }
    }
}
