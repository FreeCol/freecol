/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

package net.sf.freecol.client.control;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.Mixer;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.sound.SoundPlayer;

/**
 * Controls the SoundPlayer.
 */
public class SoundController {

    private static final Logger logger = Logger.getLogger(SoundController.class.getName());

    /** The internal sound player. */
    private SoundPlayer soundPlayer;


    /**
     * Prepare the sound system.
     * 
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param sound Enable sound if true.
     */
    public SoundController(FreeColClient freeColClient, boolean sound) {
        final ClientOptions opts = freeColClient.getClientOptions();
        this.soundPlayer = null;
        if (sound) {
            AudioMixerOption amo = null;
            try {
                amo = opts.getOption(ClientOptions.AUDIO_MIXER,
                                     AudioMixerOption.class);
            } catch (Exception ex) {
                logger.warning(ex.getMessage());
            }
            PercentageOption vo = null;
            try {
                vo = opts.getOption(ClientOptions.AUDIO_VOLUME,
                                    PercentageOption.class);
            } catch (Exception ex) {
                logger.warning(ex.getMessage());
            }
            if (amo == null || vo == null) return;
            // Unless totally disabled, the sound player is always
            // created, but if it has a bad mixer sound output will be
            // suspended.  The hope is that the user will change the
            // mixer option to one that works.
            logger.info("Create sound player with " + amo + "/" + vo);
            this.soundPlayer = new SoundPlayer(amo, vo);
        }
    }

    /**
     * Can this client play sounds?
     *
     * @return True if there is a sound player with a valid mixer present.
     */
    public boolean canPlaySound() {
        return this.soundPlayer != null && this.soundPlayer.getMixer() != null;
    }

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        if (!canPlaySound()) return;
        this.soundPlayer.stop(); // Always stop, including sound==null
        if (sound == null) return;
        File file = ResourceManager.getAudio(sound);
        if (file == null) return;
        boolean playing = soundPlayer.playOnce(file);
        logger.finest(((playing) ? "Queued" : "Fail on")
            + " sound: " + sound);
    }

    /**
     * Get the label text for the sound player mixer.
     *
     * Needed by the audio mixer option UI.
     *
     * @return The text.
     */
    public String getSoundMixerLabelText() {
        Mixer mixer;
        String text = (this.soundPlayer == null)
            ? Messages.message("nothing")
            : ((mixer = this.soundPlayer.getMixer()) == null)
                ? Messages.message("none")
                : mixer.getMixerInfo().getName();
        return Messages.message("current") + ":  " + text;
    }
}
