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

import java.util.Hashtable;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;

import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.option.RangeOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.RangeOption} in order to enable
 * values to be both seen and changed.
 */
public final class RangeOptionUI extends SliderOptionUI<RangeOption>  {

    /**
     * Creates a new <code>RangeOptionUI</code> for the given
     * <code>RangeOption</code>.
     *
     * @param option The <code>RangeOption</code> to make a user interface for
     * @param editable boolean whether user can modify the setting
     */
    public RangeOptionUI(final RangeOption option, boolean editable) {
        super(option, editable);

        JSlider slider = getComponent();

        slider.setModel(new DefaultBoundedRangeModel(option.getValueRank(), 0,
                0, option.getItemValues().size()-1));

        Hashtable<Integer, JComponent> labels
            = new Hashtable<>();
        int index = 0;
        for (String string : option.getItemValues().values()) {
            if (option.localizeLabels()) {
                labels.put(index, Utility.localizedLabel(string));
            } else {
                labels.put(index, new JLabel(string));
            }
            index++;
        }

        slider.setLabelTable(labels);
        slider.setValue(option.getValueRank());
        slider.setMajorTickSpacing(1);
        slider.setSnapToTicks(true);
    }


    // Implement OptionUpdater

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValueRank(getComponent().getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        getComponent().setValue(getOption().getValueRank());
    }
}
