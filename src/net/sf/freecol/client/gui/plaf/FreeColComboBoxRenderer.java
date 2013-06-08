/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

package net.sf.freecol.client.gui.plaf;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.plaf.UIResource;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.client.gui.i18n.Messages;


/**
 * A <code>ListCellRenderer</code> to be used by <code>FreeColListUI</code>.
 */
public class FreeColComboBoxRenderer implements ListCellRenderer, UIResource {

    private final SelectedComponent SELECTED_COMPONENT = new SelectedComponent();
    private final NormalComponent NORMAL_COMPONENT = new NormalComponent();


    /**
     * Returns a <code>ListCellRenderer</code> for the given <code>JList</code>.
     *
     * @param list The <code>JList</code>.
     * @param value The list cell.
     * @param index The index in the list.
     * @param isSelected <code>true</code> if the given list cell is selected.
     * @param hasFocus <code>false</code> if the given list cell has the focus.
     * @return The <code>ListCellRenderer</code>
     */
    public Component getListCellRendererComponent(JList list, Object value, int index,
                                                  boolean isSelected, boolean hasFocus) {

        JLabel c = isSelected ? SELECTED_COMPONENT : NORMAL_COMPONENT;
        c.setForeground(list.getForeground());
        c.setFont(list.getFont());
        setLabelValues(c, value);

        return c;
    }


    public void setLabelValues(JLabel c, Object value) {
        if (value instanceof Icon) {
            // TODO: does this even make sense? Value should never be an icon!
            c.setIcon((Icon) value);
        } else if (value instanceof FreeColObject) {
            if (value instanceof Nameable) {
                String name = ((Nameable) value).getName();
                if (name != null) {
                    c.setText(name);
                    return;
                }
            }
            c.setText(Messages.getName((FreeColObject) value));
        } else {
            c.setText((value == null) ? null : value.toString());
        }
    }

    private class SelectedComponent extends JLabel {

        public SelectedComponent() {
            setOpaque(false);
        }

        public void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            Composite oldComposite = g2d.getComposite();
            Color oldColor = g2d.getColor();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.1f));
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(oldComposite);
            g2d.setColor(oldColor);

            super.paintComponent(g);
        }
    }


    private class NormalComponent extends JLabel {

        public NormalComponent() {
            setOpaque(false);
        }
    }

}
