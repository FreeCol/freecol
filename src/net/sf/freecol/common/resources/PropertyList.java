/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

package net.sf.freecol.common.resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows properties to be stored in strings. This is used when storing several
 * property strings that should all be overriden when one of them are.
 */
public class PropertyList {
    
    private Map<String, String> values = new HashMap<>();

    public PropertyList(String propertiesString) {
        for (String pair : propertiesString.split(",")) {
            final String[] s = pair.split("=");
            values.put(s[0], s[1]);
        }
    }
    
    public String getString(String key) {
        return values.get(key);
    }
    
    public int getInt(String key, int defaultValue) {
        final String value = values.get(key);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return defaultValue;
    }
    
    public int getInt(String key) {
        final String value = values.get(key);
        if (value == null) {
            throw new NullPointerException("Missing value for key: " + key);
        }
        return Integer.parseInt(value);
    }
}
