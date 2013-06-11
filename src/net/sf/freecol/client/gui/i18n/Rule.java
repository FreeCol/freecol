/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;


/**
 * A rule consists of any number of relations combined with "and" and
 * "or" operators. The "and" operator binds more strongly, and there
 * are no grouping features.
 */
public class Rule {

    List<List<Relation>> conditions = new ArrayList<List<Relation>>();

    public Rule(String input) {
	parse(input);
    }


    /**
     * Adds a list of relations combined with the "and" operator.
     *
     * @param condition a list of relations combined with the "and" operator
     */
    public void add(List<Relation> condition) {
	conditions.add(condition);
    }

    /**
     * Returns true if this rule matches the given number.
     *
     * @param number a <code>double</code> value
     * @return a <code>boolean</code> value
     */
    public boolean matches(double number) {

	outer: for (List<Relation> andCondition : conditions) {
	    for (Relation relation : andCondition) {
		if (!relation.matches(number)) {
		    continue outer;
		}
	    }
	    return true;
	}
	return false;

    }

    /**
     * Parses a string.
     *
     * @param input a <code>String</code> value
     */
    public void parse(String input) {
	StringTokenizer st = new StringTokenizer(input.toLowerCase(Locale.US), " .");
	List<String> tokens = new ArrayList<String>();
	while (st.hasMoreTokens()) {
	    String token = st.nextToken();
	    if ("or".equals(token)) {
		conditions.add(parseCondition(tokens));
		tokens.clear();
	    } else {
		tokens.add(token);
	    }
	}
	conditions.add(parseCondition(tokens));
    }

    private List<Relation> parseCondition(List<String> input) {
	List<String> tokens = new ArrayList<String>();
	List<Relation> result = new ArrayList<Relation>();
	for (String token : input) {
	    if ("and".equals(token)) {
		result.add(new Relation(tokens));
		tokens.clear();
	    } else {
		tokens.add(token);
	    }
	}
	result.add(new Relation(tokens));
	return result;
    }

    @Override
    public String toString() {

	String result = new String();
	for (List<Relation> andCondition : conditions) {
	    String condition = new String();
	    for (Relation relation : andCondition) {
		condition += " and " + relation.toString();
	    }
	    result += " or " + condition.substring(5);
	}
	return result.substring(4);

    }


}
