
package net.sf.freecol.client.gui;


import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.MissingResourceException;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPopupMenu;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;

import net.sf.freecol.client.networking.Client;

import net.sf.freecol.client.gui.panel.ChatPanel;

import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.QuitDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.StatusPanel;

import net.sf.freecol.client.gui.panel.MapControls;

import net.sf.freecol.client.gui.panel.ImageProvider;

import net.sf.freecol.client.gui.sound.MusicLibrary;
import net.sf.freecol.client.gui.sound.SfxLibrary;
import net.sf.freecol.client.gui.sound.SoundPlayer;
import net.sf.freecol.client.gui.i18n.Messages;

import net.sf.freecol.common.FreeColException;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;

import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.client.FreeColClient;

import net.sf.freecol.client.control.*;




/**
* The main container for the other GUI components in FreeCol.
* This container is where the panels, dialogs and menus are added.
* In addition, this is the component in which the map graphics are displayed.
*
* <br><br>
*
* <br><b>Displaying panels and a dialogs</b>
* <br><br>
*
* <code>Canvas</code> contains  methods to display various panels
* and dialogs. Most of these methods use
* {@link net.sf.freecol.client.gui.i18n i18n} to get localized
* text. Here is an example:
*
* <br>
*
* <PRE>
* if (canvas.showConfirmDialog("choice.text", "choice.yes", "choice.no")) {
*     // DO SOMETHING.
* }
* </PRE>
*
* <br>
*
* where "choice.text", "choice.yes" and "choice.no" are keys for a localized
* message. See {@link net.sf.freecol.client.gui.i18n i18n} for more
* information.
*
* <br><br>
*
* <br><b>The difference between a panel and a dialog</b>
* <br><br>
*
* When displaying a dialog, using a <code>showXXXDialog</code>, the calling thread
* will wait until that dialog is dismissed before returning. In contrast, a
* <code>showXXXPanel</code>-method returns immediatly.
*/
public final class Canvas extends JLayeredPane {
    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    private final FreeColClient     freeColClient;
    private final MainPanel         mainPanel;
    private final NewPanel          newPanel;
    private final ErrorPanel        errorPanel;
    private final StartGamePanel    startGamePanel;
    private final QuitDialog        quitDialog;
    private final ColonyPanel       colonyPanel;
    private final EuropePanel       europePanel;
    private final StatusPanel       statusPanel;
    private final ChatPanel         chatPanel;
    private final GUI               gui;
    private final ChatDisplayThread chatDisplayThread;
    private TakeFocusThread         takeFocusThread;
    private MapControls             mapControls;
    private JMenuBar                jMenuBar;



    /**
    * The constructor to use.
    *
    * @param freeColClient The main control class.
    * @param bounds The bounds of this <code>Canvas</code>.
    * @param gui The object responsible of drawing the map onto this component.
    */
    public Canvas(FreeColClient freeColClient, Rectangle bounds, GUI gui) {
        this.freeColClient = freeColClient;
        this.gui = gui;

        setBounds(bounds);

        setOpaque(false);
        setLayout(null);

        takeFocusThread = null;

        mainPanel = new MainPanel(this);
        newPanel = new NewPanel(this, freeColClient.getConnectController());
        errorPanel = new ErrorPanel(this);
        startGamePanel = new StartGamePanel(this, freeColClient);
        quitDialog = new QuitDialog(this);
        colonyPanel = new ColonyPanel(this, freeColClient);

        europePanel = new EuropePanel(this, freeColClient, freeColClient.getInGameController());
        statusPanel = new StatusPanel(this);

        chatPanel = new ChatPanel(this);

        showMainPanel();

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        takeFocus();

        chatDisplayThread = new ChatDisplayThread();
        chatDisplayThread.start();
        logger.info("Canvas created.");
    }





    /**
    * Sets the menu bar. The menu bar will be resized to fit the width
    * of the gui and made visible.
    *
    * @param mb The menu bar.
    */
    public void setJMenuBar(JMenuBar mb) {
        if (jMenuBar != null) {
            remove(jMenuBar);
        }

        mb.setLocation(0, 0);
        mb.setSize(getWidth(), (int) mb.getPreferredSize().getHeight());
        add(mb);

        jMenuBar = mb;
    }


