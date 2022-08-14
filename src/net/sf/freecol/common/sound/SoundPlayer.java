/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import static net.sf.freecol.common.util.Utils.delay;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.sound.sampled.UnsupportedAudioFileException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.AudioMixerOption.MixerWrapper;
import net.sf.freecol.common.option.PercentageOption;


/**
 * Stripped down class for playing sound.
 *
 * The sound player depends on getting a usable value for its mixer, which
 * does not necessarily happen.  It has been hacked to just refuse to play
 * anything in this case, in the hope that a valid mixer will eventually be
 * specified.
 */
public final class SoundPlayer {

    private static final Logger logger = Logger.getLogger(SoundPlayer.class.getName());

    /**
     * Thread for playing sound files.
     */
    private class SoundPlayerThread extends Thread {

        /** How long to sleep when idle. */
        private static final int WAIT_TIMEOUT = 100; // 100ms

        /** A buffer to hold data to be written to the mixer. */
        private final byte[] data = new byte[16384];

        /** A playlist of files queued to be played. */
        private final List<File> playList = new ArrayList<>();

        /**
         * Flag to allow a sound that is being played to be cancelled.
         * Volatile to allow asynchronous update from SoundPlayer.
         */
        private volatile boolean playDone = true;


        /**
         * Create a new sound player thread.
         */
        public SoundPlayerThread() {
            super(FreeCol.CLIENT_THREAD + "SoundPlayer");
        }

        /**
         * Signal that a sound that is being played should stop.
         */
        public void stopPlaying() {
            this.playDone = true;
            synchronized (this.playList) {
                this.playList.clear();
            }
        }

        /**
         * Queue a sound to be played.
         *
         * @param sound The new sound {@code File}.
         */
        public void add(File sound) {
            synchronized (this.playList) {
                this.playList.add(sound);
            }
        }

        /**
         * Dequeue the next file to be played.
         *
         * @return A {@code File} to be played, or null if none present.
         */
        private File remove() {
            synchronized (this.playList) {
                return (this.playList.isEmpty()) ? null
                    : this.playList.remove(0);
            }
        }

        /**
         * Play a sound.
         *
         * @param sound The {@code File} to play.
         * @return True if the sound was played without incident.
         * @exception IOException if unable to read or write the sound data.
         */
        private boolean playSound(File sound) throws IOException {
            setVolume(volumeOption.getValue());
            
            boolean ret = false;
            PropertyChangeListener volumeListener = null;
            try (AudioInputStream in = getAudioInputStream(sound)) {
                final SourceDataLine line = openLine(in.getFormat(), getMixer(), data.length);
                if (line == null) {
                    return false;
                }
                
                volumeListener = (PropertyChangeEvent e) -> {
                    setVolume((Integer) e.getNewValue());
                    changeVolume(line, getVolume());
                };
                volumeOption.addPropertyChangeListener(volumeListener);
                changeVolume(line, getVolume());
                try {
                    this.playDone = false;
                    int rd;
                    while (!this.playDone && (rd = in.read(data)) > 0) {
                        line.write(data, 0, rd);
                    }
                    ret = true;
                    //logger.finest("Played " + sound);
                } finally {
                    this.playDone = true;
                    line.drain();
                    line.stop();
                    line.close();
                }
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Can not open "
                    + sound + " as audio stream", ioe);
                return false;
            } finally {
                if (volumeListener != null) {
                    volumeOption.removePropertyChangeListener(volumeListener);
                }
            }
            return ret;
        }

