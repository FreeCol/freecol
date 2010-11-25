/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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


import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles zero or one rule for each number category.
 */
public class DefaultNumberRule extends Number {

    Map<Category, Rule> rules = new EnumMap<Category, Rule>(Category.class);


    /**
     * Add a parsed rule for category.
     *
     * @param number a <code>Category</code> value
     * @param rule a <code>Rule</code> value
     */
    public void addRule(Category number, Rule rule) {
	rules.put(number, rule);
    }

    /**
     * Add an unparsed rule for category.
     *
     * @param number a <code>Category</code> value
     * @param input a <code>String</code> value
     */
    public void addRule(Category number, String input) {
	rules.put(number, new Rule(input));
    }

    /**
     * Return the number of rules added.
     *
     * @return an <code>int</code> value
     */
    public int countRules() {
        return rules.values().size();
    }

    /**
     * Return the rule for the given category.
     *
     * @param category a <code>Category</code> value
     * @return a <code>Rule</code> value
     */
    public Rule getRule(Category category) {
        return rules.get(category);
    }

    public Category getCategory(double input) {

	for (Category number : Category.values()) {
	    Rule r = rules.get(number);
	    if (r != null && r.matches(input)) {
		return number;
	    }
	}
	return Category.other;
    }

    public int getIndex(double input) {

	int index = 0;
	for (Category number : Category.values()) {
	    Rule r = rules.get(number);
	    if (r != null) {
		if (r.matches(input)) {
		    return index;
		} else {
		    index++;
		}
	    }
	}
	return index;

    }

}