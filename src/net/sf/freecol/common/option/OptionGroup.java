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

package net.sf.freecol.common.option;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Used for grouping {@link Option}s.
 */
public class OptionGroup extends AbstractOption<OptionGroup> {

    private static final Logger logger = Logger.getLogger(OptionGroup.class.getName());

    /** The options in this group. */
    private final List<Option> options = new ArrayList<>();

    /**
     * A map of all option ids to its option.  Unlike the options
     * array, this contains all child options of options that are
     * themselves groups.
     */
    private final Map<String, Option> optionMap = new HashMap<>();

    /** Is this option group user editable? */
    private boolean editable = true;


    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param id The object identifier.
     */
    public OptionGroup(String id) {
        super(id);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param specification The <code>Specification</code> to refer to.
     */
    public OptionGroup(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public OptionGroup(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new <code>OptionGroup</code>.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @param specification The <code>Specification</code> to refer to.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public OptionGroup(FreeColXMLReader xr,
                       Specification specification) throws XMLStreamException {
        super(specification);

        readFromXML(xr);
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
        return Messages.getName(getId());
    }

    /**
     * Gets the i18n short description of this <code>Option</code>.
     * Should be suitable for use as a tooltip text.
     *
     * @return A short description of this <code>Option</code>.
     */
    public String getShortDescription() {
        return Messages.getShortDescription(getId());
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
     * Get an option in this group (or descendents) by object identifier.
     *
     * @param id The object identifier.
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
        return any(options, o -> o instanceof OptionGroup);
    }

    /**
     * Adds the given option to this group.  The option is assumed to
     * be correct.
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
     * Merges the given <code>Option</code> into this group.
     *
     * - Option groups are *not* merged, but their leaf options are.
     * - If an option is not already present it is ignored.
     * - If an option is present, it is merged but in the option group where
     *   it is already placed.
     *
     * The intent is that the option group structure is never subject to
     * merging.
     *
     * @param option The <code>Option</code> to merge.
     * @return True if the merge was accepted.
     */
    public boolean merge(Option option) {
        final String id = option.getId();

        // Check first, it is valid to merge an option group onto
        // one at the same level, for which it will not contain a key.
        if (option instanceof OptionGroup) {
            OptionGroup optionGroup = (OptionGroup)option;
            boolean result = true;
            for (Option o : optionGroup.getOptions()) {
                // Merge from the top level, so that the new
                // option will end up in the group inherited
                // from the standard client-options.xml.
                result = result && this.merge(o);
            }
            if (result) {
                optionGroup.setEditable(editable && optionGroup.isEditable());
            }
            logger.finest("Merged option group " + id
                + " contents into " + this.getId());
            return result;
        }
                
        if (!optionMap.containsKey(id)) {
            logger.warning("Ignoring unknown option " + id);
            return false;
        }

        for (int index = 0; index < options.size(); index++) {
            Option o = options.get(index);
            if (id.equals(o.getId())) { // Found it, replace and return true
                options.remove(index);
                options.add(index, option);
                optionMap.put(id, option);
                logger.finest("Merged option " + id + " into " + this.getId()
                    + ": " + option.toString() + "/");
                return true;
            }
            if (o instanceof OptionGroup) {
                OptionGroup og = (OptionGroup)o;
                if (og.optionMap.containsKey(id) && og.merge(option)) {
                    optionMap.put(id, option);
                    return true;
                }
            }
        }
        logger.warning("Option " + id + " registered but not found!");
        return false;
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
     * Remove an option.
     *
     * @param id The identifier of the option to remove.
     * @return The <code>Option</code> removed if any.
     */
    public Option remove(String id) {
        Option op = optionMap.remove(id);
        if (op != null) options.remove(op);
        return op;
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
     * @param id The object identifier.
     * @return The <code>OptionGroup</code> value.
     * @exception IllegalArgumentException If there is no option group
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public OptionGroup getOptionGroup(String id) {
        try {
            return ((OptionGroup)getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No option group"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Gets the integer value of an option.
     *
     * @param id The object identifier.
     * @return The integer value.
     * @exception IllegalArgumentException If there is no integer
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public int getInteger(String id) {
        try {
            return ((IntegerOption)getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Sets the integer value of an option.
     *
     * @param id The object identifier.
     * @param value The new integer value of the option.
     * @exception IllegalArgumentException If there is no integer
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setInteger(String id, int value) {
        try {
            ((IntegerOption)getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No integer"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Gets the boolean value of an option.
     *
     * @param id The object identifier.
     * @return The boolean value.
     * @exception IllegalArgumentException If there is no boolean
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public boolean getBoolean(String id) {
        try {
            return ((BooleanOption)getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Sets the boolean value of an option.
     *
     * @param id The object identifier.
     * @param value The new boolean value of the option.
     * @exception IllegalArgumentException If there is no boolean
     *     value associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setBoolean(String id, boolean value) {
        try {
            ((BooleanOption)getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No boolean"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Gets the string value of an option.
     *
     * @param id The object identifier.
     * @return The string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public String getString(String id) {
        try {
            return ((StringOption)getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Sets the string value of an option.
     *
     * @param id The object identifier.
     * @param value The new string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setString(String id, String value) {
        try {
            ((StringOption)getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Gets the string value of an option.
     *
     * @param id The object identifier.
     * @return The string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public String getText(String id) {
        try {
            return ((TextOption)getOption(id)).getValue();
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String"
                + " value associated with the specified option: " + id, e);
        }
    }

    /**
     * Sets the string value of an option.
     *
     * @param id The object identifier.
     * @param value The new string value.
     * @exception IllegalArgumentException If there is no string value
     *     associated with the specified option.
     * @exception NullPointerException if the given
     *     <code>Option</code> does not exist.
     */
    public void setText(String id, String value) {
        try {
            ((TextOption)getOption(id)).setValue(value);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("No String"
                + " value associated with the specified option: " + id, e);
        }
    }


    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup clone() {
        OptionGroup result = new OptionGroup(this.getId(), getSpecification());
        result.editable = this.editable;
        result.setValues(this);
        result.options.addAll(this.options);
        result.optionMap.putAll(this.optionMap);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getValue() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setValue(OptionGroup value) {
        if (value != null) {
            for (Option other : value.getOptions()) {
                Option mine = getOption(other.getId());
                // could be null if using custom options generated
                // from an older version of the specification
                if (mine != null) {
                    mine.setValue(other.getValue());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(EDITABLE_TAG, editable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (Option o : options) o.toXML(xw);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        editable = xr.getAttribute(EDITABLE_TAG, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Do *not* clear containers.
        // ATM OptionGroups are purely additive/overwriting.

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readChild(FreeColXMLReader xr) throws XMLStreamException {
        String optionId = xr.readId();
        Option option = getOption(optionId);
        if (option == null) {
            AbstractOption abstractOption = readOption(xr);
            if (abstractOption != null) {
                add(abstractOption);
                abstractOption.setGroup(this.getId());
            }
        } else {
            // FreeColActions are read here.
            option.readFromXML(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(getId()).append(" <");
        for (Option o : getOptions()) {
            sb.append(" ").append(o.toString());
        }
        sb.append(" >]");
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
     * @return "optionGroup".
     */
    public static String getXMLElementTagName() {
        return "optionGroup";
    }
}
