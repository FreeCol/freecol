package net.sf.freecol.client.gui.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.Properties;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;

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

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Messages.class.getName());

    public static final String STRINGS_DIRECTORY = "strings";
    public static final String FILE_PREFIX = "FreeColMessages";
    public static final String FILE_SUFFIX = ".properties";

    private static Properties messageBundle = null;


    /**
     * Finds the message with a particular id in the default locale.
     * 
     * The first call to this method will load the messages from the property
     * file.
     * 
     * @param messageId the id of the message to find
     * @return the message with the specified id or null if not found.
     * 
     * @throws NullPointerException if messageId is null.
     */
    public static String message(String messageId) {
        if (messageId == null) {
            throw new NullPointerException();
        }

        if (messageBundle == null) {
            setMessageBundle(Locale.getDefault());
        }

        return messageBundle.getProperty(messageId);
    }

    /**
     * Returns the resource bundle for the given locale
     * 
     * @param locale
     * @return The ResourceBundle containing the messages for the given locale.
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
     * Returns the resource bundle for the given locale
     * 
     * @param language The language for this locale.
     * @param country The language for this locale.
     * @param variant The variant for this locale.
     * @return The ResourceBundle containing the messages for the given locale.
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
        String[] fileNames = {
            FILE_PREFIX + FILE_SUFFIX,
            FILE_PREFIX + language + FILE_SUFFIX,
            FILE_PREFIX + language + country + FILE_SUFFIX,
            FILE_PREFIX + language + country + variant + FILE_SUFFIX
        };

        for (String fileName : fileNames) {
            File resourceFile = new File(getI18nDirectory(), fileName);
            loadResources(resourceFile);
        }
    }

    public static File getI18nDirectory() {
        return new File(FreeCol.getDataDirectory(), STRINGS_DIRECTORY);
    }

    /**
     * Finds the message with a particular ID in the default locale and performs
     * string replacements.
     * 
     * @param messageId The key of the message to find
     * @param data Every occuranse of <code>data[x][0]</code> is replaced with
     *            <code>data[x][1]</code> for every <code>x</code>.
     * @return The message with the specified id or null if message could not be found.
     * 
     * @throws NullPointerException if messageId is null
     */
    public static String message(String messageId, String[][] data) {

        String message = Messages.message(messageId);

        if (data != null && message != null) {
            for (int i = 0; i < data.length; i++) {
                if (data[i] == null || data[i].length != 2) {
                    logger.warning("Data has a wrong format for message: " + message);
                } else if (data[i][0] == null || data[i][1] == null) {
                    logger.warning("Data in model message is 'null': " + message + ", " + data[i][0] + ", "
                            + data[i][1]);
                } else {
                    message = message.replaceAll(data[i][0], data[i][1]);
                }
            }
        }

        return message;
    }

    /**
     * Calling this method can be used to replace the messages used currently
     * with a new bundle. This is used only in the debugging of FreeCol.
     * 
     * @param resourceFile
     */
    public static void loadResources(File resourceFile) {

        if ((resourceFile != null) && resourceFile.exists() && resourceFile.isFile() &&
            resourceFile.canRead()) {
            try {
                messageBundle.load(new FileInputStream(resourceFile));
            } catch(Exception e) {
                logger.warning("Unable to load resource file " + resourceFile.getPath());
            }
        }
    }

}