    /**
    * Paints this component. This method will use
    * {@link GUI#display} to draw the map/background on this component.
    *
    * @param g The Graphics context in which to draw this component.
    */
    public void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        gui.display(g2d);
    }


    /**
    * Displays the <code>StartGamePanel</code>.
    *
    * @param game The <code>Game</code> that is about to start.
    * @param player The <code>Player</code> using this client.
    * @param singlePlayerMode 'true' if the user wants to start a single player game,
    *        'false' otherwise.
    */
    public void showStartGamePanel(Game game, Player player, boolean singlePlayerMode) {
        closeMenus();

        if (game != null && player != null) {
            startGamePanel.initialize(singlePlayerMode);
            startGamePanel.setLocation(getWidth() / 2 - startGamePanel.getWidth() / 2, getHeight() / 2 - startGamePanel.getHeight() / 2);
            add(startGamePanel);
            startGamePanel.requestFocus();
        } else {
            logger.warning("Tried to open 'StartGamePanel' without having 'game' and/or 'player' set.");
        }
    }


    /**
    * Displays the <code>ChatPanel</code>.
    */
    public void showChatPanel() {
        closeMenus();
        chatPanel.setLocation(getWidth() / 2 - chatPanel.getWidth() / 2, getHeight() / 2 - chatPanel.getHeight() / 2);
        setEnabled(false);
        add(chatPanel);
        chatPanel.requestFocus();
    }


    /**
    * Displays the <code>NewGamePanel</code>.
    */
    public void showNewGamePanel() {
        closeMenus();
        newPanel.setLocation(getWidth() / 2 - newPanel.getWidth() / 2, getHeight() / 2 - newPanel.getHeight() / 2);
        add(newPanel);
        newPanel.requestFocus();
    }


    /**
    * Displays a dialog with a text and a ok/cancel option.
    *
    * @param text The text that explains the choice for the user.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return <i>true</i> if the user clicked the "ok"-button
    *         and <i>false</i> otherwise.
    */
    public boolean showConfirmDialog(String text, String okText, String cancelText) {
        try {
            text = Messages.message(text);
            okText = Messages.message(okText);
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + ", " + okText + " or " + cancelText + ".");
        }

        FreeColDialog confirmDialog = FreeColDialog.createConfirmDialog(text, okText, cancelText);
        confirmDialog.setLocation(getWidth() / 2 - confirmDialog.getWidth() / 2, getHeight() / 2 - confirmDialog.getHeight() / 2);
        add(confirmDialog, new Integer(POPUP_LAYER.intValue() - 1));
        confirmDialog.requestFocus();

        boolean response = confirmDialog.getResponseBoolean();

        remove(confirmDialog);

        return response;
    }


    /**
    * Displays a dialog with a text field and a ok/cancel option.
    *
    * @param text The text that explains the action to the user.
    * @param defaultValue The default value appearing in the text field.
    * @param okText The text displayed on the "ok"-button.
    * @param cancelText The text displayed on the "cancel"-button.
    * @return The text the user have entered or <i>null</i> if the
    *         user chose to cancel the action.
    */
    public String showInputDialog(String text, String defaultValue, String okText, String cancelText) {
        try {
            text = Messages.message(text);
            okText = Messages.message(okText);
            cancelText = Messages.message(cancelText);
        } catch (MissingResourceException e) {
            logger.warning("could not find message with id: " + text + ", " + okText + " or " + cancelText + ".");
        }

        FreeColDialog inputDialog = FreeColDialog.createInputDialog(text, defaultValue, okText, cancelText);
        inputDialog.setLocation(getWidth() / 2 - inputDialog.getWidth() / 2, getHeight() / 2 - inputDialog.getHeight() / 2);
        add(inputDialog, new Integer(POPUP_LAYER.intValue() - 1));
        inputDialog.requestFocus();

        String response = (String) inputDialog.getResponse();

        remove(inputDialog);

        return response;
    }


    /**
    * Shows a status message that cannot be dismissed.
    * The panel will be removed when another component
    * is added to this <code>Canvas</code>. This includes
    * all the <code>showXXX</code>-methods. In addition,
    * {@link #closeMenus} also removes this panel.
    *
    * @param message The text message to display on the
    *                status panel.
    */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);

        statusPanel.setLocation(getWidth() / 2 - statusPanel.getWidth() / 2, getHeight() / 2 - statusPanel.getHeight() / 2);
        add(statusPanel, new Integer(POPUP_LAYER.intValue() - 10));
    }


    /**
    * Displays the <code>EuropePanel</code>.
    */
    public void showEuropePanel() {
        closeMenus();

        if (freeColClient.getGame() == null) {
            errorMessage("europe.noGame");
        } else {
            europePanel.initialize(freeColClient.getMyPlayer().getEurope(), freeColClient.getGame());
            europePanel.setLocation(getWidth() / 2 - europePanel.getWidth() / 2,
                                    getHeight() / 2 - europePanel.getHeight() / 2);
            mapControls.removeFromComponent(this);
            setEnabled(false);
            add(europePanel);
            europePanel.requestFocus();
        }
    }


    /**
    * Displays the colony panel of the given <code>Colony</code>.
    * @param colony The colony whose panel needs to be displayed.
    */
    public void showColonyPanel(Colony colony) {
        closeMenus();

        gui.setActiveUnit(null);
        gui.setSelectedTile(null);

        colonyPanel.initialize(colony, freeColClient.getGame());
        colonyPanel.setLocation(getWidth() / 2 - colonyPanel.getWidth() / 2,
                                getHeight() / 2 - colonyPanel.getHeight() / 2);
        mapControls.removeFromComponent(this);
        setEnabled(false);
        add(colonyPanel);

        colonyPanel.requestFocus();
    }


    /**
    * Removes the given component from this Container.
    * @param comp The component to remove from this Container.
    */
    public void remove(Component comp) {
        boolean takeFocus = true;
        if (comp == statusPanel) {
            takeFocus = false;
        }

        Rectangle bounds = comp.getBounds();
        setEnabled(true);
        super.remove(comp);

        if (takeFocus) {
            takeFocus();
        }

        repaint(bounds.x, bounds.y, bounds.width, bounds.height);
    }


    /**
    * Adds a component to this Canvas.
    * @param comp The component to add to this ToEuropePanel.
    * @return The component argument.
    */
    public Component add(Component comp) {
        if ((takeFocusThread != null) && (takeFocusThread.isAlive())) {
            takeFocusThread.stopWorking();
        }

        remove(statusPanel);
        Component c = super.add(comp);
        return c;
    }


    /**
    * Adds a component to this Canvas. Removes the statuspanel if visible
    * (and <code>comp != statusPanel</code>).
    * @param comp The component to add to this ToEuropePanel.
    * @param i The layer to add the component to (see JLayeredPane).
    */
    public void add(Component comp, Integer i) {
        if ((takeFocusThread != null) && (takeFocusThread.isAlive())) {
            takeFocusThread.stopWorking();
        }

        if (comp != statusPanel) {
            remove(statusPanel);
        }

        super.add(comp, i);
    }


    /**
    * Makes sure that this Canvas takes the focus. It will keep on trying for
    * a while even its request doesn't get granted immediately.
    */
    private void takeFocus() {
        JComponent c = this;

        if (startGamePanel.isShowing()) {
            c = startGamePanel;
        } else if (newPanel.isShowing()) {
            c = newPanel;
        } else if (mainPanel.isShowing()) {
            c = mainPanel;
        } else if (europePanel.isShowing()) {
            c = europePanel;
        } else if (colonyPanel.isShowing()) {
            c = colonyPanel;
        }

        c.requestFocus();

        // Later:
        /*if ((takeFocusThread == null) || (!takeFocusThread.isAlive()) || (!takeFocusThread.isStillWorking())
                || takeFocusThread.getComponent() != c) {

            if (takeFocusThread != null) {
                takeFocusThread.stopWorking();
            }

            takeFocusThread = new TakeFocusThread(c);
            takeFocusThread.start();
        }*/
    }


    /**
    * Enables or disables this component depending on the given argument.
    * @param b Must be set to 'true' if this component needs to be enabled
    * or to 'false' otherwise.
    */
    public void setEnabled(boolean b) {
        for (int i = 0; i < getComponentCount(); i++) {
            getComponent(i).setEnabled(b);
        }
        super.setEnabled(b);
    }


    /**
    * Shows the map controls on this Canvas.
    */
    public void showMapControls() {
        mapControls.addToComponent(this);
        takeFocus();
    }


    /**
    * Sets the map controls.
    */
    public void setMapControls(MapControls mapControls) {
        this.mapControls = mapControls;
    }


    /**
    * Gets the map controls.
    */
    public MapControls getMapControls() {
        return mapControls;
    }


    /**
    * Shows the given popup at the given position on the screen.
    *
    * @param popup The JPopupMenu to show.
    * @param x The x-coordinate at which to show the popup.
    * @param y The y-coordinate at which to show the popup.
    */
    public void showPopup(JPopupMenu popup, int x, int y) {
        closeMenus();
        popup.show(this, x, y);
    }


    /**
    * Shows a tile popup.
    *
    * @param pos The coordinates of the Tile where the popup occured.
    * @param x The x-coordinate on the screen where the popup needs to be placed.
    * @param y The y-coordinate on the screen where the popup needs to be placed.
    */
    public void showTilePopup(Map.Position pos, int x, int y) {
        if (pos != null) {
            Tile t = freeColClient.getGame().getMap().getTileOrNull(pos.getX(), pos.getY());

            if (t != null) {
                showPopup(new TilePopup(t, freeColClient, this, getGUI()), x, y);
            }
        }
    }


    /**
    * Displays an error message.
    * @param messageID The i18n-keyname of the error message to display.
    */
    public void errorMessage(String messageID) {
        errorMessage(messageID, "Unspecified error: " + messageID);
    }


    /**
    * Displays an error message.
    *
    * @param messageID The i18n-keyname of the error message to display.
    * @param message An alternativ message to display if the resource
    *                specified by <code>messageID</code> is unavailable.
    */
    public void errorMessage(String messageID, String message) {
        if (messageID != null) {
            try {
                message = Messages.message(messageID);
            } catch (MissingResourceException e) {
                logger.warning("could not find message with id: " + messageID);
            }
        }

        errorPanel.initialize(message);
        errorPanel.setLocation(getWidth() / 2 - errorPanel.getWidth() / 2, getHeight() / 2 - errorPanel.getHeight() / 2);
        //setEnabled(false);
        add(errorPanel, JLayeredPane.MODAL_LAYER);
        errorPanel.requestFocus();
        errorPanel.getResponse();
        remove(errorPanel);
    }


    /**
    * Shows a message with some information and
    * a "Ok"-button.
    */
    public void showInformationMessage(String messageId) {
        errorMessage(messageId);
    }

    /**
    * Closes the <code>ErrorPanel</code>.
    */
    public void closeErrorPanel() {
        remove(errorPanel);
    }


    /**
    * Refreshes this Canvas visually.
    */
    public void refresh() {
        gui.forceReposition();
        repaint(0, 0, getWidth(), getHeight());
    }


    /**
    * Refreshes the screen at the specified Tile.
    *
    * @param x The x-coordinate of the Tile to refresh.
    * @param y The y-coordinate of the Tile to refresh.
    */
    public void refreshTile(int x, int y) {
        if (x >= 0 && y >= 0) {
            repaint(gui.getTileBounds(x, y));
        }
    }


    /**
    * Refreshes the screen at the specified Tile.
    * @param t The tile to refresh.
    */
    public void refreshTile(Tile t) {
        refreshTile(t.getX(), t.getY());
    }


    /**
    * Refreshes the screen at the specified Tile.
    * @param p The position of the tile to refresh.
    */
    public void refreshTile(Position p) {
        refreshTile(p.getX(), p.getY());
    }


    /**
    * Returns the image provider that is being used by this canvas.
    * @return The image provider that is being used by this canvas.
    */
    public ImageProvider getImageProvider() {
        return gui.getImageLibrary();
    }


    /**
    * Closes all the menus that are currently open.
    */
    public void closeMenus() {
        remove(newPanel);
        remove(startGamePanel);
        remove(colonyPanel);
        remove(europePanel);
        remove(statusPanel);
    }


    /**
    * Shows the <code>MainPanel</code>.
    */
    public void showMainPanel() {
        mainPanel.setLocation(getWidth() / 2 - mainPanel.getWidth() / 2, getHeight() / 2 - mainPanel.getHeight() / 2);
        add(mainPanel, new Integer(-100));
        mainPanel.requestFocus();
    }


    /**
    * Closes the {@link MainPanel}.
    */
    public void closeMainPanel() {
        remove(mainPanel);
    }


    /**
    * Shows the <code>OpenGamePanel</code>.
    */
    public void showOpenGamePanel() {
        errorMessage("openGame.unimplemented");
    }


    /**
    * Gets the <code>StartGamePanel</code> that lies in this container.
    * @return The <code>StartGamePanel</code>.
    */
    public StartGamePanel getStartGamePanel() {
        return startGamePanel;
    }


    /**
    * Tells the map controls that a chat message was recieved.
    * @param sender The player who sent the chat message to the server.
    * @param message The chat message.
    * @param privateChat 'true' if the message is a private one, 'false' otherwise.
    */
    public void displayChatMessage(Player sender, String message, boolean privateChat) {
        gui.addMessage(new GUIMessage(sender.getName() + ": " + message, sender.getColor()));
    }


    /**
    * Displays a chat message originating from this client.
    * @param message The chat message.
    */
    public void displayChatMessage(String message) {
        displayChatMessage(freeColClient.getMyPlayer(), message, false);
    }


    /**
    * Quits the application. Uses {@link #confirmQuitDialog} to get
    * a "Are you sure"-confirmation from the user.
    */
    public void quit() {
        if (confirmQuitDialog()) {
            System.exit(0);
        }
    }


    public boolean confirmQuitDialog() {
        quitDialog.setLocation(getWidth() / 2 - quitDialog.getWidth() / 2, getHeight() / 2 - quitDialog.getHeight() / 2);
        add(quitDialog, JLayeredPane.POPUP_LAYER);
        quitDialog.requestFocus();

        return quitDialog.getResponseBoolean();
    }


    /**
    * Returns this <code>Canvas</code>'s <code>GUI</code>.
    * @return The <code>GUI</code>.
    */
    public GUI getGUI() {
        return gui;
    }

    /** Returns the freeColClient.
    * @returns The <code>freeColClient</code> associated with this <code>Canvas</code>.
    */
    public FreeColClient getClient() {
      return freeColClient;
    }

    /**
    * Makes sure that old chat messages are removed in time.
    */
    private final class ChatDisplayThread extends Thread {
        /**
        * The constructor to use.
        */
        public ChatDisplayThread() {
            super("ChatDisplayThread");
        }

        /**
        * Removes old chat messages regularly.
        */
        public void run() {
            for (;;) {
                if (gui.removeOldMessages()) {
                    refresh();
                }
                try {
                    sleep(500);
                }
                catch (InterruptedException e) {
                }
            }
        }
    }




    /**
    * Makes sure that a given component takes the focus.
    */
    private final class TakeFocusThread extends Thread {
        private final JComponent component;
        private boolean doYourWork;

        /**
        * The constructor to use.
        * @param component The component that needs focus.
        */
        public TakeFocusThread(JComponent component) {
            super("TakeFocusThread");
            this.component = component;
            doYourWork = true;
        }

        /**
        * Makes sure that this thread stops working.
        */
        public void stopWorking() {
            doYourWork = false;
        }

        /**
        * Returns 'true' if this thread is going to keep on working, 'false' otherwise.
        * @return 'true' if this thread is going to keep on working, 'false' otherwise.
        */
        public boolean isStillWorking() {
            return doYourWork;
        }


        /**
        * Gets the component this thread is trying to take focus for.
        */
        public JComponent getComponent() {
            return component;
        }


        /**
        * Makes sure that the given component takes the focus.
        */
        public void run() {
            int count = 0;

            while ((!component.hasFocus()) && doYourWork) {
                component.requestFocus();

                try {
                    sleep(100);
                }
                catch (InterruptedException e) {
                }
                count++;
                if (count > 50) {
                    // We're already been trying for 5 seconds, there must be something wrong.
                    logger.warning("Component can't get focus: " + component.toString());
                }
            }
        }
    }

}
