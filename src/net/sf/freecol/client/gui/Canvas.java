/**
 *  Copyright (C) 2002-2013   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuItem;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI.MissionaryAction;
import net.sf.freecol.client.gui.GUI.ScoutColonyAction;
import net.sf.freecol.client.gui.GUI.ScoutIndianSettlementAction;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.AboutPanel;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.panel.CaptureGoodsDialog;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.panel.ChoiceItem;
import net.sf.freecol.client.gui.panel.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.panel.ClientOptionsDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.ColopediaPanel;
import net.sf.freecol.client.gui.panel.CompactLabourReport;
import net.sf.freecol.client.gui.panel.ConfirmDeclarationDialog;
import net.sf.freecol.client.gui.panel.DeclarationPanel;
import net.sf.freecol.client.gui.panel.DialogHandler;
import net.sf.freecol.client.gui.panel.DifficultyDialog;
import net.sf.freecol.client.gui.panel.DumpCargoDialog;
import net.sf.freecol.client.gui.panel.EditOptionDialog;
import net.sf.freecol.client.gui.panel.EditSettlementDialog;
import net.sf.freecol.client.gui.panel.EmigrationDialog;
import net.sf.freecol.client.gui.panel.EndTurnDialog;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FindSettlementPanel;
import net.sf.freecol.client.gui.panel.FreeColChoiceDialog;
import net.sf.freecol.client.gui.panel.FreeColConfirmDialog;
import net.sf.freecol.client.gui.panel.FreeColDialog;
import net.sf.freecol.client.gui.panel.FreeColOldDialog;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.panel.FreeColStringInputDialog;
import net.sf.freecol.client.gui.panel.GameOptionsDialog;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InformationPanel;
import net.sf.freecol.client.gui.panel.LabourData.UnitData;
import net.sf.freecol.client.gui.panel.LoadDialog;
import net.sf.freecol.client.gui.panel.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.panel.MapSizeDialog;
import net.sf.freecol.client.gui.panel.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.panel.MonarchDialog;
import net.sf.freecol.client.gui.panel.NegotiationDialog;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.panel.Parameters;
import net.sf.freecol.client.gui.panel.ParametersDialog;
import net.sf.freecol.client.gui.panel.PreCombatDialog;
import net.sf.freecol.client.gui.panel.PurchasePanel;
import net.sf.freecol.client.gui.panel.RecruitPanel;
import net.sf.freecol.client.gui.panel.ReportCargoPanel;
import net.sf.freecol.client.gui.panel.ReportColonyPanel;
import net.sf.freecol.client.gui.panel.ReportContinentalCongressPanel;
import net.sf.freecol.client.gui.panel.ReportEducationPanel;
import net.sf.freecol.client.gui.panel.ReportExplorationPanel;
import net.sf.freecol.client.gui.panel.ReportForeignAffairPanel;
import net.sf.freecol.client.gui.panel.ReportHighScoresPanel;
import net.sf.freecol.client.gui.panel.ReportHistoryPanel;
import net.sf.freecol.client.gui.panel.ReportIndianPanel;
import net.sf.freecol.client.gui.panel.ReportLabourDetailPanel;
import net.sf.freecol.client.gui.panel.ReportLabourPanel;
import net.sf.freecol.client.gui.panel.ReportMilitaryPanel;
import net.sf.freecol.client.gui.panel.ReportNavalPanel;
import net.sf.freecol.client.gui.panel.ReportProductionPanel;
import net.sf.freecol.client.gui.panel.ReportReligiousPanel;
import net.sf.freecol.client.gui.panel.ReportRequirementsPanel;
import net.sf.freecol.client.gui.panel.ReportTradePanel;
import net.sf.freecol.client.gui.panel.ReportTurnPanel;
import net.sf.freecol.client.gui.panel.RiverStyleDialog;
import net.sf.freecol.client.gui.panel.SaveDialog;
import net.sf.freecol.client.gui.panel.ScaleMapSizeDialog;
import net.sf.freecol.client.gui.panel.SelectAmountDialog;
import net.sf.freecol.client.gui.panel.SelectDestinationDialog;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TradeRouteDialog;
import net.sf.freecol.client.gui.panel.TradeRouteInputDialog;
import net.sf.freecol.client.gui.panel.TrainPanel;
import net.sf.freecol.client.gui.panel.VictoryPanel;
import net.sf.freecol.client.gui.panel.WarehouseDialog;
import net.sf.freecol.client.gui.panel.WorkProductionPanel;
import net.sf.freecol.client.gui.video.VideoComponent;
import net.sf.freecol.client.gui.video.VideoListener;
import net.sf.freecol.common.ServerInfo;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.io.FreeColFileFilter;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.resources.Video;


/**
 * The main container for the other GUI components in FreeCol. This
 * container is where the panels, dialogs and menus are added. In
 * addition, this is the component in which the map graphics are
 * displayed.
 *
 * <b>Displaying panels and a dialogs</b> <br>
 * <br>
 * <code>Canvas</code> contains methods to display various panels and dialogs.
 * Most of these methods use {@link net.sf.freecol.client.gui.i18n i18n} to get
 * localized text.  Dialogs return results, and may be modal or non-modal.
 */
public final class Canvas extends JDesktopPane {

    private static final Logger logger = Logger.getLogger(Canvas.class.getName());

    /** A wrapper class for non-modal dialogs. */
    private class DialogCallback<T> implements Runnable {
        
        /** The dialog to show. */
        private final FreeColDialog<T> fcd;

        /** An optional tile to guide the dialog placement. */
        private final Tile tile;

        /** The handler for the dialog response. */
        private final DialogHandler<T> handler;


        /**
         * Constructor.
         */
        public DialogCallback(FreeColDialog<T> fcd, Tile tile,
                              DialogHandler<T> handler) {
            this.fcd = fcd;
            this.tile = tile;
            this.handler = handler;
        }


        // Implement Runnable

