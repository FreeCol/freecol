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

package net.sf.freecol.client.gui.panel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;

import net.sf.freecol.client.gui.action.ActionManager;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * A button with a set of images which is used to give commands
 * to a unit with the mouse instead of the keyboard. The UnitButton
 * has rollover highlighting, can be grayed out if it is unusable,
 * and will use a separate image for being pressed.
 * The UnitButton is useless by itself, this object needs to
 * be placed on a JComponent in order to be useable.
 */
public final class UnitButton extends JButton {

    private final String actionId;
    private final ActionManager am;

    /**
     * The basic constructor.
     *
     * @param am The action manager which holds all FreeColAction.
     * @param actionId The key for the action to be used with this button
     */
    public UnitButton(ActionManager am, String actionId) {
        super(am.getFreeColAction(actionId));
        this.actionId = actionId;
        this.am = am;
    }

    /**
     * Refreshes the the reference to the FreeColAction stored in the
     * action manager.
     */
    public void refreshAction() {
        setAction(null);
        setAction(am.getFreeColAction(actionId));
    }


    @Override
    protected void configurePropertiesFromAction(Action a) {
        super.configurePropertiesFromAction(a);

        if (a != null) {
            setRolloverEnabled(true);
            String key = (String) a.getValue(FreeColAction.BUTTON_IMAGE);
            ImageIcon bi = new ImageIcon(ResourceManager.getImage(key));
            setIcon(bi);
            key = (String) a.getValue(FreeColAction.BUTTON_ROLLOVER_IMAGE);
            setRolloverIcon(new ImageIcon(ResourceManager.getImage(key)));
            key = (String) a.getValue(FreeColAction.BUTTON_PRESSED_IMAGE);
            setPressedIcon(new ImageIcon(ResourceManager.getImage(key)));
            key = (String) a.getValue(FreeColAction.BUTTON_DISABLED_IMAGE);
            setDisabledIcon(new ImageIcon(ResourceManager.getImage(key)));
            setToolTipText((String) a.getValue(FreeColAction.NAME));
            setText(null);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);

            setSize(bi.getIconWidth(), bi.getIconHeight());
        }
    }

    @Override
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new UnitButtonActionPropertyChangeListener(this);
    }

    private static class UnitButtonActionPropertyChangeListener implements PropertyChangeListener {
        private final AbstractButton button;

        UnitButtonActionPropertyChangeListener(AbstractButton button) {
            this.button = button;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if (Action.NAME.equals(e.getPropertyName())
                || Action.SHORT_DESCRIPTION.equals(e.getPropertyName())) {
                String text = (String) e.getNewValue();
                button.setToolTipText(text);
            } else if ("enabled".equals(propertyName)) {
                Boolean enabledState = (Boolean) e.getNewValue();
                button.setEnabled(enabledState);
                button.repaint();
            } else if (Action.SMALL_ICON.equals(e.getPropertyName())) {
                Icon icon = (Icon) e.getNewValue();
                button.setIcon(icon);
                button.repaint();
            } else if (FreeColAction.BUTTON_IMAGE.equals(e.getPropertyName())) {
                String key = (String) e.getNewValue();
                button.setIcon(new ImageIcon(ResourceManager.getImage(key)));
                button.repaint();
            } else if (FreeColAction.BUTTON_ROLLOVER_IMAGE.equals(e.getPropertyName())) {
                String key = (String) e.getNewValue();
                button.setRolloverIcon(new ImageIcon(ResourceManager.getImage(key)));
                button.repaint();
            } else if (FreeColAction.BUTTON_PRESSED_IMAGE.equals(e.getPropertyName())) {
                String key = (String) e.getNewValue();
                button.setPressedIcon(new ImageIcon(ResourceManager.getImage(key)));
                button.repaint();
            } else if (FreeColAction.BUTTON_DISABLED_IMAGE.equals(e.getPropertyName())) {
                String key = (String) e.getNewValue();
                button.setDisabledIcon(new ImageIcon(ResourceManager.getImage(key)));
                button.repaint();
            } else if (Action.MNEMONIC_KEY.equals(e.getPropertyName())) {
                Integer mn = (Integer) e.getNewValue();
                button.setMnemonic(mn);
                button.repaint();
            } else if (Action.ACTION_COMMAND_KEY.equals(e.getPropertyName())) {
                button.setActionCommand((String)e.getNewValue());
            }
        }
    }
}
