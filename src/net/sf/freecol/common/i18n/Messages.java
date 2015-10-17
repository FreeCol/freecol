/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

package net.sf.freecol.common.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.UIManager;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.StringTemplate.TemplateType;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Represents a collection of messages in a particular locale.
 *
 * The individual messages are read from property files in the
 * <code>data/strings</code> directory. The property files are called
 * "FreeColMessages[_LANGUAGE[_COUNTRY[_VARIANT]]].properties", where
 * LANGUAGE should be an ISO 639-2 or ISO 639-3 language code, COUNTRY
 * should be an ISO 3166-2 country code, and VARIANT is an arbitrary
 * string. The encoding of the property files is UTF-8. Since the Java
 * Properties class is unable to handle UTF-8 directly, this class
 * uses its own implementation.
 *
 * The individual messages may include variables, which must be
 * delimited by percent characters (e.g. "%nation%"), and will be
 * replaced when the message is formatted. Furthermore, the messages
 * may include choice formats consisting of a tag followed by a colon
 * (":"), a selector and one or several choices separated from the
 * selector and each other by pipe characters ("|"). The entire choice
 * format must be enclosed in double brackets ("{{" and "}}",
 * respectively).
 *
 * Each choice must consist of a key and a value separated by an
 * equals character ("="), unless it is a variable, in which case the
 * variable must resolve to another choice format. The selector may
 * also be a variable. If the selector is omitted, then one of the
 * choices should use the key "default". Choice formats may be
 * nested.
 *
 * <pre>
 *   key1=%colony% tuottaa tuotetta {{tag:acc|%goods%}}.
 *   key2={{plural:%amount%|one=ruoka|other=ruokaa|default={{tag:|acc=viljaa|default=Vilja}}}}
 *   key3={{tag:|acc=viljaa|default={{plural:%amount%|one=ruoka|other=ruokaa|default=Ruoka}}}}
 * </pre>
 *
 * This class is NOT thread-safe. (CO: I cannot find any place that
 * really has a problem)
 */
public class Messages {

    private static final Logger logger = Logger.getLogger(Messages.class.getName());

    public static final String MESSAGE_FILE_PREFIX = "FreeColMessages";
    public static final String MOD_MESSAGE_FILE_PREFIX = "ModMessages";
    public static final String MESSAGE_FILE_SUFFIX = ".properties";

    public static final String DESCRIPTION_SUFFIX = ".description";
    public static final String SHORT_DESCRIPTION_SUFFIX = ".shortDescription";
    public static final String NAME_SUFFIX = ".name";
    public static final String RULER_SUFFIX = ".ruler";

    private static final String[] DESCRIPTION_KEYS = {
        DESCRIPTION_SUFFIX, SHORT_DESCRIPTION_SUFFIX, NAME_SUFFIX
    };

    /** Automatic language choice from default locale. */
    public static final String AUTOMATIC = "automatic";

    /**
     * The mapping from language-independent key to localized message
     * for the established locale.
     */
    private static final Map<String, String> messageBundle = new HashMap<>();

    /**
     * A map with Selector values and the tag keys used in choice
     * formats.
     */
    private static final Map<String, Selector> tagMap = new HashMap<>();


    // Message bundle initialization

    /**
     * Gets the Selector with the given tag.
     *
     * @param tag The tag to check.
     * @return A suitable <code>Selector</code>.
     */
    private static Selector getSelector(String tag) {
        return tagMap.get(tag.toLowerCase(Locale.US));
    }

    /**
     * Set the grammatical number rule.
     *
     * @param number a <code>Number</code> value
     */
    public static void setGrammaticalNumber(Number number) {
        tagMap.put("plural", number);
    }

    /**
     * Get a list of candidate message file names for a given locale.
     *
     * @param locale The <code>Locale</code> to generate file names for.
     * @return A list of message file names.
     */
    private static List<String> getMessageFileNames(Locale locale) {
        return FreeColDataFile.getFileNames(MESSAGE_FILE_PREFIX,
            MESSAGE_FILE_SUFFIX, locale);
    }

    /**
     * Get a list of candidate mod message file names for a given locale.
     *
     * @param locale The <code>Locale</code> to generate file names for.
     * @return A list of mod message file names.
     */
    private static List<String> getModMessageFileNames(Locale locale) {
        return FreeColDataFile.getFileNames(MOD_MESSAGE_FILE_PREFIX,
            MESSAGE_FILE_SUFFIX, locale);
    }
        
