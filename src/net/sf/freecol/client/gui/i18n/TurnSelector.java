/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sf.freecol.common.model.Turn;


public class TurnSelector implements Selector {

    private static final String turn = "(SPRING|AUTUMN)?\\s*(\\d+)";
    private static final Pattern turnPattern = Pattern
        .compile(turn + "\\s*-\\s*" + turn + "|" + turn);


    /**
     * Transform the given string selector into a replacement key for
     * a choice format.
     *
     * @param selector a <code>String</code> value
     * @param template the <code>String</code> template that contains
     * the selector (only used for error reporting)
     * @return a <code>String</code> value
     */
    public String getKey(String selector, String template) {
        String season = null;
        int offset = 5;
        if (!selector.startsWith("YEAR ")) {
            season = selector.substring(0, 6);
            offset = 7;
        }
        int year = Integer.parseInt(selector.substring(offset));
        Matcher matcher = turnPattern.matcher(template);
        while (matcher.find()) {
            if (matcher.group(6) != null) {
                // must be a single year, not a range
                if (compare(matcher.group(5), Integer.parseInt(matcher.group(6)),
                            season, year) == 0) {
                    return matcher.group(0);
                }
            } else {
                // must be a range
                if (compare(matcher.group(1), Integer.parseInt(matcher.group(2)), season, year) <= 0
                    && compare(matcher.group(3), Integer.parseInt(matcher.group(4)), season, year) >= 0) {
                    return matcher.group(0);
                }
            }
        }
        return selector;
    }


    public int compare(String season1, int year1, String season2, int year2) {
        if (year1 < year2) {
            return -1;
        } else if (year1 > year2) {
            return 1;
        } else if (year1 == year2) {
            if (season1 == null || season2 == null) {
                return 0;
            } else if ("SPRING".equals(season1) && "AUTUMN".equals(season2)) {
                return -1;
            } else if ("SPRING".equals(season2) && "AUTUMN".equals(season1)) {
                return 1;
            }
        }
        return 0;
    }


}