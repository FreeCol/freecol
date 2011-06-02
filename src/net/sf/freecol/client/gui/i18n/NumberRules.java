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

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Number.Category;

/**
 * See the
 * <a href="http://cldr.unicode.org/index/cldr-spec/plural-rules">
 * Common Locale Data Repository</a>.
 */
public class NumberRules {

    private static final Logger logger = Logger.getLogger(NumberRules.class.getName());

    public static Number OTHER_NUMBER_RULE = new OtherNumberRule();
    public static Number DUAL_NUMBER_RULE = new DualNumberRule();
    public static Number PLURAL_NUMBER_RULE = new PluralNumberRule();
    public static Number ZERO_ONE_NUMBER_RULE = new ZeroOneNumberRule();

    private static Map<String, Number> numberMap = new HashMap<String, Number>();

    public NumberRules(InputStream in) {
        load(in);
    }


    public static Number getNumberForLanguage(String lang) {
        return numberMap.get(lang);
    }

    public static boolean isInitialized() {
        return !numberMap.isEmpty();
    }


    public static void load(InputStream in) {

        try {
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(in);
            readFromXML(xsr);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.warning(sw.toString());
            throw new RuntimeException("Error parsing number rules.");
        }
    }

    private static void readFromXML(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String tag = in.getLocalName();
            if ("version".equals(tag)) {
                in.nextTag();
            } else if ("generation".equals(tag)) {
                in.nextTag();
            } else if ("plurals".equals(tag)) {
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    tag = in.getLocalName();
                    if ("pluralRules".equals(tag)) {
                        readChild(in);
                    }
                }
            }
        }
    }

    private static void readChild(XMLStreamReader in) throws XMLStreamException {

        String[] locales = in.getAttributeValue(null, "locales").split(" ");
        if (locales != null) {
            DefaultNumberRule numberRule = new DefaultNumberRule();
            while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                if ("pluralRule".equals(in.getLocalName())) {
                    Category category = Category.valueOf(in.getAttributeValue(null, "count"));
                    Rule rule = new Rule(in.getElementText());
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
                    if ("n is 1".equals(rule.toString())) {
                        number = PLURAL_NUMBER_RULE;
                    } else if ("n in 0..1".equals(rule.toString())) {
                        number = ZERO_ONE_NUMBER_RULE;
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