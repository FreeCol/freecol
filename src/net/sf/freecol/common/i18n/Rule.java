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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A rule consists of any number of relations combined with "and" and
 * "or" operators. The "and" operator binds more strongly, and there
 * are no grouping features.
 */
public class Rule {

    private final List<List<Relation>> conditions = new ArrayList<>();


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
     * The outer conditions are or-combined (using anyMatch), the
     * inner conditions are and-combined (using allMatch).
     *
     * @param number The number to test.
     * @return True if the number matches this rule.
     */
    public boolean matches(double number) {
        return any(conditions,
            andConditions -> all(andConditions, r -> r.matches(number)));
    }

    /**
     * Parses a string.
     *
     * @param input a <code>String</code> value
     */
    public final void parse(String input) {
        StringTokenizer st = new StringTokenizer(input.toLowerCase(Locale.US), " .");
        List<String> tokens = new ArrayList<>();
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
        List<String> tokens = new ArrayList<>();
        List<Relation> result = new ArrayList<>();
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
        final String andString = " and ";
        final String orString = " or ";
        StringBuilder sb = new StringBuilder(32);
        for (List<Relation> andCondition : conditions) {
            for (Relation relation : andCondition) {
                sb.append(relation).append(andString);
            }
            sb.setLength(sb.length() - andString.length());
            sb.insert(0, orString);
        }
        sb.delete(0, orString.length());
        return sb.toString();
    }
}
