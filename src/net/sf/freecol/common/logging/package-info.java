/**
 * <h1>FreeCol Common Logging package</h1>
 *
 * <p>Contains classes for handling logging information within FreeCol.</p>
 *
 * <p>Each class uses its own logger by adding the following line:</p>
 *
 * <p style="font-family: monospace; margin-left: 40px;">{@code private static final Logger logger = Logger.getLogger(FreeColGameObject.class.getName());}</p>
 *
 * Adding the logger to a FreeCol class (and replacing "FreeColGameObject" with the name of the class) allows the use of
 * methods such as {@code logger.warning("message");} to generate log entries.
 *
 * @see "java.util.logging.Logger"
 *
 * @since 0.2.1
 *
 */
package net.sf.freecol.common.logging;