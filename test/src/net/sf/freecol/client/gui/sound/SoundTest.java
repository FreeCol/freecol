/**
 *  Copyright (C) 2002-2012  The FreeCol Team
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
import javax.sound.sampled.AudioSystem;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.util.test.FreeColTestCase;


public class SoundTest extends FreeColTestCase {

    private SoundPlayer soundPlayer = null;

    @Override
    public void setUp() {
        ClientOptions clientOptions = new ClientOptions();
        final AudioMixerOption amo = (AudioMixerOption) clientOptions.getOption(ClientOptions.AUDIO_MIXER);
        final PercentageOption po = (PercentageOption) clientOptions.getOption(ClientOptions.AUDIO_VOLUME);
        po.setValue(10); // 10% volume
        try {
            soundPlayer = new SoundPlayer(amo, po);
        } catch (Exception e) {
            fail("Could not construct sound player: " + e.getMessage());
        }
    }

    @Override
    public void tearDown() {
        soundPlayer = null;
    }

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
            try {
                assertNotNull(AudioSystem.getAudioInputStream(file));
                soundPlayer.playOnce(file);
                try { // Just play the beginning of the sound to check it works
                    Thread.sleep(100);
                    soundPlayer.stop();
                    Thread.sleep(50);
                } catch (InterruptedException e) {}
            } catch (Exception e) {
                fail("Could not play " + id + ": " + e.getMessage());
            }
        }
    }

    public void testSound() {
        File baseDirectory = new File(FreeCol.getDataDirectory(), "base");
        FreeColDataFile baseData = new FreeColDataFile(baseDirectory);
        ResourceManager.setBaseMapping(baseData.getResourceMapping());
        ResourceManager.preload(new Dimension(1,1));

        // these sounds are base resources, and should be enough for a test
        playSound("sound.intro.general");
        playSound("sound.event.illegalMove");
        // other sounds require loading a rule set
    }

    public void testClassic() {
        File baseDirectory = new File(FreeCol.getDataDirectory(), "rules/classic");
        FreeColDataFile baseData = new FreeColDataFile(baseDirectory);
        ResourceManager.setBaseMapping(baseData.getResourceMapping());
        ResourceManager.preload(new Dimension(1,1));

        playSound("sound.intro.model.nation.english");
        playSound("sound.intro.model.nation.dutch");
        playSound("sound.intro.model.nation.french");
        playSound("sound.intro.model.nation.spanish");
        playSound("sound.anthem.model.nation.dutch");
        playSound("sound.anthem.model.nation.english");
        playSound("sound.anthem.model.nation.french");
        playSound("sound.anthem.model.nation.spanish");
        playSound("sound.attack.artillery");
        playSound("sound.attack.mounted");
        playSound("sound.attack.naval");
        playSound("sound.event.meet.model.nation.aztec");
        playSound("sound.event.buildingComplete");
        playSound("sound.event.captureColony");
        playSound("sound.event.fountainOfYouth");
        playSound("sound.event.loadCargo");
        playSound("sound.event.missionEstablished");
        playSound("sound.event.sellCargo");
        playSound("sound.event.shipSunk");
    }
}
