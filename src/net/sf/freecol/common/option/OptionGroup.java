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

package net.sf.freecol.common.option;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.LogBuilder;


/**
 * Used for grouping {@link Option}s.
 */
public class OptionGroup extends AbstractOption<OptionGroup>
    implements OptionContainer {

    private static final Logger logger = Logger.getLogger(OptionGroup.class.getName());

    public static final String TAG = "optionGroup";

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

    /** Should this option group be visible? */
    private boolean visible = true;
    

    /**
     * Creates a new {@code OptionGroup}.
     *
     * @param id The object identifier.
     */
    public OptionGroup(String id) {
        super(id);
    }

    /**
     * Creates a new {@code OptionGroup}.
     *
     * @param specification The {@code Specification} to refer to.
     */
    public OptionGroup(Specification specification) {
        super(specification);
    }

    /**
     * Creates a new {@code OptionGroup}.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public OptionGroup(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Creates a new {@code OptionGroup}.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param specification The {@code Specification} to refer to.
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
     * Should this option group be visible?
     *
     * @return True if the option group should be visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Gets the i18n-name of this {@code Option}.
     *
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return Messages.getName(getId());
    }

    /**
     * Gets the i18n short description of this {@code Option}.
     * Should be suitable for use as a tooltip text.
     *
     * @return A short description of this {@code Option}.
     */
    public String getShortDescription() {
        return Messages.getShortDescription(getId());
    }

    /**
     * Get the options in this group.
     *
     * @return The list of {@code Option}s.
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
     * @return True if there are any child {@code OptionGroup}s present.
     */
    public boolean hasOptionGroup() {
        return any(options, o -> o instanceof OptionGroup);
    }

    /**
     * Adds the given option to this group.  The option is assumed to
     * be correct.
     *
     * @param option The {@code Option} to add.
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
     * Merges the given {@code Option} into this group.
     *
     * - Option groups are *not* merged, but their leaf options are.
     * - If an option is not already present it is ignored.
     * - If an option is present, it is merged but in the option group where
     *   it is already placed.
     *
     * The intent is that the option group structure is never subject to
     * merging.
     *
     * @param option The {@code Option} to merge.
     * @param lb A {@code LogBuilder} to log to.
     * @return True if the merge was accepted.
     */
    public boolean merge(Option option, LogBuilder lb) {
        final String id = option.getId();
        OptionGroup etc = null;

        // Check first, it is valid to merge an option group onto
        // one at the same level, for which it will not contain a key.
        if (option instanceof OptionGroup) {
            OptionGroup optionGroup = (OptionGroup)option;
            boolean result = true;
            for (Option o : optionGroup.getOptions()) {
                // @compat 0.11.6
                // Placement options move to their own group
                if (o.getId().startsWith("net.sf.freecol.client.gui.panel.")) {
                    if (etc == null) etc = getOptionGroup(ClientOptions.ETC);
                    etc.add(o);
                    lb.add(o.getId(), " moved to ", ClientOptions.ETC, ".\n");
                    continue;
                }
                // end @compat 0.11.6
                
                // Merge from the top level, so that the new
                // option will end up in the group inherited
                // from the standard client-options.xml.
                result = result && this.merge(o, lb);
            }
            if (result) {
                optionGroup.setEditable(editable && optionGroup.isEditable());
            }
            lb.add(id, " group merged contents into ", this.getId(), ".\n");
            return result;
        }
                
        if (!optionMap.containsKey(id)) {
            lb.add(id, " unknown/ignored.\n");
            return false;
        }

        for (int index = 0; index < options.size(); index++) {
            Option o = options.get(index);
            if (id.equals(o.getId())) { // Found it, replace and return true
                options.remove(index);
                options.add(index, option);
                optionMap.put(id, option);
                lb.add(id, " merged into ", this.getId(),
                    ": ", option.toString() + ".\n");
                return true;
            }
            if (o instanceof OptionGroup) {
                OptionGroup og = (OptionGroup)o;
                if (og.optionMap.containsKey(id) && og.merge(option, lb)) {
                    optionMap.put(id, option);
                    return true;
                }
            }
        }
        lb.add(id, " registered but not found!\n");
        return false;
    }
        
    /**
     * Helper function to recursively add option group members to the
     * optionMap.
     *
     * @param group The initial {@code OptionGroup} to add.
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
     * @return The {@code Option} removed if any.
     */
    public Option remove(String id) {
        Option op = optionMap.remove(id);
        if (op != null) options.remove(op);
        return op;
    }

    /**
     * Removes all of the {@code Option}s from this
     * {@code OptionGroup}.
     */
    public void removeAll() {
        options.clear();
        optionMap.clear();
    }

    /**
     * Loads the options from the given reader.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @return True if the options were loaded without error.
     * @exception XMLStreamException if there is an error reading the stream.
     */
    protected boolean load(FreeColXMLReader xr) throws XMLStreamException {
        if (xr == null) return false;
        xr.nextTag();
        readFromXML(xr);
        return true;
    }
    
    /**
     * Loads the options from the given file.
     *
     * @param file The {@code File} to read the options from.
     * @return True if the options were loaded without error.
     */
    public boolean load(File file) {
        if (file == null) return false;
        boolean ret = false;
        try (
             FreeColXMLReader xr = new FreeColXMLReader(file);
             ) {
            ret = load(xr);
        } catch (IOException|XMLStreamException xse) {
            logger.log(Level.WARNING, "Load OptionGroup(" + getId()
                + ") from file " + file.getPath() + " crashed", xse);
            return false;
        }
        logger.info("Load OptionGroup(" + getId() + ") from " + file.getPath()
            + ((ret) ? " succeeded" : " failed"));
        return ret;
    }

    /**
     * Load a specific option group from a file by identifier.
     *
     * @param file The {@code File} to load from.
     * @param optionId The option identifier to look for.
     * @param spec A base {@code Specification}.
     * @return The {@code OptionGroup} found, or null on error or
     *     not found.
     */
    public static OptionGroup loadOptionGroup(File file, String optionId,
                                              Specification spec) {
        OptionGroup ret = new OptionGroup(spec);
        if (ret.load(file)) {
            if (!optionId.equals(ret.getId())) {
                Option op = ret.getOption(optionId);
                ret = (op instanceof OptionGroup) ? (OptionGroup)op : null;
            }
        } else {
            ret = null;
        }
        return ret;
    }


    // Interface OptionContainer

    /**
     * {@inheritDoc}
     */    
    public <T extends Option> boolean hasOption(String id,
                                                Class<T> returnClass) {
        if (id == null) return false;
        Option val = this.optionMap.get(id);
        return (val == null) ? false
            : returnClass.isAssignableFrom(val.getClass());
    }

    /**
     * {@inheritDoc}
     */
    public <T extends Option> T getOption(String id,
                                          Class<T> returnClass) {
        if (id == null) {
            throw new RuntimeException("Null id: " + this);
        } else if (!this.optionMap.containsKey(id)) {
            throw new RuntimeException("Missing option: " + id);
        } else {
            try {
                return returnClass.cast(optionMap.get(id));
            } catch (ClassCastException cce) {
                throw new RuntimeException("Not a " + returnClass.getName()
                    + ": " + id, cce);
            }
        }
    }

    
    // Interface Option

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup cloneOption() {
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
    public void generateChoices() {
        for (Option o : options) {
            if (o instanceof OptionGroup) {
                ((OptionGroup)o).generateChoices();
            } else if (o instanceof AbstractOption) {
                ((AbstractOption)o).generateChoices();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isNullValueOK() {
        return true;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean save(File file) {
        boolean ret = false;
        try {
            ret = this.save(file, null, true);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Save OptionGroup(" + getId()
                + ") to " + file.getPath() + " crashed", e);
            return false;
        }
        logger.info("Save OptionGroup(" + getId() + ") to " + file.getPath()
            + ((ret) ? " succeeded" : " failed"));
        return ret;
    }
    

    // Serialization

    private static final String EDITABLE_TAG = "editable";
    private static final String VISIBLE_TAG = "visible";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(EDITABLE_TAG, editable);

        xw.writeAttribute(VISIBLE_TAG, visible);
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

        visible = xr.getAttribute(VISIBLE_TAG, true);
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
    public String getXMLTagName() { return TAG; }


    // Override Object
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(getId()).append(" <");
        for (Option o : getOptions()) {
            sb.append(' ').append(o.toString());
        }
        sb.append(" >]");
        return sb.toString();
    }
}