        public void run() {
            // Display the dialog...
            viewFreeColDialog(fcd, tile);
            // ...and use another thread to wait for a dialog response...
            new Thread(fcd.toString()) {
                public void run() {
                    while (!fcd.responded()) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {}
                    }
                    // ...before handling the result.
                    handler.handle(fcd.getResponse());
                }
            }.start();
        }
    };


    public static enum BoycottAction {
        CANCEL,
        PAY_ARREARS,
        DUMP_CARGO
    }

    public static enum BuyAction {
        CANCEL,
        BUY,
        HAGGLE
    }

    public static enum ClaimAction {
        CANCEL,
        ACCEPT,
        STEAL
    }

    public static enum EventType {
        FIRST_LANDING,
        MEETING_NATIVES,
        MEETING_EUROPEANS,
        MEETING_AZTEC,
        MEETING_INCA,
        DISCOVER_PACIFIC
    }

    public static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    public static enum SellAction {
        CANCEL,
        SELL,
        HAGGLE,
        GIFT
    }

    public static enum TradeAction {
        CANCEL,
        BUY,
        SELL,
        GIFT
    }

    /** The extension for FreeCol save files. */
    private static final String FSG_EXTENSION = ".fsg";

    /** Number of tries to find a clear spot on the canvas. */
    private static final int MAXTRY = 3;

    private static final Integer MAIN_LAYER = JLayeredPane.DEFAULT_LAYER;

    private static final Integer STATUS_LAYER = JLayeredPane.POPUP_LAYER;

    /** A class for frames being used as tool boxes. */
    class ToolBoxFrame extends JInternalFrame {}

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The parent GUI. */
    private GUI gui;

    private MainPanel mainPanel;

    private final StartGamePanel startGamePanel;

    private final StatusPanel statusPanel;

    private final ChatPanel chatPanel;

    private final MapViewer mapViewer;

    private final ServerListPanel serverListPanel;

    private final LoadingSavegameDialog loadingSavegameDialog;    

    /** Used to detect resizing. */
    private Dimension oldSize = null;

    private Dimension initialSize = null;

    private boolean clientOptionsDialogShowing = false;

    /** Filters for loadable game files. */
    private FileFilter[] fileFilters = null;

    /** The dialogs in view. */
    private final List<FreeColDialog<?>> dialogs
        = new ArrayList<FreeColDialog<?>>();


    /**
     * The constructor to use.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param size The bounds of this <code>Canvas</code>.
     * @param mapViewer The object responsible of drawing the map onto
     *     this component.
     */
    public Canvas(final FreeColClient freeColClient, Dimension size,
                  MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.gui = freeColClient.getGUI();
        this.mapViewer = mapViewer;

        this.initialSize = size;

        setLocation(0, 0);
        setSize(size);

        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);

        startGamePanel = new StartGamePanel(freeColClient);
        serverListPanel = new ServerListPanel(freeColClient,
            freeColClient.getConnectController());
        statusPanel = new StatusPanel(freeColClient);
        chatPanel = new ChatPanel(freeColClient);
        loadingSavegameDialog = new LoadingSavegameDialog(freeColClient);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        // takeFocus();

        // chatDisplayThread = new ChatDisplayThread();
        // chatDisplayThread.start();

        // TODO: move shutdown hook from GUI to (say) client!
        Runtime runtime = Runtime.getRuntime();
        runtime.addShutdownHook(new Thread(FreeCol.CLIENT_THREAD+"Quitting Game") {
            @Override
            public void run() {
                freeColClient.getConnectController().quitGame(true);
            }
        });

        createKeyBindings();

        logger.info("Canvas created.");
    }


    // Internals

    /**
     * Adds a component on this Canvas inside a frame.  Removes the
     * StatusPanel if visible (and <code>comp != statusPanel</code>).
     *
     * @param comp The component to add to the canvas.
     * @param toolBox Should be set to true if the resulting frame is
     *     used as a toolbox (that is: it should not be counted as a
     *     frame).
     * @param popupPosition A preferred <code>PopupPosition</code>.
     * @param resizable Whether this component can be resized.
     * @return The <code>JInternalFrame</code> that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox,
                                      PopupPosition popupPosition,
                                      boolean resizable) {
        final int FRAME_EMPTY_SPACE = 60;

        final JInternalFrame f = (toolBox) ? new ToolBoxFrame()
            : new JInternalFrame();
        if (f.getContentPane() instanceof JComponent) {
            JComponent c = (JComponent) f.getContentPane();
            c.setOpaque(false);
            c.setBorder(null);
        }

        if (comp.getBorder() != null) {
            if (comp.getBorder() instanceof EmptyBorder) {
                f.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            } else {
                f.setBorder(comp.getBorder());
                comp.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            }
        } else {
            f.setBorder(null);
        }

        final FrameMotionListener fml = new FrameMotionListener(f);
        comp.addMouseMotionListener(fml);
        comp.addMouseListener(fml);
        if (f.getUI() instanceof BasicInternalFrameUI) {
            BasicInternalFrameUI biu = (BasicInternalFrameUI) f.getUI();
            biu.setNorthPane(null);
            biu.setSouthPane(null);
            biu.setWestPane(null);
            biu.setEastPane(null);
        }

        f.getContentPane().add(comp);
        f.setOpaque(false);
        f.pack();
        int width = f.getWidth();
        int height = f.getHeight();
        if (width > getWidth() - FRAME_EMPTY_SPACE) {
            width = Math.min(width, getWidth());
        }
        if (height > getHeight() - FRAME_EMPTY_SPACE) {
            height = Math.min(height, getHeight());
        }
        f.setSize(width, height);
        Point p = chooseLocation(comp, width, height, popupPosition);
        f.setLocation(p);
        add(f, MODAL_LAYER);
        f.setName(comp.getClass().getSimpleName());

        f.setFrameIcon(null);
        f.setVisible(true);
        f.setResizable(resizable);
        try {
            f.setSelected(true);
        } catch (java.beans.PropertyVetoException e) {}

        return f;
    }

    /**
     * Adds a component centered on this Canvas.
     *
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addCentered(Component comp, Integer i) {
        comp.setLocation((getWidth() - comp.getWidth()) / 2,
                         (getHeight() - comp.getHeight()) / 2);

        add(comp, i);
    }

    /**
     * Adds a component to this Canvas.  Removes the statusPanel if
     * visible (and <code>comp != statusPanel</code>).
     *
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addToCanvas(Component comp, Integer i) {
        if (comp != statusPanel && !(comp instanceof JMenuItem)
            && !(comp instanceof FreeColOldDialog<?>)
            && statusPanel.isVisible()) {
            removeFromCanvas(statusPanel);
        }

        try {
            if (i == null) {
                super.add(comp);
            } else {
                super.add(comp, i);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "addToCanvas(" + comp + ", " + i
                + ") failed.", e);
        }
    }

    /**
     * Choose a location for a component.
     *
     * @param comp The <code>Component</code> to place.
     * @param width The component width to use.
     * @param height The component height to use.
     * @param popupPosition An optional <code>PopupPosition</code> hint.
     * @return A suitable <code>Point</code> to place the component.
     */
    private Point chooseLocation(Component comp, int width, int height, 
                                 PopupPosition popupPosition) {
        Point p = null;
        if ((comp instanceof FreeColPanel || comp instanceof FreeColDialog<?>)
            && (p = getSavedPosition(comp)) != null) {
            // Sanity check stuff coming out of client options.
            if (p.getX() < 0
                || p.getX() >= getWidth() - width
                || p.getY() < 0
                || p.getY() >= getHeight() - height) {
                p = null;
            }
        }
        int x = 0, y = 0;
        if (p != null) {
            x = (int)p.getX();
            y = (int)p.getY();
        } else if (popupPosition != null) {
            switch (popupPosition) {
            case CENTERED:
                x = (getWidth() - width) / 2;
                y = (getHeight() - height) / 2;
                break;
            case CENTERED_LEFT:
                x = (getWidth() - width) / 4;
                y = (getHeight() - height) / 2;
                break;
            case CENTERED_RIGHT:
                x = ((getWidth() - width) * 3) / 4;
                y = (getHeight() - height) / 2;
                break;
            case ORIGIN:
                x = y = 0;
                break;
            }
        }
        if ((p = getClearSpace((Component)comp, x, y, MAXTRY)) != null
            && p.x >= 0 && p.x < getWidth()
            && p.y >= 0 && p.y < getHeight()) {
            x = p.x;
            y = p.y;
        }
        return new Point(x, y);
    }

    /**
     * Create key bindings for all actions.
     */
    private void createKeyBindings() {
        for (Option option : freeColClient.getActionManager().getOptions()) {
            FreeColAction action = (FreeColAction) option;
            getInputMap().put(action.getAccelerator(), action.getId());
            getActionMap().put(action.getId(), action);
        }
    }

    /**
     * Filters out and displays the EventPanel messages.
     *
     * @param messages The list of <code>ModelMessage</code> to filter.
     * @return The list of messages without any EventPanel messages.
     */
    private List<ModelMessage> filterEventPanels(ModelMessage[] messages) {
        final String eventMatch = "EventPanel.";
        List<ModelMessage> normal = new ArrayList<ModelMessage>();
        for (int i = 0; i < messages.length; i++) {
            String id = messages[i].getId();
            if (id.startsWith(eventMatch)) {
                id = id.substring(eventMatch.length());
                final EventType e = EventType.valueOf(id);
                SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            showEventPanel(e);
                        }
                    });
            } else {
                normal.add(messages[i]);
            }
        }
        return normal;
    }

    /**
     * Try to find some free space on the canvas for a component,
     * starting at x,y.
     *
     * @param comp The <code>Component</code> to place.
     * @param x A starting x coordinate.
     * @param y A starting y coordinate.
     * @return A <code>Point</code> to place the component at or null
     *     on failure.
     */
    private Point getClearSpace(Component comp, int x, int y, int tries) {
        if (!this.getBounds().contains(x, y)) return null;

        tries = 3 * tries + 1; // 3 new candidates per level
        List<Point> todo = new ArrayList<Point>();
        Point p = new Point(x, y);
        todo.add(p);
        while (!todo.isEmpty()) {
            p = todo.remove(0);
            Rectangle r = new Rectangle(p.x, p.y, 
                                        comp.getWidth(), comp.getHeight());
            Component c = null;
            for (Component child : this.getComponents()) {
                if (child.getBounds().intersects(r)) {
                    c = child;
                    break;
                }
            }
            if (c == null) {
                for (FreeColDialog<?> fcd : dialogs) {
                    if (fcd.getBounds().intersects(r)) {
                        c = fcd;
                        break;
                    }
                }
            }
            if (c == null) return p;
            if (--tries <= 0) break;

            int n = todo.size();
            todo.add(n, new Point(c.getX() + c.getWidth() + 1,
                                  c.getY() + c.getHeight() + 1));
            todo.add(n, new Point(x, c.getY() + c.getHeight() + 1));
            todo.add(n, new Point(c.getX() + c.getWidth() + 1, y));
        }
        return new Point(x, y);
    }

    /**
     * Gets any currently displayed colony panel for the specified colony.
     *
     * This is distinct from {@link #getExistingFreeColPanel} because
     * there can be multiple ColonyPanels.
     *
     * @param colony The <code>Colony</code> to check.
     * @return A currently displayed colony panel, or null if not found.
     */
    private ColonyPanel getColonyPanel(Colony colony) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame) c1).getContentPane()
                         .getComponents()) {
                    if (c2 instanceof ColonyPanel
                        && ((ColonyPanel)c2).getColony() == colony) {
                        return (ColonyPanel)c2;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Gets the internal frame for the given component.
     *
     * @param c The <code>Component</code>.
     * @return The given component if this is an internal frame or the
     *     first parent that is an internal frame.  Returns
     *     <code>null</code> if no internal frame is found.
     */
    private JInternalFrame getInternalFrame(final Component c) {
        Component temp = c;

        while (temp != null && !(temp instanceof JInternalFrame)) {
            temp = temp.getParent();
        }
        return (JInternalFrame) temp;
    }

    /**
     * Given a tile to be made visible, determine a position to popup
     * a panel.
     *
     * @param tile A <code>Tile</code> to be made visible.
     * @return A <code>PopupPosition</code> for a panel to be displayed.
     */
    private PopupPosition getPopupPosition(Tile tile) {
        if (tile == null) return PopupPosition.CENTERED;
        int where = mapViewer.setOffsetFocus(tile);
        return (where > 0) ? PopupPosition.CENTERED_LEFT
            : (where < 0) ? PopupPosition.CENTERED_RIGHT
            : PopupPosition.CENTERED;
    }

    /**
     * Gets the saved position of a component.
     *
     * @param comp The <code>Component</code> to use.
     * @return The saved position as a <code>Point</code>, or null if no
     *     saved position is found.
     */
    private Point getSavedPosition(Component comp) {
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return null;
        try {
            if (!co.getBoolean(ClientOptions.REMEMBER_PANEL_POSITIONS)) {
                return null;
            }
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        try {
            return new Point(co.getInteger(className + ".x"),
                             co.getInteger(className + ".y"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the saved size of a component.
     *
     * @param comp The <code>Component</code> to use.
     * @return A <code>Dimension</code> for the component or null if
     *     no saved size is found.
     */
    private Dimension getSavedSize(Component comp) {
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return null;
        try {
            if (!co.getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) {
                return null;
            }
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        try {
            return new Dimension(co.getInteger(className + ".w"),
                                 co.getInteger(className + ".h"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * A component is closing.  Some components need position and size
     * to be saved.
     *
     * @param c The closing <code>Component</code>.
     * @param frame The enclosing <code>JInternalFrame</code>.
     */
    private void notifyClose(Component c, JInternalFrame frame) {
        if (frame == null) return;

        if (c instanceof FreeColPanel) {
            FreeColPanel fcp = (FreeColPanel)c;
            fcp.firePropertyChange("closing", false, true);
            
            savePosition(fcp, frame.getLocation());
            saveSize(fcp, fcp.getSize());
        }
    }

    /**
     * Remove the panels derived from the EuropePanel.
     */
    private void removeEuropeanSubpanels() {
        FreeColPanel panel;
        if ((panel = getExistingFreeColPanel(RecruitPanel.class)) != null)
            removeFromCanvas(panel);
        if ((panel = getExistingFreeColPanel(PurchasePanel.class)) != null)
            removeFromCanvas(panel);
        if ((panel = getExistingFreeColPanel(TrainPanel.class)) != null)
            removeFromCanvas(panel);
    }

    /**
     * Save an <code>int</code> value to the saved ClientOptions,
     * using the name of the components class plus the given key as
     * and identifier.
     *
     * @param className The class name for the component.
     * @param key The key to save.
     * @param value The value to save.
     */
    private void saveInteger(String className, String key, int value) {
        if (freeColClient != null
            && freeColClient.getClientOptions() != null) {
            Option o = freeColClient.getClientOptions()
                .getOption(className + key);
            if (o == null) {
                Specification specification = (freeColClient.getGame() == null)
                    ? null : freeColClient.getGame().getSpecification();
                IntegerOption io = new IntegerOption(className + key,
                                                     specification);
                io.setValue(value);
                freeColClient.getClientOptions().add(io);
            } else if (o instanceof IntegerOption) {
                ((IntegerOption)o).setValue(value);
            }
        }
    }

    /**
     * Save the position of a component.
     *
     * @param comp The <code>Component</code> to use.
     * @param position The position to save.
     */
    private void savePosition(Component comp, Point position) {
        try {
            if (!freeColClient.getClientOptions()
                .getBoolean(ClientOptions.REMEMBER_PANEL_POSITIONS)) return;
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        saveInteger(className, ".x", position.x);
        saveInteger(className, ".y", position.y);
    }

    /**
     * Save the size of a component.
     *
     * @param comp The <code>Component</code> to use.
     * @param size The <code>Dimension</code> to save.
     */
    private void saveSize(Component comp, Dimension size) {
        try {
            if (!freeColClient.getClientOptions()
                .getBoolean(ClientOptions.REMEMBER_PANEL_SIZES)) return;
        } catch (Exception e) {}

        String className = comp.getClass().getName();
        saveInteger(className, ".w", size.width);
        saveInteger(className, ".h", size.height);
    }

    /**
     * Displays the given dialog, making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile A <code>Tile</code> to make visible (not under the dialog!)
     * @param resizable Should the dialog be resizable?
     * @return The {@link FreeColOldDialog#getResponse reponse} returned by
     *         the dialog.
     */
    private <T> T showFreeColOldDialog(FreeColOldDialog<T> freeColDialog,
                                       Tile tile, boolean resizable) {
        showFreeColPanel(freeColDialog, tile, resizable);
        T response = freeColDialog.getResponse();
        remove(freeColDialog);
        return response;
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @return The {@link FreeColDialog#getResponse reponse} returned by
     *     the dialog.
     */
    private <T> T showFreeColDialog(FreeColDialog<T> freeColDialog,
                                    Tile tile) {
        viewFreeColDialog(freeColDialog, tile);
        T response = freeColDialog.getResponse();
        remove(freeColDialog);
        dialogRemove(freeColDialog);
        return response;
    }

    /**
     * Displays the given panel, making sure a tile is visible.
     *
     * @param panel The panel to be displayed
     * @param tile A <code>Tile</code> to make visible (not under the panel!)
     * @param resizable Should the panel be resizable?
     */
    private void showFreeColPanel(FreeColPanel panel, Tile tile,
                                  boolean resizable) {
        showSubPanel(panel, getPopupPosition(tile), resizable);
    }

    /**
     * Displays a <code>FreeColPanel</code>.
     *
     * @param panel <code>FreeColPanel</code>, panel to show
     * @param resizable Should the panel be resizable?
     */
    private void showSubPanel(FreeColPanel panel, boolean resizable) {
        showSubPanel(panel, PopupPosition.CENTERED, resizable);
    }

    /**
     * Displays a <code>FreeColPanel</code> at a generalized position.
     *
     * @param panel <code>FreeColPanel</code>, panel to show
     * @param popupPosition <code>PopupPosition</code> The generalized
     *     position to place the panel.
     * @param resizable Should the panel be resizable?
     */
    private void showSubPanel(FreeColPanel panel, PopupPosition popupPosition,
                              boolean resizable) {
        repaint();
        addAsFrame(panel, false, popupPosition, resizable);
        panel.requestFocus();
    }


    // Public API

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The <code>Component</code> to add.
     * @return The component argument.
     */
    @Override
    public Component add(Component comp) {
        add(comp, null);
        return comp;
    }

    /**
     * Adds a component to this Canvas.  Removes the statusPanel if
     * visible (and <code>comp != statusPanel</code>).
     *
     * @param comp The <code>Component</code> to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void add(Component comp, Integer i) {
        addToCanvas(comp, i);
        gui.updateMenuBar();
        freeColClient.updateActions();
    }

    /**
     * Closes all the menus that are currently open.
     */
    public void closeMenus() {
        for (JInternalFrame frame : getAllFrames()) {
            for (Component c : frame.getContentPane().getComponents()) {
                notifyClose(c, frame);
            }
            frame.dispose();
        }
        while (!dialogs.isEmpty()) {
            FreeColDialog<?> dialog = dialogs.remove(0);
            dialog.dispose();
        }
    }

    /**
     * Closes the {@link MainPanel}.
     */
    public void closeMainPanel() {
       if (mainPanel != null) {
          remove(mainPanel);
          mainPanel = null;
       }
    }

    /**
     * Closes the <code>StatusPanel</code>.
     *
     * @see #showStatusPanel
     */
    public void closeStatusPanel() {
        if (statusPanel.isVisible()) {
            remove(statusPanel);
        }
    }

    /**
     * Checks if this <code>Canvas</code> contains any in game components.
     *
     * @return <code>true</code> if there is any in game components.
     */
    public boolean containsInGameComponents() {
        KeyListener[] keyListeners = getKeyListeners();
        if (keyListeners.length > 0) {
            return true;
        }

        MouseListener[] mouseListeners = getMouseListeners();
        if (mouseListeners.length > 0) {
            return true;
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        if (mouseMotionListeners.length > 0) {
            return true;
        }

        return false;
    }

    /**
     * Add a dialog to the current dialog list.
     *
     * @param fcd The dialog to add.
     */
    public void dialogAdd(FreeColDialog<?> fcd) {
        dialogs.add(fcd);
    }

    /**
     * Remove a dialog from the current dialog list.
     *
     * @param fcd The dialog to remove.
     */
    public void dialogRemove(FreeColDialog<?> fcd) {
        dialogs.remove(fcd);
    }

    /**
     * Gets a currently displayed FreeColPanel of a given type.
     *
     * @param type The type of <code>FreeColPanel</code> to look for.
     * @return A currently displayed <code>FreeColPanel</code> of the
     *     requested type, or null if none found.
     */
    public <T extends FreeColPanel> T getExistingFreeColPanel(Class<T> type) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame)c1).getContentPane()
                         .getComponents()) {
                    try {
                        T ret = type.cast(c2);
                        if (ret != null) {
                            final JInternalFrame jif = (JInternalFrame)c1;
                            SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        jif.toFront();
                                        jif.repaint();
                                    }
                                });
                            return ret;
                        }
                    } catch (ClassCastException cce) {}
                }
            }
        }
        return null;
    }

    /**
     * Gets the <code>LoadingSavegameDialog</code>.
     *
     * @return The <code>LoadingSavegameDialog</code>.
     */
    public LoadingSavegameDialog getLoadingSavegameDialog() {
        return loadingSavegameDialog;
    }

    /**
     * Get any panel this <code>Canvas</code> is displaying.
     *
     * @return A <code>Component</code> the <code>Canvas</code> is
     *         displaying, or null if none found.
     */
    public Component getShowingSubPanel() {
        for (Component c : getComponents()) {
            if (c instanceof ToolBoxFrame) {
                continue;
            }
            if (c instanceof JInternalFrame) {
                return c;
            } else if (c instanceof JInternalFrame.JDesktopIcon) {
                return c;
            }
        }
        return null;
    }

    /**
     * Checks if a client options dialog is present.
     *
     * @return True if the client options are showing.
     */
    public boolean isClientOptionsDialogShowing() {
        return clientOptionsDialogShowing;
    }

    /**
     * Checks if mapboard actions should be enabled.
     *
     * @return <code>true</code> if no internal frames are open.
     */
    public boolean isMapboardActionsEnabled() {
        return !isShowingSubPanel();
    }

    /**
     * Checks if this <code>Canvas</code> displaying another panel.
     * <p>
     * Note that the previous implementation could throw exceptions
     * in some cases, thus the change.
     *
     * @return <code>true</code> if the <code>Canvas</code> is displaying an
     *         internal frame.
     */
    public boolean isShowingSubPanel() {
        return getShowingSubPanel() != null;
    }

    /**
     * Refresh this canvas.
     */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Removes the given component from this canvas.
     *
     * @param comp The <code>Component</code> to remove.
     */
    public void removeFromCanvas(Component comp) {
        if (comp == null) return;

        final Rectangle updateBounds = comp.getBounds();
        final JInternalFrame frame = getInternalFrame(comp);
        notifyClose(comp, frame);
        if (frame != null && frame != comp) {
            frame.dispose();
        } else {
            // Java 1.7.0 as seen on Fedora with:
            //   Java version: 1.7.0_40
            //   Java WM version: 24.0-b56
            // crashes here deep in the java libraries.
            try {
                super.remove(comp);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Java crash", e);
            }
        }
        repaint(updateBounds.x, updateBounds.y,
                updateBounds.width, updateBounds.height);
    }

    /**
     * Removes components that is only used when in game.
     */
    public void removeInGameComponents() {
        // remove listeners, they will be added when launching the new game...
        KeyListener[] keyListeners = getKeyListeners();
        for (int i = 0; i < keyListeners.length; ++i) {
            removeKeyListener(keyListeners[i]);
        }

        MouseListener[] mouseListeners = getMouseListeners();
        for (int i = 0; i < mouseListeners.length; ++i) {
            removeMouseListener(mouseListeners[i]);
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        for (int i = 0; i < mouseMotionListeners.length; ++i) {
            removeMouseMotionListener(mouseMotionListeners[i]);
        }

        for (Component c : getComponents()) {
            removeFromCanvas(c);
        }
    }

    /**
     * Set preferred size to saved size, or to the given
     * <code>Dimension</code> if no saved size was found. Call this
     * method in the constructor of a FreeColPanel in order to
     * remember its size and position.
     *
     * @param comp The <code>Component</code> to use.
     * @param d The <code>Dimension</code> to restore from.
     */
    public void restoreSavedSize(Component comp, Dimension d) {
        Dimension size = getSavedSize(comp);
        if (size == null) {
            size = d;
            saveSize(comp, size);
        }
        if (!comp.getPreferredSize().equals(size)) {
            comp.setPreferredSize(size);
        }
    }

    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    public void returnToTitle() {
        // TODO: check if the GUI object knows that we're not
        // inGame. (Retrieve value of GUI::inGame.)  If GUI thinks
        // we're still in the game then log an error because at this
        // point the GUI should have been informed.
        closeMenus();
        removeInGameComponents();

        showMainPanel(null);
        gui.playSound("sound.intro.general");
        repaint();
    }

    public void setupMouseListeners(MapViewer mapViewer) {
        addMouseListener(new CanvasMouseListener(freeColClient, this,
                                                 mapViewer));
        addMouseMotionListener(new CanvasMouseMotionListener(freeColClient));
    }

    /**
     * Updates the sizes of the components on this Canvas.
     */
    public void updateSizes() {
        if (oldSize == null) {
            oldSize = getSize();
        }
        if (oldSize.width != getWidth() || oldSize.height != getHeight()) {
            gui.updateMapControlsInCanvas();
            mapViewer.setSize(getSize());
            mapViewer.forceReposition();
            oldSize = getSize();
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void paintComponent(Graphics g) {
        updateSizes();
        Graphics2D g2d = (Graphics2D) g;
        mapViewer.display(g2d);
    }


    // Override Container

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Component comp) {
        removeFromCanvas(comp);
        final boolean takeFocus = (comp != statusPanel);
        gui.updateMenuBar();
        freeColClient.updateActions();
        if (takeFocus && !isShowingSubPanel()) {
            requestFocus();
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(640, 480);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getPreferredSize() {
        return initialSize;
    }


    // Special handling for the startGamePanel.

    /**
     * Display a chat message in the start panel.
     *
     * @param sender The sending player name.
     * @param message The message to send.
     * @param privateChat Should the message be private?
     */
    public void displayChat(String sender, String message, boolean privateChat) {
        startGamePanel.displayChat(sender, message, privateChat);

    }

    /**
     * Refresh the player's table (called when a new player is added
     * from PreGameInputHandler.addPlayer).
     */
    public void refreshPlayersTable() {
        startGamePanel.refreshPlayersTable();
    }

    /**
     * Update the game options in the start panel.
     */
    public void updateGameOptions() {
        startGamePanel.updateGameOptions();
    }

    /**
     * Update the map generator options in the start panel.
     */
    public void updateMapGeneratorOptions() {
        startGamePanel.updateMapGeneratorOptions();
    }


    // Dialog display

    /**
     * Displays a dialog with text and a choice of options.
     *
     * @param modal True if this dialog should be modal.
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices The <code>List</code> containing the ChoiceItems to
     *            create buttons for.
     * @return The corresponding member of the values array to the selected
     *     option.
     */
    public <T> T showChoiceDialog(boolean modal, Tile tile, Object obj,
                                  ImageIcon icon, String cancelKey,
                                  List<ChoiceItem<T>> choices) {
        FreeColChoiceDialog<T> fcd
            = new FreeColChoiceDialog<T>(freeColClient, modal, obj, icon,
                                         cancelKey, choices);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a dialog with a text and a cancel-button, in addition
     * to buttons for each of the objects returned for the given list.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param text The text that explains the choice for the user.
     * @param cancelText The text displayed on the "cancel"-button.
     * @param choices The <code>List</code> containing the ChoiceItems to
     *            create buttons for.
     * @return The chosen object, or <i>null</i> for the cancel-button.
     */
    public <T> T showOldChoiceDialog(Tile tile, String text, String cancelText,
                                     List<ChoiceItem<T>> choices) {
        FreeColOldDialog<ChoiceItem<T>> choiceDialog
            = FreeColOldDialog.createChoiceDialog(freeColClient, text,
                                                  cancelText, choices);
        if (choiceDialog.getHeight() > getHeight() / 3) {
            choiceDialog.setSize(choiceDialog.getWidth(), (getHeight() * 2)/3);
        }
        ChoiceItem<T> response = showFreeColOldDialog(choiceDialog, tile, true);
        return (response == null) ? null : response.getObject();
    }

    /**
     * Displays a dialog with a text and a cancel-button, in addition
     * to buttons for each of the objects in the list.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param textKey A key for text that explains the choice for the user.
     * @param cancelKey A key for the text displayed on the "cancel"-button.
     * @param objects The List containing the objects to create buttons for.
     * @return The chosen object, or <i>null</i> for the cancel-button.
     */
    public <T> T showSimpleChoiceDialog(Tile tile, String textKey,
                                        String cancelKey, List<T> objects) {
        List<ChoiceItem<T>> choices = new ArrayList<ChoiceItem<T>>();
        for (T object : objects) {
            choices.add(new ChoiceItem<T>(object));
        }
        return showOldChoiceDialog(tile,
                                   Messages.message(textKey),
                                   Messages.message(cancelKey),
                                   choices);
    }

    /**
     * Displays a dialog with a text and a ok/cancel option.
     *
     * @param modal True if this dialog should be modal.
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     * @return True if the user clicked the "ok"-button.
     */
    public boolean showConfirmDialog(boolean modal, Tile tile,
                                     Object obj, ImageIcon icon,
                                     String okKey, String cancelKey) {
        FreeColConfirmDialog fcd
            = new FreeColConfirmDialog(freeColClient, modal, obj, icon,
                                       okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a dialog with a text and a ok/cancel option.
     *
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     * @param text The text that explains the choice for the user (should
     *     have been i18n-expanded in GUI).
     * @param icon An optional icon to display.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the "cancel"-button.
     * @return <i>true</i> if the user clicked the "ok"-button and <i>false</i>
     *     otherwise.
     * @see FreeColOldDialog
     */
    public boolean showOldConfirmDialog(Tile tile, String text, ImageIcon icon,
                                        String okText, String cancelText) {
        FreeColOldDialog<Boolean> dialog
            = FreeColOldDialog.createConfirmDialog(freeColClient, text, icon,
                Messages.message(okText),
                Messages.message(cancelText));
        return showFreeColOldDialog(dialog, tile, true);
    }

    /**
     * Displays a dialog with a text field and a ok/cancel option.
     *
     * @param modal True if this dialog should be modal.
     * @param tile An optional tile to make visible (not under the dialog).
     * @param template A <code>StringTemplate</code> that explains the
     *     action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     * @return The text the user entered, or null if cancelled.
     */
    public String showInputDialog(boolean modal, Tile tile,
                                  StringTemplate template, String defaultValue,
                                  String okKey, String cancelKey) {
        FreeColStringInputDialog fcd
            = new FreeColStringInputDialog(freeColClient, modal,
                                           Messages.message(template),
                                           defaultValue, okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a dialog with a text field and a ok/cancel option.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param text The text that explains the action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okText The text displayed on the "ok"-button.
     * @param cancelText The text displayed on the
     *     "cancel"-button. Use <i>null</i> to disable the cancel-option.
     * @return The text the user have entered or <i>null</i> if the user chose
     *     to cancel the action.
     * @see FreeColOldDialog
     */
    public String showOldInputDialog(Tile tile, StringTemplate text,
                                     String defaultValue,
                                     String okText, String cancelText) {
        FreeColOldDialog<String> inputDialog
            = FreeColOldDialog.createInputDialog(freeColClient,
                Messages.message(text), defaultValue,
                Messages.message(okText),
                (cancelText == null) ? null : Messages.message(cancelText));
        return showFreeColOldDialog(inputDialog, tile, true);
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional <code>Tile</code> to make visible (not
     *     under the dialog!)
     */
    public <T> void viewFreeColDialog(final FreeColDialog<T> freeColDialog,
                                      Tile tile) {
        freeColDialog.setLocation(chooseLocation(freeColDialog,
                freeColDialog.getWidth(), freeColDialog.getHeight(),
                getPopupPosition(tile)));
        dialogAdd(freeColDialog);
        freeColDialog.setVisible(true);
    }


    // Simple front ends to display each panel or dialog.

    /**
     * Display the AboutPanel.
     */
    public void showAboutPanel() {
        showSubPanel(new AboutPanel(freeColClient), false);
    }

    /**
     * Displays a dialog that asks the user what he wants to do with
     * his armed unit in a native settlement.
     *
     * @param settlement The <code>IndianSettlement</code> to consider.
     * @return The chosen action, tribute, attack or cancel.
     */
    public ScoutIndianSettlementAction
        showArmedUnitIndianSettlementDialog(IndianSettlement settlement) {
        final Player player = freeColClient.getMyPlayer();

        StringTemplate nation = settlement.getOwner().getNationName();
        JTextArea text = GUI.getDefaultTextArea(Messages.message(StringTemplate
                .template(settlement.getAlarmLevelMessageId(player))
                    .addStringTemplate("%nation%", nation)));

        List<ChoiceItem<ScoutIndianSettlementAction>> choices
            = new ArrayList<ChoiceItem<ScoutIndianSettlementAction>>();
        choices.add(new ChoiceItem<ScoutIndianSettlementAction>(
                Messages.message("scoutSettlement.tribute"),
                ScoutIndianSettlementAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<ScoutIndianSettlementAction>(
                Messages.message("scoutSettlement.attack"),
                ScoutIndianSettlementAction.INDIAN_SETTLEMENT_ATTACK));

        return showChoiceDialog(true, settlement.getTile(), text,
                                gui.getImageIcon(settlement, false),
                                "cancel", choices);
    }

    /**
     * Displays a dialog that asks the user whether to pay arrears for
     * boycotted goods or to dump them instead.
     *
     * @param goods The <code>Goods</code> to possibly dump.
     * @param europe The player <code>Europe</code> where the boycott
     *     is in force.
     * @return The chosen <code>BoycottAction</code>.
     */
    public BoycottAction showBoycottedGoodsDialog(Goods goods, Europe europe) {
        int arrears = europe.getOwner().getArrears(goods.getType());
        List<ChoiceItem<BoycottAction>> choices
            = new ArrayList<ChoiceItem<BoycottAction>>();
        choices.add(new ChoiceItem<BoycottAction>(
                Messages.message("boycottedGoods.payArrears"),
                BoycottAction.PAY_ARREARS));
        choices.add(new ChoiceItem<BoycottAction>(
                Messages.message("boycottedGoods.dumpGoods"),
                BoycottAction.DUMP_CARGO));
        BoycottAction result = showOldChoiceDialog(null,
            Messages.message(StringTemplate.template("boycottedGoods.text")
                .add("%goods%", goods.getNameKey())
                .add("%europe%", europe.getNameKey())
                .addAmount("%amount%", arrears)),
            Messages.message("cancel"),
            choices);
        return (result == null) ? BoycottAction.CANCEL : result;
    }

    /**
     * Show the BuildQueuePanel for a given colony.
     *
     * @param colony The <code>Colony</code> to show the build queue of.
     */
    public void showBuildQueuePanel(Colony colony) {
        showSubPanel(new BuildQueuePanel(freeColClient, colony), true);
    }

    /**
     * Show a build queue panel, with a special callback when it is closed.
     *
     * @param colony The <code>Colony</code> to show the build queue of.
     * @param callBack The <code>Runnable</code> that is run when the
     *     panel closes.
     */
    public void showBuildQueuePanel(Colony colony, Runnable callBack) {
        FreeColPanel panel = new BuildQueuePanel(freeColClient, colony);
        panel.addClosingCallback(callBack);
        showSubPanel(panel, true);
    }

    /**
     * Displays the panel for negotiating a purchase from a settlement.
     *
     * @param unit The <code>Unit</code> that is buying.
     * @param settlement The <code>Settlement</code> to buy from.
     * @param goods The <code>Goods</code> to buy.
     * @param gold The current negotiated price.
     * @param canBuy True if buy is a valid option.
     * @return The chosen action, buy, haggle, or cancel.
     */
    public BuyAction showBuyDialog(Unit unit, Settlement settlement,
                                   Goods goods, int gold, boolean canBuy) {
        StringTemplate goodsTemplate
            = StringTemplate.template("model.goods.goodsAmount")
            .add("%goods%", goods.getType().getNameKey())
            .addAmount("%amount%", goods.getAmount());
        StringTemplate nation = settlement.getOwner().getNationName();
        List<ChoiceItem<BuyAction>> choices
            = new ArrayList<ChoiceItem<BuyAction>>();
        choices.add(new ChoiceItem<BuyAction>(
                Messages.message("buy.takeOffer"),
                BuyAction.BUY, canBuy));
        choices.add(new ChoiceItem<BuyAction>(
                Messages.message("buy.moreGold"),
                BuyAction.HAGGLE));
        BuyAction result = showOldChoiceDialog(unit.getTile(),
                Messages.message(StringTemplate.template("buy.text")
                                 .addStringTemplate("%nation%", nation)
                                 .addStringTemplate("%goods%", goodsTemplate)
                                 .addAmount("%gold%", gold)),
                Messages.message("buyProposition.cancel"),
                choices);
        return (result == null) ? BuyAction.CANCEL : result;
    }

    /**
     * Display the <code>CaptureGoodsDialog</code>.
     *
     * @param unit The <code>Unit</code> capturing goods.
     * @param gl The list of <code>Goods</code> to choose from.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showCaptureGoodsDialog(Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<List<Goods>>(
                new CaptureGoodsDialog(freeColClient, unit, gl),
                null, handler));
    }

    /**
     * Displays the <code>ChatPanel</code>.
     *
     * @see ChatPanel
     */
    public void showChatPanel() {
        // TODO: does it have state, or can we create a new one?
        if (freeColClient.isSinglePlayer()) {
            return; // In single player, no chat available
        }
        showSubPanel(chatPanel, true);
    }

    /**
     * Displays the <code>ChooseFoundingFatherDialog</code>.
     *
     * @param ffs The <code>FoundingFather</code>s to choose from.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showChooseFoundingFatherDialog(List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<FoundingFather>(
                new ChooseFoundingFatherDialog(freeColClient, ffs),
                null, handler));
    }

    /**
     * Display the panel for claiming land.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param player The <code>Player</code> that is claiming.
     * @param price An asking price, if any.
     * @param owner The <code>Player</code> that owns the land.
     * @param canAccept True if accept is a valid option.
     * @return The chosen action, accept, steal or cancel.
     */
    public ClaimAction showClaimDialog(Tile tile, Player player, int price,
                                       Player owner, boolean canAccept) {
        List<ChoiceItem<ClaimAction>> choices
            = new ArrayList<ChoiceItem<ClaimAction>>();
        choices.add(new ChoiceItem<ClaimAction>(Messages.message(StringTemplate.template("indianLand.pay")
                    .addAmount("%amount%", price)),
                ClaimAction.ACCEPT, canAccept));
        choices.add(new ChoiceItem<ClaimAction>(Messages.message("indianLand.take"),
                ClaimAction.STEAL));
        ClaimAction result = showOldChoiceDialog(tile,
            Messages.message(StringTemplate.template("indianLand.text")
                .addStringTemplate("%player%", owner.getNationName())),
            Messages.message("indianLand.cancel"), choices);
        return (result == null) ? ClaimAction.CANCEL : result;
    }

    /**
     * Displays a dialog for setting client options.
     *
     * @return The modified <code>OptionGroup</code>, or null if not modified.
     */
    public OptionGroup showClientOptionsDialog() {
        ClientOptionsDialog dialog = new ClientOptionsDialog(freeColClient);
        //clientOptionsDialogShowing = true;
        OptionGroup group = showFreeColDialog(dialog, null);
        clientOptionsDialogShowing = false;
        if (group != null) {
            freeColClient.updateActions();
            gui.resetMenuBar();
            // Immediately redraw the minimap if that was updated.
            gui.updateMapControls();
        }
        return group;
    }

    /**
     * Displays the colony panel of the given <code>Colony</code>.
     * Defends against duplicates as this can duplicate messages
     * generated by multiple property change listeners registered
     * against the same colony.
     *
     * @param colony The colony whose panel needs to be displayed.
     * @return The colony panel.
     * @see ColonyPanel
     */
    public ColonyPanel showColonyPanel(Colony colony) {
        ColonyPanel panel = getColonyPanel(colony);
        if (panel == null) {
            panel = new ColonyPanel(freeColClient, colony);
            showFreeColPanel(panel, colony.getTile(), true);
        } else {
            panel.requestFocus();
        }
        return panel;
    }

    /**
     * Show the colony panel for a colony on a given tile.
     *
     * @param t The <code>Tile</code> to look for a colony on.
     * @return The colony panel, or null on failure.
     */
    public ColonyPanel showColonyPanel(Tile t) {
        if (gui.getCurrentViewMode() == GUI.MOVE_UNITS_MODE
            && t != null && t.getColony() != null
            && freeColClient.getMyPlayer().owns(t.getColony())) {
            mapViewer.setFocus(t);
            mapViewer.stopBlinking();
            return showColonyPanel(t.getColony());
        }
        return null;
    }

    /**
     * Show the colopedia entry for a given node.
     *
     * @param nodeId The node identifier to display.
     */
    public void showColopediaPanel(String nodeId) {
        showSubPanel(new ColopediaPanel(freeColClient, nodeId), false);
    }

    /**
     * Show the compat labour report.
     */
    public void showCompactLabourReport() {
        CompactLabourReport details = new CompactLabourReport(freeColClient);
        details.initialize();
        showSubPanel(details, false);

    }

    /**
     * Show the compat labour report for the specified unit data.
     *
     * @param unitData The <code>UnitData</code> to display.
     */
    public void showCompactLabourReport(UnitData unitData) {
        CompactLabourReport details = new CompactLabourReport(freeColClient,
                                                              unitData);
        details.initialize();
        showSubPanel(details, false);
    }

    /**
     * Display a dialog to confirm a declaration of independence.
     *
     * @return A list of names for a new nation.
     */
    public List<String> showConfirmDeclarationDialog() {
        return showFreeColDialog(new ConfirmDeclarationDialog(freeColClient),
                                 null);
    }

    /**
     * Display a panel showing the declaration of independence with
     * animated signature.
     */
    public void showDeclarationPanel() {
        showSubPanel(new DeclarationPanel(freeColClient),
                     PopupPosition.CENTERED, false);
    }

    /**
     * Display the difficulty dialog.
     *
     * @param editable Should this difficulty level be editable?
     */
    public void showDifficultyDialog(boolean editable) {
        Game game = freeColClient.getGame();
        Specification spec = game.getSpecification();
        showFreeColDialog(new DifficultyDialog(freeColClient, spec,
                                               spec.getDifficultyLevel(),
                                               editable),
                          null);
    }

    /**
     * Display the difficulty dialog for a given group.
     *
     * @param spec The enclosing <code>Specification</code>.
     * @param group The <code>OptionGroup</code> containing the difficulty.
     */
    public void showDifficultyDialog(Specification spec, OptionGroup group) {
        showFreeColDialog(new DifficultyDialog(freeColClient, spec, 
                                               group, group.isEditable()),
                          null);
    }

    /**
     * Displays the <code>DumpCargoDialog</code>.
     *
     * @param unit The <code>Unit</code> that is dumping.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showDumpCargoDialog(Unit unit,
                                    DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<List<Goods>>(
                new DumpCargoDialog(freeColClient, unit),
                unit.getTile(), handler));
    }

    /**
     * Display the EditOptionDialog.
     *
     * @param option The <code>Option</code> to edit.
     */
    public boolean showEditOptionDialog(Option option) {
        return showFreeColDialog(new EditOptionDialog(freeColClient, option),
                                 null);
    }

    /**
     * Display the EditSettlementDialog.
     *
     * @param settlement The <code>IndianSettlement</code> to edit.
     */
    public void showEditSettlementDialog(IndianSettlement settlement) {
        showFreeColDialog(new EditSettlementDialog(freeColClient, settlement),
                          null);
    }

    /**
     * Shows the panel that allows the user to choose which unit will emigrate
     * from Europe.  This method may only be called if the user has William
     * Brewster in congress.
     *
     * @param player The <code>Player</code> whose unit is emigrating.
     * @param fountainOfYouth Is this dialog displayed as a result of a
     *     fountain of youth.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showEmigrationDialog(Player player, boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<Integer>(
                new EmigrationDialog(freeColClient, player.getEurope(),
                                     fountainOfYouth),
                null, handler));
    }

    /**
     * Display the EndTurnDialog with given units that could still move.
     *
     * @param units A list of <code>Unit</code>s that could still move.
     */
    public boolean showEndTurnDialog(List<Unit> units) {
        return showFreeColOldDialog(new EndTurnDialog(freeColClient, units),
                                 null, true);
    }

    /**
     * Displays an error message.
     *
     * @param messageId The i18n-keyname of the error message to display.
     */
    public void showErrorMessage(String messageId) {
        showErrorMessage(messageId, "Unspecified error: " + messageId);
    }

    /**
     * Displays an error message.
     *
     * @param messageId The i18n-keyname of the error message to display.
     * @param message An alternative (possibly non-i18n) message to
     *     display if the resource specified by <code>messageId</code>
     *     is unavailable.
     */
    public void showErrorMessage(String messageId, String message) {
        String display = null;
        if (messageId != null) {
            display = Messages.message(messageId);
        }
        if (display == null || "".equals(display)) display = message;
        ErrorPanel errorPanel = new ErrorPanel(freeColClient, display);
        showSubPanel(errorPanel, true);
    }

    /**
     * Displays the <code>EuropePanel</code>.
     *
     * @see EuropePanel
     */
    public void showEuropePanel() {
        if (freeColClient.getGame() == null) {
            showErrorMessage("europe.noGame");
        } else {
            EuropePanel panel = getExistingFreeColPanel(EuropePanel.class);
            if (panel == null) {
                panel = new EuropePanel(freeColClient, this);
                panel.addClosingCallback(new Runnable() {
                        public void run() {
                            removeEuropeanSubpanels();
                        }
                    });
            }
            showSubPanel(panel, false);
        }
    }

    /**
     * Display an event panel.
     *
     * @param type The <code>EventType</code>.
     */
    public void showEventPanel(EventType type) {
        showSubPanel(new EventPanel(freeColClient, type),
                     PopupPosition.CENTERED, false);
    }

    /**
     * Display the FindSettlementPanel.
     */
    public void showFindSettlementPanel() {
        showSubPanel(new FindSettlementPanel(freeColClient),
                     PopupPosition.ORIGIN, true);
    }

    /**
     * Detailed view of a foreign colony when in debug mode.
     *
     * @param settlement The <code>Settlement</code> with the colony
     */
    public void showForeignColony(Settlement settlement) {
        if (settlement instanceof Colony) {
            Colony colony = freeColClient.getFreeColServer().getGame()
                .getFreeColGameObject(settlement.getId(), Colony.class);
            showColonyPanel(colony);
        }
    }

    /**
     * Display the GameOptionsDialog.
     *
     * @param editable Should the game options be editable?
     * @param loadCustomOptions
     */
    public void showGameOptionsDialog(boolean editable,
                                      boolean loadCustomOptions) {
        showFreeColDialog(new GameOptionsDialog(freeColClient, editable,
                                                loadCustomOptions),
                          null);
    }

    /**
     * Displays the high scores panel.
     *
     * @param messageId An optional message to add to the high scores panel.
     */
    public void showHighScoresPanel(String messageId) {
        showSubPanel(new ReportHighScoresPanel(freeColClient, messageId),
                     PopupPosition.ORIGIN, false);
    }

    /**
     * Displays the panel of the given native settlement.
     *
     * @param indianSettlement The <code>IndianSettlement</code> to display.
     */
    public void showIndianSettlementPanel(IndianSettlement indianSettlement) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(freeColClient, indianSettlement);
        showFreeColPanel(panel, indianSettlement.getTile(), true);
    }

    /**
     * Displays the panel for trading with an <code>IndianSettlement</code>.
     *
     * @param settlement The native settlement to trade with.
     * @param canBuy Show a "buy" option.
     * @param canSell Show a "sell" option.
     * @param canGift Show a "gift" option.
     * @return The chosen action, buy, sell, gift or cancel.
     */
    public TradeAction showIndianSettlementTradeDialog(Settlement settlement,
                                                       boolean canBuy,
                                                       boolean canSell,
                                                       boolean canGift) {
        ArrayList<ChoiceItem<TradeAction>> choices
            = new ArrayList<ChoiceItem<TradeAction>>();
        choices.add(new ChoiceItem<TradeAction>(
                Messages.message("tradeProposition.toBuy"),
                TradeAction.BUY, canBuy));
        choices.add(new ChoiceItem<TradeAction>(
                Messages.message("tradeProposition.toSell"),
                TradeAction.SELL, canSell));
        choices.add(new ChoiceItem<TradeAction>(
                Messages.message("tradeProposition.toGift"),
                TradeAction.GIFT, canGift));
        TradeAction result = showOldChoiceDialog(settlement.getTile(),
            Messages.message(StringTemplate.template("tradeProposition.welcome")
                .addStringTemplate("%nation%", settlement.getOwner().getNationName())
                .addName("%settlement%", settlement.getName())),
            Messages.message("tradeProposition.cancel"), choices);
        return (result == null) ? TradeAction.CANCEL : result;
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying an icon.
     * @param messageId The messageId of the message to display.
     */
    public void showInformationMessage(FreeColObject displayObject,
                                       String messageId) {
        showInformationMessage(displayObject, StringTemplate.key(messageId));
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying an icon.
     * @param template The <code>StringTemplate</code> to display.
     */
    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        String text = Messages.message(template);
        ImageIcon icon = (displayObject == null) ? null
            : gui.getImageIcon(displayObject, false);
        Tile tile = (displayObject instanceof Location)
            ? ((Location)displayObject).getTile()
            : null;

        // Plays an alert sound on each information message if the
        // option for it is turned on
        if (freeColClient.getClientOptions()
            .getBoolean(ClientOptions.AUDIO_ALERTS)) {
            gui.playSound("sound.event.alertSound");
        }

        showFreeColPanel(new InformationPanel(freeColClient, text, 
                                              displayObject, icon),
                         tile, true);
    }

    /**
     * Show an information message using a model message.
     *
     * @param message The <code>ModelMessage</code> to display.
     */
    public void showInformationMessage(ModelMessage message) {
        showInformationMessage(freeColClient.getGame()
            .getMessageDisplay(message), message);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param messageId The messageId of the message to display.
     */
    public void showInformationMessage(String messageId) {
        showInformationMessage(null, StringTemplate.key(messageId));
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param template The <code>StringTemplate</code> to display.
     */
    public void showInformationMessage(StringTemplate template) {
        showInformationMessage(null, template);
    }

    /**
     * Displays a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @return The selected <code>File</code>.
     */
    public File showLoadDialog(File directory) {
        if (fileFilters == null) {
            fileFilters = new FileFilter[] {
                FreeColFileFilter.getFSGFileFilter()
            };
        }
        return showFreeColDialog(new LoadDialog(freeColClient, directory,
                                                fileFilters),
                                 null);
    }

    /**
     * Displays a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param fileFilters The file filters which the user can select in the
     *     dialog.
     * @return The selected <code>File</code>.
     */
    public File showLoadDialog(File directory, FileFilter[] fileFilters) {
        File response = null;
        for (;;) {
            response = showFreeColDialog(new LoadDialog(freeColClient,
                                                        directory, fileFilters),
                                         null);
            if (response == null || response.isFile()) break;
            showErrorMessage("noSuchFile");
        }
        return response;
    }

    /**
     * Displays a dialog for setting options when loading a savegame.  The
     * settings can be retrieved directly from {@link LoadingSavegameDialog}
     * after calling this method.
     *
     * @param publicServer Default value.
     * @param singlePlayer Default value.
     * @return <code>true</code> if the "ok"-button was pressed and
     *         <code>false</code> otherwise.
     */
    public boolean showLoadingSavegameDialog(boolean publicServer,
                                             boolean singlePlayer) {
        loadingSavegameDialog.reset(publicServer, singlePlayer);
        return showFreeColDialog(loadingSavegameDialog, null);
    }

    /**
     * Show a panel containing the log file.
     */
    public void showLogFilePanel() {
        showSubPanel(new ErrorPanel(freeColClient), true);

    }

    /**
     * Shows the <code>MainPanel</code>.
     *
     * @param userMsg An option message key to show.
     * @see MainPanel
     */
    public void showMainPanel(String userMsg) {
        closeMenus();
        gui.getFrame().setJMenuBar(null);
        mainPanel = new MainPanel(freeColClient);
        addCentered(mainPanel, MAIN_LAYER);
        if (userMsg != null) showInformationMessage(userMsg);
        mainPanel.requestFocus();
    }

    /**
     * Display the map editor transform panel.
     */
    public void showMapEditorTransformPanel() {
        JInternalFrame f = addAsFrame(new MapEditorTransformPanel(freeColClient),
            true, PopupPosition.CENTERED, false);
        f.setLocation(f.getX(), 50);
        repaint();
    }

    /**
     * Display the map generator options dialog.
     *
     * @param mgo The <code>OptionGroup</code> containing the map
     *     generator options.
     * @param editable Should these options be editable.
     * @param loadCustomOptions Whether to load custom options.
     */
    public OptionGroup showMapGeneratorOptionsDialog(OptionGroup mgo,
        boolean editable, boolean loadCustomOptions) {
        return showFreeColDialog(new MapGeneratorOptionsDialog(freeColClient,
                mgo, editable, loadCustomOptions), null);
    }

    /**
     * Display the map size dialog.
     */
    public Dimension showMapSizeDialog() {
        return showFreeColDialog(new MapSizeDialog(freeColClient), null);
    }

    /**
     * Displays a number of ModelMessages.
     *
     * @param modelMessages The <code>ModelMessage</code>s to display.
     */
    public void showModelMessages(ModelMessage... modelMessages) {
        List<ModelMessage> messages = filterEventPanels(modelMessages);
        final int n = messages.size();
        if (n <= 0) return;
        final Game game = freeColClient.getGame();
        String[] texts = new String[n];
        FreeColObject[] fcos = new FreeColObject[n];
        ImageIcon[] icons = new ImageIcon[n];
        Tile tile = null;
        for (int i = 0; i < n; i++) {
            ModelMessage m = messages.get(i);
            texts[i] = Messages.message(m);
            fcos[i] = game.getMessageSource(m);
            icons[i] = gui.getImageIcon(game.getMessageDisplay(m), false);
            if (tile == null && fcos[i] instanceof Location) {
                tile = ((Location)fcos[i]).getTile();
            }
        }

        showFreeColPanel(new InformationPanel(freeColClient, texts, fcos,
                                              icons),
                         tile, true);
    }

    /**
     * Display the monarch dialog.
     *
     * @param action The <code>MonarchAction</code> underway.
     * @param template The <code>StringTemplate</code> describing the
     *     situation.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showMonarchDialog(MonarchAction action,
                                  StringTemplate template,
                                  DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<Boolean>(
                new MonarchDialog(freeColClient, action, template),
                null, handler));
    }

    /**
     * Display a dialog to set a new land name.
     *
     * @param key A key for the message to explain the dialog.
     * @param defaultName The default name for the new land.
     * @param unit The <code>Unit</code> discovering the new land.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showNameNewLandDialog(String key, String defaultName,
                                      Unit unit,
                                      DialogHandler<String> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<String>(
                new FreeColStringInputDialog(freeColClient, false,
                                             Messages.message(key),
                                             defaultName, "ok", null),
                unit.getTile(), handler));
    }

    /**
     * Display a dialog to set a new region name.
     *
     * @param template A <code>StringTemplate</code> for the message
     *     to explain the dialog.
     * @param defaultName The default name for the new region.
     * @param unit The <code>Unit</code> discovering the new region.
     * @param handler A <code>DialogHandler</code> for the dialog response.
     */
    public void showNameNewRegionDialog(StringTemplate template,
                                        String defaultName, Unit unit,
                                        DialogHandler<String> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<String>(
                new FreeColStringInputDialog(freeColClient, false,
                                             Messages.message(template),
                                             defaultName, "ok", null),
                unit.getTile(), handler));
    }

    /**
     * Displays the <code>NegotiationDialog</code>.
     *
     * @param unit The <code>Unit</code> that is negotiating.
     * @param settlement A <code>Settlement</code> that is negotiating.
     * @param agreement The current <code>DiplomaticTrade</code> agreement.
     * @return An updated agreement.
     * @see NegotiationDialog
     */
    public DiplomaticTrade showNegotiationDialog(Unit unit,
                                                 Settlement settlement,
                                                 DiplomaticTrade agreement) {
        NegotiationDialog negotiationDialog
            = new NegotiationDialog(freeColClient, unit, settlement, agreement);
        negotiationDialog.initialize();
        return showFreeColOldDialog(negotiationDialog, unit.getTile(), true);
    }

    /**
     * Display the NewPanel.
     */
    public void showNewPanel() {
        showSubPanel(new NewPanel(freeColClient), false);
    }

    /**
     * Display the NewPanel for a given specification.
     *
     * @param specification The <code>Specification</code> to use.
     */
    public void showNewPanel(Specification specification) {
        showSubPanel(new NewPanel(freeColClient, specification), false);
    }

    /**
     * Shows the <code>OpenGamePanel</code>.
     */
    public void showOpenGamePanel() {
        showErrorMessage("openGame.unimplemented");
    }

    /**
     * Shows the <code>VideoPanel</code>.
     *
     * @param userMsg An optional user message.
     */
    public void showOpeningVideoPanel(final String userMsg) {
        closeMenus();
        final Video video = ResourceManager.getVideo("Opening.video");
        boolean muteAudio = !gui.canPlaySound();
        final VideoComponent vp = new VideoComponent(video, muteAudio);
        addCentered(vp, MAIN_LAYER);
        vp.play();

        final class AbortListener implements KeyListener, MouseListener, 
            VideoListener {
            public void keyPressed(KeyEvent e) {}

            public void keyReleased(KeyEvent e) {
                execute();
            }

            public void keyTyped(KeyEvent e) {}

            public void mouseClicked(MouseEvent e) {
                execute();
            }

            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}

            public void stopped() {
                execute();
            }

            private void execute() {
                removeKeyListener(this);
                removeMouseListener(this);
                vp.removeMouseListener(this);
                vp.removeVideoListener(this);
                vp.stop();
                Canvas.this.remove(vp);
                gui.playSound("sound.intro.general");
                showMainPanel(userMsg);
            }
        }

        AbortListener l = new AbortListener();
        addMouseListener(l);
        addKeyListener(l);
        vp.addMouseListener(l);
        vp.addVideoListener(l);
    }

    /**
     * Display the parameters dialog.
     */
    public Parameters showParametersDialog() {
        return showFreeColDialog(new ParametersDialog(freeColClient),
                                 null);
    }

    /**
     * Display a dialog to confirm a combat.
     *
     * @param attacker The attacker.
     * @param defender The defender.
     * @param tile A <code>Tile</code> to make visible.
     * @return True if the combat is to proceed.
     */
    public boolean showPreCombatDialog(FreeColGameObject attacker,
                                       FreeColGameObject defender,
                                       Tile tile) {
        return showFreeColDialog(new PreCombatDialog(freeColClient,
                                                     attacker, defender),
                                 tile);
    }

    /**
     * Displays the purchase panel.
     */
    public void showPurchasePanel() {
        PurchasePanel panel = getExistingFreeColPanel(PurchasePanel.class);
        if (panel == null) {
            panel = new PurchasePanel(freeColClient);
        }
        showFreeColPanel(panel, null, false);
    }

    /**
     * Displays the recruit panel.
     */
    public void showRecruitPanel() {
        RecruitPanel panel = getExistingFreeColPanel(RecruitPanel.class);
        if (panel == null) {
            panel = new RecruitPanel(freeColClient);
        }
        showFreeColPanel(panel, null, false);
    }

    /**
     * Display the labour detail panel.
     *
     * @param unitType The <code>UnitType</code> to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player <code>Colony</code>s.
     */
    public void showReportLabourDetailPanel(UnitType unitType,
        Map<UnitType, Map<Location, Integer>> data,
        TypeCountMap<UnitType> unitCount, List<Colony> colonies) {
        ReportLabourDetailPanel details
            = new ReportLabourDetailPanel(freeColClient, unitType, data,
                                          unitCount, colonies);
        details.initialize();
        showSubPanel(details, true);
    }

    /**
     * Display the river style dialog.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     */
    public String showRiverStyleDialog(Tile tile) {
        return showFreeColDialog(new RiverStyleDialog(freeColClient), tile);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param defaultName Default filename for the savegame.
     * @return The selected <code>File</code>.
     */
    public File showSaveDialog(File directory, String defaultName) {
        if (fileFilters == null) {
            fileFilters = new FileFilter[] {
                FreeColFileFilter.getFSGFileFilter()
            };
        }
        return showSaveDialog(directory, fileFilters, defaultName,
                              FSG_EXTENSION);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param fileFilters The available file filters in the dialog.
     * @param defaultName Default filename for the savegame.
     * @param extension This extension will be added to the specified
     *     filename (if not added by the user).
     * @return The selected <code>File</code>.
     */
    public File showSaveDialog(File directory, FileFilter[] fileFilters,
                               String defaultName, String extension) {
        return showFreeColDialog(new SaveDialog(freeColClient, directory,
                                                fileFilters, defaultName,
                                                extension),
                                 null);
    }

    /**
     * Display the scale map size dialog.
     */
    public Dimension showScaleMapSizeDialog() {
        return showFreeColDialog(new ScaleMapSizeDialog(freeColClient),
                                 null);
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his scout
     * in the foreign colony.
     *
     * @param colony The <code>Colony</code> to be scouted.
     * @param unit The <code>Unit</code> that is scouting.
     * @param neg True if negotation is a valid choice.
     * @return The selected action, either negotiate, spy, attack or cancel.
     */
    public ScoutColonyAction showScoutForeignColonyDialog(Colony colony,
                                                          Unit unit,
                                                          boolean neg) {
        JTextArea text = GUI.getDefaultTextArea(Messages.message(StringTemplate
                .template("scoutColony.text")
                    .addStringTemplate("%unit%", unit.getFullLabel())
                    .addName("%colony%", colony.getName())));

        List<ChoiceItem<ScoutColonyAction>> choices
            = new ArrayList<ChoiceItem<ScoutColonyAction>>();
        choices.add(new ChoiceItem<ScoutColonyAction>(
                Messages.message("scoutColony.negotiate"),
                ScoutColonyAction.FOREIGN_COLONY_NEGOTIATE, neg));
        choices.add(new ChoiceItem<ScoutColonyAction>(
                Messages.message("scoutColony.spy"),
                ScoutColonyAction.FOREIGN_COLONY_SPY));
        choices.add(new ChoiceItem<ScoutColonyAction>(
                Messages.message("scoutColony.attack"),
                ScoutColonyAction.FOREIGN_COLONY_ATTACK));

        return showChoiceDialog(true, unit.getTile(), text,
                                gui.getImageIcon(colony, false),
                                "cancel", choices);
    }

    /**
     * Displays a dialog that asks the user what he wants to do with
     * his scout in a native settlement.
     *
     * @param settlement The <code>IndianSettlement</code> to be scouted.
     * @param number The number of settlements in the settlement owner nation.
     * @return The chosen action, speak, tribute, attack or cancel.
     */
    public ScoutIndianSettlementAction
        showScoutIndianSettlementDialog(IndianSettlement settlement,
                                        String number) {
        final Player player = freeColClient.getMyPlayer();
        final Player owner = settlement.getOwner();

        StringBuilder sb = new StringBuilder(400);
        sb.append(Messages.message(StringTemplate
                .template(settlement.getAlarmLevelMessageId(player))
                    .addStringTemplate("%nation%", owner.getNationName())));
        sb.append("\n\n");
        String key = ((IndianNationType)owner.getNationType())
            .getSettlementTypeKey(true);
        sb.append(Messages.message(StringTemplate
                .template("scoutSettlement.greetings")
                    .addStringTemplate("%nation%", owner.getNationName())
                    .addName("%settlement%", settlement.getName())
                    .add("%number%", number)
                    .add("%settlementType%", key)));
        sb.append(" ");
        if (settlement.getLearnableSkill() != null) {
            key = settlement.getLearnableSkill().getNameKey();
            sb.append(Messages.message(StringTemplate
                    .template("scoutSettlement.skill")
                        .add("%skill%", key)));
            sb.append(" ");
        }
        GoodsType[] wantedGoods = settlement.getWantedGoods();
        int present = 0;
        for (present = 0; present < wantedGoods.length; present++) {
            if (wantedGoods[present] == null) break;
        }
        if (present > 0) {
            StringTemplate t = StringTemplate.template("scoutSettlement.trade."
                + Integer.toString(present));
            for (int i = 0; i < present; i++) {
                t.add("%goods" + Integer.toString(i+1) + "%",
                    wantedGoods[i].getNameKey());
            }
            sb.append(Messages.message(t) + "\n\n");
        }
        JTextArea text = GUI.getDefaultTextArea(sb.toString());

        List<ChoiceItem<ScoutIndianSettlementAction>> choices
            = new ArrayList<ChoiceItem<ScoutIndianSettlementAction>>();
        choices.add(new ChoiceItem<ScoutIndianSettlementAction>(
                Messages.message("scoutSettlement.speak"),
                ScoutIndianSettlementAction.INDIAN_SETTLEMENT_SPEAK));
        choices.add(new ChoiceItem<ScoutIndianSettlementAction>(
                Messages.message("scoutSettlement.tribute"),
                ScoutIndianSettlementAction.INDIAN_SETTLEMENT_TRIBUTE));
        choices.add(new ChoiceItem<ScoutIndianSettlementAction>(
                Messages.message("scoutSettlement.attack"),
                ScoutIndianSettlementAction.INDIAN_SETTLEMENT_ATTACK));

        return showChoiceDialog(true, settlement.getTile(), text,
                                gui.getImageIcon(settlement, false),
                                "cancel", choices);
    }

    /**
     * Display the select-amount dialog.
     *
     * @param goodsType The <code>GoodsType</code> to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param needToPay If true, check the player has sufficient funds.
     * @return The amount selected.
     */
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        return showFreeColOldDialog(new SelectAmountDialog(freeColClient,
                goodsType, available, defaultAmount, needToPay), null, true);
    }

    /**
     * Display a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The <code>Unit</code> to select a destination for.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit) {
        return showFreeColDialog(new SelectDestinationDialog(freeColClient,
                unit), unit.getTile());
    }

    /**
     * Displays the panel for negotiating a sale to a settlement.
     *
     * @param unit The <code>Unit</code> that is selling.
     * @param settlement The <code>Settlement</code> to sell to.
     * @param goods The <code>Goods</code> to sell.
     * @param gold The current negotiated price.
     * @return The chosen action, sell, gift or haggle, or null.
     */
    public SellAction showSellDialog(Unit unit, Settlement settlement,
                                     Goods goods, int gold) {
        StringTemplate goodsTemplate
            = StringTemplate.template("model.goods.goodsAmount")
            .add("%goods%", goods.getType().getNameKey())
            .addAmount("%amount%", goods.getAmount());
        StringTemplate nation = settlement.getOwner().getNationName();
        List<ChoiceItem<SellAction>> choices
            = new ArrayList<ChoiceItem<SellAction>>();
        choices.add(new ChoiceItem<SellAction>(
                Messages.message("sell.takeOffer"),
                SellAction.SELL));
        choices.add(new ChoiceItem<SellAction>(
                Messages.message("sell.moreGold"),
                SellAction.HAGGLE));
        choices.add(new ChoiceItem<SellAction>(
                Messages.message(StringTemplate.template("sell.gift")
                                 .addStringTemplate("%goods%", goodsTemplate)),
                SellAction.GIFT));
        SellAction result = showOldChoiceDialog(unit.getTile(),
                Messages.message(StringTemplate.template("sell.text")
                                 .addStringTemplate("%nation%", nation)
                                 .addStringTemplate("%goods%", goodsTemplate)
                                 .addAmount("%gold%", gold)),
                Messages.message("sellProposition.cancel"),
                choices);
        return (result == null) ? SellAction.CANCEL : result;
    }

    /**
     * Displays the <code>ServerListPanel</code>.
     *
     * @param serverList The list containing the servers retrieved from the
     *            metaserver.
     * @see ServerListPanel
     */
    public void showServerListPanel(List<ServerInfo> serverList) {
        closeMenus();

        serverListPanel.initialize(serverList);
        showSubPanel(serverListPanel, true);
    }

    /**
     * Display the appropriate panel for a given settlement.
     *
     * @param settlement The <code>Settlement</code> to display.
     */
    public void showSettlement(Settlement settlement) {
        if (settlement instanceof Colony) {
            if (settlement.getOwner().equals(freeColClient.getMyPlayer())) {
                showColonyPanel((Colony)settlement);
            } else if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                showForeignColony(settlement);
            }
        } else if (settlement instanceof IndianSettlement) {
            showIndianSettlementPanel((IndianSettlement)settlement);
        } else {
            throw new IllegalStateException("Bogus settlement");
        }
    }

    /**
     * Displays the <code>StartGamePanel</code>.
     *
     * @param game The <code>Game</code> that is about to start.
     * @param player The <code>Player</code> using this client.
     * @param singlePlayerMode True to start a single player game.
     * @see StartGamePanel
     */
    public void showStartGamePanel(Game game, Player player,
                                   boolean singlePlayerMode) {
        if (game == null) {
            logger.warning("StartGamePanel requires game != null.");
        } else if (player == null) {
            logger.warning("StartGamePanel requires player != null.");
        } else {
            closeMenus();
            startGamePanel.initialize(singlePlayerMode);
            showSubPanel(startGamePanel, false);
        }
    }

    /**
     * Display the statistics panel.
     */
    public void showStatisticsPanel() {
        showSubPanel(new StatisticsPanel(freeColClient), true);
    }

    /**
     * Shows a status message that cannot be dismissed.  The panel
     * will be removed when another component is added to this
     * <code>Canvas</code>.  This includes all the
     * <code>showXXX</code>-methods. In addition,
     * {@link #closeStatusPanel()} also removes this panel.
     *
     * @param message The text message to display on the status panel.
     * @see StatusPanel
     */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, STATUS_LAYER);
    }

    /**
     * Display the tile panel for a given tile.
     *
     * @param tile The <code>Tile</code> to display.
     */
    public void showTilePanel(Tile tile) {
        showSubPanel(new TilePanel(freeColClient, tile), false);
    }

    /**
     * Shows a tile popup.
     *
     * @param tile The Tile where the popup occurred.
     * @param x The x-coordinate on the screen where the popup needs to be
     *            placed.
     * @param y The y-coordinate on the screen where the popup needs to be
     *            placed.
     * @see TilePopup
     */
    public void showTilePopup(Tile tile, int x, int y) {
        if (tile == null)
            return;
        TilePopup tp = new TilePopup(freeColClient, tile);
        if (tp.hasItem()) {
            tp.show(this, x, y);
            tp.repaint();
        } else if (tile.isExplored()) {
            showTilePanel(tile);
        }
    }

    /**
     * Display a dialog to select a trade route for a unit.
     *
     * @param unit The <code>Unit</code> to select a trade route for.
     * @return True if the trade route changed.
     */
    public boolean showTradeRouteDialog(Unit unit) {
        return showFreeColOldDialog(new TradeRouteDialog(freeColClient, unit),
                                 (unit == null) ? null : unit.getTile(), true);
    }

    /**
     * Display the trade route input dialog for a given trade route.
     *
     * @param newRoute The <code>TradeRoute</code> to display.
     * @return True if a new route was added.
     */
    public boolean showTradeRouteInputDialog(TradeRoute newRoute) {
        return showFreeColOldDialog(new TradeRouteInputDialog(freeColClient,
                newRoute), null, true);
    }

    /**
     * Displays the training panel.
     */
    public void showTrainPanel() {
        TrainPanel panel = getExistingFreeColPanel(TrainPanel.class);
        if (panel == null) {
            panel = new TrainPanel(freeColClient);
        }
        showFreeColPanel(panel, null, false);
    }

    /**
     * Displays a dialog that asks the user what he wants to do with his
     * missionary in the indian settlement.
     *
     * @param unit The <code>Unit</code> speaking to the settlement.
     * @param settlement The <code>IndianSettlement</code> being visited.
     * @param canEstablish Is establish a valid option.
     * @param canDenounce Is denounce a valid option.
     * @return The chosen action, establish mission, denounce, incite
     *         or cancel.
     */
    public MissionaryAction
        showUseMissionaryDialog(Unit unit, IndianSettlement settlement,
                                boolean canEstablish, boolean canDenounce) {
        String messageId = settlement.getAlarmLevelMessageId(unit.getOwner());
        StringBuilder sb = new StringBuilder(256);
        sb.append(Messages.message(StringTemplate.template(messageId)
                .addStringTemplate("%nation%",
                    settlement.getOwner().getNationName())));
        sb.append("\n\n");
        sb.append(Messages.message(StringTemplate
                .template("missionarySettlement.question")
                    .addName("%settlement%", settlement.getName())));
        JTextArea text = GUI.getDefaultTextArea(sb.toString());

        List<ChoiceItem<MissionaryAction>> choices
            = new ArrayList<ChoiceItem<MissionaryAction>>();
        choices.add(new ChoiceItem<MissionaryAction>(
                Messages.message("missionarySettlement.establish"),
                MissionaryAction.ESTABLISH_MISSION, canEstablish));
        choices.add(new ChoiceItem<MissionaryAction>(
                Messages.message("missionarySettlement.heresy"),
                MissionaryAction.DENOUNCE_HERESY, canDenounce));
        choices.add(new ChoiceItem<MissionaryAction>(
                Messages.message("missionarySettlement.incite"),
                MissionaryAction.INCITE_INDIANS));

        return showChoiceDialog(true, unit.getTile(), text,
                                gui.getImageIcon(settlement, false),
                                "cancel", choices);
    }

    /**
     * Display the victory panel.
     */
    public void showVictoryPanel() {
        showSubPanel(new VictoryPanel(freeColClient), true);
    }

    /**
     * Display the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The <code>Colony</code> to display.
     */
    public boolean showWarehouseDialog(Colony colony) {
        return showFreeColDialog(new WarehouseDialog(freeColClient, colony),
                                 null);
    }

    /**
     * Display the production of a unit.
     *
     * @param unit The <code>Unit</code> to display.
     */
    public void showWorkProductionPanel(Unit unit) {
        showSubPanel(new WorkProductionPanel(freeColClient, unit), true);
    }

    /**
     * Update all panels derived from the EuropePanel.
     */
    public void updateEuropeanSubpanels() {
        RecruitPanel rp
            = (RecruitPanel)getExistingFreeColPanel(RecruitPanel.class);
        if (rp != null) rp.update();
        PurchasePanel pp
            = (PurchasePanel)getExistingFreeColPanel(PurchasePanel.class);
        if (pp != null) pp.update();
        TrainPanel tp
            = (TrainPanel)getExistingFreeColPanel(TrainPanel.class);
        if (tp != null) tp.update();
    }

    // Singleton specialist reports

    public void showReportCargoPanel() {
        ReportCargoPanel r
            = getExistingFreeColPanel(ReportCargoPanel.class);
        if (r == null) {
            showSubPanel(new ReportCargoPanel(freeColClient), true);
        }
    }

    public void showReportColonyPanel() {
        ReportColonyPanel r
            = getExistingFreeColPanel(ReportColonyPanel.class);
        if (r == null) {
            showSubPanel(new ReportColonyPanel(freeColClient), true);
        }
    }

    public void showReportContinentalCongressPanel() {
        ReportContinentalCongressPanel
            r = getExistingFreeColPanel(ReportContinentalCongressPanel.class);
        if (r == null) {
            showSubPanel(new ReportContinentalCongressPanel(freeColClient),
                         true);
        }
    }

    public void showReportEducationPanel() {
        ReportEducationPanel r
            = getExistingFreeColPanel(ReportEducationPanel.class);
        if (r == null) {
            showSubPanel(new ReportEducationPanel(freeColClient), true);
        }
    }

    public void showReportExplorationPanel() {
        ReportExplorationPanel r
            = getExistingFreeColPanel(ReportExplorationPanel.class);
        if (r == null) {
            showSubPanel(new ReportExplorationPanel(freeColClient), true);
        }
    }

    public void showReportForeignAffairPanel() {
        ReportForeignAffairPanel r
            = getExistingFreeColPanel(ReportForeignAffairPanel.class);
        if (r == null) {
            showSubPanel(new ReportForeignAffairPanel(freeColClient), true);
        }
    }

    public void showReportHistoryPanel() {
        ReportHistoryPanel r
            = getExistingFreeColPanel(ReportHistoryPanel.class);
        if (r == null) {
            showSubPanel(new ReportHistoryPanel(freeColClient), true);
        }
    }

    public void showReportIndianPanel() {
        ReportIndianPanel r
            = getExistingFreeColPanel(ReportIndianPanel.class);
        if (r == null) {
            showSubPanel(new ReportIndianPanel(freeColClient), true);
        }
    }

    public void showReportLabourPanel() {
        ReportLabourPanel r
            = getExistingFreeColPanel(ReportLabourPanel.class);
        if (r == null) {
            showSubPanel(new ReportLabourPanel(freeColClient), true);
        }
    }

    public void showReportMilitaryPanel() {
        ReportMilitaryPanel r
            = getExistingFreeColPanel(ReportMilitaryPanel.class);
        if (r == null) {
            showSubPanel(new ReportMilitaryPanel(freeColClient), true);
        }
    }

    public void showReportNavalPanel() {
        ReportNavalPanel r
            = getExistingFreeColPanel(ReportNavalPanel.class);
        if (r == null) {
            showSubPanel(new ReportNavalPanel(freeColClient), true);
        }
    }

    public void showReportProductionPanel() {
        ReportProductionPanel r
            = getExistingFreeColPanel(ReportProductionPanel.class);
        if (r == null) {
            showSubPanel(new ReportProductionPanel(freeColClient), true);
        }
    }

    public void showReportReligiousPanel() {
        ReportReligiousPanel r
            = getExistingFreeColPanel(ReportReligiousPanel.class);
        if (r == null) {
            showSubPanel(new ReportReligiousPanel(freeColClient), true);
        }
    }

    public void showReportRequirementsPanel() {
        ReportRequirementsPanel r
            = getExistingFreeColPanel(ReportRequirementsPanel.class);
        if (r == null) {
            showSubPanel(new ReportRequirementsPanel(freeColClient), true);
        }
    }

    public void showReportTradePanel() {
        ReportTradePanel r
            = getExistingFreeColPanel(ReportTradePanel.class);
        if (r == null) {
            showSubPanel(new ReportTradePanel(freeColClient), true);
        }
    }

    /**
     * Show the turn report.
     *
     * @param messages The <code>ModelMessage</code>s to show.
     */
    public void showReportTurnPanel(ModelMessage... messages) {
        ReportTurnPanel r = getExistingFreeColPanel(ReportTurnPanel.class);
        if (r == null) {
            showSubPanel(new ReportTurnPanel(freeColClient, messages), true);
        } else {
            r.setMessages(messages);
        }
    }
}
