/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import net.sf.freecol.client.gui.i18n.Messages;
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

    /**
     * Whether the Option should be editable.
     */
    private boolean editable;

    /**
     * The label to use for the Option.
     */
    private JLabel label = new JLabel();

    /**
     * The Option value itself.
     */
    private T option;

    protected final GUI gui;

    /**
     * Constructor.
     *
     * @param option the Option
     * @param editable whether the Option should be editable
     */
    public OptionUI(GUI gui, T option, boolean editable) {
        this.gui = gui;
        this.option = option;
        this.editable = editable;

        String name = Messages.getName(option);
        String text = Messages.getDescription(option);
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


    /**
     * Get the <code>Option</code> value.
     *
     * @return an <code>T</code> value
     */
    public final T getOption() {
        return option;
    }

    /**
     * Set the <code>Option</code> value.
     *
     * @param newOption The new Option value.
     */
    public final void setOption(final T newOption) {
        this.option = newOption;
    }

    /**
     * Get the <code>Editable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isEditable() {
        return editable;
    }

    /**
     * Set the <code>Editable</code> value.
     *
     * @param newEditable The new Editable value.
     */
    public final void setEditable(final boolean newEditable) {
        this.editable = newEditable;
    }

    /**
     * Get the <code>Label</code> value.
     *
     * @return a <code>JLabel</code> value
     */
    public JLabel getLabel() {
        return label;
    }

    /**
     * Set the <code>Label</code> value.
     *
     * @param label a <code>JLabel</code> value
     */
    protected void setLabel(JLabel label) {
        this.label = label;
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
    public abstract void updateOption();

    /**
     * Reset the value of the UI's component from the Option.
     */
    public abstract void reset();

    /**
     * Returns a ListCellRenderer suitable for the Option wrapped.
     *
     * @return a ListCellRenderer
     */
    public ListCellRenderer getListCellRenderer() {
        return null;
    }


    @SuppressWarnings("unchecked")
    public static OptionUI getOptionUI(GUI gui, Option option, boolean editable) {
        if (option instanceof BooleanOption) {
            return new BooleanOptionUI(gui, (BooleanOption) option, editable);
        } else if (option instanceof FileOption) {
            return new FileOptionUI(gui, (FileOption) option, editable);
        } else if (option instanceof PercentageOption) {
            return new PercentageOptionUI(gui, (PercentageOption) option, editable);
        } else if (option instanceof RangeOption) {
            return new RangeOptionUI(gui, (RangeOption) option, editable);
        } else if (option instanceof SelectOption) {
            return new SelectOptionUI(gui, (SelectOption) option, editable);
        } else if (option instanceof IntegerOption) {
            return new IntegerOptionUI(gui, (IntegerOption) option, editable);
        } else if (option instanceof StringOption) {
            return new StringOptionUI(gui, (StringOption) option, editable);
        } else if (option instanceof LanguageOption) {
            return new LanguageOptionUI(gui, (LanguageOption) option, editable);
        } else if (option instanceof AudioMixerOption) {
            return new AudioMixerOptionUI(gui, (AudioMixerOption) option, editable);
        } else if (option instanceof FreeColAction) {
            return new FreeColActionUI(gui, (FreeColAction) option, editable);
        } else if (option instanceof AbstractUnitOption) {
            return new AbstractUnitOptionUI(gui, (AbstractUnitOption) option, editable);
        } else if (option instanceof ModOption) {
            return new ModOptionUI(gui, (ModOption) option, editable);
        } else if (option instanceof UnitListOption) {
            return new ListOptionUI(gui, (UnitListOption) option, editable);
        } else if (option instanceof ModListOption) {
            return new ListOptionUI(gui, (ModListOption) option, editable);
        } else if (option instanceof TextOption) {
            return new TextOptionUI(gui, (TextOption) option, editable);
        } else {
            return null;
        }
    }

}
