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

package net.sf.freecol.client.gui.i18n;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.StringTemplate.TemplateType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.Option;

/**
 * <p>Represents a collection of messages in a particular locale.</p>
 *
 * <p>The individual messages are read from property files in the
 * <code>data/strings</code> directory. The property files are called
 * "FreeColMessages[_LANGUAGE[_COUNTRY[_VARIANT]]].properties", where
 * LANGUAGE should be an ISO 639-2 or ISO 639-3 language code, COUNTRY
 * should be an ISO 3166-2 country code, and VARIANT is an arbitrary
 * string. The encoding of the property files is UTF-8. Since the Java
 * Properties class is unable to handle UTF-8 directly, this class
 * uses its own implementation.</p>
 *
 * <p>The individual messages may include variables, which must be
 * delimited by percent characters (e.g. "%nation%"), and will be
 * replaced when the message is formatted. Furthermore, the messages
 * may include choice formats consisting of a tag followed by a colon
 * (":"), a selector and one or several choices separated from the
 * selector and each other by pipe characters ("|"). The entire choice
 * format must be enclosed in double brackets ("{{" and "}}",
 * respectively).</p>
 *
 * <p>Each choice must consist of a key and a value separated by an
 * equals character ("="), unless it is a variable, in which case the
 * variable must resolve to another choice format. The selector may
 * also be a variable. If the selector is omitted, then one of the
 * choices should use the key "default". Choice formats may be
 * nested.</p>
 *
 * <pre>
 *   key1=%colony% tuottaa tuotetta {{tag:acc|%goods%}}.
 *   key2={{plural:%amount%|one=ruoka|other=ruokaa|default={{tag:|acc=viljaa|default=Vilja}}}}
 *   key3={{tag:|acc=viljaa|default={{plural:%amount%|one=ruoka|other=ruokaa|default=Ruoka}}}}
 * </pre>
 *
 * <p>This class is NOT thread-safe. (CO: I cannot find any place that
 * really has a problem)</p>
 *
 */
public class Messages {

    private static final Logger logger = Logger.getLogger(Messages.class.getName());

    public static final String STRINGS_DIRECTORY = "strings";

    public static final String FILE_PREFIX = "FreeColMessages";

    public static final String FILE_SUFFIX = ".properties";

    private static final String[] DESCRIPTION_KEYS = new String[] {
        ".description", ".shortDescription", ".name"
    };


    private static Map<String, String> messageBundle =
        new HashMap<String, String>();

    /**
     * A map with Selector values and the tag keys used in choice
     * formats.
     */
    private static Map<String, Selector> tagMap =
        new HashMap<String, Selector>();



