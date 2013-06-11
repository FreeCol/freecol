/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;


/**
 * Selects one of a number of choices.
 */
public final class ChoiceDialog<T> extends FreeColDialog<T> {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(ChoiceDialog.class.getName());

    private JList choiceList;


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param text <code>String</code>
     * @param cancelText <code>String</code>
     * @param choices List of <code>ChoiceItem<T></code>
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public ChoiceDialog(FreeColClient freeColClient, GUI gui, String text,
                        String cancelText, List<ChoiceItem<T>> choices) {
        super(freeColClient, gui);

        MigLayout layout = new MigLayout("wrap 1, fill", "[align center]", "[]30[]30[]");
        setLayout(layout);

        add(getDefaultTextArea(text));

        DefaultListModel model = new DefaultListModel();
        for (ChoiceItem<T> item : choices) {
            model.addElement(item);
        }
        choiceList = new JList(model);
        JScrollPane listScroller = new JScrollPane(choiceList);
        listScroller.setPreferredSize(new Dimension(250, 250));

        Action selectAction = new AbstractAction() {
                @SuppressWarnings("unchecked")
                public void actionPerformed(ActionEvent e) {
                    setResponse(((ChoiceItem<T>) choiceList.getSelectedValue()).getObject());
                }
            };

        Action quitAction = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    getGUI().removeFromCanvas(ChoiceDialog.this);
                }
            };

        choiceList.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "select");
        choiceList.getActionMap().put("select", selectAction);
        choiceList.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "quit");
        choiceList.getActionMap().put("quit", quitAction);

        MouseListener mouseListener = new MouseAdapter() {
                @SuppressWarnings("unchecked")
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        setResponse(((ChoiceItem<T>) choiceList.getSelectedValue()).getObject());
                    }
                }
            };
        choiceList.addMouseListener(mouseListener);

        add(listScroller, "growx, growy");

        if (cancelText != null) {
            cancelButton.setText(cancelText);
            add(okButton, "split 2, tag ok");
            add(cancelButton, "tag cancel");
        } else {
            add(okButton, "tag ok");
        }

        okButton.addActionListener(this);

        setSize(getPreferredSize());
    }


    @Override
    public void requestFocus() {
        choiceList.requestFocus();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     * 
     * @param event The incoming ActionEvent.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        ChoiceItem<T> item;
        if (OK.equals(command)
            && (item = (ChoiceItem<T>) choiceList.getSelectedValue()) != null) {
            setResponse(item.getObject());
        } else {
            super.actionPerformed(event);
        }
    }


} 

