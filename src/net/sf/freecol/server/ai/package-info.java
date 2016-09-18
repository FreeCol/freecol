/**
 * <h1>FreeCol Artifical Intelligence</h1>
 *
 * <p>The main package of the Artifical Intelligence (AI) package tree.</p>
 *
 * <p>{@link net.sf.freecol.server.ai.AIMain} has
 * the responsibility of creating and managing AI-objects.
 * Each instance of {@link net.sf.freecol.server.ai.AIObject} stores AI-specific information
 * relating to a single {@code FreeColGameObject}. For example:
 * {@link net.sf.freecol.server.ai.AIUnit AIUnit} contains information about a single unit and has
 * the methods the AI needs for controlling this unit.</p>
 *
 * <h2>Communication with the server</h2>
 *
 * <p>The server uses a {@code Connection} when communicating with the clients.
 * The subclass {@link net.sf.freecol.server.networking.DummyConnection DummyConnection}
 * is used for the computer controlled players, in order to avoid unnecessary network traffic.
 * {@link net.sf.freecol.server.ai.AIInGameInputHandler} handles the messages received on
 * the {@code DummyConnection} and calls the appropriate methods in
 * {@link net.sf.freecol.server.ai.AIPlayer}. An example: the method
 * {@link net.sf.freecol.server.ai.AIPlayer#startWorking} gets invoked when it is the
 * AI-player's turn.</p>
 *
 * <p>The AI package is a part of the server so the server model is used by
 * the computer players. We have defined the following interface for getting/modifying
 * data within the model:</p>
 * <ul>
 *  <li>The AI may access information in the model directly.</li>
 *  <li>Any changes to the model should be done by sending a network message through the "DummyConnection" the computer
 *          player is using. The reason for not changing the model directly, is that the server's control code has the
 *          responsibility of updating the clients when a change occurs.</li>
 * </ul>
 *
 * <p>This interface is a bit confusing and will probably be changed in the future
 * (possibly by supporting direct manipulation of the model from the ai-code).</p>
 */
package net.sf.freecol.server.ai;