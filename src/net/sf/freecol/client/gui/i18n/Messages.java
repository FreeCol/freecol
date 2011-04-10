/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

/**
 * Represents a collection of messages in a particular locale. <p/>
 *
 * This class is NOT thread-safe. (CO: I cannot find any place that really has a
 * problem) <p/>
 *
 * Messages are put in the file "FreeColMessages.properties". This file is
 * presently located in the same directory as the source file of this class.
 */
public class Messages {

    private static final Logger logger = Logger.getLogger(Messages.class.getName());

    public static final String STRINGS_DIRECTORY = "strings";

    public static final String FILE_PREFIX = "FreeColMessages";

    public static final String FILE_SUFFIX = ".properties";

    private static Map<String, String> messageBundle =
        new HashMap<String, String>();

    private static Number grammaticalNumber = NumberRules.OTHER_NUMBER_RULE;



    public static void setGrammaticalNumber(Number number) {
        grammaticalNumber = number;
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
                               + stringDirectory.getName());
            }
        }

        Number number = NumberRules.getNumberForLanguage(language);
        if (number != null) {
            grammaticalNumber = number;
        }

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
                                  + fcmf.getModInfo().getName() + ".");
                } catch (IOException e) {
                    logger.fine("No message bundle " + fileName + " in "
                                + fcmf.getModInfo().getName() + ".");
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
     * Finds the message with a particular ID in the default locale and performs
     * string replacements.
     *
     * @param messageId The key of the message to find
     * @param data consists of pairs of strings, each time the first of the pair
     *       is replaced by the second in the messages.
     */
    public static String message(String messageId, String... data) {
        // Check that all the values are correct.
        if (messageId == null) {
            throw new NullPointerException();
        }
        if (data!=null && data.length % 2 != 0) {
            throw new IllegalArgumentException("Programming error, the data should consist of only pairs.");
        }
        if (messageBundle == null) {
            setMessageBundle(Locale.getDefault());
        }

        String message = messageBundle.get(messageId);
        if (message == null) {
            return messageId;
        }

        if (data != null && data.length > 0) {
            for (int i = 0; i < data.length; i += 2) {
                if (data[i] == null || data[i+1] == null) {
                    throw new IllegalArgumentException("Programming error, no data should be <null>.");
                }
                message = message.replace(data[i], data[i+1]);
            }
        }
        message = replaceChoices(message);
        return message.trim();
    }

    public static String replaceChoices(String input) {
        return replaceChoices(input, null);
    }

    public static String replaceChoices(String input, StringTemplate template) {
        int openChoice = 0;
        int closeChoice = 0;
        int highWaterMark = 0;
        StringBuilder result = new StringBuilder();
        while ((openChoice = input.indexOf("{{", highWaterMark)) >= 0) {
            result.append(input.substring(highWaterMark, openChoice));
            closeChoice = input.indexOf("}}", openChoice + 2);
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
                        if ("plural".equalsIgnoreCase(tag)) {
                            selector = grammaticalNumber.getKey(selector);
                        }
                    }
                }
            } else if ("plural".equalsIgnoreCase(tag)) {
                selector = grammaticalNumber.getKey(selector);
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
                    logger.warning("Unknown key or untagged choice: " + otherKey);
                    continue;
                }
            } else {
                int start = keyIndex + selector.length() + 1;
                int replacementIndex = input.indexOf("|", start);
                if (replacementIndex < 0 || replacementIndex > closeChoice) {
                    // must be last choice
                    result.append(input.substring(start, closeChoice));
                } else {
                    result.append(input.substring(start, replacementIndex));
                }
            }
        }
        result.append(input.substring(highWaterMark));
        return result.toString();
    }

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
     * Localizes a StringTemplate.
     *
     * @param template a <code>StringTemplate</code> value
     * @return a <code>String</code> value
     */
    public static String message(StringTemplate template) {
        String result = "";
        switch (template.getTemplateType()) {
        case LABEL:
            if (template.getReplacements() == null) {
                return message(template.getId());
            } else {
                for (StringTemplate other : template.getReplacements()) {
                    result += template.getId() + message(other);
                }
                if (result.length() > template.getId().length()) {
                    return result.substring(template.getId().length());
                } else {
                    logger.warning("Incorrect use of template with id " + template.getId());
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
     * @return The given unit type as a String
     */
    public static String getLabel(UnitType someType, Unit.Role someRole) {
        String key = someRole.toString().toLowerCase();
        if (someRole == Unit.Role.DEFAULT) {
            key = "name";
        }
        String messageID = someType.getId() +  "." + key;
        if (containsKey(messageID)) {
            return message(messageID);
        } else {
            return message("model.unit." + key + ".name", "%unit%",
                           Messages.message(someType.getNameKey()));
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
            return message("model.unit." + key + ".name", "%unit%",
                           Messages.message(unit.getId() + ".name"));
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
     * Gets a list of settlement names and a fallback prefix for a player.
     *
     * @param player The <code>Player</code> to get names for.
     * @return A list of settlement names, with the first being the
     *     fallback prefix.
     */
    public static List<String> getSettlementNames(Player player) {
        final String prefix = player.getNationID() + ".settlementName.";
        List<String> names = new ArrayList<String>();

        // Fallback prefix first
        names.add(message((player.isEuropean()) ? "Colony" : "Settlement"));

        // Collect all the names
        int i = 0;
        while (Messages.containsKey(prefix + Integer.toString(i))) {
            names.add(Messages.message(prefix + Integer.toString(i)));
            i++;
        }

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


}
