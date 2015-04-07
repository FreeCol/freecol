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

import net.sf.freecol.common.option.PercentageOption;


/**
 * This class provides visualization for a
 * {@link net.sf.freecol.common.option.PercentageOption}
 * in order to enable values to be both seen and changed.
 */
public final class PercentageOptionUI extends SliderOptionUI<PercentageOption>  {

    /**
     * Creates a new <code>PercentageOptionUI</code> for the given
     * <code>PercentageOption</code>.
     *
     * @param option The <code>PercentageOption</code> to make a user
     *     interface for.
     * @param editable boolean whether user can modify the setting
     */
    public PercentageOptionUI(final PercentageOption option, boolean editable) {
        super(option, editable);

        JSlider slider = getComponent();

        slider.setModel(new DefaultBoundedRangeModel(option.getValue(), 0, 0, 100));
        Hashtable<Integer, JComponent> labels
            = new Hashtable<>();
        labels.put(0,   new JLabel("0 %"));
        labels.put(25,  new JLabel("25 %"));
        labels.put(50,  new JLabel("50 %"));
        labels.put(75,  new JLabel("75 %"));
        labels.put(100, new JLabel("100 %"));
        slider.setLabelTable(labels);
        slider.setValue(option.getValue());
        slider.setMajorTickSpacing(5);
        slider.setSnapToTicks(false);
    }
}
