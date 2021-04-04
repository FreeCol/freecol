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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.*;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Option for selecting an audio mixer.
 */
public class AudioMixerOption extends AbstractOption<AudioMixerOption.MixerWrapper> {

    private static Logger logger = Logger.getLogger(AudioMixerOption.class.getName());

    public static final String TAG = "audioMixerOption";

    /**
     * A wrapper for the mixer, including the Mixer.Info including a
     * potentially null value.
     */
    public static class MixerWrapper implements Comparable<MixerWrapper> {

        /** The name for this wrapper. */
        private final String name;

        /** The mixer info for a mixer. */
        private final Mixer.Info mixerInfo;


        public MixerWrapper(String name, Mixer.Info mixerInfo) {
            this.name = name;
            this.mixerInfo = mixerInfo;
        }

        public String getKey() {
            return name;
        }

        public Mixer.Info getMixerInfo() {
            return mixerInfo;
        }

        // Implement Comparable<MixerWrapper>

        @Override
        public int compareTo(MixerWrapper mw) {
            return getKey().compareTo(mw.getKey());
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof MixerWrapper) {
                return ((MixerWrapper)o).getKey().equals(getKey());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return getKey().hashCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(32);
            sb.append(this.name).append('/')
                .append((this.mixerInfo == null) ? "null" : this.mixerInfo.toString());
            return sb.toString();
        }
    }

    /** Autodetect the mixer? */
    public static final String AUTO_NAME
        = Messages.message("clientOptions.audio.audioMixer.automatic");

    /** The autodetected mixer. */
    private static final Mixer AUTODETECTED_MIXER;
    static {
        Mixer mixer = null;
        try {
            mixer = AudioSystem.getMixer(null);
        } catch (IllegalArgumentException e) {
            logger.log(Level.FINE, "No Audio Mixer installed on the system.", e);
        }
        AUTODETECTED_MIXER = mixer;
    }

    /** The default mixer wrapper around the autodetected mixer. */
    private static final MixerWrapper DEFAULT_MIXER_WRAPPER
        = new MixerWrapper(AUTO_NAME, (AUTODETECTED_MIXER == null) ? null
            : AUTODETECTED_MIXER.getMixerInfo());

    /** The available audio mixers. */
    private static final List<MixerWrapper> audioMixers = new ArrayList<>();
    static {
        audioMixers.addAll(transform(AudioSystem.getMixerInfo(),
                alwaysTrue(), mi -> new MixerWrapper(mi.getName(), mi),
                Comparator.naturalOrder()));
        audioMixers.add(0, DEFAULT_MIXER_WRAPPER);
    }


    /** The value of this option. */
    private MixerWrapper value = null;


    /**
     * Creates a new {@code AudioMixerOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public AudioMixerOption(Specification specification) {
        super(specification);
    }


    /**
     * Gets a mixer wrapper by name.
     *
     * @param name The mixer wrapper name.
     * @return The mixer wrapper with the name given, or null if none.
     */
    private MixerWrapper getMixerWrapperByName(String name) {
        return find(audioMixers, mw -> mw.getKey().equals(name));
    }

    /**
     * Gets a list of the available audio mixers.
     *
     * @return The available mixers.
     */
    public List<MixerWrapper> getChoices() {
        return new ArrayList<>(audioMixers);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public AudioMixerOption cloneOption() {
        AudioMixerOption result = new AudioMixerOption(getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final MixerWrapper getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setValue(MixerWrapper newValue) {
        final MixerWrapper oldValue = this.value;
        if (newValue == null) newValue = DEFAULT_MIXER_WRAPPER;
        this.value = newValue;
        if (!newValue.equals(oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
    }


    // Override AbstractOption
    // generateChoices() is effectively done in the audioMixers initialization.
    
    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings(value="NP_LOAD_OF_KNOWN_NULL_VALUE")
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        MixerWrapper mw = null;
        if (mw == null && valueString != null) {
            mw = getMixerWrapperByName(valueString);
        }
        if (mw == null && defaultValueString != null) {
            mw = getMixerWrapperByName(defaultValueString);
        }
        if (mw == null) mw = DEFAULT_MIXER_WRAPPER;
        setValue(mw);
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        if (value != null) {
            xw.writeAttribute(VALUE_TAG, value.getKey());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(16);
        sb.append('[').append(getId())
            .append(' ').append(getValue()).append(']');
        return sb.toString();
    }
}