        // Override Thread

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            for (;;) {
                File sound = remove();
                if (sound == null) {
                    if (!defaultPlayList.isEmpty()) {
                        synchronized (this.playList) {
                            this.playList.addAll(defaultPlayList);
                            Collections.shuffle(this.playList);
                        }
                        sound = remove();
                    } else {
                        delay(WAIT_TIMEOUT, null);
                    }
                }
                if (sound != null) {
                    try {
                        playSound(sound);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Failure playing audio.", e);
                    }
                }
            }
        }
    }


    /** The mixer to write to. */
    private Mixer mixer;

    /** The current volume. */
    private int volume;

    /** The subthread that actually writes sound data to the mixer. */
    private final SoundPlayerThread soundPlayerThread;
    
    private List<File> defaultPlayList = new ArrayList<>();
    
    private PercentageOption volumeOption;


    /**
     * Creates a sound player.
     *
     * @param mixerOption The option for setting the mixer to use.
     * @param volumeOption The volume option to use when playing audio.
     */
    public SoundPlayer(AudioMixerOption mixerOption,
                       PercentageOption volumeOption) {
        setMixer(mixerOption.getValue());
        mixerOption.addPropertyChangeListener((PropertyChangeEvent e) -> {
                setMixer((MixerWrapper)e.getNewValue());
            });
        setVolume(volumeOption.getValue());

        this.volumeOption = volumeOption;
        this.soundPlayerThread = new SoundPlayerThread();
        
        /*
         * A high thread priority is necessary in order to avoid stuttering on
         * slow systems.
         */
        soundPlayerThread.setPriority(Thread.MAX_PRIORITY);
        this.soundPlayerThread.start();
    }

    /**
     * Gets the mixer.
     *
     * @return The current mixer.
     */
    public Mixer getMixer () {
        return this.mixer;
    }

    /**
     * Sets the mixer.
     *
     * Note: This can throw SecurityException and IllegalArgumentException
     * in various circumstances.  We used to catch this in the constructor
     * above, but now we let it go through to the caller.
     *
     * @param mw The new mixer.
     */
    private void setMixer(MixerWrapper mw) {
        try {
            this.mixer = AudioSystem.getMixer(mw.getMixerInfo());
            logger.info("Mixer " + mw + " selected");
        } catch (Exception e) {
            this.mixer = null;
            logger.log(Level.WARNING, "Bad mixer: " + mw
                + ", sound suspended", e);
        }
    }

    /**
     * Gets the volume.
     *
     * @return The current volume.
     */
    public int getVolume() {
        return this.volume;
    }

    /**
     * Set the volume.
     *
     * @param volume The new volume.
     */
    private void setVolume(int volume) {
        this.volume = volume;
    }

    /**
     * Plays a file once.
     *
     * @param file The {@code File} to be played.
     * @return True if the file was queued to play.
     */
    public boolean playOnce(File file) {
        if (getMixer() == null) return false; // Fail faster.
        this.soundPlayerThread.add(file);
        return true;
    }
    
    /**
     * Defines the default playlist.
     *
     * @param file The {@code File}s to be played when
     *      nothing else is being played.
     */
    public void setDefaultPlaylist(File... files) {
        final List<File> newDefaultPlayList = new ArrayList<>(List.of(files));
        this.defaultPlayList = newDefaultPlayList;
    }

    /**
     * Stop any playing sound.
     */
    public void stop() {
        this.soundPlayerThread.stopPlaying();
    }


    // Audio manipulation utilities

    /**
     * Gets an audio input stream given a file, hopefully containing
     * audio data.
     *
     * Public so the file can be tested at load time for suitability for
     * use as audio.
     *
     * @param file The {@code File} to examine.
     * @return An {@code AudioInputStream}, or null on failure.
     * @exception IOException if the file does not contain valid audio.
     */
    public static AudioInputStream getAudioInputStream(File file)
        throws IOException {
        AudioInputStream in;
        if (file.getName().endsWith(".ogg")) {
            // We used to use tritonus to provide ogg (strictly,
            // Vorbis-audio-in-ogg-container) decoding to the Java
            // sound system.  It was buggy and appears to be
            // unmaintained since 2009.  So now for ogg we have our
            // own jorbis-based decoder.
            in = new OggVorbisDecoderFactory().getOggStream(file);
        } else {
            try {
                in = AudioSystem.getAudioInputStream(file);
            } catch (UnsupportedAudioFileException uafe) {
                // Hide the UAFE, upstream only cares that failure occurred
                throw new IOException(uafe);
            }
        }
        return in;
    }

    /**
     * Change the volume on a line.
     *
     * @param line The {@code SourceDataLine} to change the volume on.
     * @param vol The new volume.
     */
    private static void changeVolume(SourceDataLine line, int vol) {
        FloatControl.Type type
            = (line.isControlSupported(FloatControl.Type.VOLUME))
            ? FloatControl.Type.VOLUME
            : (line.isControlSupported(FloatControl.Type.MASTER_GAIN))
            ? FloatControl.Type.MASTER_GAIN
            : null;
        if (type == null) {
            logger.warning("No volume or master gain controls.");
            return;
        }
        FloatControl control;
        try {
            control = (FloatControl)line.getControl(type);
        } catch (IllegalArgumentException e) {
            return; // Should not happen
        }
        //
        // The units of MASTER_GAIN seem to consistently be dB, but
        // in the case of VOLUME this is unclear (there is even a query
        // to that effect in the source).  getUnits() says "pulseaudio
        // units" on my boxen, and the PulseAudio doco talks about dB
        // so for now we are assuming that the controls we are using
        // are both logarithmic:
        //
        //   gain = A.log_10(k.vol)
        // So scale vol <= 1 to gain_min and vol >= 100 to gain_max
        //   gain_min = A.log_10(k.1)
        //   gain_max = A.log_10(k.100)
        // Solving for A,k yields:
        //   A = (gain_max - gain_min)/2
        //   k = 10^(gain_min/A)
        // =>
        //   gain = gain_min + (gain_max - gain_min)/2 * log_10(vol)
        //
        float min = control.getMinimum();
        float max = control.getMaximum();
        float gain = (vol <= 0) ? min
            : (vol >= 100) ? max
            : min + 0.5f * (max - min) * (float)Math.log10(vol);
        try {
            control.setValue(gain);
            logger.finest("Using volume " + vol + "%, "
                + control.getUnits() + "=" + gain
                + " control=" + type);
        } catch (IllegalArgumentException ie) {
            logger.log(Level.WARNING, "Could not set volume "
                + " (control=" + type + " in [" + min + "," + max + "])"
                + " to " + gain + control.getUnits(), ie);
        }
    }

    /**
     * Open a line to the mixer for a given format.
     *
     * @param audioFormat The {@code AudioFormat} to write.
     * @param mixer The {@code Mixer} to write to.
     * @param len The size of buffer to expect.
     * @return The newly opened {@code SourceDataLine} or null on error.
     */
    private static SourceDataLine openLine(AudioFormat audioFormat,
                                           Mixer mixer, int len) {
        DataLine.Info info = new DataLine.Info(SourceDataLine.class,
            audioFormat);
        if (mixer == null) {
            logger.log(Level.WARNING, "Null mixer");
            return null;
        }
        if (!mixer.isLineSupported(info)) {
            logger.log(Level.WARNING, "Mixer does not support " + info);
            return null;
        }
        SourceDataLine line = null;
        try {
            line = (SourceDataLine)mixer.getLine(info);
            line.open(audioFormat, len);
        } catch (LineUnavailableException|IllegalArgumentException|SecurityException e) {
            logger.log(Level.WARNING, "Can not open SourceDataLine", e);
            return null;
        }
        line.start();
        return line;
    }
}
