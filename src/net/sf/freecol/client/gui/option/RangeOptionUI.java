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

package net.sf.freecol.client.gui.option;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.RangeOption;

/**
 * This class provides visualization for an {@link RangeOption}. In order to
 * enable values to be both seen and changed.
 */
public final class RangeOptionUI extends JSlider implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(RangeOptionUI.class.getName());

    private final RangeOption option;
    private int originalValue;


    /**
     * Creates a new <code>RangeOptionUI</code> for the given
     * <code>RangeOption</code>.
     * 
     * @param option The <code>RangeOption</code> to make a user interface
     *            for.
     */
    public RangeOptionUI(final RangeOption option, boolean editable) {

        this.option = option;
        this.originalValue = option.getValue();

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK), 
                                                   Messages.getName(option)));
        String name = Messages.getName(option);
        String description = Messages.getShortDescription(option);

        setModel(new DefaultBoundedRangeModel(option.getValueRank(), 0, 0, option.getItemValues().size() - 1));
        setOrientation(JSlider.HORIZONTAL);
        Hashtable<Integer, JComponent> labels = new Hashtable<Integer, JComponent>();
        int index = 0;
        for (String string : option.getItemValues().values()) {
            if (option.localizeLabels()) {
                labels.put(index, new JLabel(Messages.message(string)));
            } else {
                labels.put(index, new JLabel(string));
            }
            index++;
        }

        setLabelTable(labels);
        setValue(option.getValueRank());
        setPaintLabels(true);
        setMajorTickSpacing(1);
        setExtent(0);
        setPaintTicks(true);
        setSnapToTicks(true);
        setPreferredSize(new Dimension(500, 50));
        setToolTipText((description != null) ? description : name);

        setEnabled(editable);
        setOpaque(false);
        addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (option.isPreviewEnabled()) {
                    final int value = getValue();
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
            if (value != getValue()) {
                setValue(value);
                originalValue = value;
            }
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValueRank(getValue());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        setValue(option.getValueRank());
    }
}
