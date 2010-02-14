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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

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
public class StringTemplate extends FreeColObject {

    /**
     * The type of this StringTemplate, either NAME, a proper name
     * that must not be localized (e.g. "George Washington"), or KEY,
     * a string that must be localized (e.g. "model.goods.food.name"),
     * or TEMPLATE, a key with replacements to apply to the localized
     * value of the key, or LABEL, a separator string that will be
     * used to join the replacement values.
     */
    public static enum TemplateType { NAME, KEY, TEMPLATE, LABEL }

    /**
     * The TemplateType of this StringTemplate. Defaults to KEY.
     */
    private TemplateType templateType = TemplateType.KEY;

    /**
     * The keys to replace within the string template.
     */
    private List<String> keys;

    /**
     * The values with which to replace the keys in the string template.
     */
    private List<StringTemplate> replacements;

    /**
     * Creates a new <code>Template</code> instance.
     *
     * @param template a <code>String</code> value
     * @param TemplateType a <code>TemplateType</code> value
     */
    protected StringTemplate(String template, TemplateType templateType) {
	setId(template);
        this.templateType = templateType;
        switch (templateType) {
        case TEMPLATE:
            keys = new ArrayList<String>();
        case LABEL:            
            replacements = new ArrayList<StringTemplate>();
        }
    }

    // Factory methods

    public static StringTemplate name(String value) {
        return new StringTemplate(value, TemplateType.NAME);
    }

    public static StringTemplate key(String value) {
        return new StringTemplate(value, TemplateType.KEY);
    }

    public static StringTemplate template(String value) {
        return new StringTemplate(value, TemplateType.TEMPLATE);
    }

    public static StringTemplate label(String value) {
        return new StringTemplate(value, TemplateType.LABEL);
    }


    /**
     * Get the <code>TemplateType</code> value.
     *
     * @return a <code>TemplateType</code> value
     */
    public final TemplateType getTemplateType() {
        return templateType;
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
     * Get the <code>Replacements</code> value.
     *
     * @return a <code>List<StringTemplate></code> value
     */
    public final List<StringTemplate> getReplacements() {
        return replacements;
    }

    /**
     * Add a new key and replacement to the StringTemplate. This is
     * only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String key, String value) {
        if (templateType == TemplateType.TEMPLATE) {
            keys.add(key);
            replacements.add(new StringTemplate(value, TemplateType.KEY));
        } else {
            throw new IllegalArgumentException("Cannot add key-value pair to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }

    /**
     * Add a replacement value without a key to the StringTemplate.
     * This is only possible if the StringTemplate is of type LABEL.
     *
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate add(String value) {
	if (templateType == TemplateType.LABEL) {
	    replacements.add(new StringTemplate(value, TemplateType.KEY));
	} else {
	    throw new IllegalArgumentException("Cannot add a single string to StringTemplate type "
                                               + templateType.toString());
	}
	return this;
    }

    /**
     * Add a new key and replacement to the StringTemplate. The
     * replacement must be a proper name. This is only possible if the
     * StringTemplate is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addName(String key, String value) {
        if (templateType == TemplateType.TEMPLATE) {
            keys.add(key);
            replacements.add(new StringTemplate(value, TemplateType.NAME));
        } else {
            throw new IllegalArgumentException("Cannot add key-value pair to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }

    /**
     * Add a replacement value without a key to the StringTemplate.
     * The replacement must be a proper name.  This is only possible
     * if the StringTemplate is of type LABEL.
     *
     * @param value a <code>String</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addName(String value) {
	if (templateType == TemplateType.LABEL) {
	    replacements.add(new StringTemplate(value, TemplateType.NAME));
	} else {
	    throw new IllegalArgumentException("Cannot add a single string to StringTemplate type "
                                               + templateType.toString());
	}
	return this;
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
        if (templateType == TemplateType.TEMPLATE) {
            keys.add(key);
            replacements.add(template);
        } else {
            throw new IllegalArgumentException("Cannot add a key-template pair to a StringTemplate type "
                                               + templateType.toString());
        }
	return this;
    }

    /**
     * Add a StringTemplate to this LABEL StringTemplate.
     *
     * @param template a <code>StringTemplate</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addStringTemplate(StringTemplate template) {
        if (templateType == TemplateType.LABEL) {
            replacements.add(template);
        } else {
	    throw new IllegalArgumentException("Cannot add a StringTemplate to StringTemplate type "
                                               + templateType.toString());
        }            
	return this;
    }

    public String toString() {
        String result = templateType.toString() + ": ";
        switch (templateType) {
        case LABEL:
            if (replacements == null) {
                result += getId();
            } else {
                for (StringTemplate object : replacements) {
                    result += object + getId();
                }
            }
            break;
        case TEMPLATE:
            result += getId() + " [";
            for (int index = 0; index < keys.size(); index++) {
                result += "[" + keys.get(index) + ": "
                    + replacements.get(index).toString() + "]";
            }
            result += "]";
            break;
        case KEY:
        case NAME:
        default:
            result += getId();
        }
        return result;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // TODO: write me
    }



}
