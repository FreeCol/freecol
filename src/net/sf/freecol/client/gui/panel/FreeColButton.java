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
