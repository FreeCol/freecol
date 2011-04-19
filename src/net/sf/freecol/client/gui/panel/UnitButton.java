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


package net.sf.freecol.client.gui.panel;


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import net.sf.freecol.client.gui.action.FreeColAction;

/**
* A button with a set of images which is used to give commands
* to a unit with the mouse instead of the keyboard. The UnitButton
* has rollover highlighting, can be grayed out if it is unusable,
* and will use a separate image for being pressed.
* The UnitButton is useless by itself, this object needs to
* be placed on a JComponent in order to be useable.
*/
public final class UnitButton extends JButton {

    /**
    * The basic constructor.
    * @param a The action to be used with this button.
    */
    public UnitButton(FreeColAction a) {
        super(a);
    }

    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);

        if (a != null) {
            setRolloverEnabled(true);
            Icon bi = (Icon) a.getValue(FreeColAction.BUTTON_IMAGE);
            setIcon(bi);
            setRolloverIcon((Icon) a.getValue(FreeColAction.BUTTON_ROLLOVER_IMAGE));
            setPressedIcon((Icon) a.getValue(FreeColAction.BUTTON_PRESSED_IMAGE));
            setDisabledIcon((Icon) a.getValue(FreeColAction.BUTTON_DISABLED_IMAGE));
            setToolTipText((String) a.getValue(FreeColAction.NAME));
            setText(null);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);

            if (bi == null) {
                throw new IllegalArgumentException("The given action is missing \"BUTTON_IMAGE\".");
            }
            setSize(bi.getIconWidth(), bi.getIconHeight());
        }
    }

    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new UnitButtonActionPropertyChangeListener(this);
    }

    private static class UnitButtonActionPropertyChangeListener implements PropertyChangeListener {
        private AbstractButton button;

        UnitButtonActionPropertyChangeListener(AbstractButton button) {
            this.button = button;
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (e.getPropertyName().equals(Action.NAME)
                    || e.getPropertyName().equals(Action.SHORT_DESCRIPTION)) {
                String text = (String) e.getNewValue();
                button.setToolTipText(text);
            } else if (propertyName.equals("enabled")) {
                Boolean enabledState = (Boolean) e.getNewValue();
                button.setEnabled(enabledState.booleanValue());
                button.repaint();
            } else if (e.getPropertyName().equals(Action.SMALL_ICON)) {
                Icon icon = (Icon) e.getNewValue();
                button.setIcon(icon);
                button.repaint();
            } else if (e.getPropertyName().equals(FreeColAction.BUTTON_IMAGE)) {
                Icon icon = (Icon) e.getNewValue();
                button.setIcon(icon);
                button.repaint();
            } else if (e.getPropertyName().equals(FreeColAction.BUTTON_ROLLOVER_IMAGE)) {
                Icon icon = (Icon) e.getNewValue();
                button.setRolloverIcon(icon);
                button.repaint();
            } else if (e.getPropertyName().equals(FreeColAction.BUTTON_PRESSED_IMAGE)) {
                Icon icon = (Icon) e.getNewValue();
                button.setPressedIcon(icon);
                button.repaint();
            } else if (e.getPropertyName().equals(FreeColAction.BUTTON_DISABLED_IMAGE)) {
                Icon icon = (Icon) e.getNewValue();
                button.setDisabledIcon(icon);
                button.repaint();
            } else if (e.getPropertyName().equals(Action.MNEMONIC_KEY)) {
                Integer mn = (Integer) e.getNewValue();
                button.setMnemonic(mn.intValue());
                button.repaint();
            } else if (e.getPropertyName().equals(Action.ACTION_COMMAND_KEY)) {
                button.setActionCommand((String)e.getNewValue());
            }
        }
    }
}
