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

package net.sf.freecol.client.gui.plaf;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.plaf.UIResource;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import java.util.logging.Logger;

import net.sf.freecol.common.ObjectWithId;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Nameable;
import net.sf.freecol.common.model.Named;
import net.sf.freecol.common.option.LanguageOption.Language;


/**
 * A <code>ListCellRenderer</code> to be used by
 * <code>FreeColListUI</code> and <code>JComboBox</code>es that
 * display <code>ObjectWithId</code> values.
 */
public class FreeColComboBoxRenderer<T>
    implements ListCellRenderer<T>, UIResource {

    private static final Logger logger = Logger.getLogger(FreeColComboBoxRenderer.class.getName());

    private final SelectedComponent SELECTED_COMPONENT = new SelectedComponent();
    private final NormalComponent NORMAL_COMPONENT = new NormalComponent();

    private final String prefix;
    private boolean localize = true;

    /**
     * Creates a new <code>FreeColComboBoxRenderer</code> instance
     * with an empty prefix.
     *
     */
    public FreeColComboBoxRenderer() {
        this("");
    }

    /**
     * Creates a new <code>FreeColComboBoxRenderer</code> instance
     * with a given prefix.
     *
     * @param prefix a <code>String</code> value
     */
    public FreeColComboBoxRenderer(String prefix) {
        this(prefix, true);
    }


    /**
     * Creates a new <code>FreeColComboBoxRenderer</code> instance
     * with a given prefix.
     *
     * @param prefix a <code>String</code> value
     * @param localize a <code>boolean</code> value
     */
    public FreeColComboBoxRenderer(String prefix, boolean localize) {
        this.prefix = prefix;
        this.localize = localize;
    }


    /**
     * Returns a <code>ListCellRenderer</code> for the given
     * <code>JList</code>.
     *
     * @param list The <code>JList</code>.
     * @param value The list cell.
     * @param index The index in the list.
     * @param isSelected <code>true</code> if the given list cell is selected.
     * @param hasFocus <code>false</code> if the given list cell has the focus.
     * @return The <code>ListCellRenderer</code>
     */
    @Override
    public Component getListCellRendererComponent(JList<? extends T> list,
                                                  T value, int index,
                                                  boolean isSelected,
                                                  boolean hasFocus) {
        JLabel c = isSelected ? SELECTED_COMPONENT : NORMAL_COMPONENT;
        c.setForeground(list.getForeground());
        c.setFont(list.getFont());
        setLabelValues(c, value);
        return c;
    }

    /**
     * Set the text to place in a label.
     *
     * @param c The <code>JLabel</code> to assign.
     * @param value The object to derive a text value from.
     */
    public void setLabelValues(JLabel c, T value) {
        if (value == null) {
            c.setText(null);
        } else if (value instanceof Integer) {
            // partial load values from SelectAmountDialog are Integers
            c.setText(value.toString());
        } else if (value instanceof Language) {
            c.setText(value.toString());
        } else if (value instanceof String) {
            String string = (String)value;
            if (localize) {
                String[] nd = Messages.getBestNameAndDescription(string);
                c.setText(nd[0]);
                if (nd[1] != null) c.setToolTipText(nd[1]);
            } else {
                c.setText(string);
            }
        } else if (value instanceof Named) {
            Named named = (Named)value;
            String[] nad = Messages.getBestNameAndDescription(named);
            c.setText(nad[0]);
            if (nad[1] != null) c.setToolTipText(nad[1]);
        } else if (value instanceof ObjectWithId) {
            String id = ((prefix == null) ? "" : prefix)
                + ((ObjectWithId)value).getId();
            String[] nd = Messages.getBestNameAndDescription(id);
            if (value instanceof Nameable) {
                String realname = ((Nameable)value).getName();
                if (realname != null) nd[0] = realname;
            }
            c.setText(nd[0]);
            if (nd[1] != null) c.setToolTipText(nd[1]);
        } else {
            logger.warning("What is this?: " + value
                + " (" + value.getClass() + ")");
        }
    }

    private static class SelectedComponent extends JLabel {

        public SelectedComponent() {
            setOpaque(false);
        }

        @Override
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


    private static class NormalComponent extends JLabel {

        public NormalComponent() {
            setOpaque(false);
        }
    }
}
