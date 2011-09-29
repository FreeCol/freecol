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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.ListOptionSelector;

/**
 * This class provides visualization for a {@link
 * net.sf.freecol.common.option.ListOption}. In order to enable values
 * to be both seen and changed.
 */
public final class ListOptionUI<T> extends OptionUI<ListOption<T>> {

    private JPanel panel = new JPanel();
    private final JList list;
    private final DefaultListModel listModel;

    private JButton addButton = new JButton(Messages.message("list.add"));
    private JButton removeButton = new JButton(Messages.message("list.remove"));
    private JButton upButton = new JButton(Messages.message("list.up"));
    private JButton downButton = new JButton(Messages.message("list.down"));



    /**
     * Creates a new <code>ListOptionUI</code> for the given
     * <code>ListOption</code>.
     *
     * @param option The <code>ListOption</code> to make a user interface
     *            for.
     * @param editable boolean whether user can modify the setting
     */
    public ListOptionUI(final ListOption<T> option, boolean editable) {
        super(option, editable);

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                         super.getLabel().getText()));

        this.listModel = new DefaultListModel();
        for (ListOptionElement<T> e : createElementList(option.getValue())) {
            this.listModel.addElement(e);
        }

        list = new JList(listModel);
        final JScrollPane sp = new JScrollPane(list);
        list.setEnabled(editable);
        panel.add(sp, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        panel.add(buttonPanel, BorderLayout.EAST);
        sp.setPreferredSize(new Dimension(500, buttonPanel.getPreferredSize().height));

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showAddElementDialog();
            }
        });
        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                listModel.removeElementAt(list.getSelectedIndex());
            }
        });
        upButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedIndex() == 0) {
                    return;
                }
                final int index = list.getSelectedIndex();
                final Object temp = listModel.getElementAt(index);
                listModel.setElementAt(listModel.getElementAt(index-1), index);
                listModel.setElementAt(temp, index-1);
                list.setSelectedIndex(index-1);
            }
        });
        downButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (list.getSelectedIndex() == listModel.getSize() - 1) {
                    return;
                }
                final int index = list.getSelectedIndex();
                final Object temp = listModel.getElementAt(index);
                listModel.setElementAt(listModel.getElementAt(index+1), index);
                listModel.setElementAt(temp, index+1);
                list.setSelectedIndex(index+1);
            }
        });

        list.getModel().addListDataListener(new ListDataListener() {
            public void contentsChanged(ListDataEvent e) {}
            public void intervalAdded(ListDataEvent e) {}
            public void intervalRemoved(ListDataEvent e) {}
        });

        initialize();
    }

    private void showAddElementDialog() {
        final Canvas canvas = FreeCol.getFreeColClient().getCanvas();
        final JButton addButton = new JButton(Messages.message("list.add"));
        final FreeColDialog<Object> addElementDialog = new FreeColDialog<Object>(canvas) {
            @Override
            public void requestFocus() {
                addButton.requestFocus();
            }
        };
        addElementDialog.setLayout(new BorderLayout());
        final JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(addButton);
        final JButton cancelButton = new JButton(Messages.message("cancel"));
        buttons.add(cancelButton);
        addElementDialog.setCancelComponent(cancelButton);
        addElementDialog.add(buttons, BorderLayout.SOUTH);

        final JComboBox mods = new JComboBox(getOption().getListOptionSelector().getOptions().toArray());
        addElementDialog.add(mods, BorderLayout.CENTER);

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addElementDialog.setResponse(mods.getSelectedItem());
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addElementDialog.setResponse(null);
            }
        });

        canvas.addAsFrame(addElementDialog);
        addElementDialog.requestFocus();

        @SuppressWarnings("unchecked")
        T response = (T) addElementDialog.getResponse();
        canvas.remove(addElementDialog);

        if (response != null) {
            listModel.addElement(new ListOptionElement<T>(response, getOption().getListOptionSelector().toString(response)));
        }
    }

    private List<ListOptionElement<T>> createElementList(List<T> list) {
        final List<ListOptionElement<T>> elementList = new ArrayList<ListOptionElement<T>>();
        for (T o : list) {
            if (o == null) continue;
            final ListOptionSelector<T> los = getOption().getListOptionSelector();
            final ListOptionElement<T> e = new ListOptionElement<T>(o, los.toString(o));
            elementList.add(e);
        }
        return elementList;
    }

    private List<T> createNormalList(List<ListOptionElement<T>> elementList) {
        final List<T> list = new ArrayList<T>(elementList.size());
        for (ListOptionElement<T> o : elementList) {
            list.add(o.object);
        }
        return list;
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

    @SuppressWarnings("unchecked")
    private List<T> getValue() {
        final List<ListOptionElement<T>> l = new ArrayList<ListOptionElement<T>>();
        for (int i=0; i<listModel.getSize(); i++) {
            l.add((ListOptionElement<T>) listModel.getElementAt(i));
        }
        return createNormalList(l);
    }

    /**
     * Reset with the value from the Option.
     */
    public void reset() {
        listModel.clear();
        for (Object o : createElementList(getOption().getValue())) {
            listModel.addElement(o);
        }
    }

    private static class ListOptionElement<T> {
        private final T object;
        private final String text;

        private ListOptionElement(final T object, final String text) {
            this.object = object;
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
