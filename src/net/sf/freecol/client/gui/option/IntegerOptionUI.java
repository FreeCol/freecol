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

import java.awt.FlowLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;



/**
* This class provides visualization for an {@link IntegerOption}. In order to
* enable values to be both seen and changed.
*/
public final class IntegerOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(IntegerOptionUI.class.getName());


    private final IntegerOption option;
    private final JSpinner spinner;
    private int originalValue;


    /**
    * Creates a new <code>IntegerOptionUI</code> for the given <code>IntegerOption</code>.
    * @param option The <code>IntegerOption</code> to make a user interface for.
    */
    public IntegerOptionUI(final IntegerOption option, boolean editable) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;
        this.originalValue = option.getValue();

        String name = option.getName();
        String description = option.getShortDescription();
        JLabel label = new JLabel(name, JLabel.LEFT);
        label.setToolTipText((description != null) ? description : name);
        add(label);

        int stepSize = Math.min((option.getMaximumValue() - option.getMinimumValue()) / 10, 1000);
        spinner = new JSpinner(new SpinnerNumberModel(option.getValue(), option.getMinimumValue(),
                option.getMaximumValue(), Math.max(1, stepSize)));
        spinner.setToolTipText(option.getShortDescription());
        add(spinner);
        
        spinner.setEnabled(editable);
        spinner.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (option.isPreviewEnabled()) {
                    final int value = (Integer) spinner.getValue();
                    if (option.getValue() != value) {
                        option.setValue(value);
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
     * @param event The event.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            final int value = (Integer) event.getNewValue();
            if (value != ((Integer) spinner.getValue()).intValue()) {
                spinner.setValue(event.getNewValue());
                originalValue = value;
            }
        }
    }
    
    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue(((Integer) spinner.getValue()).intValue());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        spinner.setValue(option.getValue());
    }
}
