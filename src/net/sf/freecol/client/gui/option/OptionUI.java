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

package net.sf.freecol.client.gui.option;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.ModListOption;
import net.sf.freecol.common.option.ModOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.SelectOption;
import net.sf.freecol.common.option.StringOption;
import net.sf.freecol.common.option.TextOption;
import net.sf.freecol.common.option.UnitListOption;


/**
 * This class provides common methods for various Option UIs.
 */
public abstract class OptionUI<T extends Option<?>> implements OptionUpdater {

    /** Whether the Option should be editable. */
    private boolean editable;

    /** The label to use for the Option. */
    private JLabel label = new JLabel();

    /** The Option value itself. */
    private T option;


    /**
     * Constructor.
     *
     * @param option The <code>Option</code> to display.
     * @param editable True if the option should be editable.
     */
    public OptionUI(T option, boolean editable) {
        this.option = option;
        this.editable = editable;

        String name = Messages.getName(option.getId());
        String text = Messages.getBestDescription(option);
        label.setText(name);
        label.setToolTipText(text);
    }


    /**
     * Set up component.
     */
    protected void initialize() {
        JComponent component = getComponent();
        component.setToolTipText(label.getToolTipText());
        component.setEnabled(editable);
        component.setOpaque(false);
    }

    public final T getOption() {
        return option;
    }

    public final void setOption(final T newOption) {
        this.option = newOption;
    }

    public final boolean isEditable() {
        return editable;
    }

    public final void setEditable(final boolean newEditable) {
        this.editable = newEditable;
    }

    /**
     * Get an option UI for a given option.
     *
     * @param gui The <code>GUI</code> to use.
     * @param option The <code>Option</code> to check.
     * @param editable Should the result be editable.
     * @return A suitable <code>OptionUI</code>, or null if none found.
     */
    public static OptionUI getOptionUI(GUI gui, Option option, boolean editable) {
        if (option instanceof BooleanOption) {
            return new BooleanOptionUI((BooleanOption)option, editable);
        } else if (option instanceof FileOption) {
            return new FileOptionUI(gui, (FileOption)option, editable);
        } else if (option instanceof PercentageOption) {
            return new PercentageOptionUI((PercentageOption)option, editable);
        } else if (option instanceof RangeOption) {
            return new RangeOptionUI((RangeOption)option, editable);
        } else if (option instanceof SelectOption) {
            return new SelectOptionUI((SelectOption)option, editable);
        } else if (option instanceof IntegerOption) {
            return new IntegerOptionUI((IntegerOption)option, editable);
        } else if (option instanceof StringOption) {
            return new StringOptionUI((StringOption)option, editable);
        } else if (option instanceof LanguageOption) {
            return new LanguageOptionUI((LanguageOption)option, editable);
        } else if (option instanceof AudioMixerOption) {
            return new AudioMixerOptionUI(gui, (AudioMixerOption)option, editable);
        } else if (option instanceof FreeColAction) {
            return new FreeColActionUI((FreeColAction)option, editable);
        } else if (option instanceof AbstractUnitOption) {
            return new AbstractUnitOptionUI((AbstractUnitOption)option, editable);
        } else if (option instanceof ModOption) {
            return new ModOptionUI((ModOption)option, editable);
        } else if (option instanceof UnitListOption) {
            return new ListOptionUI<>(gui, (UnitListOption)option, editable);
        } else if (option instanceof ModListOption) {
            return new ListOptionUI<>(gui, (ModListOption)option, editable);
        } else if (option instanceof TextOption) {
            return new TextOptionUI((TextOption)option, editable);
        } else {
            return null;
        }
    }


    // Routines to be implemented/overridden

    public JLabel getJLabel() {
        return label;
    }

    protected void setLabel(JLabel label) {
        this.label = label;
    }

    /**
     * Get a ListCellRenderer suitable for the wrapped Option.
     *
     * @return A suitable ListCellRenderer.
     */
    public ListCellRenderer getListCellRenderer() {
        return null;
    }

    /**
     * Get the <code>Component</code> used to set the value of the
     * Option.
     *
     * @return a <code>JComponent</code> value
     */
    public abstract JComponent getComponent();

    /**
     * Update the value of the Option from the UI's component.
     */
    @Override
    public abstract void updateOption();

    /**
     * Reset the value of the UI's component from the Option.
     */
    @Override
    public abstract void reset();
}
