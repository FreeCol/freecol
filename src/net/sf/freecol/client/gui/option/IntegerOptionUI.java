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

package net.sf.freecol.client.gui.option;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.sf.freecol.common.option.IntegerOption;


/**
 * This class provides visualization for an
 * {@link net.sf.freecol.common.option.IntegerOption} in order to enable
 * values to be both seen and changed.
 */
public final class IntegerOptionUI extends OptionUI<IntegerOption>  {

    private final JSpinner spinner = new JSpinner();


    /**
     * Creates a new {@code IntegerOptionUI} for the given
     * {@code IntegerOption}.
     *
     * @param option The {@code IntegerOption} to make a user interface for.
     * @param editable boolean whether user can modify the setting
     */
    public IntegerOptionUI(final IntegerOption option, boolean editable) {
        super(option, editable);

        final int value = option.getValue();
        int min = option.getMinimumValue();
        int max = option.getMaximumValue();
        if (min > max) {
            int tmp = min;
            min = max;
            max = tmp;
        }
        final int stepSize = Math.max(1, Math.min((max - min) / 10, 1000));
        this.spinner.setModel(new SpinnerNumberModel(value, min, max, stepSize));
        initialize();
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public JComponent getComponent() {
        return this.spinner;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue((Integer)this.spinner.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        this.spinner.setValue(getOption().getValue());
    }
}
