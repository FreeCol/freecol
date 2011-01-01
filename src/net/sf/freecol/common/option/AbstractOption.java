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

package net.sf.freecol.common.option;

import java.util.logging.Logger;

import net.sf.freecol.common.model.FreeColObject;

/**
 * The super class of all options. GUI components making use of this class can
 * refer to its name and shortDescription properties. The complete keys of these
 * properties consist of the id of the option group (if any), followed by a "."
 * unless the option group is null, followed by the id of the option object,
 * followed by a ".", followed by "name" or "shortDescription".
 */
abstract public class AbstractOption<T> extends FreeColObject implements Option {

    @SuppressWarnings("unused")
    private static Logger logger = Logger.getLogger(AbstractOption.class.getName());

    private String optionGroup = "";

    // Determine if the option has been defined
    // When defined an option won't change when a default value is read from an
    // XML file.
    protected boolean isDefined = false;

    protected boolean previewEnabled = false;


    /**
     * Creates a new <code>AbstractOption</code>.
     *
     * @param id The identifier for this option. This is used when the object
     *            should be found in an {@link OptionGroup}.
     */
    public AbstractOption(String id) {
        setId(id);
    }

    /**
     * Should this option be updated directly so that
     * changes may be previewes?
     *
     * @return <code>true</code> if changes to this
     *      option should be made directly (and reset
     *      back later if the changes are not stored).
     */
    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    /**
     * Sets if this option should be updated directly.
     *
     * @param previewEnabled <code>true</code> if changes
     *      to this option should be made directly (and
     *      reset back later if the changes are not stored).
     */
    public void setPreviewEnabled(boolean previewEnabled) {
        this.previewEnabled = previewEnabled;
    }

    /**
     * Returns the string prefix that identifies the group of this
     * <code>Option</code>.
     *
     * @return The string prefix provided by the OptionGroup.
     */
    public String getGroup() {
        return optionGroup;
    }

    /**
     * Set the option group
     *
     * @param group <code>OptionGroup</code> to set
     *
     */
    public void setGroup(String group) {
        if (group == null) {
            optionGroup = "";
        } else {
            optionGroup = group;
        }
    }

    public abstract T getValue();

    public abstract void setValue(T value);

}