    /**
     * Load the message bundle for the given locale
     *
     * Error messages have to go to System.err as this routine is called
     * before logging is enabled.
     *
     * @param locale The <code>Locale</code> to set resources for.
     */
    public static void loadMessageBundle(Locale locale) {
        messageBundle.clear(); // Reset the message bundle.

        if (!Locale.getDefault().equals(locale)) {
            Locale.setDefault(locale);
        }

        File i18nDirectory = FreeColDirectories.getI18nDirectory();
        if (!NumberRules.isInitialized()) {
            // attempt to read grammatical rules
            File cldr = new File(i18nDirectory, "plurals.xml");
            if (cldr.exists()) {
                try {
                    try (FileInputStream in = new FileInputStream(cldr)) {
                        NumberRules.load(in);
                    }
                } catch (IOException e) {
                    System.err.println("Failed to read CLDR rules: "
                        + e.getMessage());
                }
            } else {
                System.err.println("Could not find CLDR rules: "
                    + cldr.getPath());
            }
        }

        Locale loc = (AUTOMATIC.equalsIgnoreCase(locale.getLanguage()))
            ? Locale.getDefault() : locale;
        setGrammaticalNumber(NumberRules.getNumberForLanguage(loc.getLanguage()));

        for (String name : getMessageFileNames(locale)) {
            File file = new File(i18nDirectory, name);
            if (!file.exists()) continue; // Expected
            try {
                loadMessages(new FileInputStream(file));
            } catch (IOException e) {
                System.err.println("Failed to load messages from " + name
                    + ": " + e.getMessage());
            }
        }
    }

    /**
     * Loads messages from a resource file into the current message bundle.
     *
     * Public for the test suite.
     *
     * @param is The <code>InputStream</code> to read from.
     * @throws IOException
     */
    public static void loadMessages(InputStream is) throws IOException {
        InputStreamReader inputReader;
        try {
            inputReader = new InputStreamReader(is, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            return; // We have big problems if UTF-8 is not supported.
        }
        BufferedReader in = new BufferedReader(inputReader);

        String line = null;
        while((line = in.readLine()) != null) {
            line = line.trim();
            int index = line.indexOf('#');
            if (index == 0) continue;
            index = line.indexOf('=');
            if (index > 0) {
                String key = line.substring(0, index).trim();
                String value = line.substring(index + 1).trim()
                    .replace("\\n", "\n").replace("\\t", "\t");
                messageBundle.put(key, value);
                if (key.startsWith("FileChooser.")) {
                    UIManager.put(key, value);
                }
            }
        }
    }

    /**
     * Load localized messages for all mods.
     *
     * We can not initially load resources from mods because not all
     * mods can be loaded until the user mod directory is initialized,
     * which depends on the command line processing, which in turn
     * requires at least the basic localized message resources to be
     * loaded.  So this routine is called separately when the mods are
     * finally loaded.
     *
     * @param locale The <code>Locale</code> to load resources for.
     */
    public static void loadModMessageBundle(Locale locale) {
        List<FreeColModFile> allMods = new ArrayList<>();
        allMods.addAll(Mods.getAllMods());
        allMods.addAll(Mods.getRuleSets());

        List<String> filenames = getMessageFileNames(locale);
        for (FreeColModFile fcmf : allMods) {
            for (String name : filenames) {
                try {
                    loadMessages(fcmf.getInputStream(name));
                } catch (IOException e) {} // Failures expected
            }
        }
    }

    /**
     * Load messages specific to active mods.
     *
     * Called when the spec is updated with the selected mods.
     *
     * @param mods The list of <code>FreeColModFile</code> for the active mods.
     * @param locale The <code>Locale</code> to load resources for.
     */
    public static void loadActiveModMessageBundle(List<FreeColModFile> mods,
                                                  Locale locale) {
        for (FreeColModFile fcmf : mods) {
            for (String name : getModMessageFileNames(locale)) {
                try {
                    loadMessages(fcmf.getInputStream(name));
                } catch (IOException e) {} // Failures expected
            }
        }
    }
    
    /**
     * Get the <code>Locale</code> corresponding to a given language name.
     *
     * Public as this is needed for language option processing and the
     * initial locale setting.
     *
     * @param languageID An underscore separated language/country/variant tuple.
     * @return The <code>Locale</code> for the specified language.
     */
    public static Locale getLocale(String languageID) {
        String language, country = "", variant = "";
        StringTokenizer st = new StringTokenizer(languageID, "_", true);
        language = st.nextToken();
        if (st.hasMoreTokens()) {
            // Skip _
            st.nextToken();
        }
        if (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!"_".equals(token)) {
                country = token;
            }
            if (st.hasMoreTokens()) {
                token = st.nextToken();
                if ("_".equals(token) && st.hasMoreTokens()) {
                    token = st.nextToken();
                }
                if (!"_".equals(token)) {
                    variant = token;
                }
            }
        }
        return new Locale(language, country, variant);
    }


