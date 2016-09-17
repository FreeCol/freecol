/**
 * <p>Contains the {@code Action}s used by the GUI in menus and other places.</p>
 * <p>The actions are stored by the {@link net.sf.freecol.client.gui.action.ActionManager} and
 * are all subclasses of {@link net.sf.freecol.client.gui.action.FreeColAction}.</p>
 * <p>If you implement a new action, you must also add a corresponding
 * line to the {@link net.sf.freecol.client.gui.action.ActionManager#initializeActions(InGameController, ConnectController)}
 * method. Each action is identified by a short ID, such as
 * "quitAction". In order to provide localization, you must also add a
 * line to the localization file
 * "data/strings/FreeColMessages.properties" using the ID of the action
 * plus ".name" as key.</p>
 * <p>You can also add a line with the ID of the action plus ".accelerator"
 * in order to define a key binding. The value of the accelerator must be
 * unique and must be valid Java KeyStroke Strings, as described
 * <a href="http://java.sun.com/j2se/1.5.0/docs/api/javax/swing/KeyStroke.html#getKeyStroke%28java.lang.String%29">here</a>.</p>
 */
package net.sf.freecol.client.gui.action;