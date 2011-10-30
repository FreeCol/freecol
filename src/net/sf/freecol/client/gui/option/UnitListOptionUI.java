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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.AbstractUnitOption;
import net.sf.freecol.common.option.UnitListOption;

/**
 * This class provides visualization for a List of {@link
 * net.sf.freecol.common.option.AbstractUnitOption}s. In order to
 * enable values to be both seen and changed.
 *
 * TODO: derive from ListOptionUI
 */
public final class UnitListOptionUI extends OptionUI<UnitListOption> {

    private JPanel panel = new JPanel();
    private JList list;
    private DefaultListModel model;

    private JButton editButton = new JButton(Messages.message("list.edit"));
    private JButton addButton = new JButton(Messages.message("list.add"));
    private JButton removeButton = new JButton(Messages.message("list.remove"));
    private JButton upButton = new JButton(Messages.message("list.up"));
    private JButton downButton = new JButton(Messages.message("list.down"));

    private JButton[] buttons = new JButton[] {
        editButton, addButton, removeButton, upButton, downButton
    };


    /**
     * Creates a new <code>UnitListOptionUI</code> for the given
     * <code>ListOption</code>.
     *
     * @param option
     * @param editable boolean whether user can modify the setting
     */
    public UnitListOptionUI(final UnitListOption option, boolean editable) {
        super(option, editable);

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         super.getLabel().getText()));

        panel.setLayout(new MigLayout("wrap 2, fill", "[fill, grow]20[fill]"));

        model = new DefaultListModel();
        for (AbstractUnitOption o : option.getValue()) {
            model.addElement(o.clone());
        }
        list = new JList(model);
        list.setCellRenderer(new AbstractUnitRenderer());
        list.setVisibleRowCount(4);
        JScrollPane pane = new JScrollPane(list);
        panel.add(pane, "grow, spany 5");

        for (JButton button : buttons) {
            button.setEnabled(editable);
            panel.add(button);
        }

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                AbstractUnitOption oldValue = (AbstractUnitOption) list.getSelectedValue();
                if (oldValue == null) {
                    oldValue = getOption().getTemplate();
                }
                AbstractUnitOption value = oldValue.clone();
                if (showEditDialog(value)) {
                    model.addElement(value);
                    list.setSelectedValue(value, true);
                    list.repaint();
                }
            }
        });
        editButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Object object = list.getSelectedValue();
                if (object != null) {
                    if (showEditDialog((AbstractUnitOption) object)) {
                        list.repaint();
                    }
                }
            }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                model.removeElementAt(list.getSelectedIndex());
            }
        });
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedIndex() == 0) {
                    return;
                }
                final int index = list.getSelectedIndex();
                final Object temp = model.getElementAt(index);
                model.setElementAt(model.getElementAt(index-1), index);
                model.setElementAt(temp, index-1);
                list.setSelectedIndex(index-1);
            }
        });
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedIndex() == model.getSize() - 1) {
                    return;
                }
                final int index = list.getSelectedIndex();
                final Object temp = model.getElementAt(index);
                model.setElementAt(model.getElementAt(index+1), index);
                model.setElementAt(temp, index+1);
                list.setSelectedIndex(index+1);
            }
        });

        initialize();
    }

    private boolean showEditDialog(AbstractUnitOption option) {
        final Canvas canvas = FreeCol.getFreeColClient().getCanvas();
        final EditDialog editDialog = new EditDialog(canvas, option);
        return canvas.showFreeColDialog(editDialog);
    }

    private class EditDialog extends FreeColDialog<Boolean> {

        private AbstractUnitOptionUI ui;

        public EditDialog(Canvas canvas, AbstractUnitOption option) {
            super(canvas);
            setLayout(new MigLayout());
            ui = new AbstractUnitOptionUI(option, true);
            add(ui.getComponent());
            add(okButton, "newline, split 2, tag ok");
            add(cancelButton, "tag cancel");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            if (OK.equals(command)) {
                ui.updateOption();
                setResponse(true);
            } else {
                setResponse(false);
            }
        }
    }

    /**
     * Returns <code>null</code>, since this OptionUI does not require
     * an external label.
     *
     * @return null
     */
    @Override
    public final JLabel getLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public JPanel getComponent() {
        return panel;
    }

    /**
     * Updates the value of the {@link net.sf.freecol.common.getOption().Option} this object keeps.
     */
    public void updateOption() {
        getOption().setValue(getValue());
    }

    private List<AbstractUnitOption> getValue() {
        List<AbstractUnitOption> result = new ArrayList<AbstractUnitOption>();
        for (Enumeration e = model.elements(); e.hasMoreElements();) {
            result.add((AbstractUnitOption) e.nextElement());
        }
        return result;
    }

    /**
     * Reset with the value from the Option.
     */
    public void reset() {
        model.clear();
        for (AbstractUnitOption o : getOption().getValue()) {
            model.addElement(o);
        }
    }



    private class AbstractUnitRenderer implements ListCellRenderer {

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

        public AbstractUnitRenderer() {
            super();
            normal.setOpaque(false);
            normal.setLayout(new MigLayout("", "[40, align right][]"));
            selected.setOpaque(false);
            selected.setLayout(new MigLayout("", "[40, align right][]"));
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
            AbstractUnit unit = ((AbstractUnitOption) value).getValue();
            String key = unit.getRole() == Role.DEFAULT ? unit.getId()
                : "model.unit." + unit.getRole().toString().toLowerCase(Locale.US);
            StringTemplate template = StringTemplate.template(key + ".name")
                .addAmount("%number%", unit.getNumber())
                .add("%unit%", unit.getId() + ".name");
            /*
            c.add(new JLabel(new ImageIcon(ResourceManager.getImage(unit.getId() + ".image", 0.5))),
                  "width 80, align center");
            */
            c.add(new JLabel(Integer.toString(unit.getNumber())));
            c.add(new JLabel(Messages.message(template)));
            return c;
        }
    }


}