    // Shortcut message accessors

    public static String nameKey(String id) {
        return id + NAME_SUFFIX;
    }

    public static String nameKey(ObjectWithId object) {
        return nameKey(object.getId());
    }

    public static String getName(String id) {
        return message(nameKey(id));
    }

    public static String getName(Named named) {
        return message(named.getNameKey());
    }

    public static String descriptionKey(String id) {
        return id + DESCRIPTION_SUFFIX;
    }

    public static String descriptionKey(ObjectWithId object) {
        return descriptionKey(object.getId());
    }

    public static String getDescription(String id) {
        return message(descriptionKey(id));
    }

    public static String getDescription(ObjectWithId object) {
        return message(descriptionKey(object));
    }


    public static String shortDescriptionKey(String id) {
        return id + SHORT_DESCRIPTION_SUFFIX;
    }

    public static String getShortDescription(String id) {
        return message(shortDescriptionKey(id));
    }

    public static String getShortDescription(ObjectWithId object) {
        return getShortDescription(object.getId());
    }


    public static String rulerKey(String id) {
        return id + RULER_SUFFIX;
    }

    public static String getRulerName(String id) {
        return message(rulerKey(id));
    }

    /**
     * Does the message bundle contain the given key?
     *
     * @param key The key <code>String</code> to check.
     * @return True if there is a message present with the given key.
     */
    public static boolean containsKey(String key) {
        return messageBundle.get(key) != null;
    }

    /**
     * Returns the preferred key if it is contained in the message
     * bundle and the default key otherwise. This should be used to
     * select the most specific message key available.
     *
     * @param preferredKey a <code>String</code> value
     * @param defaultKey a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String getKey(String preferredKey, String defaultKey) {
        if (containsKey(preferredKey)) {
            return preferredKey;
        } else {
            return defaultKey;
        }
    }

    public static String getBestDescription(ObjectWithId object) {
        return getBestDescription(object.getId());
    }

    public static String getBestDescription(String id) {
        String key = find(map(DESCRIPTION_KEYS, s -> id + s),
            k -> containsKey(k), null);
        return (key == null) ? id : message(key);
    }

    /**
     * Get the name and best description for a given named object.
     *
     * Favour the .name form, but degrade gracefully if it is not present.
     * If .name is present, also look for a description.
     *
     * @param named The <code>Named</code> to look up.
     * @return A 2-element array of name and description found.
     */
    public static String[] getBestNameAndDescription(Named named) {
        return getBestNameAndDescription(named.getNameKey());
    }

    /**
     * Get the name and best description for a given identifier.
     *
     * Favour the .name form, but degrade gracefully if it is not present.
     * If .name is present, also look for a description.
     *
     * @param id The identifier to look up.
     * @return A 2-element array of name and description found.
     */
    public static String[] getBestNameAndDescription(String id) {
        if (id != null && id.endsWith(NAME_SUFFIX)) { // Temporary hack
            id = id.substring(0, id.length() - NAME_SUFFIX.length());
        }
        String name = (containsKey(nameKey(id))) ? getName(id) : null;
        String desc = null;
        if (name == null) {
            name = (containsKey(id)) ? message(id) : null;
            if (name == null) name = id;
        } else {
            desc = getBestDescription(id);
            if (id.equals(desc)) desc = null;
        }
        return new String[] { name, desc };
    }


    // Special purpose unit labelling

