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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
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
    private List<String> keys = null;

    /**
     * The values with which to replace the keys in the string template.
     */
    private List<StringTemplate> replacements = null;


    /**
     * Empty constructor for serialization.
     */
    protected StringTemplate() {}

    /**
     * Creates a new <code>StringTemplate</code> instance.
     *
     * @param id The object id.
     * @param template A <code>StringTemplate</code> to copy.
     */
    public StringTemplate(String id, StringTemplate template) {
        setId(id);
        this.templateType = template.templateType;
        this.keys = template.keys;
        this.replacements = template.replacements;
    }

    /**
     * Creates a new <code>StringTemplate</code> instance.
     *
     * @param id The object id.
     * @param templateType The <code>TemplateType</code> for this template.
     */
    protected StringTemplate(String id, TemplateType templateType) {
        setId(id);
        this.templateType = templateType;
        this.keys = null;
        this.replacements = null;
    }

    /**
     * Get the default id.
     *
     * @return The default id.
     */
    public final String getDefaultId() {
        return defaultId;
    }

    /**
     * Set the default id.
     *
     * @param newDefaultId The new default id.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate setDefaultId(final String newDefaultId) {
        this.defaultId = newDefaultId;
        return this;
    }

    /**
     * Get the template type.
     *
     * @return The template type.
     */
    public final TemplateType getTemplateType() {
        return templateType;
    }

    /**
     * Get the keys.
     *
     * @return A list of keys.
     */
    public final List<String> getKeys() {
        if (keys == null) return Collections.emptyList();
        return keys;
    }

    private void requireKeys() {
        if (keys == null) keys = new ArrayList<String>();
    }

    /**
     * Get the replacements.
     *
     * @return A list of replacements.
     */
    public final List<StringTemplate> getReplacements() {
        if (replacements == null) return Collections.emptyList();
        return replacements;
    }

    private void requireReplacements() {
        if (replacements == null) {
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
     * Get the replacement value for a given key.
     *
     * @param key The key to find a replacement for.
     * @return The replacement found, or null if none found.
     */
    public final StringTemplate getReplacement(String key) {
        if (keys != null && replacements != null) {
            for (int index = 0; index < keys.size(); index++) {
                if (key.equals(keys.get(index))) {
                    if (replacements.size() > index) {
                        return replacements.get(index);
                    } else {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Add a new key and replacement to the StringTemplate.  This is
     * only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate add(String key, String value) {
        if (templateType == TemplateType.TEMPLATE) {
            requireKeys(); requireReplacements();
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
     * @param value The replacement value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate add(String value) {
        if (templateType == TemplateType.LABEL) {
            requireReplacements();
            replacements.add(new StringTemplate(value, TemplateType.KEY));
        } else {
            throw new IllegalArgumentException("Cannot add a single string to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }

    /**
     * Add a new key and replacement to the StringTemplate.  The
     * replacement must be a proper name.  This is only possible if the
     * StringTemplate is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String key, String value) {
        if (templateType == TemplateType.TEMPLATE) {
            requireKeys(); requireReplacements();
            keys.add(key);
            replacements.add(new StringTemplate(value, TemplateType.NAME));
        } else {
            throw new IllegalArgumentException("Cannot add key-value pair to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }

    /**
     * Add a new key and replacement to the StringTemplate.  The
     * replacement must be a proper name.  This is only possible if the
     * StringTemplate is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param object The corresponding value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String key, FreeColObject object) {
        if (templateType == TemplateType.TEMPLATE) {
            requireKeys(); requireReplacements();
            keys.add(key);
            replacements.add(new StringTemplate(object.getId() + ".name",
                                                TemplateType.KEY));
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
     * @param value The replacement value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String value) {
        if (templateType == TemplateType.LABEL) {
            requireReplacements();
            replacements.add(new StringTemplate(value, TemplateType.NAME));
        } else {
            throw new IllegalArgumentException("Cannot add a single string to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }

    /**
     * Add a key and an integer value to replace it to this StringTemplate.
     *
     * @param key The key to add.
     * @param amount The integer value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addAmount(String key, Number amount) {
        addName(key, amount.toString());
        return this;
    }

    /**
     * Add a key and a StringTemplate to replace it to this StringTemplate.
     *
     * @param key The key to add.
     * @param template The template value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addStringTemplate(String key,
                                                          StringTemplate template) {
        if (templateType == TemplateType.TEMPLATE) {
            requireKeys(); requireReplacements();
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
     * @param template The replacement <code>StringTemplate</code>.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addStringTemplate(StringTemplate template) {
        if (templateType == TemplateType.LABEL) {
            requireReplacements();
            replacements.add(template);
        } else {
            throw new IllegalArgumentException("Cannot add a StringTemplate to StringTemplate type "
                                               + templateType.toString());
        }
        return this;
    }


    // Interface Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof StringTemplate) {
            requireKeys(); requireReplacements();
            StringTemplate t = (StringTemplate)o;
            t.requireKeys(); t.requireReplacements();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int result = 17;
        result = result * 31 + getId().hashCode();
        result = result * 31 + templateType.ordinal();
        if (defaultId != null) {
            result = result * 31 + defaultId.hashCode();
        }
        if (templateType == TemplateType.LABEL) {
            requireReplacements();
            for (StringTemplate replacement : replacements) {
                result = result * 31 + replacement.hashCode();
            }
        } else if (templateType == TemplateType.TEMPLATE) {
            requireKeys(); requireReplacements();
            for (int index = 0; index < keys.size(); index++) {
                result = result * 31 + keys.get(index).hashCode();
                result = result * 31 + replacements.get(index).hashCode();
            }
        }
        return result;
    }


    // Serialization

    private static final String DEFAULT_ID_TAG = "defaultId";
    private static final String KEY_TAG = "key";
    private static final String TEMPLATE_TYPE_TAG = "templateType";
    // @compat 0.9.x
    private static final String DATA_TAG = "data";
    private static final String STRINGS_TAG = "strings";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, TEMPLATE_TYPE_TAG, templateType);

        if (defaultId != null) {
            writeAttribute(out, DEFAULT_ID_TAG, defaultId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (keys != null) {
            for (String key : keys) {
                out.writeStartElement(KEY_TAG);

                writeAttribute(out, VALUE_TAG, key);

                out.writeEndElement();
            }
        }

        if (replacements != null) {
            for (StringTemplate replacement : replacements) {
                replacement.toXMLImpl(out);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        templateType = getAttribute(in, TEMPLATE_TYPE_TAG,
                                    TemplateType.class, TemplateType.TEMPLATE);

        defaultId = getAttribute(in, DEFAULT_ID_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear containers
        if (keys != null) keys.clear();
        if (replacements != null) replacements.clear();

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (KEY_TAG.equals(tag)) {
            requireKeys();
            keys.add(getAttribute(in, VALUE_TAG, (String)null));
            in.nextTag(); // close key

        } else if (getXMLElementTagName().equals(tag)) {
            requireReplacements();
            StringTemplate replacement = new StringTemplate();
            replacement.readFromXML(in);
            replacements.add(replacement);
        
        // @compat 0.9.x
        } else if (DATA_TAG.equals(tag)) {
            readOldFormat(readFromArrayElement(DATA_TAG, in, new String[0]));
        // end @compat

        // @compat 0.9.x
        } else if (STRINGS_TAG.equals(tag)) {
            // TODO: remove compatibility code for HistoryEvent
            readOldFormat(readFromArrayElement(STRINGS_TAG, in, new String[0]));
        // end @compat

        } else {
            super.readChild(in);
        }
    }

    // @compat 0.9.x
    private void readOldFormat(String[] data) {
        for (int index = 0; index < data.length; index += 2) {
            requireKeys(); requireReplacements();
            keys.add(data[index]);
            replacements.add(new StringTemplate(data[index + 1],
                             TemplateType.NAME));
        }
    }
    // end @compat

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(templateType.toString());
        sb.append(": ");
        switch (templateType) {
        case LABEL:
            if (replacements == null) {
                sb.append(getId());
            } else {
                for (StringTemplate object : replacements) {
                    sb.append(object).append(getId());
                }
            }
            break;
        case TEMPLATE:
            sb.append(getId());
            if (defaultId != null) {
                sb.append(" (").append(defaultId).append(")");
            }
            sb.append(" [");
            for (int index = 0; index < keys.size(); index++) {
                sb.append("[").append(keys.get(index)).append(": ")
                    .append(replacements.get(index).toString()).append("]");
            }
            sb.append("]");
            break;
        case KEY:
            sb.append(getId());
            if (defaultId != null) {
                sb.append(" (").append(defaultId).append(")");
            }
            break;
        case NAME:
        default:
            sb.append(getId());
            break;
        }
        return sb.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "stringTemplate".
     */
    public static String getXMLElementTagName() {
        return "stringTemplate";
    }
}
