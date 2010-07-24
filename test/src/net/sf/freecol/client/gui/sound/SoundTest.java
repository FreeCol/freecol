/**
 *  Copyright (C) 2002-2008  The FreeCol Team
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

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.util.test.FreeColTestCase;

public class SoundTest extends FreeColTestCase {
    
    MusicLibrary musicLibrary = null;
    SfxLibrary sfxLibrary = null;
    SoundPlayer soundPlayer = null;
    
    private void playSound(SoundLibrary library, String id) {
        Playlist playlist = library.get(id);
        assertNotNull(playlist);
        soundPlayer.playOnce(playlist);
        try {
            // just play the beginning of the sound, just to check it works
            Thread.sleep(300);
        } catch (InterruptedException e) {
        }
    }
    
    public void testSound() {
        
        ClientOptions clientOptions = new ClientOptions(spec());
        final AudioMixerOption amo = (AudioMixerOption) clientOptions.getObject(ClientOptions.AUDIO_MIXER);
        final PercentageOption po = (PercentageOption) clientOptions.getObject(ClientOptions.MUSIC_VOLUME);
        po.setValue(10); // 10% volume

        File dir = new File(FreeCol.getDataDirectory(), "audio");
        try {
            musicLibrary = new MusicLibrary(dir);
        } catch (FreeColException e) {
            System.out.println("The music files could not be found.");
            fail();
        }
        try {
            sfxLibrary = new SfxLibrary(dir);
        } catch (FreeColException e) {
            System.out.println("The music files could not be found.");
            fail();
        }
        soundPlayer = new SoundPlayer(amo, po, false, true);
        playSound(musicLibrary, "aztec");
        playSound(musicLibrary, "england");
        playSound(musicLibrary, "fountain");
        playSound(musicLibrary, "intro");
        
        playSound(sfxLibrary, "attack_artillery");
        playSound(sfxLibrary, "attack_dragoon");
        playSound(sfxLibrary, "attack_naval");
        playSound(sfxLibrary, "building_complete");
        playSound(sfxLibrary, "captured_by_artillery");
        playSound(sfxLibrary, "illegal_move");
        playSound(sfxLibrary, "load_cargo");
        playSound(sfxLibrary, "sell_cargo");
        playSound(sfxLibrary, "sunk");
        playSound(sfxLibrary, "anthem_dutch");
        playSound(sfxLibrary, "anthem_english");
        playSound(sfxLibrary, "anthem_french");
        playSound(sfxLibrary, "anthem_spanish");
    }

}
