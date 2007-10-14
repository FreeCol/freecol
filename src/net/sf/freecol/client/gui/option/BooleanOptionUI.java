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

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.Option;



/**
* This class provides visualization for an {@link BooleanOption}. In order to
* enable values to be both seen and changed.
*/
public final class BooleanOptionUI extends JPanel implements OptionUpdater, PropertyChangeListener {
    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BooleanOptionUI.class.getName());


    private final BooleanOption option;
    private final JCheckBox checkBox;
    

    /**
    * Creates a new <code>BooleanOptionUI</code> for the given <code>BooleanOption</code>.
    * @param option The <code>BooleanOption</code> to make a user interface for.
    */
    public BooleanOptionUI(BooleanOption option, boolean editable) {
        super(new FlowLayout(FlowLayout.LEFT));

        this.option = option;

        String name = option.getName();
        String description = option.getShortDescription();
        checkBox = new JCheckBox(name, option.getValue());
        checkBox.setEnabled(editable);
        checkBox.setToolTipText((description != null) ? description : name);
        
        option.addPropertyChangeListener(this);
        
        add(checkBox);
        setBorder(null);
        setOpaque(false);
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
            checkBox.setSelected(((Boolean) event.getNewValue()).booleanValue());
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue(checkBox.isSelected());
    }

    /**
     * Reset with the value from the option.
     */
    public void reset() {
        checkBox.setSelected(option.getValue());
    }
    
    /**
     * Sets the value of this component.
     */
    public void setValue(boolean b) {
        checkBox.setSelected(b);
    }
}
