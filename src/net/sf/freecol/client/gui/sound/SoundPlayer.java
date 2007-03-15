
package net.sf.freecol.client.gui.sound;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;


/**
* Class for playing sound. See the package description for {@link net.sf.freecol.client.gui.sound} for information on how to play sfx/music.
*/
public class SoundPlayer {

    private static final Logger logger = Logger.getLogger(SoundPlayer.class.getName());
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The thread-group containing all of the <i>SoundPlayerThreads</i>. */
    private ThreadGroup soundPlayerThreads = new ThreadGroup("soundPlayerThreads");

    /** Is the sound paused? */
    private boolean soundPaused = false;

    /** Is the sound stopped? */
    private boolean soundStopped = true;

    /** Is MIDI enabled? */
    private boolean isMIDIEnabled = false;

    /**
    * Should the <i>SoundPlayer</i> play multiple sounds at the same time, or only one?
    * If it does not allow multiple sounds, then using <i>play</i> will stop the sound
    * currently playing and play the new instead.
    */
    @SuppressWarnings("unused")
    private boolean multipleSounds;

    /** Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>. */
    private boolean defaultPlayContinues;

    /**
    * This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    *
    * @see Playlist
    */
    private final int defaultRepeatMode;

    /**
    * This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    *
    * @see Playlist
    */
    private final int defaultPickMode;


    /**
    * Use this constructor.
    *
    * @param multipleSounds Should the <i>SoundPlayer</i> play multiple sounds at the same time,
    *                       or only one? If it does not allow multiple sounds, then using <i>play</i> will
    *                       stop the sound currently playing and play the new instead.
    *
    * @param isMIDIEnabled Should MIDI be enabled?
    * @param defaultPlayContinues Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>.
    *
    */
    public SoundPlayer(boolean multipleSounds, boolean isMIDIEnabled, boolean defaultPlayContinues) {
        this(multipleSounds, isMIDIEnabled, defaultPlayContinues, Playlist.REPEAT_ALL, Playlist.FORWARDS);
    }



    /**
    * Or this.
    *
    * @param multipleSounds Should the <i>SoundPlayer</i> play multiple sounds at the same time,
    *                       or only one? If it does not allow multiple sounds, then using <i>play</i> will
    *                       stop the sound currently playing and play the new instead.
    *
    * @param isMIDIEnabled Should MIDI be enabled?
    * @param defaultRepeatMode This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    * @param defaultPickMode This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
    * @param defaultPlayContinues Should the player continue playing after it it finished with a sound-clip? This is the default used with the <i>play(Playlist playlist)</i>.
    *
    */
    public SoundPlayer(boolean multipleSounds, boolean isMIDIEnabled, boolean defaultPlayContinues, int defaultRepeatMode, int defaultPickMode) {
        this.multipleSounds = multipleSounds;
        this.isMIDIEnabled = isMIDIEnabled;
        this.defaultPlayContinues = defaultPlayContinues;
        this.defaultRepeatMode = defaultRepeatMode;
        this.defaultPickMode = defaultPickMode;
    }



    /**
    * Plays a playlist using the default play-continues, repeat-mode and pick-mode for this <i>SoundPlayer</i>.
    * @param playlist The <code>Playlist</code> to be played.
    */
    public void play(Playlist playlist) {
        play(playlist, defaultPlayContinues, defaultRepeatMode, defaultPickMode);
    }



