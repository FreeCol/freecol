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
 * A modal dialog returning a value when it's closed.
 * 
 * @param <T> The return type of the modal dialog.
 */
public class FreeColModalDialog<T> {
    /*
     * This is really a workaround to get a nice interface for making custom
     * modal dialogs. The dialog is displayed using the JOptionPane, while
     * the custom rendering and handling is implemented by FreeColOptionPaneUI.
     */

    private final ModalContentCreator<T> modalContentCreator;
    

    /**
     * Creates a {@code FreeColModalDialog} using the given creator.
     * @param modalContentCreator The code responsible for generating the content.
     */
    public FreeColModalDialog(ModalContentCreator<T> modalContentCreator) {
        this.modalContentCreator = modalContentCreator;
    }
    
    
    /**
     * This method is called by {@link FreeColOptionPaneUI} in order to
     * render the custom panel.
     * 
     * @param api The API provided by {@link FreeColOptionPaneUI} to acces
     *      functionality provided by JOptionPane.
     * @return The panel to be rendered.
     */
    public JPanel createContent(ModalApi<T> api) {
        return modalContentCreator.createContent(api);
    }
    
    
    /**
     * A content creator for the modal dialog.
     * @param <T> The return type of the modal dialog.
     */
    public interface ModalContentCreator<T> {
        
        /**
         * Creates the content to be displayed for this dialog.
         * 
         * @param modalApi An API for setting the result.
         * @return The panel that will be displayed for this dialog.
         */
        JPanel createContent(ModalApi<T> modalApi);
    }
    
    /**
     * Access to modal operations.
     * @param <T> The return type of the modal dialog.
     */
    public interface ModalApi<T> {
        
        /**
         * Sets the value returned by this dialog. Setting a value
         * will remove the dialog.
         * 
         * @param value The value to be returned. Use {@code null} for indicating
         *      that the dialog has been cancelled. {@code null} will also be returned
         *      if the dialog is closed by the escape key.
         */
        void setValue(T value);
        
        void setInitialFocusComponent(Component component);
    }
}
