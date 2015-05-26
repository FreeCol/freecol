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

package net.sf.freecol.common.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.util.Utils;


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

    /** The TemplateType of this StringTemplate. Defaults to KEY. */
    private TemplateType templateType = TemplateType.KEY;

    /**
     * An alternative key to use if the identifier is not contained in
     * the message bundle.
     */
    private String defaultId = null;

    /** The keys to replace within the string template. */
    private List<String> keys = null;

    /** The values with which to replace the keys in the string template. */
    private List<StringTemplate> replacements = null;


    /**
     * Deliberately empty constructor.
     */
    protected StringTemplate() {}

    /**
     * Copy an existing template, but with a new identifier.
     *
     * @param id The object identifier.
     * @param template A <code>StringTemplate</code> to copy.
     */
    protected StringTemplate(String id, StringTemplate template) {
        setId(id);
        this.templateType = template.templateType;
        this.defaultId = template.defaultId;
        this.keys = template.keys;
        this.replacements = template.replacements;
    }

    /**
     * Creates a new <code>StringTemplate</code> instance.
     *
     * @param id The object identifier.
     * @param defaultId The default identifier.
     * @param templateType The <code>TemplateType</code> for this template.
     */
    protected StringTemplate(String id, String defaultId,
                             TemplateType templateType) {
        setId(id);
        this.defaultId = defaultId;
        this.templateType = templateType;
        this.keys = null;
        this.replacements = null;
    }

    /**
     * Create a new <code>StringTemplate</code> by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public StringTemplate(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    // Factory methods

    public static StringTemplate copy(String id, StringTemplate template) {
        return new StringTemplate(id, template);
    }

    public static StringTemplate name(String value) {
        if (value == null) {
            logger.warning("NULL NAME TEMPLATE\n" + net.sf.freecol.common.debug.FreeColDebugger.stackTraceToString());
        }
        return new StringTemplate(value, null, TemplateType.NAME);
    }

    public static StringTemplate key(Named named) {
        return key(named.getNameKey());
    }

    public static StringTemplate key(String value) {
        return new StringTemplate(value, null, TemplateType.KEY);
    }

    public static StringTemplate template(Named named) {
        return template(named.getNameKey());
    }

    public static StringTemplate template(String value) {
        return new StringTemplate(value, null, TemplateType.TEMPLATE);
    }

    public static StringTemplate label(String value) {
        return new StringTemplate(value, null, TemplateType.LABEL);
    }


    /**
     * Get the template type.
     *
     * @return The template type.
     */
    public final TemplateType getTemplateType() {
        return this.templateType;
    }

    /**
     * Get the default identifier.
     *
     * @return The default identifier.
     */
    public final String getDefaultId() {
        return this.defaultId;
    }

    /**
     * Set the default identifier.
     *
     * @param newDefaultId The new default identifier
     */
    public final void setDefaultId(final String newDefaultId) {
        this.defaultId = newDefaultId;
    }

    /**
     * Get the keys.
     *
     * @return A list of keys.
     */
    public final List<String> getKeys() {
        return (this.keys == null) ? Collections.<String>emptyList()
            : this.keys;
    }

    /**
     * Add a key.
     * 
     * @param key The key to add.
     */
    private void addKey(String key) {
        if (this.keys == null) this.keys = new ArrayList<>();
        this.keys.add(key);
    }

    /**
     * Get the replacements.
     *
     * @return A list of replacements.
     */
    public final List<StringTemplate> getReplacements() {
        return (this.replacements == null)
            ? Collections.<StringTemplate>emptyList()
            : this.replacements;
    }
    
    /**
     * Add a replacement.
     *
     * @param replacement The <code>StringTemplate</code> replacement to add.
     */
    private void addReplacement(StringTemplate replacement) {
        if (this.replacements == null) this.replacements = new ArrayList<>();
        this.replacements.add(replacement);
    }

    /**
     * Get the replacement value for a given key.
     *
     * @param key The key to find a replacement for.
     * @return The replacement found, or null if none found.
     */
    public final StringTemplate getReplacement(String key) {
        if (this.keys != null && this.replacements != null) {
            for (int index = 0; index < this.keys.size(); index++) {
                if (key.equals(this.keys.get(index))) {
                    return (this.replacements.size() <= index) ? null
                        : this.replacements.get(index);
                }
            }
        }
        return null;
    }

    /**
     * Add a new key and replacement value to this template.
     *
     * This is only possible if the template is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate add(String key, String value) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new IllegalArgumentException("Cannot add key-value pair"
                + " to StringTemplate." + this.templateType);
        }
        addKey(key);
        addReplacement(this.key(value));
        return this;
    }

    /**
     * Add a replacement value without a key to this template.
     *
     * This is only possible if the template is of type LABEL.
     *
     * @param value The replacement value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate add(String value) {
        if (this.templateType != TemplateType.LABEL) {
            throw new IllegalArgumentException("Cannot add a single string"
                + " to StringTemplate." + this.templateType);
        }
        addReplacement(this.key(value));
        return this;
    }

    /**
     * Add a new key and replacement proper name to this template.
     *
     * This is only possible if the template is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String key, String value) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new IllegalArgumentException("Cannot add key-name pair"
                + " to StringTemplate." + this.templateType);
        }
        addKey(key);
        addReplacement(this.name(value));
        return this;
    }

    /**
     * Add a new key and replacement namable object to this template.
     *
     * This is only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param object The replacement <code>FreeColObject</code>.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String key, FreeColObject object) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new IllegalArgumentException("Cannot add key-object pair"
                + " to StringTemplate." + this.templateType);
        }
        addKey(key);
        addReplacement(this.key(Messages.nameKey(object.getId())));
        return this;
    }

    /**
     * Add a replacement proper name without a key to this template.
     *
     * This is only possible if the StringTemplate is of type LABEL.
     *
     * @param value The replacement value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addName(String value) {
        if (this.templateType != TemplateType.LABEL) {
            throw new IllegalArgumentException("Cannot add a single string"
                + " to StringTemplate." + this.templateType);
        }
        addReplacement(this.name(value));
        return this;
    }

    /**
     * Add a key and named object to this template.
     *
     * @param key The key to add.
     * @param named The <code>Named</code> to add.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addNamed(String key, Named named) {
        return add(key, named.getNameKey());
    }

    /**
     * Add named object without key to this template.
     *
     * @param named The <code>Named</code> to add.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addNamed(Named named) {
        return add(named.getNameKey());
    }

    /**
     * Add a key and an integer value to replace it to this template.
     *
     * @param key The key to add.
     * @param amount The <code>Number</code> value to add.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addAmount(String key, Number amount) {
        return addName(key, amount.toString());
    }

    /**
     * Add a key and a replacement StringTemplate to this template.
     *
     * This is only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param key The key to add.
     * @param template The <code>StringTemplate</code> value.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addStringTemplate(String key,
                                            StringTemplate template) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new IllegalArgumentException("Cannot add key-template pair"
                + " to StringTemplate." + this.templateType);
        }
        addKey(key);
        addReplacement(template);
        return this;
    }

    /**
     * Add a StringTemplate to this template.
     *
     * This is only possible if the StringTemplate is of type LABEL.
     *
     * @param template The replacement <code>StringTemplate</code>.
     * @return This <code>StringTemplate</code>.
     */
    public StringTemplate addStringTemplate(StringTemplate template) {
        if (this.templateType != TemplateType.LABEL) {
            throw new IllegalArgumentException("Cannot add a template"
                + " to StringTemplate." + this.templateType);
        }
        addReplacement(template);
        return this;
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof StringTemplate) {
            StringTemplate t = (StringTemplate)o;
            if (!super.equals(o)
                || this.templateType != t.templateType
                || !Utils.equals(this.defaultId, t.defaultId)) return false;
            switch (this.templateType) {
            case TEMPLATE:
                if ((this.keys == null) != (t.keys == null))
                    return false;
                if (this.keys != null) {
                    if (this.keys.size() != t.keys.size()
                        || this.keys.size() != this.replacements.size())
                        return false;
                    for (int i = 0; i < this.keys.size(); i++) {
                        if (!this.keys.get(i)
                            .equals(t.keys.get(i))) return false;
                    }
                }
                // Fall through
            case LABEL:
                if ((this.replacements == null) != (t.replacements == null))
                    return false;
                if (this.replacements != null) {
                    if (this.replacements.size() != t.replacements.size())
                        return false;
                    for (int i = 0; i < this.replacements.size(); i++) {
                        if (!this.replacements.get(i)
                            .equals(t.replacements.get(i))) return false;
                    }
                }
                break;
            default:
                break;
            }
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 31 * hash + this.templateType.ordinal();
        hash = 31 * hash + Utils.hashCode(this.defaultId);
        switch (this.templateType) {
        case TEMPLATE:
            for (String key : getKeys()) {
                hash = 31 * hash + Utils.hashCode(key);
            }
            // Fall through
        case LABEL:
            for (StringTemplate replacement : getReplacements()) {
                hash = 31 * hash + Utils.hashCode(replacement);
            }
            break;
        default:
            break;
        }
        return hash;
    }


    // Serialization

    private static final String DEFAULT_ID_TAG = "defaultId";
    private static final String KEY_TAG = "key";
    private static final String TEMPLATE_TYPE_TAG = "templateType";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(TEMPLATE_TYPE_TAG, templateType);

        if (defaultId != null) {
            xw.writeAttribute(DEFAULT_ID_TAG, defaultId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (keys != null) {
            for (String key : keys) {
                xw.writeStartElement(KEY_TAG);

                xw.writeAttribute(VALUE_TAG, key);

                xw.writeEndElement();
            }
        }

        if (replacements != null) {
            for (StringTemplate replacement : replacements) {
                replacement.toXML(xw);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        templateType = xr.getAttribute(TEMPLATE_TYPE_TAG,
            TemplateType.class, TemplateType.TEMPLATE);

        defaultId = xr.getAttribute(DEFAULT_ID_TAG, (String)null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (keys != null) keys.clear();
        if (replacements != null) replacements.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (KEY_TAG.equals(tag)) {
            addKey(xr.getAttribute(VALUE_TAG, (String)null));
            xr.closeTag(KEY_TAG);

        } else if (StringTemplate.getXMLElementTagName().equals(tag)) {
            addReplacement(new StringTemplate(xr));
        
        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(templateType).append(": ");
        switch (templateType) {
        case LABEL:
            if (this.replacements == null) {
                sb.append(getId());
            } else {
                for (StringTemplate object : this.replacements) {
                    sb.append(object).append(getId());
                }
            }
            break;
        case TEMPLATE:
            sb.append(getId());
            if (this.defaultId != null) {
                sb.append(" (").append(this.defaultId).append(")");
            }
            sb.append(" [");
            if (this.keys != null) {
                for (int index = 0; index < this.keys.size(); index++) {
                    sb.append("[").append(this.keys.get(index)).append(": ")
                        .append(this.replacements.get(index)).append("]");
                }
            }
            sb.append("]");
            break;
        case KEY:
            sb.append(getId());
            if (this.defaultId != null) {
                sb.append(" (").append(this.defaultId).append(")");
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
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "stringTemplate".
     */
    public static String getXMLElementTagName() {
        return "stringTemplate";
    }
}
