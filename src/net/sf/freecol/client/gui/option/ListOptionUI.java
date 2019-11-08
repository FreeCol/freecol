/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.Option;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This class provides visualization for a list of
 * {@link net.sf.freecol.common.option.AbstractOption}s in order to enable
 * values to be both seen and changed.
 */
public final class ListOptionUI<T> extends OptionUI<ListOption<T>>
    implements ListSelectionListener {

    private static final Logger logger = Logger.getLogger(ListOptionUI.class.getName());

    private final JPanel panel;
    private final JList<AbstractOption<T>> list;
    private final DefaultListModel<AbstractOption<T>> model;

    private final JButton editButton = Utility.localizedButton("list.edit");
    private final JButton addButton = Utility.localizedButton("list.add");
    private final JButton removeButton = Utility.localizedButton("list.remove");
    private final JButton upButton = Utility.localizedButton("list.up");
    private final JButton downButton = Utility.localizedButton("list.down");


    /**
     * Creates a new {@code ListOptionUI} for the given
     * {@code ListOption}.
     *
     * @param gui The {@code GUI} to display on.
     * @param option The {@code ListOption} to display.
     * @param editable boolean whether user can modify the setting
     */
    public ListOptionUI(final GUI gui, final ListOption<T> option,
                        boolean editable) {
        super(option, editable);

        this.panel = new MigPanel(new MigLayout("wrap 2, fill",
                                                "[fill, grow]20[fill]"));
        this.panel.setBorder(Utility.localizedBorder(super.getJLabel().getText(),
                                                 Utility.BORDER_COLOR));

        this.model = new DefaultListModel<>();
        for (AbstractOption<T> o : option.getValue()) {
            try {
                AbstractOption<T> c = o.cloneOption();
                this.model.addElement(c);
            } catch (CloneNotSupportedException e) {
                logger.log(Level.WARNING, "Can not clone " + o.getId(), e);
            }
        }
        this.list = new JList<>(this.model);
        AbstractOption<T> o = first(option.getValue());
        if (o == null) o = option.getTemplate();
        if (o != null) {
            setCellRenderer(gui, o, editable);
            this.list.setSelectedIndex(0);
        }
        this.list.setVisibleRowCount(4);
        JScrollPane pane = new JScrollPane(this.list);
        this.panel.add(pane, "grow, spany 5");
        
        for (JButton button : new JButton[] {
                editButton, addButton, removeButton, upButton, downButton }) {
            button.setEnabled(editable);
            this.panel.add(button);
        }

        // Disable add button if there is no template to generate choices with
        if (option.getTemplate() == null) addButton.setEnabled(false);
        
        addButton.addActionListener((ActionEvent ae) -> {
                try {
                    AbstractOption<T> ao = option.getTemplate().cloneOption();
                    if (gui.showEditOptionDialog(ao) && option.canAdd(ao)) {
                        this.model.addElement(ao);
                        this.list.setSelectedValue(ao, true);
                        this.list.repaint();
                    }
                } catch (CloneNotSupportedException cnse) {
                    logger.log(Level.WARNING, "Can not clone: "
                        + option.getTemplate(), cnse);
                }
            });
        editButton.addActionListener((ActionEvent ae) -> {
                AbstractOption<T> ao = this.list.getSelectedValue();
                if (gui.showEditOptionDialog(ao)) {
                    this.list.repaint();
                }
            });
        removeButton.addActionListener((ActionEvent ae) -> {
                this.model.removeElementAt(this.list.getSelectedIndex());
            });
        upButton.addActionListener((ActionEvent ae) -> {
                if (this.list.getSelectedIndex() == 0) return;
                final int index = this.list.getSelectedIndex();
                final AbstractOption<T> temp = this.model.getElementAt(index);
                this.model.setElementAt(this.model.getElementAt(index-1), index);
                this.model.setElementAt(temp, index-1);
                this.list.setSelectedIndex(index-1);
            });
        downButton.addActionListener((ActionEvent ae) -> {
                if (this.list.getSelectedIndex() == this.model.getSize() - 1) return;
                final int index = this.list.getSelectedIndex();
                final AbstractOption<T> temp = this.model.getElementAt(index);
                this.model.setElementAt(this.model.getElementAt(index+1), index);
                this.model.setElementAt(temp, index+1);
                this.list.setSelectedIndex(index+1);
            });

        this.list.addListSelectionListener(this);
        initialize();
    }

    @SuppressWarnings("unchecked")
    private void setCellRenderer(GUI gui, AbstractOption<T> o,
                                 boolean editable) {
        OptionUI ui = OptionUI.getOptionUI(gui, o, editable);
        if (ui != null && ui.getListCellRenderer() != null) {
            this.list.setCellRenderer(ui.getListCellRenderer());
        }
    }

    private List<AbstractOption<T>> getValue() {
        List<AbstractOption<T>> result = new ArrayList<>();
        for (Enumeration<AbstractOption<T>> e = this.model.elements();
             e.hasMoreElements();) {
            result.add(e.nextElement());
        }
        return result;
    }


    // Implement OptionUI

    /**
     * {@inheritDoc}
     */
    @Override
    public final JLabel getJLabel() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPanel getComponent() {
        return this.panel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOption() {
        getOption().setValue(getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() {
        this.model.clear();
        for (AbstractOption<T> o : getOption().getValue()) {
            this.model.addElement(o);
        }
    }

    // Interface ListSelectionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting() == false) {
            boolean enabled = isEditable()
                && this.list.getSelectedValue() != null;
            this.editButton.setEnabled(enabled);
            this.addButton.setEnabled(isEditable()
                && getOption().getTemplate() != null);
            this.removeButton.setEnabled(enabled);
            this.upButton.setEnabled(enabled);
            this.downButton.setEnabled(enabled);
        }
    }
}
