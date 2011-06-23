/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;
import net.sf.freecol.common.option.PercentageOption;


/**
 * Stripped down class for playing sound.
 */
public class SoundPlayer {

    private static Logger logger = Logger.getLogger(SoundPlayer.class.getName());

    private SoundPlayerThread soundPlayerThread;

    private Mixer mixer;

    private PercentageOption volume;


    /**
     * Creates a sound player.
     *
     * @param mixerOption The option for setting the mixer to use.
     * @param volume The volume option to use when playing audio.
     */
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume) {
        this.volume = volume;

        mixerOption.addPropertyChangeListener(new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent e) {
                    mixer = AudioSystem.getMixer(((MixerWrapper) e.getNewValue())
                                                .getMixerInfo());
                }
            });
        mixer = AudioSystem.getMixer(mixerOption.getValue().getMixerInfo());
        soundPlayerThread = new SoundPlayerThread();
    }

    public Mixer getCurrentMixer () {
        return mixer;
    }

    /**
     * Plays a file once.
     *
     * @param file The <code>File</code> to be played.
     */
    public void playOnce(File file) {
        soundPlayerThread.add(file);
        soundPlayerThread.stopPlaying();
    }

    /**
     * Stops the current sound.
     */
    public void stop() {
        soundPlayerThread.stopPlaying();
    }

    /**
     * Thread for playing sound files.
     */
    private class SoundPlayerThread extends Thread {

        private final List<File> playList = new ArrayList<File>();

        private boolean playDone = true;

        public SoundPlayerThread() {
            super(FreeCol.CLIENT_THREAD + "SoundPlayer");
        }

        private synchronized boolean keepPlaying() {
            return !playDone;
        }

        public synchronized void startPlaying() {
            playDone = false;
        }

        public synchronized void stopPlaying() {
            playDone = true;
        }

        public synchronized void add(File file) {
            playList.add(file);
        }

        public void run() {
            for (;;) {
                if (playList.isEmpty()) {
                    sleep(250);
                } else {
                    playSound(playList.remove(0));
                }
            }
        }

        private void sleep(int t) {
            try { Thread.sleep(t); } catch (InterruptedException e) {}
        }

        private void playSound(File file) {
            try {
                BufferedInputStream bis = new BufferedInputStream( new FileInputStream(file) );
                bis.mark(1000); bis.skip(1); bis.reset();
                AudioInputStream in = AudioSystem.getAudioInputStream(bis);
                if (in != null) {
                    AudioFormat baseFormat = in.getFormat();
                    AudioFormat decodedFormat = new AudioFormat(
                            AudioFormat.Encoding.PCM_SIGNED,
                            baseFormat.getSampleRate(),
                            16,
                            baseFormat.getChannels(),
                            baseFormat.getChannels() * (16 / 8),
                            baseFormat.getSampleRate(),
                            baseFormat.isBigEndian());
                    AudioInputStream din
                        = AudioSystem.getAudioInputStream(decodedFormat, in);
                    startPlaying();
                    rawplay(decodedFormat, din);
                    din.close();
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not play audio file: "
                           + file.getName(), e);
            }
        }

        private void setVolume(SourceDataLine line, int vol) {
            try {
                FloatControl control = (FloatControl) line
                    .getControl(FloatControl.Type.MASTER_GAIN);
                if (control != null) {
                    // The gain (dB) and volume (percent) are log related.
                    //   100% volume = 0dB attenuation
                    //   50% volume  = -6dB
                    //   10% volume  = -20dB
                    //   1% volume   = -40dB
                    float gain = 20 * (float)Math.log10(vol / 100);
                    control.setValue(gain);
                }
            } catch (Exception e) {}
        }

        private void rawplay(AudioFormat targetFormat, AudioInputStream din)
            throws IOException, LineUnavailableException {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                                   targetFormat);
            // Open the line
            SourceDataLine line;
            try {
                line = (SourceDataLine) mixer.getLine(info);
            } catch (IllegalArgumentException e) {
                throw new LineUnavailableException(e.toString());
            }
            if (line == null) return;
            line.open(targetFormat);
            line.start();

            // Read and write audio data.
            byte[] data = new byte[8192];
            while (keepPlaying()) {
                int read = din.read(data, 0, data.length);
                if (read < 0) {
                    break;
                } else if (read > 0) {
                    line.write(data, 0, read);
                } else {
                    sleep(50);
                }
            }

            line.drain();
            line.stop();
            line.close();
        }
    }
}
