/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.RangeOption;

/**
 * This class provides visualization for an {@link RangeOption}. In order to
 * enable values to be both seen and changed.
 */
public final class RangeOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RangeOptionUI.class.getName());

    private final RangeOption option;
    private final JSlider slider;
    private int originalValue;


    /**
     * Creates a new <code>RangeOptionUI</code> for the given
     * <code>RangeOption</code>.
     * 
     * @param option The <code>RangeOption</code> to make a user interface
     *            for.
     */
    public RangeOptionUI(final RangeOption option, boolean editable) {

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), 
                                                   option.getName()));
        this.option = option;
        this.originalValue = option.getValue();

        String name = option.getName();
        String description = option.getShortDescription();
        //JLabel label = new JLabel(name, JLabel.LEFT);
        //label.setToolTipText((description != null) ? description : name);
        //add(label);

        String[] strings = option.getItemValues().values().toArray(new String[0]);

        slider = new JSlider(JSlider.HORIZONTAL, 0, strings.length - 1, option.getValueRank());
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        for (int i = 0; i < strings.length; i++) {
            labels.put(new Integer(i), new JLabel(strings[i]));
        }

        slider.setLabelTable(labels);
        slider.setValue(option.getValueRank());
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(1);
        slider.setExtent(0);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.setPreferredSize(new Dimension(500, 50));
        slider.setToolTipText((description != null) ? description : name);
        add(slider);

        slider.setEnabled(editable);
        slider.setOpaque(false);
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (option.isPreviewEnabled()) {
                    final int value = slider.getValue();
                    if (option.getValue() != value) {
                        option.setValueRank(value);
                    }
                }
            }
        });

        option.addPropertyChangeListener(this);
        setOpaque(false);
    }

    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        option.setValue(originalValue);
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        option.removePropertyChangeListener(this);
    }

    /**
     * Updates this UI with the new data from the option.
     * 
     * @param event The event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            final int value = ((Integer) event.getNewValue()).intValue();
            if (value != slider.getValue()) {
                slider.setValue(value);
                originalValue = value;
            }
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValueRank(slider.getValue());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        slider.setValue(option.getValueRank());
    }
}
