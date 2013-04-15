/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Specification;


/**
 * Option for selecting a language.  The possible choices are determined
 * using the available language files in the i18n directory.
 */
public class LanguageOption extends AbstractOption<LanguageOption.Language> {

    private static final Logger logger = Logger.getLogger(LanguageOption.class.getName());

    public static final String AUTO = "automatic";

    /** Extra languages with alternate names. */
    private static final Map<String, String> languageNames
        = new HashMap<String, String>();
    static { // Add non-standard language names here.
        languageNames.put("arz", "\u0645\u0635\u0631\u064A");
        languageNames.put("hsb", "Serb\u0161\u0107ina");
        languageNames.put("nds", "Plattd\u00fc\u00fctsch");
        languageNames.put("pms", "Piemont\u00e9s");
        languageNames.put("be-tarask", "\u0411\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f "
            + "(\u0442\u0430\u0440\u0430\u0448\u043a\u0435\u0432\u0456\u0446\u0430)");
    }

    public static class Language implements Comparable<Language> {

        /** The language name. */
        private String key;

        /** The corresponding Locale. */
        private Locale locale;


        public Language(String key, Locale locale) {
            this.key = key;
            this.locale = locale;
        }

        /**
         * Get the key.
         *
         * @return The key.
         */
        public final String getKey() {
            return key;
        }

        /**
         * Set the key.
         *
         * @param newKey The new key.
         */
        public final void setKey(final String newKey) {
            this.key = newKey;
        }

        /**
         * Get the locale.
         *
         * @return The <code>Locale</code>.
         */
        public final Locale getLocale() {
            return locale;
        }

        /**
         * Set the locale.
         *
         * @param newLocale The new <code>Locale</code>.
         */
        public final void setLocale(final Locale newLocale) {
            this.locale = newLocale;
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo(Language l) {
            return toString().compareTo(l.toString());
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            if ((o instanceof Language) && ((Language)o).getKey().equals(key)) {
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (getKey().equals(AUTO)) {
                return Messages.message("clientOptions.gui.languageOption.autoDetectLanguage");
            }

            String name = locale.getDisplayName(locale);
            if (name.equals(key) && languageNames.containsKey(key)) {
                name = languageNames.get(key);
            }
            return name.substring(0, 1).toUpperCase(locale) + name.substring(1);
        }
    }

    /**
     * A list of know languages.
     * Initialized on demand with initializeLanguages(), as it depends
     * on the location of the i18n directory having stabilized.
     */
    private static final List<Language> languages = new ArrayList<Language>();

    /** The default language. */
    private static final Language DEFAULT_LANGUAGE
        = new Language(AUTO, Locale.getDefault());

    /** The value of this option. */
    private Language value = DEFAULT_LANGUAGE;


    /**
     * Creates a new <code>LanguageOption</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public LanguageOption(Specification specification) {
        super(specification);

        initializeLanguages();
    }

    /**
     * Initialize the languages list.
     */
    private static void initializeLanguages() {
        if (!languages.isEmpty()) return;

        File i18nDirectory = FreeColDirectories.getI18nDirectory();
        File[] files = i18nDirectory.listFiles();
        if (files == null) {
            throw new RuntimeException("No language files could be found"
                + " in the <" + i18nDirectory + "> directory.");
        }
        String prefix = Messages.FILE_PREFIX + "_";
        int prefixLength = prefix.length();
        for (File file : files) {
            if (file.getName() == null
                || !file.getName().startsWith(prefix)) continue;
            final String languageID = file.getName()
                .substring(prefixLength, file.getName().indexOf("."));
            // qqq contains explanations only
            if ("qqq".equals(languageID)) continue;
            try {
                languages.add(new Language(languageID,
                                           Messages.getLocale(languageID)));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to add: " + languageID, e);
            }
        }
        Collections.sort(languages);
        languages.add(0, DEFAULT_LANGUAGE);
    }

    /**
     * Find the language with the given key.
     *
     * @param key The key to search for.
     * @return The corresponding language, or null if none found.
     */
    private Language getLanguage(String key) {
        for (Language l : languages) {
            if (key.equals(l.getKey())) return l;
        }
        return null;
    }

    /**
     * Gets a list of the available languages.
     *
     * @return A list of the available languages.
     */
    public List<Language> getChoices() {
        return new ArrayList<Language>(languages);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    public LanguageOption clone() throws CloneNotSupportedException {
        LanguageOption result = new LanguageOption(getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public final Language getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public final void setValue(final Language newValue) {
        final Language oldValue = this.value;
        this.value = newValue;

        if (!newValue.equals(oldValue)) {
            firePropertyChange(VALUE_TAG, oldValue, value);
        }
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    protected void setValue(String valueString, String defaultValueString) {
        Language l = null;
        if (l == null && valueString != null) {
            l = getLanguage(valueString);
        }
        if (l == null && defaultValueString != null) {
            l = getLanguage(defaultValueString);
        }
        if (l == null) {
            l = getLanguage(AUTO);
        }
        setValue(l);
    }


    // Serialization

    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, VALUE_TAG, getValue().getKey());
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "languageOption".
     */
    public static String getXMLElementTagName() {
        return "languageOption";
    }
}
