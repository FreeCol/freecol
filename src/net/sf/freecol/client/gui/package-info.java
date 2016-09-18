/**
 * <h1>FreeCol Client GUI package</h1>
 *
 * <p>Contains the graphical user interface (GUI) classes.</p>
 *
 * <p>A {@code JFrame} is created during the startup of the
 * program. This frame will be a {@link net.sf.freecol.client.gui.FreeColFrame}
 * which handles both windowed and full screen presentations.</p>
 *
 * <p>A {@link net.sf.freecol.client.gui.Canvas} will then be added to the frame.</p>
 *
 * <p>{@code Canvas} is the main container for the other GUI components in FreeCol.
 * This class is where the panels, dialogs and menus are added. In addition, {@code Canvas}
 * is the component in which the map graphics are displayed.</p>
 *
 * <h2>Other important classes:</h2>
 *
 * <ul>
 *      <li>The {@link net.sf.freecol.client.gui.GUI} contains the methods to draw
 *          the map upon {@code Canvas}, in addition to other useful GUI methods.</li>
 *      <li>The {@link net.sf.freecol.client.gui.menu.FreeColMenuBar} is the menu bar that is
 *          displayed on top corner of the {@code Canvas}.</li>
 * </ul>
 *
 * @since 0.2.0
 *
 */
package net.sf.freecol.client.gui;