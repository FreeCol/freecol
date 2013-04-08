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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Specification;


/**
 * Used for grouping {@link Option}s.
 */
public class OptionGroup extends AbstractOption<OptionGroup> {

    private static Logger logger = Logger.getLogger(OptionGroup.class.getName());

    /** The options in this group. */
    private final List<Option> options = new ArrayList<Option>();

    /**
     * A map of all option ids to its option.  Unlike the options
     * array, this contains all child options of options that are
     * themselves groups.
     */
    private final Map<String, Option> optionMap = new HashMap<String, Option>();

    /** Is this option group user editable? */
    private boolean editable = true;


    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     */
    public OptionGroup(String id) {
        super(id);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param specification The enclosing <code>Specification</code>.
     */
    public OptionGroup(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param id The identifier for this option.  This is used when
     *     the object should be found in an {@link OptionGroup}.
     * @param specification The enclosing <code>Specification</code>.
     */
    public OptionGroup(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this option group editable?
     *
     * @return True if the option group is editable.
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Set the editable status of this group.
     *
     * @param editable The new editable status.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Gets the i18n-name of this <code>Option</code>.
     *
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.message(getId() + ".name");
    }

    /**
     * Gets the i18n short description of this <code>Option</code>.
     * Should be suitable for use as a tooltip text.
     *
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.message(getId() + ".shortDescription");
    }

    /**
     * Get the options in this group.
     *
     * @return The list of <code>Option</code>s.
     */
    public List<Option> getOptions() {
        return options;
    }

    /**
     * Get an option in this group (or descendents) by id.
     *
     * @param id The id to look for.
     * @return The option, or null if not found.
     */
    public Option getOption(String id) {
        return optionMap.get(id);
    }

    /**
     * Does this option group contain any subgroups?
     *
     * @return True if there are any child <code>OptionGroup</code>s present.
     */
    public boolean hasOptionGroup() {
        for (Option o : options) {
            if (o instanceof OptionGroup) return true;
        }
        return false;
    }

    /**
     * Adds the given <code>Option</code> to this group.
     *
     * @param option The <code>Option</code> to add.
     */
    public void add(Option option) {
        String id = option.getId();
        if (optionMap.containsKey(id)) {
            for (int index = 0; index < options.size(); index++) {
                if (id.equals(options.get(index).getId())) {
                    options.remove(index);
                    options.add(index, option);
                    break;
                }
            }
        } else {
            options.add(option);
        }
        optionMap.put(id, option);
        if (option instanceof OptionGroup) {
            OptionGroup group = (OptionGroup) option;
            group.setEditable(editable && group.isEditable());
            addOptionGroup(group);
        }
    }

    /**
     * Helper function to recursively add option group members to the
     * optionMap.
     *
     * @param group The initial <code>OptionGroup</code> to add.
     */
    private void addOptionGroup(OptionGroup group) {
        for (Option option : group.getOptions()) {
            optionMap.put(option.getId(), option);
            if (option instanceof OptionGroup) {
                addOptionGroup((OptionGroup) option);
            }
        }
    }

    /**
     * Removes all of the <code>Option</code>s from this
     * <code>OptionGroup</code>.
     */
    public void removeAll() {
        options.clear();
        optionMap.clear();
    }

    /**
     * Gets an <code>Iterator</code> for the <code>Option</code>s.
     *
     * @return The <code>Iterator</code>.
     */
    public Iterator<Option> iterator() {
        return options.iterator();
    }


    // Convenience accessors.

    /**
     * Gets the value of an option as an option group.
     *
     * @param id The id of the option.
     * @return The <code>OptionGroup</code> value.
     * @exception IllegalArgumentException If there is no option group
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public OptionGroup getOptionGroup(String id) {
        try {
            return ((OptionGroup) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No option group value associated with the specified option.");
        }
    }

    /**
     * Gets the integer value of an option.
     *
     * @param id The id of the option.
     * @return The integer value.
     * @exception IllegalArgumentException If there is no integer
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public int getInteger(String id) {
        try {
            return ((IntegerOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }

    /**
     * Sets the integer value of an option.
     *
     * @param id The id of the option.
     * @param value The new integer value of the option.
     * @exception IllegalArgumentException If there is no integer
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setInteger(String id, int value) {
        try {
            ((IntegerOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer value associated with the specified option.");
        }
    }

    /**
     * Gets the boolean value of an option.
     *
     * @param id The id of the option.
     * @return The boolean value.
     * @exception IllegalArgumentException If there is no boolean
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public boolean getBoolean(String id) {
        try {
            return ((BooleanOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }

    /**
     * Sets the boolean value of an option.
     *
     * @param id The id of the option.
     * @param value The new boolean value of the option.
     * @exception IllegalArgumentException If there is no boolean
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setBoolean(String id, boolean value) {
        try {
            ((BooleanOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean value associated with the specified option.");
        }
    }

    /**
     * Gets the string value of an option.
     *
     * @param id The id of the option.
     * @return The string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public String getString(String id) {
        try {
            return ((StringOption) getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String value associated with the specified option.");
        }
    }

    /**
     * Sets the string value of an option.
     *
     * @param id The id of the option.
     * @param value The new string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setString(String id, String value) {
        try {
            ((StringOption) getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String value associated with the specified option.");
        }
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup clone() throws CloneNotSupportedException {
        OptionGroup result = new OptionGroup(this.getId());
        result.editable = this.editable;
        result.setValues(this);
        result.options.addAll(this.options);
        result.optionMap.putAll(this.optionMap);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public OptionGroup getValue() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setValue(OptionGroup value) {
        if (value != null) {
            for (Option other : value.getOptions()) {
                Option mine = getOption(other.getId());
                // could be null if using custom.xml generated from an
                // older version of the specification, for example
                if (mine != null) {
                    mine.setValue(other.getValue());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setValue(String valueString, String defaultValueString) {
        // No op.  Needed to avoid endless warnings from parent implementation.
    }


    // Override AbstractOption

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }


    // Serialization

    private static final String EDITABLE_TAG = "editable";


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

        writeAttribute(out, EDITABLE_TAG, editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Option o : options) o.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        editable = getAttribute(in, EDITABLE_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Do *not* clear containers.
        // ATM OptionGroups are purely additive/overwriting.

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String optionId = readId(in);
        Option option = getOption(optionId);
        if (option == null) {
            AbstractOption abstractOption = readOption(in);
            if (abstractOption != null) {
                add(abstractOption);
                abstractOption.setGroup(this.getId());
            }
        } else {
            option.readFromXML(in);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder g = new StringBuilder();
        g.append("[").append(getId()).append("<");
        for (Option o : getOptions()) {
            g.append(" ").append(o.toString());
        }
        g.append(" >]\n");
        return g.toString();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "optionGroup".
     */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }
}
