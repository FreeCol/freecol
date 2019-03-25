/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The {@code StringTemplate} represents a non-localized string
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

    public static final String TAG = "stringTemplate";

    /** Fixed trivial return value for entryList(). */
    private static final List<SimpleEntry<String,StringTemplate>> emptyList
        = Collections.<SimpleEntry<String,StringTemplate>>emptyList();

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

    /** The key,value paris to apply within the string template. */
    private List<SimpleEntry<String,StringTemplate>> kv = null;


    /**
     * Trivial constructor to allow creation with Game.newInstance.
     */
    public StringTemplate() {}

    /**
     * Copy an existing template, but with a new identifier.
     *
     * @param id The object identifier.
     * @param template A {@code StringTemplate} to copy.
     */
    protected StringTemplate(String id, StringTemplate template) {
        setId(id);
        this.templateType = template.templateType;
        this.defaultId = template.defaultId;
        this.kv = template.kv;
    }

    /**
     * Creates a new {@code StringTemplate} instance.
     *
     * @param id The object identifier.
     * @param defaultId The default identifier.
     * @param templateType The {@code TemplateType} for this template.
     */
    protected StringTemplate(String id, String defaultId,
                             TemplateType templateType) {
        setId(id);
        this.defaultId = defaultId;
        this.templateType = templateType;
        this.kv = null;
    }

    /**
     * Create a new {@code StringTemplate} by reading a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read.
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
     * @param id The new default identifier.
     */
    public final void setDefaultId(String id) {
        this.defaultId = id;
    }

    /**
     * Wrapper for subclasses to set the default identifier and return the
     * setting object.
     *
     * @param <T> The actual return type.
     * @param id The new default identifier
     * @param returnClass The expected return class.
     * @return The setting object.
     */
    protected <T extends StringTemplate> T setDefaultId(final String id,
                                                        Class<T> returnClass) {
        setDefaultId(id);
        try {
            return returnClass.cast(this);
        } catch (ClassCastException cce) {
            logger.log(Level.WARNING, "Invalid class "
                + this.getClass() + " referenced.", cce);
        }
        return null;            
    }

    /**
     * Has nothing been added to this template?
     *
     * @return True if the template is empty.
     */
    public boolean isEmpty() {
        return this.kv == null || this.kv.isEmpty();
    }

    /**
     * Get the list of key, value pairs.
     *
     * @return The pairs.
     */
    public List<SimpleEntry<String,StringTemplate>> entryList() {
        return (this.kv != null) ? this.kv : emptyList;
    }

    /**
     * Try to find the replacement for a given key.
     *
     * @param key The key to look for.
     * @return The value found, otherwise null.
     */
    public StringTemplate getReplacement(String key) {
        if (this.kv == null) return null;
        SimpleEntry<String,StringTemplate> val
            = find(this.kv, matchKeyEquals(key, SimpleEntry::getKey));
        return (val == null) ? null : val.getValue();
    }

    /**
     * Add a key, value pair.
     *
     * @param key The {@code String} key.
     * @param value The {@code StringTemplate} value.
     */
    private void addPair(String key, StringTemplate value) {
        if (key == null && value == null) {
            throw new RuntimeException("Null key and pair: " + this);
        }
        if (this.kv == null) this.kv = new ArrayList<>();
        this.kv.add(new SimpleEntry<>(key, value));
    }

    // @compat 0.11.x
    /**
     * Add a key.
     * 
     * @param key The key to add.
     */
    private void addKey(String key) {
        addPair(key, null);
    }

    /**
     * Add a replacement.
     *
     * @param replacement The {@code StringTemplate} replacement to add.
     */
    private void addReplacement(StringTemplate replacement) {
        if (this.kv == null) this.kv = new ArrayList<>();
        for (SimpleEntry<String,StringTemplate> e : this.kv) {
            if (e.getValue() == null) {
                e.setValue(replacement);
                return;
            }
        }
        addPair(null, replacement);
    }
    // end @compat 0.11.x

    /**
     * Add an optional key and replacement.  Helper function for the add*()
     * routines that follow.
     *
     * @param <T> The actual return type.
     * @param key The optional key.
     * @param value The replacement {@code StringTemplate}.
     * @return This object, cast back to its original class.
     */
    @SuppressWarnings("unchecked")
    private final <T extends StringTemplate> T complete(String key,
                                                        StringTemplate value) {
        addPair(key, value);
        return (T)this;
    }

    /**
     * Add a new key and replacement value to this template.
     *
     * This is only possible if the template is of type TEMPLATE.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This.
     */
    public <T extends StringTemplate> T add(String key, String value) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new RuntimeException("Cannot add key-value pair"
                + " to StringTemplate." + this.templateType);
        }
        return complete(key, StringTemplate.key(value));
    }

    /**
     * Add a replacement value without a key to this template.
     *
     * This is only possible if the template is of type LABEL.
     *
     * @param <T> The actual return type.
     * @param value The replacement value.
     * @return This.
     */
    public <T extends StringTemplate> T add(String value) {
        if (this.templateType != TemplateType.LABEL) {
            throw new RuntimeException("Cannot add a single string"
                + " to StringTemplate." + this.templateType);
        }
        return complete(null, StringTemplate.key(value));
    }

    /**
     * Add a new key and replacement proper name to this template.
     *
     * This is only possible if the template is of type TEMPLATE.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param value The corresponding replacement.
     * @return This.
     */
    public <T extends StringTemplate> T addName(String key, String value) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new RuntimeException("Cannot add key-name pair"
                + " to StringTemplate." + this.templateType);
        }
        return complete(key, StringTemplate.name(value));
    }

    /**
     * Add a new key and replacement namable object to this template.
     *
     * This is only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param object The replacement {@code FreeColObject}.
     * @return This.
     */
    public <T extends StringTemplate> T addName(String key, FreeColObject object) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new RuntimeException("Cannot add key-object pair"
                + " to StringTemplate." + this.templateType);
        }
        return complete(key, StringTemplate.key(Messages.nameKey(object.getId())));
    }

    /**
     * Add a replacement proper name without a key to this template.
     *
     * This is only possible if the StringTemplate is of type LABEL.
     *
     * @param <T> The actual return type.
     * @param value The replacement value.
     * @return This.
     */
    public <T extends StringTemplate> T addName(String value) {
        if (this.templateType != TemplateType.LABEL) {
            throw new RuntimeException("Cannot add a single string"
                + " to StringTemplate." + this.templateType);
        }
        return complete(null, StringTemplate.name(value));
    }

    /**
     * Add a key and named object to this template.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param named The {@code Named} to add.
     * @return This.
     */
    public <T extends StringTemplate> T addNamed(String key, Named named) {
        return add(key, named.getNameKey());
    }

    /**
     * Add named object without key to this template.
     *
     * @param <T> The actual return type.
     * @param named The {@code Named} to add.
     * @return This.
     */
    public <T extends StringTemplate> T addNamed(Named named) {
        return add(named.getNameKey());
    }

    /**
     * Add a key and an integer value to replace it to this template.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param amount The {@code Number} value to add.
     * @return This.
     */
    public <T extends StringTemplate> T addAmount(String key, Number amount) {
        return addName(key, amount.toString());
    }

    /**
     * Add a key and a replacement StringTemplate to this template.
     *
     * This is only possible if the StringTemplate is of type TEMPLATE.
     *
     * @param <T> The actual return type.
     * @param key The key to add.
     * @param template The {@code StringTemplate} value.
     * @return This.
     */
    public <T extends StringTemplate> T addStringTemplate(String key,
                                                          StringTemplate template) {
        if (this.templateType != TemplateType.TEMPLATE) {
            throw new RuntimeException("Cannot add key-template pair"
                + " to StringTemplate." + this.templateType);
        }
        return complete(key, template);
    }

    /**
     * Add a StringTemplate to this template.
     *
     * This is only possible if the StringTemplate is of type LABEL.
     *
     * @param <T> The actual return type.
     * @param template The replacement {@code StringTemplate}.
     * @return This.
     */
    public <T extends StringTemplate> T addStringTemplate(StringTemplate template) {
        if (this.templateType != TemplateType.LABEL) {
            throw new RuntimeException("Cannot add a template"
                + " to StringTemplate." + this.templateType);
        }
        return complete(null, template);
    }

    /**
     * Add a tagged value.
     *
     * Functionally identical to add(), but used to distinguish the special
     * cases at the point they are used.
     *
     * @param <T> The actual return type.
     * @param key The tag.
     * @param value The special tag value.
     * @return This.
     */
    public <T extends StringTemplate> T addTagged(String key, String value) {
        return add(key, value);
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        StringTemplate o = copyInCast(other, StringTemplate.class);
        if (o == null || !super.copyIn(o)) return false;
        this.templateType = o.getTemplateType();
        this.defaultId = o.getDefaultId();
        this.kv = o.kv;
        return true;
    }


    // Serialization

    private static final String DEFAULT_ID_TAG = "defaultId";
    private static final String PAIR_TAG = "pair";
    private static final String TEMPLATE_TYPE_TAG = "templateType";
    // @compat 0.11.x
    private static final String OLD_KEY_TAG = "key";
    // end @compat 0.11.x


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

        if (this.kv != null) {
            for (SimpleEntry<String,StringTemplate> e : this.kv) {
                xw.writeStartElement(PAIR_TAG);
                
                String key = e.getKey(); // OK if null
                if (key != null) xw.writeAttribute(VALUE_TAG, key);

                e.getValue().toXML(xw); // value is always present

                xw.writeEndElement();
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
        if (kv != null) kv.clear();

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (PAIR_TAG.equals(tag)) {
            // key == null is allowed
            String key = xr.getAttribute(VALUE_TAG, (String)null);
            StringTemplate val = null;
            while (xr.moreTags()) {
                final String inner = xr.getLocalName();
                if (StringTemplate.TAG.equals(inner)) {
                    if (val == null) {
                        val = new StringTemplate(xr);
                    } else {
                        xr.expectTag(PAIR_TAG);
                    }
                } else {
                    xr.expectTag(StringTemplate.TAG);
                }
            }
            addPair(key, val);

        // @compat 0.11.x
        // old format where key and value were separated
        } else if (OLD_KEY_TAG.equals(tag)) {
            addKey(xr.getAttribute(VALUE_TAG, (String)null));
            xr.closeTag(OLD_KEY_TAG);

        } else if (StringTemplate.TAG.equals(tag)) {
            addReplacement(new StringTemplate(xr));
        // end @compat 0.11.x

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof StringTemplate) {
            StringTemplate other = (StringTemplate)o;
            int i;
            if (this.templateType != other.templateType
                || !Utils.equals(this.defaultId, other.defaultId))
                return false;
            if ((this.kv == null) != (other.kv == null)) return false;
            if (this.kv != null) {
                if (this.kv.size() != other.kv.size()) return false;
                i = 0;
                for (SimpleEntry<String,StringTemplate> e : this.kv) {
                    if (!e.getKey().equals(other.kv.get(i).getKey()))
                        return false;
                    if (!e.getValue().equals(other.kv.get(i).getValue()))
                        return false;
                    i++;
                }
            }
            return super.equals(other);
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
        if (this.kv != null) {
            for (SimpleEntry<String,StringTemplate> e : this.kv) {
                hash = 31 * hash + Utils.hashCode(e.getKey());
                hash = 31 * hash + Utils.hashCode(e.getValue());
            }
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append(templateType).append(':').append(getId());
        if (this.defaultId != null) {
            sb.append('(').append(this.defaultId).append(')');
        }
        String start;
        switch (templateType) {
        case LABEL:
            if (this.kv != null) {
                int index = 0;
                start = "[";
                for (SimpleEntry<String,StringTemplate> e : this.kv) {
                    sb.append(start).append(index++)
                        .append(':').append(e.getValue());
                    start = " ";
                }
                sb.append(']');
            }
            break;
        case TEMPLATE:
            sb.append('[');
            if (this.kv != null) {
                start = "";
                for (SimpleEntry<String,StringTemplate> e : this.kv) {
                    sb.append(start).append(e.getKey())
                        .append('=').append(e.getValue());
                    start = "";
                }
            }
            sb.append(']');
            break;
        case KEY: case NAME: default:
            break;
        }
        return sb.toString();
    }
}
