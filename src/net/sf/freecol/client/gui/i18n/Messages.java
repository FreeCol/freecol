/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIManager;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.io.Mods;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.StringTemplate.TemplateType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.util.Utils;


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
 * Te individual messages may include variables, which must be
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

    public static final String MESSAGE_FILE_SUFFIX = ".properties";

    public static final String DESCRIPTION_SUFFIX = ".description";
    public static final String SHORT_DESCRIPTION_SUFFIX = ".shortDescription";
    public static final String NAME_SUFFIX = ".name";

    private static final String[] DESCRIPTION_KEYS = new String[] {
        DESCRIPTION_SUFFIX, SHORT_DESCRIPTION_SUFFIX, NAME_SUFFIX
    };

    /**
     * The mapping from language-independent key to localized message
     * for the established locale.
     */
    private static final Map<String, String> messageBundle
        = new HashMap<String, String>();

    /**
     * A map with Selector values and the tag keys used in choice
     * formats.
     */
    private static Map<String, Selector> tagMap
        = new HashMap<String, Selector>();
    static {
        tagMap.put("turn", new TurnSelector());
    }

    /** Extra river names. */
    private static List<String> otherRivers = null;


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
     * Set the message bundle for the given locale
     *
     * Error messages have to go to System.err as this routine is called
     * before logging is enabled.
     *
     * @param locale The <code>Locale</code> to set resources for.
     */
    public static void setMessageBundle(Locale locale) {
        messageBundle.clear(); // Reset the message bundle.

        if (!Locale.getDefault().equals(locale)) {
            Locale.setDefault(locale);
        }

        File i18nDirectory = FreeColDirectories.getI18nDirectory();
        if (i18nDirectory == null) return;

        if (!NumberRules.isInitialized()) {
            // attempt to read grammatical rules
            File cldr = new File(i18nDirectory, "plurals.xml");
            if (cldr.exists()) {
                try {
                    FileInputStream in = new FileInputStream(cldr);
                    NumberRules.load(in);
                    in.close();
                } catch (Exception e) {
                    System.err.println("Failed to read CLDR rules: "
                        + e.getMessage());
                }
            } else {
                System.err.println("Could not find CLDR rules: "
                    + cldr.getPath());
            }
        }

        setGrammaticalNumber(NumberRules.getNumberForLanguage(locale.getLanguage()));
        for (String name : getMessageFileNames(locale)) {
            File file = new File(i18nDirectory, name);
            if (!file.exists()) continue; // Expected
            try {
                loadMessages(new FileInputStream(file));
            } catch (Exception e) {
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
     * Load localized messages for mods.
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
    public static void setModMessageBundle(Locale locale) {
        List<FreeColModFile> allMods = new ArrayList<FreeColModFile>();
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


    public static String getName(ObjectWithId object) {
        return getName(object.getId());
    }

    public static String getDescription(ObjectWithId object) {
        return getDescription(object.getId());
    }

    public static String getShortDescription(ObjectWithId object) {
        return getShortDescription(object.getId());
    }

    public static String getBestDescription(ObjectWithId object) {
        return getBestDescription(object.getId());
    }

    public static String getName(String id) {
        return message(id + NAME_SUFFIX);
    }

    public static String getDescription(String id) {
        return message(id + DESCRIPTION_SUFFIX);
    }

    public static String getShortDescription(String id) {
        return message(id + SHORT_DESCRIPTION_SUFFIX);
    }

    public static String getBestDescription(String id) {
        for (String suffix : DESCRIPTION_KEYS) {
            String key = id + suffix;
            if (containsKey(key)) {
                return message(key);
            }
        }
        return id;
    }

    /**
     * Get a label for a unit given the type and role identifiers and
     * the number of units.  This is the fundamental routine called
     * from Unit and AbstractUnit.getLabel().
     *
     * @param typeId The unit type identifier.
     * @param roleId The unit role identifier.
     * @param number The number of units.
     * @return A <code>StringTemplate</code> to describe the given unit.
     */
    public static StringTemplate getLabel(String typeId, String roleId,
                                          int number) {
        // Check for special role-specific key, which will not have a
        // %role% argument.  These exist so we can avoid mentioning
        // the role twice, e.g. "Seasoned Scout Scout".
        String baseKey = typeId + "." + Role.getRoleSuffix(roleId);
        StringTemplate result;
        if (containsKey(baseKey)) {
            return StringTemplate.label("")
                .addStringTemplate(StringTemplate.template(baseKey)
                    .addAmount("%number%", number));
        }
        StringTemplate baseTemplate
            = StringTemplate.template(typeId + ".name")
                .addAmount("%number%", number);
        StringTemplate roleTemplate = StringTemplate.label("");
        String roleKey = Role.getRoleKey(roleId);
        if (roleKey != null) roleTemplate.addName(" ").add(roleKey);
        return StringTemplate.label("")
            .addStringTemplate(baseTemplate)
            .addStringTemplate(roleTemplate);
    }

    public static String getLabel(Unit unit) {
        return message(getLabelTemplate(unit));
    }

    public static StringTemplate getLabelTemplate(Unit unit) {
        UnitType type = unit.getType();
        Role role = unit.getRole();
        Role defaultRole = unit.getSpecification().getRole("model.role.default");

        String nationName = getName(unit.getOwner().getNation());
        String unitName = getName(type);
        String roleName = getName(role);
        String extra = unitName;
        boolean showRole = true;

        String key = type.getId() + "." + role.getSuffix();
        if (role.getMaximumCount() == 1 && containsKey(key)) {
            // first, check for special unit/role combinations (ignore
            // pioneer for historical reasons)
            showRole = false;
            unitName = message(StringTemplate.template(key)
                               .addAmount("%number%", 1));
        } else if (defaultRole == role) {
            if (unit.canCarryTreasure()) {
                // treasure trains display amount of gold
                roleName = unitName;
                extra = message(StringTemplate.template("goldAmount")
                                .addAmount("%amount%", unit.getTreasureAmount()));
            } else {
                boolean noEquipment = false;
                // unequipped expert has no-equipment label
                List<Role> expertRoles = type.getExpertRoles();
                for (Role someRole : expertRoles) {
                    if (containsKey(someRole.getId() + ".noequipment")) {
                        roleName = unitName;
                        extra = message(someRole.getId() + ".noequipment");
                        noEquipment = true;
                        break;
                    }
                }
                if (!noEquipment) {
                    // no extra label
                    showRole = false;
                }
            }
        } else if (role.getExpertUnit() == type) {
            // expert equipped as expert has no additional label,
            // unless maximum equipment count is greater than one
            if (role.getMaximumCount() > 1) {
                List<AbstractGoods> requiredGoods = role.getRequiredGoods();
                if (!requiredGoods.isEmpty()) {
                    int count = unit.getEquipmentCount();
                    AbstractGoods goods = requiredGoods.get(0);
                    roleName = unitName;
                    extra = message(StringTemplate.template("model.goods.goodsAmount")
                                    .addAmount("%amount%", goods.getAmount() * count)
                                    .addName("%goods%", goods.getType()));
                }
            } else {
                showRole = false;
            }
        }

        StringTemplate result = null;
        if (unit.getName() == null) {
            if (showRole) {
                result = StringTemplate.template("model.unit.nationUnitRole");
            } else {
                result = StringTemplate.template("model.unit.nationUnit");
            }
        } else {
            if (showRole) {
                result = StringTemplate.template("model.unit.namedNationUnitRole");
            } else {
                result = StringTemplate.template("model.unit.namedNationUnit");
            }
            result.addName("%name%", unit.getName());
        }
        result.addName("%nation%", nationName)
            .addName("%unit%", unitName)
            .addName("%role%", roleName)
            .addName("%extra%", extra);
        return result;
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

    /**
     * Get the new land name for a player.
     *
     * @param player The <code>Player</code> to query.
     * @return The new land name of a player.
     */
    public static String getNewLandName(Player player) {
        return (player.getNewLandName() == null)
            ? message(player.getNationId() + ".newLandName")
            : player.getNewLandName();
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
        // Try national names first.
        net.sf.freecol.common.model.Map map = player.getGame().getMap();
        int index = player.getNameIndex(regionType.getNameIndexKey());
        if (index < 1) index = 1;
        String prefix = player.getNationId() + ".region."
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

        // There are a bunch of extra rivers not attached to a specific
        // nation at model.other.region.river.*.
        if (name == null && regionType == RegionType.RIVER) {
            if (otherRivers == null) {
                otherRivers = new ArrayList<String>();
                collectNames("model.other.region.river.", otherRivers);
                // Does not need to use player or system PRNG
                Collections.shuffle(otherRivers);
            }
            while (!otherRivers.isEmpty()) {
                name = otherRivers.remove(0);
                if (map.getRegionByName(name) == null) return name;
            }
            name = null;
        }

        // Fall back to generic names.
        if (name == null) {
            String rtype = "model.region."
                + regionType.toString().toLowerCase(Locale.US) + ".name";
            do {
                name = message(StringTemplate.template("model.region.default")
                    .addStringTemplate("%nation%", player.getNationName())
                    .add("%type%", rtype)
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

        collectNames(player.getNationId() + ".settlementName.", names);

        // Try the spec-qualified version.
        if (names.isEmpty()) {
            collectNames(player.getNationId() + ".settlementName."
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
        final String prefix = player.getNationId() + ".ship.";
        List<String> names = new ArrayList<String>();

        // Fallback prefix first
        names.add(message("Ship"));

        // Collect the rest
        collectNames(prefix, names);
        return names;
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
            if (!token.equals("_")) {
                country = token;
            }
            if (st.hasMoreTokens()) {
                token = st.nextToken();
                if (token.equals("_") && st.hasMoreTokens()) {
                    token = st.nextToken();
                }
                if (!token.equals("_")) {
                    variant = token;
                }
            }
        }
        return new Locale(language, country, variant);
    }
}
