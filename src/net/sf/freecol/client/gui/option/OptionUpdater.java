
package net.sf.freecol.client.gui.option;


/**
* Interface for classes which temporarily stores changes for an
* <code>Option</code>. Calling {@link #updateOption} should update
* the {@link net.sf.freecol.common.option.Option} with that new
* information.
*/
public interface OptionUpdater {

    /**
    * Updates the value of the {@link net.sf.freecol.common.option.Option}
    * this object keeps.
    */
    public void updateOption();

}
