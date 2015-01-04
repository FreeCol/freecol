/**
 *  Copyright (C) 2002-2015  The FreeCol Team
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

import java.io.File;
import java.io.FileInputStream;

import net.sf.freecol.common.i18n.Number.Category;
import net.sf.freecol.util.test.FreeColTestCase;


public class CLDRTest extends FreeColTestCase {

    public void testRuleParsing() {
        Rule rule = new Rule("n mod 10 in 2..4 and n mod 100 not in 12..14");
        assertTrue(rule.matches(2));
        assertTrue(rule.matches(102));
        assertTrue(rule.matches(103));
        assertFalse(rule.matches(1));
        assertFalse(rule.matches(5));
        assertFalse(rule.matches(112));
        
        DefaultNumberRule arabic = new DefaultNumberRule();
        arabic.addRule(Category.zero, "n is 0");
        arabic.addRule(Category.one, "n is 1");
        arabic.addRule(Category.two, "n is 2");
        arabic.addRule(Category.few, "N Mod 100 in 3.. 10");
        arabic.addRule(Category.many, "n MOD 100 in 11   ..99");
        
        assertEquals(Category.zero, arabic.getCategory(0));
        assertEquals(Category.one, arabic.getCategory(1));
        assertEquals(Category.two, arabic.getCategory(2));
        assertEquals(Category.few, arabic.getCategory(3));
        assertEquals(Category.few, arabic.getCategory(7));
        assertEquals(Category.few, arabic.getCategory(10));
        assertEquals(Category.many, arabic.getCategory(11));
        assertEquals(Category.many, arabic.getCategory(99));
        assertEquals(Category.many, arabic.getCategory(2345));
    }

    public void testPlurals() {
        FileInputStream in = null;
        File inputFile = new File("data/strings/plurals.xml");
        assertTrue(inputFile.exists());
        try {
            in = new FileInputStream(inputFile);
        } catch (Exception e) {
            fail("Failed to open input stream.");
        }
        NumberRules numberRules = new NumberRules(in);

        try {
            in.close();
        } catch (Exception e) {
            fail("Failed to close input stream.");
        }

        assertNotNull(NumberRules.getNumberForLanguage("az"));
        assertTrue(NumberRules.getNumberForLanguage("az") instanceof OtherNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("ko"));
        assertTrue(NumberRules.getNumberForLanguage("ko") instanceof OtherNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("th"));
        assertTrue(NumberRules.getNumberForLanguage("th") instanceof OtherNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("en"));
        assertTrue(NumberRules.getNumberForLanguage("en") instanceof PluralNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("it"));
        assertTrue(NumberRules.getNumberForLanguage("it") instanceof PluralNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("pt"));
        assertTrue(NumberRules.getNumberForLanguage("pt") instanceof PluralNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("ak"));
        assertTrue(NumberRules.getNumberForLanguage("ak") instanceof ZeroOneNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("hi"));
        assertTrue(NumberRules.getNumberForLanguage("hi") instanceof ZeroOneNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("wa"));
        assertTrue(NumberRules.getNumberForLanguage("wa") instanceof ZeroOneNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("ga"));
        assertTrue(NumberRules.getNumberForLanguage("ga") instanceof DualNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("smi"));
        assertTrue(NumberRules.getNumberForLanguage("smi") instanceof DualNumberRule);

        assertNotNull(NumberRules.getNumberForLanguage("sms"));
        assertTrue(NumberRules.getNumberForLanguage("sms") instanceof DualNumberRule);
    }
}