    /**
     * Returns the Selector with the given tag.
     *
     * @param tag a <code>String</code> value
     * @return a <code>Selector</code> value
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
     * Set the resource bundle for the given locale
     *
     * @param locale
     */
    public static void setMessageBundle(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("Parameter locale must not be null");
        } else {
            if (!Locale.getDefault().equals(locale)) {
                Locale.setDefault(locale);
            }
            setMessageBundle(locale.getLanguage(), locale.getCountry(), locale.getVariant());
        }
    }

    /**
     * Set the resource bundle to the given locale
     *
     * @param language The language for this locale.
     * @param country The language for this locale.
     * @param variant The variant for this locale.
     */
    private static void setMessageBundle(String language, String country, String variant) {

        messageBundle = new HashMap<String, String>();
        List<String> filenames = FreeColModFile.getFileNames(FILE_PREFIX, FILE_SUFFIX, language, country, variant);

        if (!NumberRules.isInitialized()) {
            // attempt to read grammatical rules
            File stringDirectory = new File(FreeCol.getDataDirectory(), "strings");
            if (stringDirectory.exists()) {
                File cldr = new File(stringDirectory, "plurals.xml");
                if (cldr.exists()) {
                    try {
                        FileInputStream in = new FileInputStream(cldr);
                        NumberRules.load(in);
                        in.close();
                    } catch(Exception e) {
                        logger.warning("Failed to read CLDR rules: "
                                       + e.toString());
                    }
                } else {
                    logger.warning("Could not find CLDR rules: "
                                   + cldr.getPath());
                }
            } else {
                logger.warning("Could not find string directory: "
                               + stringDirectory.getPath());
            }
        }

        setGrammaticalNumber(NumberRules.getNumberForLanguage(language));

        for (String fileName : filenames) {
            File resourceFile = new File(getI18nDirectory(), fileName);
            loadResources(resourceFile);
            logger.finest("Loaded message bundle " + fileName + " from messages.");
        }

        List<FreeColModFile> allMods = new ArrayList<FreeColModFile>();
        allMods.addAll(Mods.getAllMods());
        allMods.addAll(Mods.getRuleSets());
        for (FreeColModFile fcmf : allMods) {
            for (String fileName : filenames) {
                try {
                    InputStream is = fcmf.getInputStream(fileName);
                    loadResources(is);
                    logger.finest("Loaded message bundle " + fileName + " from "
                                  + fcmf.getId() + ".");
                } catch (IOException e) {
                    logger.fine("No message bundle " + fileName + " in "
                                + fcmf.getId() + ".");
                }
            }
        }
    }

    /**
     * Returns the directory containing language property files.
     *
     * @return a <code>File</code> value
     */
    public static File getI18nDirectory() {
        return new File(FreeCol.getDataDirectory(), STRINGS_DIRECTORY);
    }

    /**
     * Returns the text mapping for a particular ID in the default locale message bundle.
     * Returns the key as the value if there is no mapping found!
     *
     * @param messageId The key of the message to find
     * @return String text mapping or the key
     */
    public static String message(String messageId) {
        // Check that all the values are correct.
        if (messageId == null) {
            throw new NullPointerException("Message ID must not be null!");
        }
        if (messageBundle == null) {
            setMessageBundle(Locale.getDefault());
        }

        // return key as value if there is no mapping found
        String message = messageBundle.get(messageId);
        if (message == null) {
            return messageId;
        }
        // otherwise replace variables in the text
        message = replaceChoices(message, null);
        return message.trim();
    }


    /**
     * Replace all choice formats in the given string, using keys and
     * replacement values from the given template, which may be null.
     *
     * A choice format is enclosed in double brackets and consists of
     * a tag, followed by a colon, followed by an optional selector,
     * followed by a pipe character, followed by one or several
     * choices separated by pipe characters. If there is only one
     * choice, it must be a message id or a variable. Otherwise, each
     * choice consists of a key and a value separated by an assignment
     * character. Example: "{{tag:selector|key1=val1|key2=val2}}".
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
            int colonIndex = input.indexOf(":", openChoice + 2);
            if (colonIndex < 0 || colonIndex > closeChoice) {
                logger.warning("No tag found: " + input);
                continue;
            }
            String tag = input.substring(openChoice + 2, colonIndex);
            int pipeIndex = input.indexOf("|", colonIndex + 1);
            if (pipeIndex < 0 || pipeIndex > closeChoice) {
                logger.warning("No choices found: " + input);
                continue;
            }
            String selector = input.substring(colonIndex + 1, pipeIndex);
            if ("".equals(selector)) {
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
                int replacementIndex = input.indexOf("|", start);
                int nextOpenIndex = input.indexOf("{{", start);
                if (nextOpenIndex >= 0 && nextOpenIndex < replacementIndex) {
                    replacementIndex = input.indexOf("|", findMatchingBracket(input, nextOpenIndex + 2) + 2);
                }
                int end = (replacementIndex < 0 || replacementIndex > closeChoice)
                    ? closeChoice : replacementIndex;
                String replacement = input.substring(start, end);
                if (replacement.indexOf("{{") < 0) {
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
            int end = input.indexOf("|", start);
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
            }
        }
        // found no matching bracket
        return -1;
    }

    /**
     * Localizes a StringTemplate.
     *
     * @param template a <code>StringTemplate</code> value
     * @return a <code>String</code> value
     */
    public static String message(StringTemplate template) {
        String result = "";
        switch (template.getTemplateType()) {
        case LABEL:
            if (template.getReplacements() == null
                || template.getReplacements().isEmpty()) {
                return message(template.getId());
            } else {
                for (StringTemplate other : template.getReplacements()) {
                    result += template.getId() + message(other);
                }
                if (result.length() > template.getId().length()) {
                    return result.substring(template.getId().length());
                } else {
                    logger.warning("incorrect use of template " + template.toString());
                    return result;
                }
            }
        case TEMPLATE:
            if (containsKey(template.getId())) {
                result = messageBundle.get(template.getId());
            } else if (template.getDefaultId() != null) {
                result = messageBundle.get(template.getDefaultId());
            }
            result = replaceChoices(result, template);
	    for (int index = 0; index < template.getKeys().size(); index++) {
                result = result.replace(template.getKeys().get(index),
                                        message(template.getReplacements().get(index)));
	    }
	    return result;
        case KEY:
            String key = messageBundle.get(template.getId());
            if (key == null) {
                return template.getId();
            } else {
                return replaceChoices(key, null);
            }
        case NAME:
        default:
            return template.getId();
        }
    }

    /**
     * Returns true if the message bundle contains the given key.
     *
     * @param key a <code>String</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean containsKey(String key) {
        if (messageBundle == null) {
            setMessageBundle(Locale.getDefault());
        }
        return (messageBundle.get(key) != null);
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


    public static String getName(FreeColObject object) {
        return message(object.getId() + ".name");
    }

    public static String getDescription(FreeColObject object) {
        return message(object.getId() + ".description");
    }

    public static String getShortDescription(FreeColObject object) {
        return message(object.getId() + ".shortDescription");
    }


    public static String getName(Option object) {
        return message(object.getId() + ".name");
    }

    public static String getDescription(Option object) {
        for (String suffix : DESCRIPTION_KEYS) {
            String key = object.getId() + suffix;
            if (containsKey(key)) {
                return message(key);
            }
        }
        return object.getId();
    }

    public static String getShortDescription(Option object) {
        return message(object.getId() + ".shortDescription");
    }


    /**
     * Returns the name of a unit in a human readable format. The
     * label consists of up to three items: If the unit has a role
     * other than the unit type's default role, the current role, the
     * proper name of the unit and the unit's type. Otherwise, the
     * unit's type, the proper name of the unit, and additional
     * information about gold (in the case of treasure trains), or
     * equipment.
     *
     * @param unit an <code>Unit</code> value
     * @return A label to describe the given unit
     */
    public static StringTemplate getLabel(Unit unit) {
        String typeKey = null;
        String infoKey = null;

        if (unit.canCarryTreasure()) {
            typeKey = unit.getType().getNameKey();
            infoKey = Integer.toString(unit.getTreasureAmount());
        } else {
            String key = (unit.getRole() == Unit.Role.DEFAULT) ? "name"
                : unit.getRole().toString().toLowerCase();
            String messageID = unit.getType().getId() + "." + key;
            if (containsKey(messageID)) {
                typeKey = messageID;
                if ((unit.getEquipment() == null || unit.getEquipment().isEmpty()) &&
                    unit.getType().getDefaultEquipmentType() != null) {
                    infoKey = unit.getType().getDefaultEquipmentType().getId() + ".none";
                }
            } else {
                typeKey = "model.unit.role." + key;
                infoKey = unit.getType().getNameKey();
            }
        }

        StringTemplate result = StringTemplate.label(" ")
            .add(typeKey);
        if (unit.getName() != null) {
            result.addName(unit.getName());
        }
        if (infoKey != null) {
            result.addStringTemplate(StringTemplate.label("")
                                     .addName("(")
                                     .add(infoKey)
                                     .addName(")"));
        }
        return result;
    }


    /**
     * Returns the name of a unit in a human readable format. The return value
     * can be used when communicating with the user.
     *
     * @param someType an <code>UnitType</code> value
     * @param someRole a <code>Role</code> value
     * @param count an <code>int</code> value
     * @return The given unit type as a String
     */
    public static String getLabel(UnitType someType, Unit.Role someRole, int count) {
        String key = someRole.toString().toLowerCase();
        if (someRole == Unit.Role.DEFAULT) {
            key = "name";
        }
        String messageID = someType.getId() +  "." + key;
        if (containsKey(messageID)) {
            return message(messageID);
        } else {
            return message(StringTemplate.template("model.unit." + key + ".name")
                           .addAmount("%number%", count)
                           .addName("%unit%", someType));
        }
    }

    /**
     * Returns the name of a unit in a human readable format. The return value
     * can be used when communicating with the user.
     *
     * @param unit an <code>AbstractUnit</code> value
     * @return The given unit type as a String
     */
    public static String getLabel(AbstractUnit unit) {
        String key = unit.getRole().toString().toLowerCase();
        if (unit.getRole() == Unit.Role.DEFAULT) {
            key = "name";
        }
        String messageID = unit.getId() +  "." + key;
        if (containsKey(messageID)) {
            return message(messageID);
        } else {
            return message(StringTemplate.template("model.unit." + key + ".name")
                           .addName("%unit%", unit));
        }
    }

    /**
     * Returns a string describing the given stance.
     *
     * @param stance The stance.
     * @return A matching string.
     */
    public static String getStanceAsString(Player.Stance stance) {
        return message("model.stance." + stance.toString().toLowerCase());
    }

    /**
     * Gets a string describing the number of turns left for a colony
     * to finish building something.
     *
     * @param turns the number of turns left
     * @return A descriptive string.
     */
    public static String getTurnsText(int turns) {
        return (turns == FreeColObject.UNDEFINED)
            ? message("notApplicable.short")
            : (turns >= 0) ? Integer.toString(turns)
            : ">" + Integer.toString(-turns);
    }

    public static String getNewLandName(Player player) {
        if (player.getNewLandName() == null) {
            return message(player.getNationID() + ".newLandName");
        } else {
            return player.getNewLandName();
        }
    }


    /**
     * Creates a unique region name by fetching a new default name
     * from the list of default names if possible.
     *
     * @param player <code>Player</code>
     * @param regionType a <code>RegionType</code> value
     * @return a <code>String</code> value
     */
    public static String getDefaultRegionName(Player player, RegionType regionType) {
        net.sf.freecol.common.model.Map map = player.getGame().getMap();
        int index = player.getNameIndex(regionType.getNameIndexKey());
        if (index < 1) index = 1;
        String prefix = player.getNationID() + ".region."
            + regionType.toString().toLowerCase(Locale.US) + ".";
        String name;
        do {
            name = null;
            if (containsKey(prefix + Integer.toString(index))) {
                name = Messages.message(prefix + Integer.toString(index));
                index++;
            }
        } while (name != null && map.getRegionByName(name) != null);
        player.setNameIndex(regionType.getNameIndexKey(), index);
        if (name == null) {
            do {
                name = message(StringTemplate.template("model.region.default")
                               .addStringTemplate("%nation%", player.getNationName())
                               .add("%type%", "model.region." + regionType.toString().toLowerCase() + ".name")
                               .addAmount("%index%", index));
                index++;
            } while (map.getRegionByName(name) != null);
        }
        return name;
    }

    /**
     * Collects all the names with a given prefix.
     *
     * @param prefix The prefix to check.
     * @param names A list to fill with the names found.
     */
    private static void collectNames(String prefix, List<String> names) {
        String name;
        int i = 0;
        while (Messages.containsKey(name = prefix + Integer.toString(i))) {
            names.add(Messages.message(name));
            i++;
        }
    }

    /**
     * Gets a list of settlement names and a fallback prefix for a player.
     *
     * @param player The <code>Player</code> to get names for.
     * @return A list of settlement names, with the first being the
     *     fallback prefix.
     */
    public static List<String> getSettlementNames(Player player) {
        List<String> names = new ArrayList<String>();

        collectNames(player.getNationID() + ".settlementName.", names);

        // Try the spec-qualified version.
        if (names.isEmpty()) {
            collectNames(player.getNationID() + ".settlementName."
                + player.getSpecification().getId() + ".", names);
        }

        return names;
    }

    /**
     * Gets a list of ship names and a fallback prefix for a player.
     *
     * @param player The <code>Player</code> to get names for.
     * @return A list of ship names, with the first being the fallback prefix.
     */
    public static List<String> getShipNames(Player player) {
        final String prefix = player.getNationID() + ".ship.";
        List<String> names = new ArrayList<String>();

        // Fallback prefix first
        names.add(message("Ship"));

        // Collect the rest
        collectNames(prefix, names);
        return names;
    }

    /**
     * Loads a new resource file into the current message bundle.
     *
     * @param resourceFile
     */
    public static void loadResources(File resourceFile) {

        if ((resourceFile != null) && resourceFile.exists()
            && resourceFile.isFile() && resourceFile.canRead()) {
            try {
                loadResources(new FileInputStream(resourceFile));
            } catch (Exception e) {
                logger.warning("Unable to load resource file " + resourceFile.getPath());
            }
        }
    }



    /**
     * Loads a new resource file into the current message bundle.
     *
     * @param is an <code>InputStream</code> value
     */
    public static void loadResources(InputStream is) {
        try {
            InputStreamReader inputReader = new InputStreamReader(is, "UTF-8");
            BufferedReader in = new BufferedReader(inputReader);

            String line = null;
            while((line = in.readLine()) != null) {
                line = line.trim();
                int index = line.indexOf('#');
                if (index == 0) {
                    continue;
                }
                index = line.indexOf('=');
                if (index > 0) {
                    String key = line.substring(0, index).trim();
                    String value = line.substring(index + 1).trim()
                        .replace("\\n", "\n").replace("\\t", "\t");
                    messageBundle.put(key, value);
                }
            }
        } catch (Exception e) {
            logger.warning("Unable to load resources from input stream.");
        }
    }

    /**
     * Breaks a line between two words. The breaking point
     * is as close to the center as possible.
     *
     * @param string The line for which we should determine a
     *               breaking point.
     * @return The best breaking point or <code>-1</code> if there
     *         are none.
     */
     public static int getBreakingPoint(String string) {
         int center = string.length() / 2;
         for (int offset = 0; offset < center; offset++) {
             if (string.charAt(center + offset) == ' ') {
                 return center + offset;
             } else if (string.charAt(center - offset) == ' ') {
                 return center - offset;
             }
         }
         return -1;
     }
}
