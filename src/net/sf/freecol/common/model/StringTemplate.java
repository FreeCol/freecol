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


package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

/**
 * The <code>StringTemplate</code> represents a non-localized string
 * that can be localized by looking up its value in a message bundle
 * or similar Map. The StringTemplate may contain variables (keys)
 * delimited by the '%' character, such as "%amount%" that will be
 * replaced with a string or a StringTemplate. If the StringTemplate
 * contains replacement values but no keys, then it is considered a
 * "label" StringTemplate, and its value will be used to join the
 * replacement values.
 *
 * @version 1.0
 */
public class StringTemplate {

    /**
     * The String value of this Template. Either a key that refers to
     * some localized string, or a separator in case of a "Label"
     * Template. In the latter case, the separator will be used to
     * join the replacement values.
     */
    private String value;

    /**
     * The keys to replace within the string template.
     */
    private List<String> keys;

    /**
     * The values with which to replace the keys in the string template.
     */
    private List<Object> replacements;

    /**
     * Whether to localize the replacement values.
     */
    private List<Boolean> localize;

    
    /**
     * Creates a new <code>Template</code> instance.
     *
     * @param template a <code>String</code> value
     */
    public StringTemplate(String template) {
	value = template;
    }

    /**
     * Creates a new <code>StringTemplate</code> instance.
     *
     * @param template a <code>String</code> value
     * @param data a list of keys and replacement strings
     */
    public StringTemplate(String template, String... data) {
        value = template;
	if (data.length % 2 != 0) {
	    throw new IllegalArgumentException("wrong number of arguments");
	}
        createLists();
	for (int index = 0; index < data.length; index += 2) {
            keys.add(data[index]);
            localize.add(true);
	    replacements.add(data[index + 1]);
	}
    }

    /**
     * Create Lists for keys, localize and replacement fields.
     *
     */
    private void createLists() {
        if (keys == null) {
            keys = new ArrayList<String>();
            localize = new ArrayList<Boolean>();
            replacements = new ArrayList<Object>();
        }
    }

    /**
     * Get the <code>Value</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getValue() {
        return value;
    }

    /**
     * Set the <code>Value</code> value.
     *
     * @param newValue The new Value value.
     */
    public final void setValue(final String newValue) {
        this.value = newValue;
    }

    /**
     * Get the <code>Keys</code> value.
     *
     * @return a <code>List<String></code> value
     */
    public final List<String> getKeys() {
        return keys;
    }

    /**
     * Set the <code>Keys</code> value.
     *
     * @param newKeys The new Keys value.
     */
    public final void setKeys(final List<String> newKeys) {
        this.keys = newKeys;
    }

    /**
     * Whether to localize the replacement value at index.
     *
     * @param index an <code>int</code> value
     * @return a <code>boolean</code> value
     */
    public boolean localize(int index) {
        return localize.get(index);
    }

    /**
     * Get the <code>Replacements</code> value.
     *
     * @return a <code>List<Object></code> value
     */
    public final List<Object> getReplacements() {
        return replacements;
    }

    /**
     * Set the <code>Replacements</code> value.
     *
     * @param newReplacements The new Replacements value.
     */
    public final void setReplacements(final List<Object> newReplacements) {
        this.replacements = newReplacements;
    }

    /**
     * Add a new key and replacement to the StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @param localized a <code>boolean</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String key, String value, boolean localized) {
        createLists();
        keys.add(key);
        localize.add(localized);
        replacements.add(value);
	return this;
    }

    /**
     * Add a replacement value without a key to the StringTemplate.
     * This is only possible if the StringTemplate is a "label"
     * template.
     *
     * @param value a <code>String</code> value
     * @param localized a <code>boolean</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String value, boolean localized) {
	if (isLabelTemplate()) {
            createLists();
            localize.add(localized);
	    replacements.add(value);
	} else {
	    throw new IllegalArgumentException("Can't add single string to template with keys.");
	}
	return this;
    }

    /**
     * Add a localized replacement value without a key to the
     * StringTemplate. This is only possible if the StringTemplate is
     * a "label" template.
     *
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String value) {
	return add(value, true);
    }

    /**
     * Add a non-localized replacement value (such as a proper name)
     * without a key to the StringTemplate. This is only possible if
     * the StringTemplate is a "label" template.
     *
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addName(String value) {
	return add(value, false);
    }

    /**
     * Add a new key and localized replacement value to the
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String key, String value) {
	return add(key, value, true);
    }

    /**
     * Add a new key and non-localized replacement value (such as a
     * proper name) to the StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addName(String key, String value) {
	return add(key, value, false);
    }

    /**
     * Add a key and a StringTemplate to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param template a <code>StringTemplate</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addStringTemplate(String key, StringTemplate template) {
        createLists();
        keys.add(key);
        localize.add(true);
	replacements.add(template);
	return this;
    }

    /**
     * Whether this StringTemplate is a "label" template.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLabelTemplate() {
        return (keys == null || keys.isEmpty());
    }

    public String toString() {
	String result = "";
        if (isLabelTemplate()) {
            if (replacements == null) {
                result = value;
            } else {
                for (Object object : replacements) {
                    result += object + value;
                }
            }
        } else {
            result += value + " [";
            for (int index = 0; index < keys.size(); index++) {
                result += "[" + keys.get(index) + " (" + localize.get(index)
                    + ") " + replacements.get(index) + "]";
            }
            result += "]";
	}
	return result;
    }
}
