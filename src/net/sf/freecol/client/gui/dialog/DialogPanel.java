/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.DialogHandler;
import net.sf.freecol.client.gui.dialog.FreeColDialog.DialogApi;
import net.sf.freecol.client.gui.panel.FreeColPanel;

/**
 * A panel for displaying a non-modal dialog.
 * @param <T> The return type of the dialog.
 */
public class DialogPanel<T> extends FreeColPanel {
    
    private Component initialFocusComponent;

    
    /**
     * 
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param dialog The dialog to be display.
     * @param handler The handler that will get the result from the dialog.
     */
    public DialogPanel(FreeColClient freeColClient, FreeColDialog<T> dialog, DialogHandler<T> handler) {
        super(freeColClient, null, new BorderLayout());
        
        final DialogApi<T> api = new DialogApi<>() {
            public void setValue(T value) {
                getGUI().removeComponent(DialogPanel.this);
                handler.handle(value);
            }
            
            @Override
            public void setInitialFocusComponent(Component component) {
                initialFocusComponent = component;
            }
        };
        
        setEscapeAction(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                getGUI().removeComponent(DialogPanel.this);
                handler.handle(null);
            }
        });
        
        add(dialog.createContent(api), BorderLayout.CENTER);
    }
    
    /**
     * Requests focus for the initialFocusComponent defined by the dialog.
     */
    @Override
    public void requestFocus() {
        if (initialFocusComponent != null) {
            initialFocusComponent.requestFocus();
        }
    }
}
