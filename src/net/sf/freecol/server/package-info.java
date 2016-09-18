/**
 * <p>The main package of the server package tree.</p>
 *
 * <p>The main class of the server is {@link net.sf.freecol.server.FreeColServer}. This class both starts and keeps
 * references to all of the server objects and the game model objects.</p>
 *
 * <p>The class responsible for network communication is {@link net.sf.freecol.server.networking.Server}. The control
 * object responsible for handling new connections is {@link net.sf.freecol.server.control.UserConnectionHandler}.</p>
 *
 * <p>The main class of the model is {@link net.sf.freecol.common.model.Game}. All
 * {@link net.sf.freecol.common.model.Player} objects in the server's game model are
 * {@link net.sf.freecol.server.model.ServerPlayer} objects.</p>
 *
 * @since 0.2.1
 */
package net.sf.freecol.server;