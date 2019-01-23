/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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


    private SoundPlayer soundPlayer;


    /**
     * Prepare the sound system.
     * 
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param sound Enable sound if true.
     */
    public SoundController(FreeColClient freeColClient, boolean sound) {
        final ClientOptions opts = freeColClient.getClientOptions();
        if (sound) {
            this.soundPlayer = null;
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
            try {
                logger.log(Level.INFO, "Create sound controller with "
                    + amo + "/" + vo);
                this.soundPlayer = new SoundPlayer(amo, vo);
            } catch (Exception e) {
                // #3168279 reports an undocumented NPE thrown by
                // AudioSystem.getMixer(null).  Workaround this and other
                // such failures by just disabling sound.
                this.soundPlayer = null;
                logger.log(Level.WARNING, "Sound disabled", e);
            }
        }
    }

    /**
     * Can this client play sounds?
     *
     * @return True if there is a sound player present.
     */
    public boolean canPlaySound() {
        return soundPlayer != null;
    }

    /**
     * Play a sound.
     *
     * @param sound The sound resource to play, or if null stop playing.
     */
    public void playSound(String sound) {
        if (!canPlaySound() || sound == null) return;

        File file = ResourceManager.getAudio(sound);
        if (file == null) {
            logger.finest("Cound not load sound: " + sound);
        } else {
            soundPlayer.stop();
            boolean playing = soundPlayer.playOnce(file);
            logger.finest(((playing) ? "Queued" : "Fail on")
                + " sound: " + sound);
        }
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
        String text = (soundPlayer == null)
            ? Messages.message("nothing")
            : ((mixer = soundPlayer.getMixer()) == null)
                ? Messages.message("none")
                : mixer.getMixerInfo().getName();
        return Messages.message("current") + ":  " + text;
    }
}
