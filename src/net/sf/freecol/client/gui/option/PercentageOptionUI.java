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
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;

/**
 * This class provides visualization for an {@link RangeOption}. In order to
 * enable values to be both seen and changed.
 */
public final class PercentageOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(PercentageOptionUI.class.getName());

    private final PercentageOption option;

    private final JSlider slider;
    private int originalValue;


    /**
     * Creates a new <code>PercentageOptionUI</code> for the given
     * <code>PercentageOption</code>.
     * 
     * @param option The <code>PercentageOption</code> to make a user interface
     *            for.
     */
    public PercentageOptionUI(final PercentageOption option, boolean editable) {

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), 
                                                   option.getName()));
        this.option = option;
        this.originalValue = option.getValue();

        String name = option.getName();
        String description = option.getShortDescription();

        slider = new JSlider(JSlider.HORIZONTAL, 0, 100, option.getValue());
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        labels.put(new Integer(0), new JLabel("0 %"));
        labels.put(new Integer(25), new JLabel("25 %"));
        labels.put(new Integer(50), new JLabel("50 %"));
        labels.put(new Integer(75), new JLabel("75 %"));
        labels.put(new Integer(100), new JLabel("100 %"));
        slider.setLabelTable(labels);
        slider.setValue(option.getValue());
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(5);
        slider.setExtent(0);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(false);
        slider.setPreferredSize(new Dimension(500, 50));
        slider.setToolTipText((description != null) ? description : name);
        add(slider);

        slider.setEnabled(editable);
        slider.setOpaque(false);

        slider.addChangeListener(new ChangeListener () {
            public void stateChanged(ChangeEvent e) {
                if (option.isPreviewEnabled()) {
                    if (option.getValue() != slider.getValue()) {
                        option.setValue(slider.getValue());
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
        option.setValue(slider.getValue());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        slider.setValue(option.getValue());
    }
}
