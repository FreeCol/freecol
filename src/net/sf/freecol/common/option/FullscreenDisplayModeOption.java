/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import java.awt.DisplayMode;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;


/**
 * Option for selecting a fullscreen display mode.
 */
public class FullscreenDisplayModeOption extends AbstractOption<FullscreenDisplayModeOption.FullscreenDisplayModeWrapper> {
    
    public static final String TAG = "fullscreenDisplayModeOption";

    /** The value of this option. */
    private FullscreenDisplayModeWrapper value = null;


    /**
     * Creates a new {@code AudioMixerOption}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public FullscreenDisplayModeOption(Specification specification) {
        super(ClientOptions.DISPLAY_MODE_FULLSCREEN, specification);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FullscreenDisplayModeOption cloneOption() {
        FullscreenDisplayModeOption result = new FullscreenDisplayModeOption(getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final FullscreenDisplayModeWrapper getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void setValue(FullscreenDisplayModeWrapper newValue) {
        final FullscreenDisplayModeWrapper oldValue = this.value;
        this.value = newValue;
        if (newValue == null && value != null
                || newValue != null && value == null
                || newValue != null && value != null && !newValue.equals(oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        setValue(FullscreenDisplayModeWrapper.fromString(valueString));
    }

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
    
    public static class FullscreenDisplayModeWrapper implements Comparable<FullscreenDisplayModeWrapper> {

        private final String key;
        private final String name;

        public FullscreenDisplayModeWrapper(DisplayMode displayMode) {
            if (displayMode == null) {
                this.key = "";
                this.name = null;
            } else {
                // Reports bogus refresh rates on my linux system:
                //this.key = displayMode.getWidth() + "x" + displayMode.getHeight() + "@" + displayMode.getRefreshRate() + ":" + displayMode.getBitDepth();
                this.key = displayMode.getWidth() + "x" + displayMode.getHeight();
                this.name = key;
            }
        }
        
        
        public static FullscreenDisplayModeWrapper fromString(String key) {
            return new FullscreenDisplayModeWrapper(toDisplayMode(key));
        }
        
        private static DisplayMode toDisplayMode(String key) {
            if (key == null || key.isEmpty()) {
                return null;
            }
            
            final String[] a = key.split("[x@:]");
            //
            //return new DisplayMode(Integer.parseInt(a[0]), Integer.parseInt(a[1]), Integer.parseInt(a[3]), Integer.parseInt(a[2]));
            return new DisplayMode(Integer.parseInt(a[0]), Integer.parseInt(a[1]), DisplayMode.BIT_DEPTH_MULTI, DisplayMode.REFRESH_RATE_UNKNOWN);
        }

        public String getKey() {
            return key;
        }
        
        public String getName() {
            if (name != null) {
                return name;
            }
            return Messages.message("model.option.fullscreenDisplayMode.automatic");
        }

        public DisplayMode getDisplayMode() {
            if (key.isEmpty()) {
                return null;
            }
            return toDisplayMode(key);
        }

        @Override
        public int compareTo(FullscreenDisplayModeWrapper mw) {
            if (key.isEmpty()) {
                return -1;
            }
            if (mw.key.isEmpty()) {
                return 1;
            }
            final String[] a = key.split("[x@:]");
            final String[] b = mw.key.split("[x@:]");
            
            int result = Integer.compare(Integer.parseInt(b[0]), Integer.parseInt(a[0]));
            if (result != 0) {
                return result;
            }
            result = Integer.compare(Integer.parseInt(b[1]), Integer.parseInt(a[1]));
            if (result != 0) {
                return result;
            }
            /*
            result = Integer.compare(Integer.parseInt(b[3]), Integer.parseInt(a[3]));
            if (result != 0) {
                return result;
            }
            result = Integer.compare(Integer.parseInt(b[2]), Integer.parseInt(a[2]));
            if (result != 0) {
                return result;
            }
            */
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof FullscreenDisplayModeWrapper) {
                return ((FullscreenDisplayModeWrapper)o).getKey().equals(getKey());
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
            return key;
        }
    }
}
