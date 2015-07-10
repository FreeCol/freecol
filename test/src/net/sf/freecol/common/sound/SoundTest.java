/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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
import java.io.IOException;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColTcFile;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.util.test.FreeColTestCase;


public class SoundTest extends FreeColTestCase {

    private SoundPlayer soundPlayer = null;

    @Override
    public void setUp() {
        ClientOptions clientOptions = new ClientOptions();
        clientOptions.load(FreeColDirectories.getBaseClientOptionsFile());
        final AudioMixerOption amo = (AudioMixerOption) clientOptions.getOption(ClientOptions.AUDIO_MIXER);
        final PercentageOption po = (PercentageOption) clientOptions.getOption(ClientOptions.AUDIO_VOLUME);
        po.setValue(10); // 10% volume
        try {
            soundPlayer = new SoundPlayer(amo, po);
        } catch (Exception e) {
            fail("Could not construct sound player: " + e.getMessage());
        }
        File baseDirectory = FreeColDirectories.getBaseDirectory();
        FreeColDataFile baseData = null;
        try {
            baseData = new FreeColDataFile(baseDirectory);
        } catch (Exception e) {
            fail("Could not load base data: " + e.getMessage());
        }
        ResourceManager.setBaseMapping(baseData.getResourceMapping());
    }

    @Override
    public void tearDown() {
        soundPlayer = null;
    }

    private void playSound(String id) {
        File file = ResourceManager.getAudio(id);
        assertNotNull("No sound resource: " + id, file);
        try {
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

    public void testSound() {
        // these sounds are base resources, and should be enough for a test
        playSound("sound.intro.general");
        // other sounds require loading a rule set
    }

    public void testClassic() {
        try {
            FreeColTcFile tcData = new FreeColTcFile("classic");
            ResourceManager.setTcMapping(tcData.getResourceMapping());
        } catch (IOException e) {
            fail("Could not load classic ruleset.");
        }

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
        playSound("sound.event.illegalMove");
        playSound("sound.event.buildingComplete");
        playSound("sound.event.captureColony");
        playSound("sound.event.fountainOfYouth");
        playSound("sound.event.loadCargo");
        playSound("sound.event.missionEstablished");
        playSound("sound.event.sellCargo");
        playSound("sound.event.shipSunk");
    }
}
