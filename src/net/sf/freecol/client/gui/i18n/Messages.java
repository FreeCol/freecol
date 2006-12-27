
package net.sf.freecol.client.gui.i18n;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;


/**
 * Represents a collection of messages in a particular locale.
 * <P>
 * 
 * This class is NOT thread-safe.
 *
 * <br><br>
 *
 * Messages are put in the file "FreeColMessages.properties".
 * This file is presently located in the same directory as
 * the source file of this class.
 */
public final class Messages {

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final Logger logger = Logger.getLogger(Messages.class.getName());

    /**
     * Construction of this map is avoided if only the default locale is used,
     * i.e. on the client
     */
    private static Map  messagesByLocale;

    /**
     * These are the messages for the default locale.
     */
    private static Messages  defaultMessages;

    private static PropertyResourceBundle  resources;


    /**
     * Convenience method that finds the message with a particular id in the
     * default locale.
     *
     * @param  messageId  the id of the message to find
     * @return  the message with the specified id
     */
    public static String message(String messageId) {

        if (null == defaultMessages) {
            defaultMessages = new Messages(Locale.getDefault());
        }

        return defaultMessages.findMessage(messageId);
    }

    /**
     * Convenience method that finds the message with a particular 
     * ID in the default locale and performs string replacements.
     *
     * @param messageId The key of the message to find
     * @param data Every occuranse of <code>data[x][0]</code> is replaced with
     *       <code>data[x][1]</code> for every <code>x</code>.
     * @return The message with the specified id
     */
    public static String message(String messageId, String[][] data) {

        if (null == defaultMessages) {
            defaultMessages = new Messages(Locale.getDefault());
        }

        String message = defaultMessages.findMessage(messageId);
        if (data != null) {
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == null) {
                        logger.warning("Data has a wrong format for message: " + message);
                    } else if (data[i][0] == null || data[i][1] == null) {
                        logger.warning("Data in model message is 'null': " +
                                       message + ", " + data[i][0] + ", " + data[i][1]);
                    } else {
                        message = message.replaceAll(data[i][0], data[i][1]);
                    }
                }
        }

        return message;
    }


    public static Messages messagesForLocale(Locale locale) {

        if (null == messagesByLocale)
            messagesByLocale = new HashMap();

        Messages  m = (Messages)messagesByLocale.get(locale);

        if (null == m) {

            boolean  instanceAvailable =
                Locale.getDefault().equals(locale)
                &&  defaultMessages != null;

            m = instanceAvailable ? defaultMessages : new Messages(locale);

            messagesByLocale.put(locale, m);
        }

        return m;
    }


    /**
     * Private constructor: This class is a Singleton.
     */
    private Messages(Locale locale) {

        /* this palaver is only necessary because Class.getPackage() can return
         * null */
        String packageName = getClass().getName().substring(0, getClass().getName().lastIndexOf('.'));
        resources = (PropertyResourceBundle) ResourceBundle.getBundle(packageName + ".FreeColMessages", locale);

    }

    public static void setResources(PropertyResourceBundle bundle) {
        resources = bundle;
    }


    public String findMessage(String messageId) {
        if (messageId == null) {
            throw new NullPointerException();
        }
        
        return resources.getString(messageId);
    }

}
