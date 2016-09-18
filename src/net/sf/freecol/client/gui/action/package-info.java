/**
 * <p>Contains the {@code Action}s used by the GUI in menus and other places.</p>
 *
 * <p>The actions are stored by the {@link net.sf.freecol.client.gui.action.ActionManager} and
 * are all subclasses of {@link net.sf.freecol.client.gui.action.FreeColAction}.</p>
 *
 * <p>If you implement a new action, you must also add a corresponding
 * line to the {@link net.sf.freecol.client.gui.action.ActionManager}
 * method. Each action is identified by a short ID, such as
 * "quitAction". In order to provide localization, you must also add a
 * line to the localization file
 * "data/strings/FreeColMessages.properties" using the ID of the action
 * plus ".name" as key.</p>
 *
 * <p>You can also add a line with the ID of the action plus ".accelerator"
 * in order to define a key binding. The value of the accelerator must be
 * unique and must be valid Java KeyStroke Strings, as described
 * <a href="https://docs.oracle.com/javase/8/docs/api/javax/swing/KeyStroke.html#getKeyStroke-java.lang.String-">here</a>.</p>
 *
 * @since 0.4.0
 */
package net.sf.freecol.client.gui.action;