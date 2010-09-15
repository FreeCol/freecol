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

import java.awt.Dimension;
import java.io.File;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.Resource;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.util.test.FreeColTestCase;


public class SoundTest extends FreeColTestCase {
    
    SoundPlayer soundPlayer = null;
    
    private void playSound(String id) {
        File file = ResourceManager.getAudio(id);
        if (file == null) {
            // Can not rely on loading a valid sound resource in the
            // test suite as the requisite ogg-support jars may not be
            // loaded.  However we can insist that the resource was at
            // least registered.
            assertTrue("Resource " + id + " should be present",
                       ResourceManager.hasResource(id));
        } else {
            soundPlayer.playOnce(file);
            try {
                // just play the beginning of the sound, just to check it works
                Thread.sleep(300);
            } catch (InterruptedException e) {
            }
        }
    }
    
    public void testSound() {
        File baseDirectory = new File(FreeCol.getDataDirectory(), "base");
        FreeColDataFile baseData = new FreeColDataFile(baseDirectory);
        ResourceManager.setBaseMapping(baseData.getResourceMapping());
        ResourceManager.preload(new Dimension(1,1));

        ClientOptions clientOptions = new ClientOptions();
        final AudioMixerOption amo = (AudioMixerOption) clientOptions.getOption(ClientOptions.AUDIO_MIXER);
        final PercentageOption po = (PercentageOption) clientOptions.getOption(ClientOptions.AUDIO_VOLUME);
        po.setValue(10); // 10% volume
        soundPlayer = new SoundPlayer(amo, po);

        // these sounds are base resources, and should be enough for a test
        playSound("sound.intro.general");
        playSound("sound.event.illegalMove");
        // other sounds require loading a rule set
   }
}
