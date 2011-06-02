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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JCheckBox;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.BooleanOption;


/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.BooleanOption}. In order to enable
 * values to be both seen and changed.
 */
public final class BooleanOptionUI extends JCheckBox implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BooleanOptionUI.class.getName());

    private final BooleanOption option;
    private boolean originalValue;


    /**
    * Creates a new <code>BooleanOptionUI</code> for the given <code>BooleanOption</code>.
     *
    * @param option The <code>BooleanOption</code> to make a user interface for.
    * @param editable boolean whether user can modify the setting
    */
    public BooleanOptionUI(final BooleanOption option, boolean editable) {

        this.option = option;
        this.originalValue = option.getValue();

        String name = Messages.getName(option);
        String description = Messages.getShortDescription(option);
        setText(name);
        setSelected(option.getValue());
        setEnabled(editable);
        setToolTipText((description != null) ? description : name);

        option.addPropertyChangeListener(this);
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (option.isPreviewEnabled()) {
                    boolean value = isSelected();
                    if (option.getValue() != value) {
                        option.setValue(value);
                    }
                }
            }
        });

    }


    /**
     * Rollback to the original value.
     *
     * This method gets called so that changes made to options with
     * {@link net.sf.freecol.common.option.Option#isPreviewEnabled()} is rolled back
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
            boolean value = ((Boolean) event.getNewValue()).booleanValue();
            if (value != isSelected()) {
                setSelected(value);
                originalValue = value;
            }
        }
    }

    /**
     * Updates the value of the {@link net.sf.freecol.common.option.Option} this object keeps.
     */
    public void updateOption() {
        option.setValue(isSelected());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        setSelected(option.getValue());
    }

    /**
     * Sets the value of this component.
     */
    public void setValue(boolean b) {
        setSelected(b);
    }

}
