/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Option for selecting an audio mixer.
 */
public class AudioMixerOption extends AbstractOption {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(AudioMixerOption.class.getName());

    private static final Map<String, MixerWrapper> audioMixers = new HashMap<String, MixerWrapper>();

    public static final String AUTO = Messages.message("clientOptions.audio.audioMixer.automatic");

    private static final Mixer AUTODETECT_MIXER = tryGetMixer();
    private static final MixerWrapper DEFAULT = new MixerWrapper(AUTO,
            (AUTODETECT_MIXER != null) ? AUTODETECT_MIXER.getMixerInfo() : null);

    private static Mixer tryGetMixer() {
        Mixer mixer = null;
        try {
            mixer = AudioSystem.getMixer(null);
        } catch (IllegalArgumentException e) {
            ; // Thrown on Ubuntu
        }
        return mixer;
    }

    
    private static Comparator<MixerWrapper> audioMixerComparator = new Comparator<MixerWrapper>() {
        public int compare(MixerWrapper m1, MixerWrapper m2) {
            if (m1.equals(DEFAULT)) {
                if (m2.equals(DEFAULT)) {
                    return 0;
                } else {
                    return -1;
                }
            } else if (m2.equals(DEFAULT)) {
                return 1;
            } else {
                return m1.getMixerInfo().getName().compareTo(m2.getMixerInfo().getName());
            }
        }
    };

    private MixerWrapper value;

    /**
     * Creates a new <code>AudioMixerOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     */
    public AudioMixerOption(String id) {
        this(id, null);
    }

    public AudioMixerOption(String id, OptionGroup optionGroup) {
        super(id, optionGroup);
        value = DEFAULT;
    }

    
    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>String</code> value
     */
    public final MixerWrapper getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final MixerWrapper newValue) {
        final MixerWrapper oldValue = this.value;
        this.value = newValue;
        if (!newValue.equals(oldValue)) {
            firePropertyChange("value", oldValue, value);
        }
    }

    /**
     * Returns a list of the available audioMixers.
     * @return The available audioMixers in a human readable format.
     */
    public MixerWrapper[] getOptions() {
        findAudioMixers();
        List<MixerWrapper> mixers = new ArrayList<MixerWrapper>(audioMixers.values());
        Collections.sort(mixers, audioMixerComparator);
        return mixers.toArray(new MixerWrapper[0]);
    }

    /**
     * Finds the audioMixers available.
     */
    private void findAudioMixers() {
        audioMixers.clear();
        audioMixers.put(AUTO, DEFAULT);
        for (Mixer.Info mi : AudioSystem.getMixerInfo()) {
            audioMixers.put(mi.getName(), new MixerWrapper(mi.getName(), mi));
        }
    }
    
    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *  
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getId());

        out.writeAttribute("value", getValue().getKey());

        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        findAudioMixers();
        MixerWrapper newValue = audioMixers.get(in.getAttributeValue(null, "value"));
        if (newValue == null) {
            newValue = audioMixers.get(AUTO);
        }
        setValue(newValue);
        in.nextTag();
    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "audioMixerOption".
     */
    public static String getXMLElementTagName() {
        return "audioMixerOption";
    }
    
    public static class MixerWrapper {
        private String name;
        private Mixer.Info mixerInfo;
        
        MixerWrapper(String name, Mixer.Info mixerInfo) {
            this.name = name;
            this.mixerInfo = mixerInfo;
        }
        
        public String getKey() {
            return name;
        }
        
        public Mixer.Info getMixerInfo() {
            return mixerInfo;
        
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof MixerWrapper) {
                return ((MixerWrapper) o).getKey().equals(getKey());
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return getKey().hashCode();
        }
    }

}
