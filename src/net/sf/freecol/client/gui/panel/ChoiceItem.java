
package net.sf.freecol.client.gui.panel;


/**
* Can be used as a single choice for the {@link FreeColDialog#createChoiceDialog choice dialog}.
*/
public class ChoiceItem {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";

    private String text;
    private Object object;


    /**
    * Creates a new <code>ChoiceItem</code> with the
    * object beeing a <code>new Integer(choice)</code>.
    *
    * @param text The text that should be used to represent
    *        this choice.
    * @param choice An <code>int</code> identifying this choice.
    */
    public ChoiceItem(String text, int choice) {
        this(text, new Integer(choice));
    }


    /**
    * Creates a new <code>ChoiceItem</code> with the
    * given object.
    *
    * @param text The text that should be used to represent 
    *        this choice.
    * @param object The <code>Object</code> contained by this
    *        choice.
    */
    public ChoiceItem(String text, Object object) {
        this.text = text;
        this.object = object;
    }


    /**
    * Gets the <code>Object</code> contained by this choice.
    * @return The <code>Object</code>.
    */
    public Object getObject() {
        return object;
    }


    /**
    * Gets the choice as an <code>int</code>.
    *
    * @return The number representing this object.
    * @exception ClassCastException if the {@link #getObject object} is
    *            not an <code>Integer</code>.
    */
    public int getChoice() {
        return ((Integer) object).intValue();
    }


    /**
    * Gets a textual presentation of this object.
    * @return The <code>text</code> set in the constructor.
    */
    public String toString() {
        return text;
    }
}
