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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Number.Category;
import net.sf.freecol.common.io.FreeColXMLReader;


/**
 * See the
 * <a href="http://cldr.unicode.org/index/cldr-spec/plural-rules">
 * Common Locale Data Repository</a>.
 */
public class NumberRules {

    private static final Logger logger = Logger.getLogger(NumberRules.class.getName());

    /**
     * A rule that always returns category "other".
     */
    public static final Number OTHER_NUMBER_RULE = new OtherNumberRule();

    /**
     * A rule that assigns 1 to category "one", 2 to category "two"
     * and all other numbers to category "other".
     */
    public static final Number DUAL_NUMBER_RULE = new DualNumberRule();

    /**
     * A rule that assigns 1 to category "one" and all other numbers
     * to category "other".
     */
    public static final Number PLURAL_NUMBER_RULE = new PluralNumberRule();

    /**
     * A rule that assigns 0 and 1 to category "one", and all other
     * number to category "other".
     */
    public static final Number ZERO_ONE_NUMBER_RULE = new ZeroOneNumberRule();


    private static final Map<String, Number> numberMap = new HashMap<>();


    /**
     * Creates a new <code>NumberRules</code> instance from the given
     * input stream, which must contain an XML representation of the
     * CLDR plural rules.
     *
     * @param in an <code>InputStream</code> value
     */
    public NumberRules(InputStream in) {
        load(in);
    }


    /**
     * Returns a rule appropriate for the given language, or the
     * OTHER_NUMBER_RULE if none has been defined.
     *
     * @param lang a <code>String</code> value
     * @return a <code>Number</code> value
     */
    public static Number getNumberForLanguage(String lang) {
        Number number = numberMap.get(lang);
        return (number == null) ? OTHER_NUMBER_RULE : number;
    }

    public static boolean isInitialized() {
        return !numberMap.isEmpty();
    }


    public static void load(InputStream in) {
        try (
            FreeColXMLReader xr = new FreeColXMLReader(in);
        ) {
            readFromXML(xr);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Load parse", e);
            throw new RuntimeException("Error parsing number rules.", e);
        }
    }


    // Serialization

    private static final String COUNT_TAG = "count";
    private static final String GENERATION_TAG = "generation";
    private static final String LOCALES_TAG = "locales";
    private static final String PLURALS_TAG = "plurals";
    private static final String PLURAL_RULE_TAG = "pluralRule";
    private static final String PLURAL_RULES_TAG = "pluralRules";
    private static final String VERSION_TAG = "version";


    private static void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = xr.getLocalName();
            if (null != tag) switch (tag) {
                case VERSION_TAG:
                    xr.nextTag();
                    break;
                case GENERATION_TAG:
                    xr.nextTag();
                    break;
                case PLURALS_TAG:
                    while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                        tag = xr.getLocalName();
                        if (PLURAL_RULES_TAG.equals(tag)) {
                            readChild(xr);
                        }
                    }   break;
            }
        }
    }

    private static void readChild(FreeColXMLReader xr) throws XMLStreamException {
        String loc = xr.getAttribute(LOCALES_TAG, (String)null);
        String[] locales = (loc == null) ? null : loc.split(" ");
        if (locales != null) {
            DefaultNumberRule numberRule = new DefaultNumberRule();
            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if (PLURAL_RULE_TAG.equals(xr.getLocalName())) {
                    String plu = xr.getAttribute(COUNT_TAG, (String)null);
                    Category category = Category.valueOf(plu);
                    Rule rule = new Rule(xr.getElementText());
                    numberRule.addRule(category, rule);
                }
            }
            Number number = null;
            switch(numberRule.countRules()) {
            case 0:
                number = OTHER_NUMBER_RULE;
                break;
            case 1:
                Rule rule = numberRule.getRule(Category.one);
                if (rule != null) {
                    if (null != rule.toString()) switch (rule.toString()) {
                    case "n is 1":
                        number = PLURAL_NUMBER_RULE;
                        break;
                    case "n in 0..1":
                        number = ZERO_ONE_NUMBER_RULE;
                        break;
                }
                }
                break;
            case 2:
                Rule oneRule = numberRule.getRule(Category.one);
                Rule twoRule = numberRule.getRule(Category.two);
                if (oneRule != null
                    && "n is 1".equals(oneRule.toString())
                    && twoRule != null
                    && "n is 2".equals(twoRule.toString())) {
                    number = DUAL_NUMBER_RULE;
                }
                break;
            default:
                number = numberRule;
            }
            for (String locale : locales) {
                numberMap.put(locale, number);
            }
        }
    }
}
