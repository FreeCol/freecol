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

import java.awt.Component;

import javax.swing.JPanel;

/**
 * A dialog returning a value when it's closed. The dialog can be displayed
 * both in a modal and a non-modal state.
 * 
 * @param <T> The return type of the modal dialog.
 */
public final class FreeColDialog<T> {
    /*
     * This is really a workaround to get a nice interface for making custom
     * modal dialogs. The modal dialog is displayed using the JOptionPane with
     * the custom rendering and handling is implemented by FreeColOptionPaneUI.
     * 
     * For non-modal dialog the implementation is straight forward, but we use the
     * same interface so that we can easily convert between modal and non-modal
     * behavior.
     */

    private final DialogContentCreator<T> dialogContentCreator;
    

    /**
     * Creates a {@code FreeColDialog} using the given creator.
     * @param dialogContentCreator The code responsible for generating the content.
     */
    public FreeColDialog(DialogContentCreator<T> dialogContentCreator) {
        this.dialogContentCreator = dialogContentCreator;
    }
    
    
    /**
     * This method is called by {@link FreeColOptionPaneUI} in order to
     * render the custom panel for modal dialogs.
     * 
     * @param api The API provided by {@link FreeColOptionPaneUI} to access
     *      functionality provided by JOptionPane.
     * @return The panel to be rendered.
     */
    public JPanel createContent(DialogApi<T> api) {
        return dialogContentCreator.createContent(api);
    }
    
    
    /**
     * A content creator for the dialog.
     * @param <T> The return type of the dialog.
     */
    public interface DialogContentCreator<T> {
        
        /**
         * Creates the content to be displayed for this dialog.
         * 
         * @param dialogApi An API for setting the result.
         * @return The panel that will be displayed for this dialog.
         */
        JPanel createContent(DialogApi<T> dialogApi);
    }
    
    /**
     * Access to to behavior that for modal dialogs are handled in
     * {@code FreeColOptionPaneUI}.
     * 
     * @param <T> The return type of the dialog.
     */
    public interface DialogApi<T> {
        
        /**
         * Sets the value returned by this dialog. Setting a value
         * will remove the dialog.
         * 
         * @param value The value to be returned. Use {@code null} for indicating
         *      that the dialog has been cancelled. {@code null} will also be returned
         *      if the dialog is closed by the escape key.
         */
        void setValue(T value);
        
        /**
         * Sets the component that should have the initial focus when displaying the dialog.
         * @param component The component that will have its {@link Component#requestFocus()} called.
         */
        void setInitialFocusComponent(Component component);
    }
}
