/**
 * <h1>FreeCol Sound and Music package</h1>
 *
 * <p>This package contains the classes for handling sound effects and music in FreeCol.
 * {@link net.sf.freecol.client.FreeColClient} initializes {@link net.sf.freecol.client.control.SoundController} which
 * initializes the players. Pointer to SoundController are stored in FreeColClient and GUI.</p>
 *
 * <p>This is the method for playing sounds (provided you have got access to the pointers):<br>
 * <span style="font-family: monospace; margin-left: 40px;">soundController.playSound(ILLEGAL_MOVE);</span>
 *
 * @since 0.2.1 As part of the Client.GUI package.
 */
package net.sf.freecol.common.sound;