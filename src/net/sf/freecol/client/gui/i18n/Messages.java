package net.sf.freecol.client.gui.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

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

    public static final String DATA_DIR = "data/strings";
    public static final String FILE_PREFIX = "FreeColMessages";
    public static final String FILE_SUFFIX = ".properties";

    private static PropertyResourceBundle messageBundle;


    /**
     * Finds the message with a particular id in the default locale.
     * 
     * The first call to this method will load the messages from the property
     * file.
     * 
     * @param messageId the id of the message to find
     * @return the message with the specified id
     * 
     * @throws MissingResourceException if the given message could not be found.
     */
    public static String message(String messageId) {

        if (messageId == null) {
            throw new NullPointerException();
        }

        if (messageBundle == null) {
            messageBundle = getMessageBundle(Locale.getDefault());
        }

        return messageBundle.getString(messageId);
    }

    /**
     * Returns the resource bundle for the given locale
     * 
     * @param locale
     * @return The ResourceBundle containing the messages for the given locale.
     */
    public static PropertyResourceBundle getMessageBundle(Locale locale) {

        if (locale == null)
            throw new NullPointerException("Parameter locale may not be null");

        /*
         * this palaver is only necessary because Class.getPackage() can return
         * null
         */
        /*
        String packageName = Messages.class.getName().substring(0, Messages.class.getName().lastIndexOf('.'));
        PropertyResourceBundle bundle = (PropertyResourceBundle) ResourceBundle.getBundle(packageName
                + ".FreeColMessages", locale);
        */
        String language = locale.getLanguage();
        if (!language.equals("")) {
            language = "_" + language;
        }
        String country = locale.getCountry();
        if (!country.equals("")) {
            country = "_" + country;
        }
        String[] fileNames = {
            FILE_PREFIX + language + country + FILE_SUFFIX,
            FILE_PREFIX + language + FILE_SUFFIX,
            FILE_PREFIX + FILE_SUFFIX
        };

        PropertyResourceBundle bundle = null;
        for (String fileName : fileNames) {
            File resourceFile = new File(DATA_DIR, fileName);
            if ((resourceFile != null) && resourceFile.exists() && resourceFile.isFile() &&
                resourceFile.canRead()) {
                try {
                    bundle = new PropertyResourceBundle(new FileInputStream(resourceFile));
                    break;
                } catch(Exception e) {
                    logger.warning("Unable to load resource file " + fileName);
                }
            }
        }

        if (bundle == null) {
            logger.warning("Could not load resource bundle for the locale " + locale.getDisplayName());
        } else {
            if (!bundle.getLocale().equals(locale)) {
                logger.warning("Could not load resource bundle for locale '" + locale.getDisplayName()
                        + "' falling back to '" + bundle.getLocale().toString() + "'");
            }
        }

        return bundle;
    }

    /**
     * Finds the message with a particular ID in the default locale and performs
     * string replacements.
     * 
     * @param messageId The key of the message to find
     * @param data Every occuranse of <code>data[x][0]</code> is replaced with
     *            <code>data[x][1]</code> for every <code>x</code>.
     * @return The message with the specified id
     */
    public static String message(String messageId, String[][] data) {

        String message = Messages.message(messageId);

        if (data != null) {
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
     * @param bundle
     */
    public static void setResources(PropertyResourceBundle bundle) {
        messageBundle = bundle;
    }

}