    /**
     * Get a label for a collection of units given a name, type,
     * number, nation, role and extra annotation.
     *
     * @param name An optional unit name.
     * @param typeId The unit type identifier.
     * @param number The number of units.
     * @param nationId An optional nation identifier.
     * @param roleId The unit role identifier.
     * @param extra An optional extra annotation.
     * @return A <code>StringTemplate</code> to describe the given unit.
     */
    public static StringTemplate getUnitLabel(String name, String typeId,
                                              int number, String nationId,
                                              String roleId,
                                              StringTemplate extra) {
        // Check for special role-specific key, which will not have a
        // role argument.  These exist so we can avoid mentioning
        // the role twice, e.g. "Seasoned Scout Scout".
        StringTemplate type;
        String roleKey;
        String baseKey = typeId + "." + Role.getRoleSuffix(roleId);
        if (containsKey(baseKey)) {
            type = StringTemplate.template(baseKey)
                .addAmount("%number%", number);
            roleKey = null;
        } else {
            type = StringTemplate.template(nameKey(typeId))
                .addAmount("%number%", number);
            roleKey = (Role.isDefaultRoleId(roleId)) ? null : roleId;
        }

        StringTemplate ret;
        if (name == null) {
            if (nationId == null) {
                if (roleKey == null) {
                    if (extra == null) {
                        // %type% | "Free Colonist"
                        ret = type;
                    } else {
                        // %type% (%extra%) | "Treasure Train (5000 gold)"
                        ret = StringTemplate.label("")
                            .addStringTemplate(type)
                            .addName(" (")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                } else {
                    if (extra == null) {
                        // %role% (%type%) | "Soldier (Free Colonist)"
                        ret = StringTemplate.label("")
                            .add(nameKey(roleKey))
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %role% (%type%/%extra%) | "Soldier (Free Colonist/50 muskets)"
                        ret = StringTemplate.label("")
                            .add(nameKey(roleKey))
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                }
            } else {
                if (roleKey == null) {
                    if (extra == null) {
                        // %nation% %type% | "Dutch Free Colonist"
                        ret = StringTemplate.label("")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .addStringTemplate(type);
                    } else {
                        // %nation% %type% (%extra%) | "Dutch Treasure Train (5000 gold)"
                        ret = StringTemplate.label("")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .addStringTemplate(type)
                            .addName(" (")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                } else {
                    if (extra == null) {
                        // %nation% %role% (%type%) | "Dutch Soldier (Free Colonist)"
                        ret = StringTemplate.label("")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .add(nameKey(roleKey))
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %nation% %role% (%type%/%extra%) | "Dutch Soldier (Free Colonist/50 muskets)"
                        ret = StringTemplate.label("")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .add(nameKey(roleKey))
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                }
            }
        } else {
            if (nationId == null) {
                if (roleKey == null) {
                    if (extra == null) {
                        // %name% (%type%) | "Bob (Free Colonist)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %name% (%type%/%extra%) | "Moolah (Treasure Train/5000 gold)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                } else {
                    if (extra == null) {
                        // %name% (%role%/%type%) | "Bob (Soldier/Free Colonist)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(roleKey))
                            .addName("/")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %name% (%role%/%type%/%extra%) | "Bob (Soldier/Free Colonist/50 muskets)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(roleKey))
                            .addName("/")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                }
            } else {
                if (roleKey == null) {
                    if (extra == null) {
                        // %name% (%nation% %type%) | "Bob (Dutch Free Colonist)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %name% (%nation% %type%/%extra%) | "Moolah (Dutch Treasure Train/5000 gold)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                } else {
                    if (extra == null) {
                        // %name% (%nation% %role%/%type%) | "Bob (Dutch Soldier/Free Colonist)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .add(nameKey(roleKey))
                            .addName("/")
                            .addStringTemplate(type)
                            .addName(")");
                    } else {
                        // %name% (%nation% %role%/%type%/%extra%) | "Bob (Dutch Soldier/Free Colonist/50 muskets)"
                        ret = StringTemplate.label("")
                            .addName(name)
                            .addName(" (")
                            .add(nameKey(nationId))
                            .addName(" ")
                            .add(nameKey(roleKey))
                            .addName("/")
                            .addStringTemplate(type)
                            .addName("/")
                            .addStringTemplate(extra)
                            .addName(")");
                    }
                }
            }
        }
        return ret;
    }


    // message().  The fundamental i18n routine, and its support.

    /**
     * Get the text mapping for a particular identifier in the
     * default locale message bundle.  Returns the key as the value if
     * there is no mapping found!
     *
     * @param messageId The key of the message to find.
     * @return String text mapping or the key
     */
    public static String message(String messageId) {
        // Check that all the values are correct.
        if (messageId == null) {
            throw new NullPointerException("Message id must not be null!");
        }

        // return key as value if there is no mapping found
        String message = messageBundle.get(messageId);
        if (message == null) return messageId;

        // otherwise replace variables in the text
        message = replaceChoices(message, null);
        return message.trim();
    }

    /**
     * Localizes a StringTemplate.
     *
     * @param template The <code>StringTemplate</code> to localize.
     * @return The localized string.
     */
    public static String message(StringTemplate template) {
        if (template == null) return null;
        String result = "";
        switch (template.getTemplateType()) {
        case LABEL:
            List<StringTemplate> replacements = template.getReplacements();
            if (replacements.isEmpty()) {
                result = message(template.getId());
            } else {
                for (StringTemplate other : replacements) {
                    result += template.getId() + message(other);
                }
                if (result.length() >= template.getId().length()) {
                    result = result.substring(template.getId().length());
                } else {
                    logger.warning("incorrect use of template " + template);
                }
            }
            break;
        case TEMPLATE:
            if (containsKey(template.getId())) {
                result = messageBundle.get(template.getId());
            } else if (template.getDefaultId() != null) {
                result = messageBundle.get(template.getDefaultId());
            }
            result = replaceChoices(result, template);
            for (String key : template.getKeys()) {
                result = result.replace(key,
                                        message(template.getReplacement(key)));
            }
            break;
        case KEY:
            String key = messageBundle.get(template.getId());
            result = (key == null) ? template.getId()
                : replaceChoices(key, null);
            break;
        case NAME:
        default:
            result = template.getId();
            break;
        }
        return result;
    }

    /**
     * Replace all choice formats in the given string, using keys and
     * replacement values from the given template, which may be null.
     *
     * A choice format is enclosed in double brackets and consists of
     * a tag, followed by a colon, followed by an optional selector,
     * followed by a pipe character, followed by one or several
     * choices separated by pipe characters. If there is only one
     * choice, it must be a message identifier or a
     * variable. Otherwise, each choice consists of a key and a value
     * separated by an assignment character. Example:
     * "{{tag:selector|key1=val1|key2=val2}}".
     *
     * @param input a <code>String</code> value
     * @param template a <code>StringTemplate</code> value
     * @return a <code>String</code> value
     */
    private static String replaceChoices(String input, StringTemplate template) {
        int openChoice = 0;
        int closeChoice = 0;
        int highWaterMark = 0;
        StringBuilder result = new StringBuilder();
        while ((openChoice = input.indexOf("{{", highWaterMark)) >= 0) {
            result.append(input.substring(highWaterMark, openChoice));
            closeChoice = findMatchingBracket(input, openChoice + 2);
            if (closeChoice < 0) {
                // no closing brackets found
                logger.warning("Mismatched brackets: " + input);
                return result.toString();
            }
            highWaterMark = closeChoice + 2;
            int colonIndex = input.indexOf(':', openChoice + 2);
            if (colonIndex < 0 || colonIndex > closeChoice) {
                logger.warning("No tag found: " + input);
                continue;
            }
            String tag = input.substring(openChoice + 2, colonIndex);
            int pipeIndex = input.indexOf('|', colonIndex + 1);
            if (pipeIndex < 0 || pipeIndex > closeChoice) {
                logger.warning("No choices found: " + input);
                continue;
            }
            String selector = input.substring(colonIndex + 1, pipeIndex);
            if (selector.isEmpty()) {
                selector = "default";
            } else if (selector.startsWith("%") && selector.endsWith("%")) {
                if (template == null) {
                    selector = "default";
                } else {
                    StringTemplate replacement = template.getReplacement(selector);
                    if (replacement == null) {
                        logger.warning("Failed to find replacement for " + selector);
                        continue;
                    } else {
                        selector = message(replacement);
                        Selector taggedSelector = getSelector(tag);
                        if (taggedSelector != null) {
                            selector = taggedSelector.getKey(selector, input);
                        }
                    }
                }
            } else {
                Selector taggedSelector = getSelector(tag);
                if (taggedSelector != null) {
                    selector = taggedSelector.getKey(selector, input);
                }
            }
            int keyIndex = input.indexOf(selector, pipeIndex + 1);
            if (keyIndex < 0 || keyIndex > closeChoice) {
                // key not found, choice might be a key itself
                String otherKey = input.substring(pipeIndex + 1, closeChoice);
                if (otherKey.startsWith("%") && otherKey.endsWith("%")
                    && template != null) {
                    StringTemplate replacement = template.getReplacement(otherKey);
                    if (replacement == null) {
                        logger.warning("Failed to find replacement for " + otherKey);
                        continue;
                    } else if (replacement.getTemplateType() == TemplateType.KEY) {
                        otherKey = messageBundle.get(replacement.getId());
                        keyIndex = otherKey.indexOf("{{");
                        if (keyIndex < 0) {
                            // not a choice format
                            result.append(otherKey);
                        } else {
                            keyIndex = otherKey.indexOf(selector, keyIndex);
                            if (keyIndex < 0) {
                                logger.warning("Failed to find key " + selector + " in replacement "
                                               + replacement.getId());
                                continue;
                            } else {
                                result.append(getChoice(otherKey, selector));
                            }
                        }
                    } else {
                        logger.warning("Choice substitution attempted, but template type was "
                                       + replacement.getTemplateType());
                        continue;
                    }
                } else if (containsKey(otherKey)) {
                    otherKey = getChoice(messageBundle.get(otherKey), selector);
                    result.append(otherKey);
                } else {
                    logger.warning("Unknown key or untagged choice: '" + otherKey
                                   + "', selector was '" + selector
                                   + "', trying 'default' instead");
                    int defaultStart = otherKey.indexOf("default=");
                    if (defaultStart >= 0) {
                        defaultStart += 8;
                        int defaultEnd = otherKey.indexOf('|', defaultStart);
                        String defaultChoice;
                        if (defaultEnd < 0) {
                            defaultChoice = otherKey.substring(defaultStart);
                        } else {
                            defaultChoice = otherKey.substring(defaultStart, defaultEnd);
                        }
                        result.append(defaultChoice);
                    } else {
                        logger.warning("No default choice found.");
                        continue;
                    }
                }
            } else {
                int start = keyIndex + selector.length() + 1;
                int replacementIndex = input.indexOf('|', start);
                int nextOpenIndex = input.indexOf("{{", start);
                if (nextOpenIndex >= 0 && nextOpenIndex < replacementIndex) {
                    replacementIndex = input.indexOf('|',
                        findMatchingBracket(input, nextOpenIndex + 2) + 2);
                }
                int end = (replacementIndex < 0
                    || replacementIndex > closeChoice) ? closeChoice
                    : replacementIndex;
                String replacement = input.substring(start, end);
                if (!replacement.contains("{{")) {
                    result.append(replacement);
                } else {
                    result.append(replaceChoices(replacement, template));
                }
            }
        }
        result.append(input.substring(highWaterMark));
        return result.toString();
    }

    /**
     * Return the choice tagged with the given key, or null, if the
     * given input string does not contain the key.
     *
     * @param input a <code>String</code> value
     * @param key a <code>String</code> value
     * @return a <code>String</code> value
     */
    private static String getChoice(String input, String key) {
        int keyIndex = input.indexOf(key);
        if (keyIndex < 0) {
            return null;
        } else {
            int start = keyIndex + key.length() + 1;
            int end = input.indexOf('|', start);
            if (end < 0) {
                end = input.indexOf("}}", start);
                if (end < 0) {
                    logger.warning("Failed to find end of choice for key " + key
                                   + " in input " + input);
                    return null;
                }
            }
            return input.substring(start, end);
        }
    }

    /**
     * Return the index of the matching pair of brackets, or -1 if
     * none is found.
     *
     * @param input a <code>String</code> value
     * @param start an <code>int</code> value
     * @return an <code>int</code> value
     */
    private static int findMatchingBracket(String input, int start) {
        char last = 0;
        int level = 0;
        for (int index = start; index < input.length(); index++) {
            switch(input.charAt(index)) {
            case '{':
                if (last == '{') {
                    last = 0;
                    level++;
                } else {
                    last = '{';
                }
                break;
            case '}':
                if (last == '}') {
                    if (level == 0) {
                        return index - 1;
                    } else {
                        last = 0;
                        level--;
                    }
                } else {
                    last = '}';
                }
                break;
            default:
                break;
            }
        }
        // found no matching bracket
        return -1;
    }
}
