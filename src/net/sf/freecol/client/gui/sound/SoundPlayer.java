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
import java.io.File;
import java.io.IOException;
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
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;


/**
 * Class for playing sound. See the package description for
 * {@link net.sf.freecol.client.gui.sound} for information on how to play
 * sfx/music.
 */
public class SoundPlayer {

    private static final Logger logger = Logger.getLogger(SoundPlayer.class.getName());

    public static final int STANDARD_DELAY = 2000;
    private static final int MAXIMUM_FADE_MS = 7000;
    private static final int FADE_UPDATE_MS = 5;
    
    /** The thread-group containing all of the <i>SoundPlayerThreads</i>. */
    private ThreadGroup soundPlayerThreads
        = new ThreadGroup("soundPlayerThreads");

    /** Is the sound paused? */
    private boolean soundPaused = false;

    /** Is the sound stopped? */
    private boolean soundStopped = true;

    /**
     * Should the <i>SoundPlayer</i> play multiple sounds at the same
     * time, or only one?  If it does not allow multiple sounds, then
     * using <i>play</i> will stop the sound currently playing and play
     * the new instead.
     */
    private boolean multipleSounds;
    
    /**
     * Used with <code>multipleSounds</code>.
     */
    private SoundPlayerThread currentSoundPlayerThread;

    /**
     * Should the player continue playing after it it finished with a
     * sound-clip? This is the default used with the <i>play(Playlist
     * playlist)</i>.
     */
    private boolean defaultPlayContinues;

    /**
     * This is the default repeat-mode for a playlist. Refer to the
     * field summary of the {@link Playlist}-class to get the
     * different values.
     *
     * @see Playlist
     */
    private final int defaultRepeatMode;

    /**
     * This is the default pick-mode for a playlist. Refer to the
     * field summary of the {@link Playlist}-class to get the
     * different values.
     *
     * @see Playlist
     */
    private final int defaultPickMode;

    private Mixer mixer;
    
