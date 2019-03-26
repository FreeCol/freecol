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

package net.sf.freecol.common.option;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Option for selecting a language.  The possible choices are determined
 * using the available language files in the i18n directory.
 */
public class LanguageOption extends AbstractOption<LanguageOption.Language> {

    private static final Logger logger = Logger.getLogger(LanguageOption.class.getName());

    public static final String TAG = "languageOption";

    /** Extra languages with alternate names. */
    private static final Map<String, String> languageNames
        = makeUnmodifiableMap(new String[] {
                "arz", "hsb", "nds", "pms", "be-tarask" },
            new String[] {
                "\u0645\u0635\u0631\u064A",
                "Serb\u0161\u0107ina",
                "Plattd\u00fc\u00fctsch",
                "Piemont\u00e9s",
                "\u0411\u0435\u043b\u0430\u0440\u0443\u0441\u043a\u0430\u044f (\u0442\u0430\u0440\u0430\u0448\u043a\u0435\u0432\u0456\u0446\u0430)" });

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
         * @return The {@code Locale}.
         */
        public final Locale getLocale() {
            return locale;
        }

        /**
         * Set the locale.
         *
         * @param newLocale The new {@code Locale}.
         */
        public final void setLocale(final Locale newLocale) {
            this.locale = newLocale;
        }

        // Implement Comparable<Language>

        /**
         * {@inheritDoc}
         */
        @Override
        public int compareTo(Language l) {
            return toString().compareTo(l.toString());
        }


        // Override Object

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof Language) {
                Language other = (Language)o;
                return Utils.equals(this.key, other.key)
                    && super.equals(other);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = super.hashCode();
            return 31 * hash + Utils.hashCode(this.key);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            if (Messages.AUTOMATIC.equals(getKey())) {
                return Messages.message("clientOptions.gui.languageOption.autoDetectLanguage");
            }

            String name = locale.getDisplayName(locale);
            if (name.equals(key) && languageNames.containsKey(key)) {
                name = languageNames.get(key);
            }
            return capitalize(name, locale);
        }
    }

    /**
     * A list of know languages.
     * Initialized on demand with initializeLanguages(), as it depends
     * on the location of the i18n directory having stabilized.
     */
    private static final List<Language> languages = new ArrayList<>();

    /** The default language. */
    private static final Language DEFAULT_LANGUAGE
        = new Language(Messages.AUTOMATIC, Locale.getDefault());

    /** The value of this option. */
    private Language value = DEFAULT_LANGUAGE;


    /**
     * Creates a new {@code LanguageOption}.
     *
     * @param specification The {@code Specification} to refer to.
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

        for (String languageId : FreeColDirectories.getLanguageIdList()) {
            if (languageId == null) continue;
            try {
                languages.add(new Language(languageId,
                        Messages.getLocale(languageId)));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to add: " + languageId, e);
            }
        }
        languages.sort(Comparator.naturalOrder());
        languages.add(0, DEFAULT_LANGUAGE);
    }

    /**
     * Find the language with the given key.
     *
     * @param key The key to search for.
     * @return The corresponding language, or null if none found.
     */
    private Language getLanguage(String key) {
        return find(languages, l -> key.equals(l.getKey()));
    }

    /**
     * Gets a list of the available languages.
     *
     * @return A list of the available languages.
     */
    public List<Language> getChoices() {
        return new ArrayList<>(languages);
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public LanguageOption cloneOption() {
        LanguageOption result = new LanguageOption(getSpecification());
        result.setValues(this);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Language getValue() {
        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @SuppressFBWarnings(value="NP_LOAD_OF_KNOWN_NULL_VALUE")
    @Override
    protected void setValue(String valueString, String defaultValueString) {
        Language l = null;
        if (l == null && valueString != null) {
            l = getLanguage(valueString);
        }
        if (l == null && defaultValueString != null) {
            l = getLanguage(defaultValueString);
        }
        if (l == null) {
            l = getLanguage(Messages.AUTOMATIC);
        }
        setValue(l);
    }


    // Serialization


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(VALUE_TAG, getValue().getKey());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return TAG; }
}
