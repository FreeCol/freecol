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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;


/**
 * This class provides visualization for an {@link IntegerOption}. In order to
 * enable values to be both seen and changed.
 */
public final class IntegerOptionUI extends JSpinner implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(IntegerOptionUI.class.getName());


    private final IntegerOption option;
    private int originalValue;
    private JLabel label;


    /**
     * Creates a new <code>IntegerOptionUI</code> for the given <code>IntegerOption</code>.
     * @param option The <code>IntegerOption</code> to make a user interface for.
     */
    public IntegerOptionUI(final IntegerOption option, boolean editable) {

        this.option = option;
        this.originalValue = option.getValue();

        String name = Messages.getName(option);
        String description = Messages.getShortDescription(option);
        String text = (description != null) ? description : name;
        label = new JLabel(name, JLabel.LEFT);
        label.setToolTipText(text);
        setToolTipText(text);

        setEnabled(editable);
        setOpaque(false);

        if (editable) {
            int stepSize = Math.min((option.getMaximumValue() - option.getMinimumValue()) / 10, 1000);
            setModel(new SpinnerNumberModel((int) option.getValue(), option.getMinimumValue(),
                                            option.getMaximumValue(), Math.max(1, stepSize)));
            addChangeListener(new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        if (option.isPreviewEnabled()) {
                            final int value = (Integer) getValue();
                            if (option.getValue() != value) {
                                option.setValue(value);
                            }
                        }
                    }
                });

            option.addPropertyChangeListener(this);
        } else {
            int value = option.getValue();
            setModel(new SpinnerNumberModel(value, value, value, 1));
        }

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
     * @param newLabel The new Label value.
     */
    public void setLabel(final JLabel newLabel) {
        this.label = newLabel;
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
     * @param event The event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            final int value = (Integer) event.getNewValue();
            if (value != ((Integer) getValue()).intValue()) {
                setValue(event.getNewValue());
                originalValue = value;
            }
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue(((Integer) getValue()).intValue());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        setValue(option.getValue());
    }
}