    private PercentageOption volume;

    
    /**
     * Creates a sound player.
     *
     * @param mixerOption The option for setting the mixer used by this
     *       <code>SoundPlayer</code>.
     * @param volume The volume to be used when playing audio.
     */
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume) {
        this(mixerOption, volume, false, false,
             Playlist.REPEAT_ALL, Playlist.FORWARDS);
    }

    /**
     * Creates a sound player.
     *
     * @param mixerOption The option for setting the mixer used by this
     *     <code>SoundPlayer</code>.
     * @param volume The volume to be used when playing audio.
     * @param multipleSounds Should the <i>SoundPlayer</i> play
     *     multiple sounds at the same time, or only one? If it does
     *     not allow multiple sounds, then using <i>play</i> will stop
     *     the sound currently playing and play the new instead.
     * @param defaultPlayContinues Should the player continue playing
     *     after it it finished with a sound-clip? This is the default
     *     used with the <i>play(Playlist playlist)</i>.
     */
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume,
                       boolean multipleSounds, boolean defaultPlayContinues) {
        this(mixerOption, volume, multipleSounds, defaultPlayContinues,
             Playlist.REPEAT_ALL, Playlist.FORWARDS);
    }

    /**
     * Creates a sound player.
     *
     * @param mixerOption The option for setting the mixer used by this
     *      <code>SoundPlayer</code>.
     * @param volume The volume to be used when playing audio.
     * @param multipleSounds Should the <i>SoundPlayer</i> play multiple
     *      sounds at the same time, or only one? If it does not allow
     *      multiple sounds, then using <i>play</i> will stop the sound
     *      currently playing and play the new instead.
     * @param defaultRepeatMode This is the default repeat-mode for a
     *      playlist. Refer to the field summary of the
     *      {@link Playlist}-class to get the different values.
     * @param defaultPickMode This is the default pick-mode for a playlist.
     *      Refer to the field summary of the {@link Playlist}-class to
     *      get the different values.
     * @param defaultPlayContinues Should the player continue playing after
     *      it it finished with a sound-clip? This is the default used
     *      with the <i>play(Playlist playlist)</i>.
     */
    public SoundPlayer(AudioMixerOption mixerOption, PercentageOption volume,
                       boolean multipleSounds, boolean defaultPlayContinues,
                       int defaultRepeatMode, int defaultPickMode) {
        if (mixerOption == null) {
            throw new NullPointerException();
        }
        if (volume == null) {
            throw new NullPointerException();
        }
        
        this.volume = volume;
        this.multipleSounds = multipleSounds;
        this.defaultPlayContinues = defaultPlayContinues;
        this.defaultRepeatMode = defaultRepeatMode;
        this.defaultPickMode = defaultPickMode;
        
        mixerOption.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                mixer = AudioSystem.getMixer(((MixerWrapper) e.getNewValue()).getMixerInfo());
            }
        });
        mixer = AudioSystem.getMixer(mixerOption.getValue().getMixerInfo());
    }

    /**
     * Plays a playlist using the default play-continues, repeat-mode
     * and pick-mode for this <i>SoundPlayer</i>.
     *
     * @param playlist The <code>Playlist</code> to be played.
     */
    public void play(Playlist playlist) {
        play(playlist, defaultPlayContinues, defaultRepeatMode,
             defaultPickMode, 0);
    }
    
    /**
     * Plays a playlist using the default play-continues, repeat-mode
     * and pick-mode for this <i>SoundPlayer</i>.
     * 
     * @param playlist The <code>Playlist</code> to be played.
     * @param delay A delay before playing the sound (ms).
     */
    public void play(Playlist playlist, int delay) {
        play(playlist, defaultPlayContinues, defaultRepeatMode,
             defaultPickMode, delay);
    }

    /**
     * Plays a file once.
     *
     * @param file The <code>File</code> to be played.
     */
    public void playOnce(File file) {
        play(new Playlist(file), false, defaultRepeatMode, defaultPickMode, 0);
    }

    /**
     * Plays a single random sound from the given playlist.
     *
     * @param playlist The <code>Playlist</code> to be played.
     */
    public void playOnce(Playlist playlist) {
        play(playlist, false, defaultRepeatMode, defaultPickMode, 0);
    }
    
    /**
     * Plays a single random sound from the given playlist.
     * 
     * @param playlist The <code>Playlist</code> to be played.
     * @param delay A delay before playing the sound (ms).
     */
    public void playOnce(Playlist playlist, int delay) {
        play(playlist, false, defaultRepeatMode, defaultPickMode, delay);
    }

    /**
     * Plays a playlist.
     *
     * @param playlist The <code>Playlist</code> to be played.
     * @param playContinues <code>true</code> if the
     *     <code>SoundPlayer</code> should continue playing
     *     after playing the first entry on the playlist.
     * @param repeatMode The method this <code>PlayList</code>
     *     should be repeated.
     * @param pickMode The method to be used for picking
     *     the songs.
     * @param delay A delay before playing the sound (ms).
     */
    public void play(Playlist playlist, boolean playContinues,
                     int repeatMode, int pickMode, int delay) {
        if (playlist != null) {
            currentSoundPlayerThread = new SoundPlayerThread(playlist, playContinues, repeatMode, pickMode, delay);
            currentSoundPlayerThread.start();
        } else {
            currentSoundPlayerThread = null;
        }
    }

    /**
     * Stop playing the sounds.
     */
    public void stop() {
        soundStopped = true;
        soundPaused = false;
    }

    /**
     * Are the sounds stopped?
     *
     * @return <code>true</code> is the sounds are stopped.
     */
    public boolean isStopped() {
        return soundStopped;
    }

    /**
     * Pauses all the sounds.
     */
    public void pause() {
        soundPaused = true;
    }

    /**
     * Are the sounds paused?
     *
     * @return <code>true</code> is the sounds are paused.
     */
    public boolean isPaused() {
        return soundPaused;
    }

    /**
     * Thread for playing a <i>Playlist</i>.
     */
    class SoundPlayerThread extends Thread {

        /**
         * An array containing the currently selected playlist. The
         * numbers in the array is used as an index in the
         * <i>soundFiles</i>-array.
         */
        private Playlist playlist;

        /**
         * Should the <i>SoundPlayer</i> continue to play when it is
         * finished with a sound-clip?
         */
        private boolean playContinues;

        /**
         * This is the default repeat-mode for a playlist. Refer to
         * the field summary of the {@link Playlist}-class to get the
         * different values.
         *
         * @see Playlist
         */
        private int repeatMode;

        /**
         * This is the default pick-mode for a playlist. Refer to the
         * field summary of the {@link Playlist}-class to get the
         * different values.
         *
         * @see Playlist
         */
        private int pickMode;

        /** Should the sound be played again when it is finished? */
        @SuppressWarnings("unused")
        private boolean repeatSound;

        private int delay;

        /**
         * The constructor to use.
         *
         * @param playlist A <i>Playlist</i> containing sound-files.
         * @param playContinues Should the player continue playing
         *     after it it finished with a sound-clip?
         * @param repeatMode This is the default repeat-mode for a
         *     playlist. Refer to the field summary of the
         *     {@link Playlist}-class to get the different values.
         * @param pickMode This is the default pick-mode for a
         *     playlist. Refer to the field summary of the
         *     {@link Playlist}-class to get the different values.
         * @param delay A delay before playing the sound (ms).
         */
        public SoundPlayerThread(Playlist playlist, boolean playContinues,
                                 int repeatMode, int pickMode, int delay) {
            super(soundPlayerThreads, FreeCol.CLIENT_THREAD+"SoundPlayer");

            this.playlist = playlist;
            this.playContinues = playContinues;
            this.repeatMode = repeatMode;
            this.pickMode = pickMode;
            this.delay = delay;
        }

        private boolean shouldStopThread() {
            return !multipleSounds && currentSoundPlayerThread != this; 
        }

        /**
         * This thread loads and plays the sound.
         */
        public void run() {
            playlist.setRepeatMode(repeatMode);
            playlist.setPickMode(pickMode);

            soundPaused = false;
            soundStopped = false;

            if (delay != 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {}
            }
            
            do {
                playSound(playlist.next());

                // Take a little break between sounds
                try { Thread.sleep(222); } catch (Exception e) {break;}
            } while (playContinues && playlist.hasNext()
                    && !soundStopped && !shouldStopThread());
        }

        public void playSound(File file) {
            try {
                AudioInputStream in = AudioSystem.getAudioInputStream(file);
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
                    rawplay(decodedFormat, din);
                    din.close();
                    in.close();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Could not play audio: "
                           + file.getName(), e);
            }
        }

        private void updateVolume(FloatControl c, int volume) {
            // The gain (in decibels) and volume (in percents) are
            // related via logarithm
            // 100% volume = 0dB attenuation
            // 50% volume = -6dB
            // 10% volume = -20dB
            // 1% volume = -40dB
            final float gain = 20*(float)Math.log10(volume / 100d);
            c.setValue(gain);
        }
        
        private void rawplay(AudioFormat targetFormat,  AudioInputStream din)
            throws IOException, LineUnavailableException {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                                                   targetFormat);
            SourceDataLine line = (SourceDataLine) mixer.getLine(info);
            if (line != null) {
                FloatControl control = null;
                PropertyChangeListener pcl = null;

                line.open(targetFormat);
                line.start();
                
                // Volume control:
                if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    control = (FloatControl)
                        line.getControl(FloatControl.Type.MASTER_GAIN);
                    final FloatControl c = control;
                    pcl = new PropertyChangeListener() {
                            public void propertyChange(PropertyChangeEvent e) {
                                int v = ((Integer) e.getNewValue()).intValue();
                                updateVolume(c, v);
                            }
                        };
                    volume.addPropertyChangeListener(pcl);
                    updateVolume(control, volume.getValue());
                }

                // Playing audio:
                byte[] data = new byte[8192];
                int read = 0;
                try {
                    while (!soundStopped && !shouldStopThread()) {
                        try {
                            while (soundPaused) {
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException e) {}
                        read = din.read(data, 0, data.length);
                        if (read < 0) break; else if (read > 0) {
                            line.write(data, 0, read);
                        }
                    }
                } finally {
                    if (pcl != null) volume.removePropertyChangeListener(pcl);
                }
                
                // Implements fading down:
                if (!soundStopped) {
                    long ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                    long fadeStop = System.currentTimeMillis() + MAXIMUM_FADE_MS;
                    while (!soundStopped
                           && System.currentTimeMillis() < fadeStop) {
                        read = din.read(data, 0, data.length);
                        if (read < 0) break; else if (read > 0) {
                            line.write(data, 0, read);
                        }
                        if (System.currentTimeMillis() > ms) {
                            // decrease the gain toward minimum (-80dB) by 1dB
                            float currentGain = control.getValue();
                            float newGain = currentGain - 1f;
                            if (newGain < control.getMinimum())
                                newGain = control.getMinimum();
                            control.setValue(newGain);
                            ms = System.currentTimeMillis() + FADE_UPDATE_MS;
                        }
                    }
                }
                
                line.drain();
                line.stop();
                line.close();
            }
        }
    }
}
