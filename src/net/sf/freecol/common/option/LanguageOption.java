package net.sf.freecol.common.option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Option for selecting a language. The possible choices are determined
 * using the available language files in "data/strings".
 */
public class LanguageOption extends SelectOption {
    private static Logger logger = Logger.getLogger(LanguageOption.class.getName());

    private static final String AUTO = "automatic";
    
    
    /**
     * Creates a new <code>LanguageOption</code>.
     *
     * @param id The identifier for this option. This is used when the object should be
     *           found in an {@link OptionGroup}.
     */
    public LanguageOption(String id, String name, String shortDescription) {
        super(id, name, shortDescription, getLanguageNames(), 0, true);
    }


     
     /**
      * Returns the <code>Locale</code> chosen by this option.
      * @return The <code>Locale</code>.
      */
     private Locale getLocale() {
         return getLocale(value);
     }
     
     /**
      * Returns the <code>Locale</code> given by the value.
      * 
      * @param value The value deciding the Locale.
      * @return The <code>Locale</code>.
      */
     public static Locale getLocale(int value) {
         if (value == 0) {
             return Locale.getDefault();    
         }

         int i = 1;
         File i18nDirectory = new File(FreeCol.getDataDirectory(), Messages.STRINGS_DIRECTORY);
         File[] files = i18nDirectory.listFiles();
         for (File file : files) {
             if (file.getName() == null) {
                 continue;
             }
             if (file.getName().startsWith(Messages.FILE_PREFIX + "_")) {
                 try {
                     if (value == i) {
                         final String languageID = file.getName().substring(16, file.getName().indexOf("."));
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
                         Locale l = new Locale(language, country, variant);
                         return l;
                     }
                     
                     i++;
                 } catch (Exception e) {
                     logger.log(Level.WARNING, "Exception in getLanguageNames()", e);
                     i++;
                     continue;
                 }
             }
         }
         return Locale.getDefault();
     }
     
     /**
      * Returns a list of the available translations.
      * @return The available translations in a human readable format.
      */
     private static String[] getLanguageNames() {
         List<String> names = new ArrayList<String>();
         
         names.add(Messages.message("clientOptions.gui.languageOption.autoDetectLanguage"));
         
         File i18nDirectory = new File(FreeCol.getDataDirectory(), Messages.STRINGS_DIRECTORY);
         File[] files = i18nDirectory.listFiles();
         if (files == null) {
        	 throw new RuntimeException("No language files could be found in the <" + i18nDirectory + 
        			 "> folder. Make sure you ran the ant correctly.");
         }
         for (File file : files) {
             if (file.getName() == null) {
                 continue;
             }
             if (file.getName().startsWith(Messages.FILE_PREFIX + "_")) {
                 try {
                     final String languageID = file.getName().substring(16, file.getName().indexOf("."));
                     names.add(getLocale(languageID).getDisplayName());
                 } catch (Exception e) {
                     logger.log(Level.WARNING, "Exception in getLanguageNames()", e);
                     continue;
                 }
             }
         }
         return names.toArray(new String[0]);
     }
     
     /**
      * Returns the language code.
      * @return language_country_variant
      */
     @Override
     protected String getStringValue() {
         if (value == 0) {
             return AUTO;
         }
         File i18nDirectory = new File(FreeCol.getDataDirectory(), Messages.STRINGS_DIRECTORY);
         File[] files = i18nDirectory.listFiles();
         int i = 1;
         for (File file : files) {
             if (file.getName() == null) {
                 continue;
             }
             if (file.getName().startsWith("FreeColMessages_")) {
                 try {
                     final String languageID = file.getName().substring(16, file.getName().indexOf("."));
                     if (value == i) {
                         return languageID;
                     }
                     i++;
                 } catch (Exception e) {
                     logger.log(Level.WARNING, "Exception in getLanguageNames()", e);
                     i++;
                     continue;
                 }
             }
         }
         return AUTO;
     }
     
     /**
      * Sets the value from a String.
      * 
      * @param stringValue A String using the same format as {@link #getStringValue()}.
      */
     @Override
     protected void setValue(String stringValue) {
         if (stringValue.equals(AUTO)) {
             value = 0;
         }
         File i18nDirectory = new File(FreeCol.getDataDirectory(), Messages.STRINGS_DIRECTORY);
         File[] files = i18nDirectory.listFiles();
         int i = 1;
         for (File file : files) {
             if (file.getName() == null) {
                 continue;
             }
             if (file.getName().startsWith("FreeColMessages_")) {
                 try {
                     final String languageID = file.getName().substring(16, file.getName().indexOf("."));
                     if (languageID.equals(stringValue)) {
                         value = i;
                     }
                     i++;
                 } catch (Exception e) {
                     logger.log(Level.WARNING, "Exception in getLanguageNames()", e);
                     i++;
                     continue;
                 }
             }
         }
     }
     
     /**
      * Returns the <code>Locale</code> decided by the given name. 
      * 
      * @param languageID A String using the same format as
      *         {@link #getStringValue()}.
      * @return The Locale.
      */
     public static Locale getLocale(String languageID) {
         if (languageID.equals(AUTO)) {
             return Locale.getDefault();
         }
         
         try {
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
         } catch (Exception e) {
             logger.log(Level.WARNING, "Cannot choose locale: " + languageID, e);
             return Locale.getDefault();
         }
     }
}