    /**
    * Plays a playlist.
    * @param playlist The <code>Playlist</code> to be played.
    * @param playContinues <code>true</code> if the
    *       <code>SoundPlayer</code> should continue playing
    *       after playing the first entry on the playlist.
    * @param repeatMode The method this <code>PlayList</code>
    *      should be repeated.
    * @param pickMode The method to be used for picking
    *      the songs.
    */
    public void play(Playlist playlist, boolean playContinues, int repeatMode, int pickMode) {
        if (playlist != null) {
            SoundPlayerThread soundPlayerThread = new SoundPlayerThread(playlist, playContinues, repeatMode, pickMode);
            soundPlayerThread.start();
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
    * @return <code>true</code> is the sounds are paused.
    */
    public boolean isPaused() {
        return soundPaused;
    }




    /** Thread for playing a <i>Playlist</i>. */
    class SoundPlayerThread extends Thread implements LineListener, MetaEventListener {

        /** An array containing the currently selected playlist. The numbers in the array is used as an index in the <i>soundFiles</i>-array. */
        private Playlist playlist;

        /** Should the <i>SoundPlayer</i> continue to play when it is finished with a sound-clip? */
        private boolean playContinues;

        /**
        * This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        *
        * @see Playlist
        */
        private int repeatMode;

        /**
        * This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        *
        * @see Playlist
        */
        private int pickMode;

        /** Should the sound be played again when it is finished? */
        @SuppressWarnings("unused")
        private boolean repeatSound;

        /** The sound that is prepared for/is playing. A <i>Sequence</i>, <i>BufferedInputStream</i> or a <i>Clip</i>. */
        private Object currentSound;

        /** True if MIDI is finished playing. */
        private boolean midiEOM;

        /** True if (not MIDI) audio is finished playing. */
        @SuppressWarnings("unused")
        private boolean audioEOM;

        /** The <i>Sequencer</i> to use while playing MIDI. */
        private Sequencer sequencer;

        private Synthesizer synthesizer;

        /** An array of <i>MidiChannel</i> to use while playing MIDI. */
        @SuppressWarnings("unused")
        private MidiChannel[] channels;




        /**
        * The constructor to use.
        *
        * @param playlist A <i>Playlist</i> containing sound-files.
        * @param playContinues Should the player continue playing after it it finished with a sound-clip?
        * @param repeatMode This is the default repeat-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        * @param pickMode This is the default pick-mode for a playlist. Refer to the field summary of the {@link Playlist}-class to get the different values.
        */
        public SoundPlayerThread(Playlist playlist, boolean playContinues, int repeatMode, int pickMode) {
            super(soundPlayerThreads, "soundPlayerThread");

            this.playlist = playlist;
            this.playContinues = playContinues;
            this.repeatMode = repeatMode;
            this.pickMode = pickMode;

            if (isMIDIEnabled)
              enableMIDI();
        }



        /**
        * This thread loads and plays the sound.
        */
        public void run() {
            playlist.setRepeatMode(repeatMode);
            playlist.setPickMode(pickMode);

            soundPaused = false;
            soundStopped = false;

            do {
                if(loadSound(playlist.next()))
                    playSound();

                // Take a little break between sounds
                try { Thread.sleep(222); } catch (Exception e) {break;}
            } while (playContinues && playlist.hasNext() && !soundStopped);
        }



        /**
        * Loads a sound into <i>currentSound</i>.
        * @param file A sound-file.
        */
        private boolean loadSound(File file) {
            try {
                currentSound = AudioSystem.getAudioInputStream(file);
            } catch(Exception e1) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    currentSound = new BufferedInputStream(fileInputStream, 1024);
                } catch (Exception e3) {
                    logger.warning("Error while loading audio file: " + e3.getMessage());
                    currentSound = null;
                    return false;
                }
            }

            if (currentSound instanceof AudioInputStream) {
            try {
                    AudioInputStream stream = (AudioInputStream) currentSound;
                    AudioFormat format = stream.getFormat();

                    if ((format.getEncoding() == AudioFormat.Encoding.ULAW) ||
                        (format.getEncoding() == AudioFormat.Encoding.ALAW))
                    {
                        AudioFormat tmp = new AudioFormat(
                                                AudioFormat.Encoding.PCM_SIGNED,
                                                format.getSampleRate(),
                                                format.getSampleSizeInBits() * 2,
                                                format.getChannels(),
                                                format.getFrameSize() * 2,
                                                format.getFrameRate(),
                                                true);
                        stream = AudioSystem.getAudioInputStream(tmp, stream);
                        format = tmp;
                    }
                    DataLine.Info info = new DataLine.Info(
                                            Clip.class,
                                            stream.getFormat(),
                                            ((int) stream.getFrameLength() *
                                                format.getFrameSize()));

                    Clip clip = (Clip) AudioSystem.getLine(info);
                    clip.addLineListener(this);
                    clip.open(stream);
                    currentSound = clip;
                } catch (Exception ex) {
                    logger.warning("Error while reading audio-stream: " + ex.getMessage());
                    currentSound = null;
                    return false;
                }
            } else if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream) {
                if (isMIDIEnabled) {
                    try {
                        sequencer.open();
                        if (currentSound instanceof Sequence) {
                            sequencer.setSequence((Sequence) currentSound);
                        } else {
                            sequencer.setSequence((BufferedInputStream) currentSound);
                        }

                    } catch (InvalidMidiDataException imde) {
                        logger.warning("Unsupported audio file: " + imde.getMessage());
                        currentSound = null;
                        return false;
                    } catch (Exception ex) {
                        logger.warning("Error while loading MIDI-file: " + ex.getMessage());
                        currentSound = null;
                        return false;
                    }
                } else {
                    logger.info("Could not load MIDI-file, because it has been disabled.");
                    currentSound = null;
                    return false;
                }
            }
            
            return true;
        }



        /**
        * Play the sound in <code>currentSound</code>.
        * @see #loadSound
        */
        private void playSound() {
            midiEOM = audioEOM = false;
            if (currentSound instanceof Sequence || currentSound instanceof BufferedInputStream && !soundStopped) { //isActive()
                sequencer.start();

                while (!midiEOM && !soundStopped) {
                    try { Thread.sleep(99); } catch (Exception e) {break;}
                }

                sequencer.stop();
                sequencer.close();
            } else if (currentSound instanceof Clip && !soundStopped) {
                Clip clip = (Clip) currentSound;
                clip.start();

                try { Thread.sleep(99); } catch (Exception e) { }

                // Just sleep while the clip is playing, but check if the sound have been stopped every second.
                while ((soundPaused || clip.isActive()) && !soundStopped) {
                    try { Thread.sleep(1000); } catch (Exception e) {break;}
                }

                clip.stop();
                clip.close();
            }
            currentSound = null;
        }



        /** This method enables MIDI, by opening a <i>sequencer</i>. */
        public void enableMIDI() {

            try {
                sequencer = MidiSystem.getSequencer();

                if (sequencer instanceof Synthesizer) {
                    synthesizer = (Synthesizer)sequencer;
                    channels = synthesizer.getChannels();
                }

            } catch (Exception ex) { ex.printStackTrace(); return; }
            sequencer.addMetaEventListener(this);
            isMIDIEnabled = true;
        }



        /** This method disables MIDI, by closing the <i>sequencer</i>.  */
        public void disableMIDI() {
            if (sequencer != null) {
                sequencer.close();
                isMIDIEnabled = false;
            }
        }



        /** This method is the implementation of <i>MetaEventListener</i>. It is used to find out when the MIDI-file is finished playing. */
        public void meta(MetaMessage message) {
            if (message.getType() == 47) {  // 47 is end of track
                midiEOM = true;
            }
        }



        /** This method is the implementation of <i>LineListener</i>. It is used to find out when the (not MIDI) audio-file is finished playing. */
        public void update(LineEvent event) {
            if (event.getType() == LineEvent.Type.STOP && !soundPaused) {
                audioEOM = true;
            }
        }


    }
}
