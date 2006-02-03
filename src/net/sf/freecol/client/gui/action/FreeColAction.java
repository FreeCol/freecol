

package net.sf.freecol.client.gui.action;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.option.Option;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * The super class of all actions in FreeCol. Subclasses of this
 * object is stored in an {@link ActionManager}.
 */
public abstract class FreeColAction extends AbstractAction implements Option {
    private static final Logger logger = Logger.getLogger(FreeColAction.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private final FreeColClient freeColClient;

    /**
     * Creates a new <code>FreeColAction</code>.
     * 
     * @param freeColClient The main controller object for the client.
     * @param name An i18n-key to identify the name of this action.
     * @param shortDescription An i18n-key to identify a short 
     *      description of this action. This value can be set to
     *      <code>null</code> if the action does not have a
     *      description.
     * @param mnemonic A mnemonic to be used for selecting this action
     *      when the action is displaying on a menu etc.
     * @param accelerator The keyboard accelerator to be used for
     *      selecting this action or <code>null</code> if this action
     *      does not have an accelerator.
     */
    protected FreeColAction(FreeColClient freeColClient, String name, 
            String shortDescription, int mnemonic, KeyStroke accelerator) {
        super(Messages.message(name));

        this.freeColClient = freeColClient;

        putValue(SHORT_DESCRIPTION, shortDescription);
        putValue(MNEMONIC_KEY, new Integer(mnemonic));
        putValue(ACCELERATOR_KEY, accelerator);
    }

    
    /**
     * Gets the main controller object for the client.
     * @return The main controller object for the client.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Disables this option if the 
     * {@link net.sf.freecol.client.gui.panel.ClientOptionsDialog} 
     * is visible. This method should be extended by subclasses if
     * the action should be disabled in other cases.
     */
    public void update() {
        if (freeColClient.getCanvas() != null) {
            setEnabled(!freeColClient.getCanvas().getClientOptionsDialog().isShowing());
        }
    }    
    
    /**
     * Sets a keyboard accelerator.
     * @param accelerator The <code>KeyStroke</code>. Using <code>null</code>
     *        is the same as disabling the keyboard accelerator.
     */
    public void setAccelerator(KeyStroke accelerator) {
        //KeyStroke oldValue = (KeyStroke) getValue(ACCELERATOR_KEY);
        putValue(ACCELERATOR_KEY, accelerator);
    }
    
    /**
     * Gets the keyboard accelerator for this option.
     * @return The <code>KeyStroke</code> or <code>null</code>
     *        if the keyboard accelerator is disabled.
     */
    public KeyStroke getAccelerator() {
        return (KeyStroke) getValue(ACCELERATOR_KEY);
    }
    
    /**
     * Gives a short description of this <code>Option</code>.
     * Can for instance be used as a tooltip text.
     * 
     * @return A short description of this action.
     */
    public String getShortDescription() {
        return (String) getValue(SHORT_DESCRIPTION);
    }

    /**
     * Returns a textual representation of this object.
     * 
     * @return The name of this <code>Option</code>.
     * @see #getName
     */
    public String toString() {
        return getName();
    }

    /**
     * Returns the id of this <code>Option</code>.
     * @return An unique identifier for this action.
     */
    public abstract String getId();

    /**
     * Returns the name of this <code>Option</code>.
     * @return The name as provided in the constructor.
     */
    public String getName() {
        return (String) getValue(NAME);
    }    

    /**
     * Creates a <code>String</code> that keeps the attributes
     * given <code>KeyStroke</code>. This <code>String</code>
     * can be used to store the key stroke in an XML-file.
     * 
     * @param keyStroke The <code>KeyStroke</code>.
     * @return A <code>String</code> that produces a key stroke
     *         equal to the given <code>KeyStroke</code> if passed
     *         as a parameter to <code>getAWTKeyStroke(String)</code>.
     */
    public static String getKeyStrokeText(KeyStroke keyStroke) {
        /* 
         * AWTKeyStroke.toString() should be used instead of this method,
         * if we choose to require java 1.5.
         */
         
        if (keyStroke == null) {
            return "";
        }

        StringBuffer buf = new StringBuffer();

        // Add modifiers:
        int modifiers = keyStroke.getModifiers();
        if ((modifiers & InputEvent.SHIFT_DOWN_MASK) != 0 ) {
            buf.append("shift ");
        }
        if ((modifiers & InputEvent.CTRL_DOWN_MASK) != 0 ) {
            buf.append("ctrl ");
        }
        if ((modifiers & InputEvent.META_DOWN_MASK) != 0 ) {
            buf.append("meta ");
        }
        if ((modifiers & InputEvent.ALT_DOWN_MASK) != 0 ) {
            buf.append("alt ");
        }
        if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0 ) {
            buf.append("altGraph ");
        }
        if ((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0 ) {
            buf.append("button1 ");
        }
        if ((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0 ) {
            buf.append("button2 ");
        }
        if ((modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0 ) {
            buf.append("button3 ");
        }
        
        if (keyStroke.getKeyCode() == KeyEvent.VK_UNDEFINED) {
            buf.append("typed " + keyStroke.getKeyChar());
        } else {
            buf.append(keyStroke.getKeyEventType() == KeyEvent.KEY_PRESSED ? "pressed" : "released"); 
            buf.append(" ");

            int em = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
            Field[] fields = KeyEvent.class.getDeclaredFields();
            for (int i = 0; i < fields.length; i++) {
                try {
                    if (fields[i].getModifiers() == em 
                            && fields[i].getType() == Integer.TYPE
                            && fields[i].getName().startsWith("VK_") 
                            && fields[i].getInt(KeyEvent.class) == keyStroke.getKeyCode()) {
                        buf.append(fields[i].getName().substring(3));
                    }                   
                } catch (IllegalAccessException e) {}
            }
        }
        
        return buf.toString();
    }

    /**
     * Makes an XML-representation of this object.
     *
     * @param document The document to use when creating new componenets.
     * @return The DOM-element ("Document Object Model") made to represent 
     *      this "Option".
     */
    public Element toXMLElement(Document document) {
        Element optionElement = document.createElement(getId());

        optionElement.setAttribute("accelerator", getKeyStrokeText(getAccelerator()));

        return optionElement;
    }

    /**
     * Initializes this object from an XML-representation of this object.
     * @param element The DOM-element ("Document Object Model") made to 
     *       represent this "Option".
     */
    public void readFromXMLElement(Element element) {
        String acc = element.getAttribute("accelerator");
        if (!acc.equals("")) {
            putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(acc));
        } else {
            putValue(ACCELERATOR_KEY, null);
        }
    }
}
