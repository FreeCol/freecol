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


package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
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
     * An alternative key to use if the Id is not contained in the
     * message bundle.
     */
    private String defaultId;

    /**
     * The keys to replace within the string template.
     */
    private List<String> keys;

    /**
     * The values with which to replace the keys in the string template.
     */
    private List<StringTemplate> replacements;



    protected StringTemplate() {
        // empty constructor
    }

    public StringTemplate(String id, StringTemplate template) {
        setId(id);
        this.templateType = template.templateType;
        this.keys = template.keys;
        this.replacements = template.replacements;
    }

    /**
     * Creates a new <code>Template</code> instance.
     *
     * @param template a <code>String</code> value
     * @param templateType a <code>TemplateType</code> value
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

    /**
     * Get the <code>DefaultId</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getDefaultId() {
        return defaultId;
    }

    /**
     * Set the <code>DefaultId</code> value.
     *
     * @param newDefaultId The new DefaultId value.
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate setDefaultId(final String newDefaultId) {
        this.defaultId = newDefaultId;
        return this;
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
     * Return the replacement value for a given key, or null if there
     * is none.
     *
     * @param key a <code>String</code> value
     * @return a <code>String</code> value
     */
    public final StringTemplate getReplacement(String key) {
        for (int index = 0; index < keys.size(); index++) {
            if (key.equals(keys.get(index))) {
                if (replacements.size() > index) {
                    return replacements.get(index);
                } else {
                    return null;
                }
            }
        }
        return null;
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
     * Add a new key and replacement to the StringTemplate. The
     * replacement must be a proper name. This is only possible if the
     * StringTemplate is of type TEMPLATE.
     *
     * @param key a <code>String</code> value
     * @param object a <code>FreeColObject</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addName(String key, FreeColObject object) {
        if (templateType == TemplateType.TEMPLATE) {
            keys.add(key);
            replacements.add(new StringTemplate(object.getId() + ".name", TemplateType.KEY));
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
     * Add a key and an integer value to replace it to this
     * StringTemplate.
     *
     * @param key a <code>String</code> value
     * @param amount a <code>Number</code> value
     * @return a <code>StringTemplate</code> value
     */
    public StringTemplate addAmount(String key, Number amount) {
        addName(key, amount.toString());
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
            result += getId();
            if (defaultId != null) {
                result += " (" + defaultId + ")";
            }
            result += " [";
            for (int index = 0; index < keys.size(); index++) {
                result += "[" + keys.get(index) + ": "
                    + replacements.get(index).toString() + "]";
            }
            result += "]";
            break;
        case KEY:
            result += getId();
            if (defaultId != null) {
                result += " (" + defaultId + ")";
            }
            break;
        case NAME:
        default:
            result += getId();
        }
        return result;
    }


    public boolean equals(Object o) {
        if (o instanceof StringTemplate) {
            StringTemplate t = (StringTemplate) o;
            if (!getId().equals(t.getId()) || templateType != t.templateType) {
                return false;
            }
            if (defaultId == null) {
                if (t.defaultId != null) {
                    return false;
                }
            } else if (t.defaultId == null) {
                return false;
            } else if (!defaultId.equals(t.defaultId)) {
                return false;
            }
            if (templateType == TemplateType.LABEL) {
                if (replacements.size() == t.replacements.size()) {
                    for (int index = 0; index < replacements.size(); index++) {
                        if (!replacements.get(index).equals(t.replacements.get(index))) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            } else if (templateType == TemplateType.TEMPLATE) {
                if (keys.size() == t.keys.size()
                    && replacements.size() == t.replacements.size()
                    && keys.size() == replacements.size()) {
                    for (int index = 0; index < replacements.size(); index++) {
                        if (!keys.get(index).equals(t.keys.get(index))
                            || !replacements.get(index).equals(t.replacements.get(index))) {
                            return false;
                        }
                    }
                } else {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public int hashCode() {
        int result = 17;
        result = result * 31 + getId().hashCode();
        result = result * 31 + templateType.ordinal();
        if (defaultId != null) {
            result = result * 31 + defaultId.hashCode();
        }
        if (templateType == TemplateType.LABEL) {
            for (StringTemplate replacement : replacements) {
                result = result * 31 + replacement.hashCode();
            }
        } else if (templateType == TemplateType.TEMPLATE) {
            for (int index = 0; index < keys.size(); index++) {
                result = result * 31 + keys.get(index).hashCode();
                result = result * 31 + replacements.get(index).hashCode();
            }
        }
        return result;
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        writeAttributes(out);
        writeChildren(out);
        out.writeEndElement();
    }

    public static String getXMLElementTagName() {
        return "stringTemplate";
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute(ID_ATTRIBUTE_TAG, getId());
        out.writeAttribute("templateType", templateType.toString());
        if (defaultId != null) {
            out.writeAttribute("defaultId", defaultId);
        }
    }


    public void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        if (keys != null) {
            for (String key : keys) {
                out.writeStartElement("key");
                out.writeAttribute(VALUE_TAG, key);
                out.writeEndElement();
            }
        }
        if (replacements != null) {
            for (StringTemplate replacement : replacements) {
                replacement.toXMLImpl(out);
            }
        }
    }

    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        // TODO: remove compatibility code
        String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
        if (id == null) {
            id = in.getAttributeValue(null, ID_ATTRIBUTE);
        }
        setId(id);
        String typeString = in.getAttributeValue(null, "templateType");
        if (typeString == null) {
            templateType = TemplateType.TEMPLATE;
        } else {
            templateType = Enum.valueOf(TemplateType.class, typeString);
        }
        // end compatibility code
        defaultId = in.getAttributeValue(null, "defaultId");
        switch (templateType) {
        case TEMPLATE:
            keys = new ArrayList<String>();
        case LABEL:
            replacements = new ArrayList<StringTemplate>();
        }
    }


    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if ("key".equals(in.getLocalName())) {
                keys.add(in.getAttributeValue(null, VALUE_TAG));
                in.nextTag();
            } else if (getXMLElementTagName().equals(in.getLocalName())) {
                StringTemplate replacement = new StringTemplate();
                replacement.readFromXMLImpl(in);
                replacements.add(replacement);
            } else if ("data".equals(in.getLocalName())) {
                // TODO: remove compatibility code for ModelMessage
                readOldFormat(readFromArrayElement("data", in, new String[0]));
                // end compatibility code
            } else if ("strings".equals(in.getLocalName())) {
                // TODO: remove compatibility code for HistoryEvent
                readOldFormat(readFromArrayElement("strings", in, new String[0]));
                // end compatibility code
            }
        }
    }

    // TODO: remove compatibility code
    private void readOldFormat(String[] data) {
        for (int index = 0; index < data.length; index += 2) {
            keys.add(data[index]);
            replacements.add(new StringTemplate(data[index + 1], TemplateType.NAME));
        }
    }

}
