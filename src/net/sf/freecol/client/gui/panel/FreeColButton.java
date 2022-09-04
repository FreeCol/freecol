/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

public class FreeColButton extends JButton {
    public static String BUTTON_STYLE_PROPERTY_NAME = "buttonStyle";
    
    public enum ButtonStyle {
        IMPORTANT,
        SIMPLE,
        TRANSPARENT;
    }
    
    private ButtonStyle buttonStyle;
    

    public FreeColButton() {
    }

    public FreeColButton(Action a) {
        super(a);
    }
    
    public FreeColButton(ButtonStyle buttonStyle, Action a) {
        super(a);
        
        this.buttonStyle = buttonStyle;
        
        updateUI();
    }

    public FreeColButton(Icon icon) {
        super(icon);
    }

    public FreeColButton(String text, Icon icon) {
        super(text, icon);
    }

    public FreeColButton(String text) {
        super(text);
    }
    
    
    public FreeColButton withButtonStyle(ButtonStyle buttonStyle) {
        final ButtonStyle oldStyle = this.buttonStyle;
        this.buttonStyle = buttonStyle;
        
        firePropertyChange(BUTTON_STYLE_PROPERTY_NAME, oldStyle, buttonStyle);
        
        return this;
    }
    
    public ButtonStyle getButtonStyle() {
        return buttonStyle;
    }
}
