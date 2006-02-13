
package net.sf.freecol.client.gui.option;


/**
* Interface for classes which temporarily stores changes for an
* <code>Option</code>. Calling {@link #updateOption} should update
* the {@link net.sf.freecol.common.option.Option} with that new
* information.
*/
public interface OptionUpdater {

    public static final  String  COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";
    public static final  String  LICENSE   = "http://www.gnu.org/licenses/gpl.html";
    public static final  String  REVISION  = "$Revision$";


    /**
    * Updates the value of the {@link net.sf.freecol.common.option.Option}
    * this object keeps.
    */
    public void updateOption();

}
