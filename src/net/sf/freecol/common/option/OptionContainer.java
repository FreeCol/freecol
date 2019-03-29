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
import java.util.List;

import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.model.AbstractUnit;


/**
 * The routines common to option containers.
 */
public interface OptionContainer {

    /**
     * Is an option present in the container.
     *
     * @param <T> The actual return type.
     * @param id The object identifier.
     * @param returnClass The expected option class.
     * @return True if the option is present.
     */
    public <T extends Option> boolean hasOption(String id, Class<T> returnClass);
    
    /**
     * Get an option in this group (or descendents) by object identifier.
     *
     * @param <T> The actual return type.
     * @param id The object identifier.
     * @param returnClass The expected option class.
     * @return The option, or a run time exception if the option does not
     *     exist or is of the wrong class.
     */
    public <T extends Option> T getOption(String id, Class<T> returnClass);

    /**
     * Gets the value of a {@code BooleanOption}.
     *
     * @param id The object identifier.
     * @return The boolean value.
     */
    default boolean getBoolean(String id) {
        return getOption(id, BooleanOption.class).getValue();
    }

    /**
     * Sets the value of a {@code BooleanOption}.
     *
     * @param id The object identifier.
     * @param value The new boolean value of the option.
     */
    default void setBoolean(String id, boolean value) {
        getOption(id, BooleanOption.class).setValue(value);
    }

    /**
     * Gets the file value of a {@code FileOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default File getFile(String id) {
        return getOption(id, FileOption.class).getValue();
    }

    /**
     * Sets the value of a {@code FileOption}.
     *
     * @param id The object identifier.
     * @param value The new value.
     */
    default void setFile(String id, File value) {
        getOption(id, FileOption.class).setValue(value);
    }

    /**
     * Gets the value of an {@code IntegerOption}.
     *
     * @param id The object identifier.
     * @return The integer value.
     */
    default int getInteger(String id) {
        return getOption(id, IntegerOption.class).getValue();
    }

    /**
     * Sets the value of an {@code IntegerOption}.
     *
     * @param id The object identifier.
     * @param value The new integer value of the option.
     */
    default void setInteger(String id, int value) {
        getOption(id, IntegerOption.class).setValue(value);
    }

    /**
     * Gets the minimum value of an {@code IntegerOption}.
     *
     * @param id The object identifier.
     * @return value The minimum value.
     */
    default int getIntegerMinimum(String id) {
        return getOption(id, IntegerOption.class).getMinimumValue();
    }

    /**
     * Sets the minimum value of an {@code IntegerOption}.
     *
     * @param id The object identifier.
     * @param value The new minimum value.
     */
    default void setIntegerMinimum(String id, Integer value) {
        getOption(id, IntegerOption.class).setMinimumValue(value);
    }

    /**
     * Gets the mod list from a {@code ModListOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default List<FreeColModFile> getModList(String id) {
        return getOption(id, ModListOption.class).getOptionValues();
    }

    /**
     * Gets the value of an {@code OptionGroup}.
     *
     * @param id The object identifier.
     * @return The {@code OptionGroup} value.
     */
    default OptionGroup getOptionGroup(String id) {
        return getOption(id, OptionGroup.class).getValue();
    }

    /**
     * Gets the percentage value of a {@code PercentageOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default int getPercentage(String id) {
        return getOption(id, PercentageOption.class).getValue();
    }

    /**
     * Gets the percentage value of a {@code PercentageOption} normalized
     * to double.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default double getPercentageMultiplier(String id) {
        return 0.01 * getPercentage(id);
    }

    /**
     * Gets the value of a {@code RangeOption}.
     *
     * @param id The object identifier.
     * @return The range value.
     */
    default int getRange(String id) {
        return getOption(id, RangeOption.class).getValue();
    }

    /**
     * Gets the string value of an option.
     *
     * @param id The object identifier.
     * @return The string value.
     */
    default String getString(String id) {
        return getOption(id, StringOption.class).getValue();
    }

    /**
     * Sets the string value of an option.
     *
     * @param id The object identifier.
     * @param value The new string value.
     */
    default void setString(String id, String value) {
        getOption(id, StringOption.class).setValue(value);
    }

    /**
     * Gets the value of a {@code SelectOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default int getSelection(String id) {
        return getOption(id, SelectOption.class).getValue();
    }

    /**
     * Gets the name associated with the current value of a
     * {@code SelectOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default String getSelectionName(String id) {
        return getOption(id, SelectOption.class).getName();
    }

    /**
     * Gets the value of a {@code TextOption}.
     *
     * @param id The object identifier.
     * @return The string value.
     */
    default String getText(String id) {
        return getOption(id, TextOption.class).getValue();
    }

    /**
     * Sets the value of a {@code TextOption}.
     *
     * @param id The object identifier.
     * @param value The new string value.
     */
    default void setText(String id, String value) {
        getOption(id, TextOption.class).setValue(value);
    }

    /**
     * Gets the unit list from a {@code UnitListOption}.
     *
     * @param id The object identifier.
     * @return The value.
     */
    default List<AbstractUnit> getUnitList(String id) {
        return getOption(id, UnitListOption.class).getOptionValues();
    }
}
