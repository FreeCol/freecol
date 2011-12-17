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


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.io.FreeColModFile;
import net.sf.freecol.common.option.ModOption;


/**
 * This class provides visualization for an {@link
 * net.sf.freecol.common.option.ModOption}. In order to enable
 * values to be both seen and changed.
 */
public final class ModOptionUI extends OptionUI<ModOption>  {

    private JComboBox box = new JComboBox();

    /**
    * Creates a new <code>ModOptionUI</code> for the given <code>ModOption</code>.
    *
    * @param option The <code>ModOption</code> to make a user interface for
    * @param editable boolean whether user can modify the setting
    */
    public ModOptionUI(GUI gui, final ModOption option, boolean editable) {
        super(gui, option, editable);

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        for (FreeColModFile choice : option.getChoices()) {
            model.addElement(choice);
        }
        box.setModel(model);
        box.setRenderer(new ChoiceRenderer());
        if (option.getValue() != null) {
            box.setSelectedItem(option.getValue());
        }
        initialize();
    }

    /**
     * {@inheritDoc}
     */
    public void updateOption() {
        getOption().setValue((FreeColModFile) box.getSelectedItem());
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        box.setSelectedItem(getOption().getValue());
    }

    /**
     * {@inheritDoc}
     */
    public JComboBox getComponent() {
        return box;
    }

    private class ChoiceRenderer extends FreeColComboBoxRenderer {

        @Override
        public void setLabelValues(JLabel label, Object value) {
            try {
                FreeColModFile modFile = (FreeColModFile) value;
                String key = "mod." + modFile.getId() + ".name";
                label.setText(Messages.message(key));
                if (Messages.containsKey(key + ".shortDescription")) {
                    label.setToolTipText(Messages.message(key + ".shortDescription"));
                }
            } catch(Exception e) {
                label.setText(value.toString());
                e.printStackTrace();
            }
        }
    }

    public ListCellRenderer getListCellRenderer() {
        return new ModFileRenderer();
    }

    private class ModFileRenderer implements ListCellRenderer {

        private final JPanel normal = new JPanel();
        private final JPanel selected = new JPanel() {
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
            };

        public ModFileRenderer() {
            super();
            normal.setOpaque(false);
            selected.setOpaque(false);
        }


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

            JPanel c = isSelected ? selected : normal;
            c.removeAll();
            c.setForeground(list.getForeground());
            c.setFont(list.getFont());

            String key = "mod." + ((FreeColModFile) ((ModOption) value).getValue()).getId() + ".name";
            JLabel label = new JLabel(Messages.message(key));
            if (Messages.containsKey(key + ".shortDescription")) {
                label.setToolTipText(Messages.message(key + ".shortDescription"));
            }
            c.add(label);
            return c;
        }
    }


}
