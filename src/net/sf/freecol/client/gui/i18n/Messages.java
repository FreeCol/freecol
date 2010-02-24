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

package net.sf.freecol.client.gui.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.model.StringTemplate;
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

    private static Properties messageBundle = null;

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

        messageBundle = new Properties();

        if (!language.equals("")) {
            language = "_" + language;
        }
        if (!country.equals("")) {
            country = "_" + country;
        }
        if (!variant.equals("")) {
            variant = "_" + variant;
        }
        String[] fileNames = { FILE_PREFIX + FILE_SUFFIX, FILE_PREFIX + language + FILE_SUFFIX,
                               FILE_PREFIX + language + country + FILE_SUFFIX,
                               FILE_PREFIX + language + country + variant + FILE_SUFFIX };

        for (String fileName : fileNames) {
            File resourceFile = new File(getI18nDirectory(), fileName);
            loadResources(resourceFile);
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
 
        String message = messageBundle.getProperty(messageId);
        if (message == null) {
            return messageId;
        }

        if (data!=null && data.length > 0) {
            for (int i = 0; i < data.length; i += 2) {
                if (data[i] == null || data[i+1] == null) {
                    throw new IllegalArgumentException("Programming error, no data should be <null>.");
                }
                // if there is a $ in the substituting string, a IllegalArgumentException will be raised
                //we need to escape it first
                String escapedStr = data[i+1].replaceAll("\\$","\\\\\\$");
                message = message.replaceAll(data[i], escapedStr);
            }
        }
        return message.trim();
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
                result = message(template.getId());
            } else if (template.getDefaultId() != null) {
                result = message(template.getDefaultId());
            }
	    for (int index = 0; index < template.getKeys().size(); index++) {
                result = result.replace(template.getKeys().get(index),
                                        message(template.getReplacements().get(index)));
	    }
	    return result;
        case KEY:
            return message(template.getId());
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
        return (messageBundle.getProperty(key) != null);
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
     * Calling this method can be used to replace the messages used currently
     * with a new bundle. This is used only in the debugging of FreeCol.
     * 
     * @param resourceFile
     */
    public static void loadResources(File resourceFile) {

        if ((resourceFile != null) && resourceFile.exists() && resourceFile.isFile() && resourceFile.canRead()) {
            try {
                messageBundle.load(new FileInputStream(resourceFile));
            } catch (Exception e) {
                logger.warning("Unable to load resource file " + resourceFile.getPath());
            }
        }
    }

}
