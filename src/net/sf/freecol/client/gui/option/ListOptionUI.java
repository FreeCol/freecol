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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
 * This class provides visualization for a {@link ListOption}. In order to
 * enable values to be both seen and changed.
 */
public final class ListOptionUI<T> extends JPanel implements OptionUpdater, PropertyChangeListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ListOptionUI.class.getName());

    private final ListOption<T> option;

    private final JList list;
    private final DefaultListModel listModel;
    private List<ListOptionElement<T>> originalValue;

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

        setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.BLACK),
                                                   Messages.getName(option)));
        this.option = option;
        this.originalValue = createElementList(option.getValue());
        this.listModel = new DefaultListModel();
        for (ListOptionElement<T> e : createElementList(option.getValue())) {
            this.listModel.addElement(e);
        }

        String name = Messages.getName(option);
        String description = Messages.getShortDescription(option);

        list = new JList(listModel);
        final JScrollPane sp = new JScrollPane(list);
        list.setToolTipText((description != null) ? description : name);
        list.setEnabled(editable);
        add(sp, BorderLayout.CENTER);

        final JPanel buttonPanel = new JPanel(new GridLayout(4, 1));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addButton);
        buttonPanel.add(removeButton);
        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        add(buttonPanel, BorderLayout.EAST);
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
            public void contentsChanged(ListDataEvent e) {
                if (option.isPreviewEnabled()) {
                    if (!option.getValue().equals(getValue())) {
                        option.setValue(getValue());
                    }
                }
            }
            public void intervalAdded(ListDataEvent e) {}
            public void intervalRemoved(ListDataEvent e) {}
        });

        option.addPropertyChangeListener(this);
        setOpaque(false);
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

        final JComboBox mods = new JComboBox(option.getListOptionSelector().getOptions().toArray());
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
            listModel.addElement(new ListOptionElement<T>(response, option.getListOptionSelector().toString(response)));
        }
    }

    private List<ListOptionElement<T>> createElementList(List<T> list) {
        final List<ListOptionElement<T>> elementList = new ArrayList<ListOptionElement<T>>(list.size());
        for (T o : list) {
            final ListOptionSelector<T> los = option.getListOptionSelector();
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
     * Rollback to the original value.
     *
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        option.setValue(createNormalList(originalValue));
    }

    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        option.removePropertyChangeListener(this);
    }

    /**
     * Updates this UI with the new data from the option.
     *
     * @param event The event.
     */
    @SuppressWarnings("unchecked")
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals("value")) {
            final List<T> value = (List<T>) event.getNewValue();
            if (!value.equals(getValue())) {
                listModel.clear();

                for (Object o : createElementList(value)) {
                    listModel.addElement(o);
                }
                originalValue = createElementList(value);
            }
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        option.setValue(getValue());
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
     * Reset with the value from the option.
     */
    public void reset() {
        listModel.clear();
        for (Object o : createElementList(option.getValue())) {
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
