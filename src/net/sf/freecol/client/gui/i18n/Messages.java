
package net.sf.freecol.client.gui.i18n;


import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;


/**
 * Represents a collection of messages in a particular locale.
 * <P>
 * 
 * This class is NOT thread-safe.
 */
public final class Messages {

    public static final String  COPYRIGHT = "Copyright (C) 2003 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";


    /**
     * Construction of this map is avoided if only the default locale is used,
     * i.e. on the client
     */
    private static Map  messagesByLocale;

    /**
     * These are the messages for the default locale
     */
    private static Messages  defaultMessages;

    private final PropertyResourceBundle  resources;


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

        return (String) defaultMessages.findMessage(messageId);
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


    public String findMessage(String messageId) {

        return resources.getString(messageId);
    }

}
