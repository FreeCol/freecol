/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicInternalFrameUI;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.panel.AboutPanel;
import net.sf.freecol.client.gui.panel.BuildQueuePanel;
import net.sf.freecol.client.gui.dialog.CaptureGoodsDialog;
import net.sf.freecol.client.gui.panel.ChatPanel;
import net.sf.freecol.client.gui.dialog.ChooseFoundingFatherDialog;
import net.sf.freecol.client.gui.dialog.ClientOptionsDialog;
import net.sf.freecol.client.gui.panel.ColonyPanel;
import net.sf.freecol.client.gui.panel.colopedia.ColopediaPanel;
import net.sf.freecol.client.gui.panel.ColorChooserPanel;
import net.sf.freecol.client.gui.panel.report.CompactLabourReport;
import net.sf.freecol.client.gui.dialog.ConfirmDeclarationDialog;
import net.sf.freecol.client.gui.panel.DeclarationPanel;
import net.sf.freecol.client.gui.dialog.DifficultyDialog;
import net.sf.freecol.client.gui.dialog.DumpCargoDialog;
import net.sf.freecol.client.gui.dialog.EditOptionDialog;
import net.sf.freecol.client.gui.dialog.EditSettlementDialog;
import net.sf.freecol.client.gui.dialog.EmigrationDialog;
import net.sf.freecol.client.gui.dialog.EndTurnDialog;
import net.sf.freecol.client.gui.panel.ErrorPanel;
import net.sf.freecol.client.gui.panel.EuropePanel;
import net.sf.freecol.client.gui.panel.EventPanel;
import net.sf.freecol.client.gui.panel.FindSettlementPanel;
import net.sf.freecol.client.gui.dialog.FirstContactDialog;
import net.sf.freecol.client.gui.dialog.FreeColChoiceDialog;
import net.sf.freecol.client.gui.dialog.FreeColConfirmDialog;
import net.sf.freecol.client.gui.dialog.FreeColDialog;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.client.gui.dialog.FreeColStringInputDialog;
import net.sf.freecol.client.gui.dialog.GameOptionsDialog;
import net.sf.freecol.client.gui.panel.IndianSettlementPanel;
import net.sf.freecol.client.gui.panel.InformationPanel;
import net.sf.freecol.client.gui.panel.report.LabourData.UnitData;
import net.sf.freecol.client.gui.dialog.LoadDialog;
import net.sf.freecol.client.gui.dialog.LoadingSavegameDialog;
import net.sf.freecol.client.gui.panel.MainPanel;
import net.sf.freecol.client.gui.panel.MapEditorTransformPanel;
import net.sf.freecol.client.gui.dialog.MapGeneratorOptionsDialog;
import net.sf.freecol.client.gui.dialog.MapSizeDialog;
import net.sf.freecol.client.gui.dialog.MonarchDialog;
import net.sf.freecol.client.gui.dialog.NativeDemandDialog;
import net.sf.freecol.client.gui.dialog.NegotiationDialog;
import net.sf.freecol.client.gui.panel.NewPanel;
import net.sf.freecol.client.gui.dialog.Parameters;
import net.sf.freecol.client.gui.dialog.ParametersDialog;
import net.sf.freecol.client.gui.dialog.PreCombatDialog;
import net.sf.freecol.client.gui.panel.PurchasePanel;
import net.sf.freecol.client.gui.panel.RecruitPanel;
import net.sf.freecol.client.gui.panel.report.ReportCargoPanel;
import net.sf.freecol.client.gui.panel.report.ReportClassicColonyPanel;
import net.sf.freecol.client.gui.panel.report.ReportCompactColonyPanel;
import net.sf.freecol.client.gui.panel.report.ReportContinentalCongressPanel;
import net.sf.freecol.client.gui.panel.report.ReportEducationPanel;
import net.sf.freecol.client.gui.panel.report.ReportExplorationPanel;
import net.sf.freecol.client.gui.panel.report.ReportForeignAffairPanel;
import net.sf.freecol.client.gui.panel.report.ReportHighScoresPanel;
import net.sf.freecol.client.gui.panel.report.ReportHistoryPanel;
import net.sf.freecol.client.gui.panel.report.ReportIndianPanel;
import net.sf.freecol.client.gui.panel.report.ReportLabourDetailPanel;
import net.sf.freecol.client.gui.panel.report.ReportLabourPanel;
import net.sf.freecol.client.gui.panel.report.ReportMilitaryPanel;
import net.sf.freecol.client.gui.panel.report.ReportNavalPanel;
import net.sf.freecol.client.gui.panel.report.ReportPanel;
import net.sf.freecol.client.gui.panel.report.ReportProductionPanel;
import net.sf.freecol.client.gui.panel.report.ReportReligiousPanel;
import net.sf.freecol.client.gui.panel.report.ReportRequirementsPanel;
import net.sf.freecol.client.gui.panel.report.ReportTradePanel;
import net.sf.freecol.client.gui.panel.report.ReportTurnPanel;
import net.sf.freecol.client.gui.dialog.RiverStyleDialog;
import net.sf.freecol.client.gui.dialog.SaveDialog;
import net.sf.freecol.client.gui.dialog.ScaleMapSizeDialog;
import net.sf.freecol.client.gui.dialog.SelectAmountDialog;
import net.sf.freecol.client.gui.dialog.SelectDestinationDialog;
import net.sf.freecol.client.gui.dialog.SelectTributeAmountDialog;
import net.sf.freecol.client.gui.panel.ServerListPanel;
import net.sf.freecol.client.gui.panel.StartGamePanel;
import net.sf.freecol.client.gui.panel.StatisticsPanel;
import net.sf.freecol.client.gui.panel.StatusPanel;
import net.sf.freecol.client.gui.panel.TilePanel;
import net.sf.freecol.client.gui.panel.TradeRouteInputPanel;
import net.sf.freecol.client.gui.panel.TradeRoutePanel;
import net.sf.freecol.client.gui.panel.TrainPanel;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.client.gui.dialog.VictoryDialog;
import net.sf.freecol.client.gui.dialog.WarehouseDialog;
import net.sf.freecol.client.gui.panel.WorkProductionPanel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDataFile;
import net.sf.freecol.common.metaserver.ServerInfo;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.DiplomaticTrade;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.Monarch.MonarchAction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TradeRoute;
import net.sf.freecol.common.model.TypeCountMap;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.StringUtils.*;
import net.sf.freecol.common.util.Utils;


/**
 * The main container for the other GUI components in FreeCol. This
 * container is where the panels, dialogs and menus are added. In
 * addition, this is the component in which the map graphics are
 * displayed.
 * <p>
 * <b>Displaying panels and a dialogs</b>
 * <p>
 * {@code Canvas} contains methods to display various panels and dialogs.
 * Most of these methods use {@link net.sf.freecol.common.i18n i18n} to get
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
         *
         * @param fcd The parent {@code FreeColDialog}.
         * @param tile An optional {@code Tile} to display.
         * @param handler The {@code DialogHandler} to call when run.
         */
        public DialogCallback(FreeColDialog<T> fcd, Tile tile,
                              DialogHandler<T> handler) {
            this.fcd = fcd;
            this.tile = tile;
            this.handler = handler;
        }


        // Implement Runnable

        @Override
        public void run() {
            // Display the dialog...
            viewFreeColDialog(fcd, tile);
            // ...and use another thread to wait for a dialog response...
            new Thread(fcd.toString()) {
                @Override
                public void run() {
                    while (!fcd.responded()) {
                        Utils.delay(500, "Dialog interrupted.");
                    }
                    // ...before handling the result.
                    handler.handle(fcd.getResponse());
                }
            }.start();
        }
    };

    private static enum PopupPosition {
        ORIGIN,
        CENTERED,
        CENTERED_LEFT,
        CENTERED_RIGHT,
    }

    /** Number of tries to find a clear spot on the canvas. */
    private static final int MAXTRY = 3;

    /** Number of pixels that must be moved before a goto is enabled. */
    private static final int DRAG_THRESHOLD = 16;

    /** The cursor to show for goto operations. */
    private static final java.awt.Cursor GO_CURSOR
        = (java.awt.Cursor)UIManager.get("cursor.go");
    
    /** A class for frames being used as tool boxes. */
    private static class ToolBoxFrame extends JInternalFrame {}

    /** The game client. */
    private final FreeColClient freeColClient;

    /** The parent GUI. */
    private final GUI gui;

    private final GraphicsDevice graphicsDevice;

    /** The parent frame, either a window or the full screen. */
    private FreeColFrame frame;

    private boolean windowed;

    private MainPanel mainPanel;

    private final StartGamePanel startGamePanel;

    private final StatusPanel statusPanel;

    private final ChatPanel chatPanel;

    private final ChatDisplay chatDisplay;

    private final MapViewer mapViewer;

    /** Where the map was drag-clicked. */
    private Point dragPoint = null;

    /** Has a goto operation started? */
    private boolean gotoStarted = false;

    private GrayLayer greyLayer;

    private final ServerListPanel serverListPanel;

    /** Used to detect resizing. */
    private Dimension oldSize = null;

    private boolean clientOptionsDialogShowing = false;

    /** The dialogs in view. */
    private final List<FreeColDialog<?>> dialogs = new ArrayList<>();


    /**
     * The constructor to use.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param graphicsDevice The used graphics device.
     * @param gui The gui.
     * @param desiredSize The desired size of the frame.
     * @param mapViewer The object responsible of drawing the map onto
     *     this component.
     */
    public Canvas(final FreeColClient freeColClient,
                  final GraphicsDevice graphicsDevice,
                  final GUI gui,
                  final Dimension desiredSize,
                  MapViewer mapViewer) {
        this.freeColClient = freeColClient;
        this.gui = gui;
        this.graphicsDevice = graphicsDevice;
        this.mapViewer = mapViewer;

        // Determine if windowed mode should be used and set the window size.
        Rectangle windowBounds = null;
        if (desiredSize == null) {
            if(graphicsDevice.isFullScreenSupported()) {
                windowed = false;
                logger.info("Full screen mode used.");
            } else {
                windowed = true;
                logger.warning("Full screen mode not supported.");
                System.err.println(Messages.message("client.fullScreen"));
            }
        } else {
            windowed = true;
            if(desiredSize.width > 0 && desiredSize.height > 0) {
                windowBounds = new Rectangle(desiredSize);
                logger.info("Windowed mode using desired window size of " + desiredSize);
            } else {
                logger.info("Windowed mode used.");
            }
        }

        setDoubleBuffered(true);
        setOpaque(false);
        setLayout(null);

        chatDisplay = new ChatDisplay();
        chatPanel = new ChatPanel(freeColClient);
        serverListPanel = new ServerListPanel(freeColClient,
            freeColClient.getConnectController());
        startGamePanel = new StartGamePanel(freeColClient);
        statusPanel = new StatusPanel(freeColClient);

        setFocusable(true);
        setFocusTraversalKeysEnabled(false);

        createKeyBindings();
        createFrame(null, windowBounds);
        mapViewer.startCursorBlinking();
        logger.info("Canvas created.");
    }

    /**
     * Check if there is a map to display.
     *
     * @return True if there is a map available.
     */
    private boolean hasMap() {
        return this.freeColClient != null
            && this.freeColClient.getGame() != null
            && this.freeColClient.getGame().getMap() != null;
    }

    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Change the windowed mode.
     */
    public void changeWindowedMode() {
        // Clean up the old frame
        JMenuBar menuBar = null;
        Rectangle windowBounds = null;
        if (frame != null) {
            menuBar = frame.getJMenuBar();
            if (windowed) {
                windowBounds = frame.getBounds();
            }
            frame.setVisible(false);
            frame.dispose();
        }
        windowed = !windowed;

        createFrame(menuBar, windowBounds);
    }

    private void createFrame(JMenuBar menuBar, Rectangle windowBounds) {
        // FIXME: Check this:
        // User might have moved window to new screen in a
        // multi-screen setup, so make this.gd point to the current screen.
        frame = new FreeColFrame(freeColClient, graphicsDevice,
            menuBar, this, windowed, windowBounds);
        updateSizes();
        frame.setVisible(true);
    }

    /**
     * Start the GUI for the map editor.
     */
    public void startMapEditorGUI() {
        if (frame == null) return;

        // We may need to reset the zoom value to the default value
        gui.resetMapZoom();

        frame.setMapEditorMenuBar();
        showMapEditorTransformPanel();

        CanvasMapEditorMouseListener listener
            = new CanvasMapEditorMouseListener(freeColClient, this);
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    /**
     * Quit the GUI.  All that is required is to exit the full screen.
     */
    public void quit() {
        if (frame != null && !windowed) {
            frame.exitFullScreen();
        }
    }

    /**
     * In game initializations.
     */
    public void initializeInGame() {
        if (frame == null) return;
        frame.setInGameMenuBar();
    }

    /**
     * Reset the menu bar.
     */
    public void resetMenuBar() {
        if (frame == null) return;
        frame.resetMenuBar();
    }

    /**
     * Update the menu bar.
     */
    public void updateMenuBar() {
        if (frame == null) return;
        frame.updateMenuBar();
    }

    /**
     * Scroll the map in the given direction.
     *
     * @param direction The {@code Direction} to scroll in.
     * @return True if scrolling occurred.
     */
    public boolean scrollMap(Direction direction) {
        return mapViewer.scrollMap(direction);
    }

    /**
     * Converts the given screen coordinates to Map coordinates.
     * It checks to see to which Tile the given pixel 'belongs'.
     *
     * @param x The x-coordinate in pixels.
     * @param y The y-coordinate in pixels.
     * @return The Tile that is located at the given position on the screen.
     */
    public Tile convertToMapTile(int x, int y) {
        return mapViewer.convertToMapTile(x, y);
    }

    /**
     * Gets the point at which the map was clicked for a drag.
     *
     * @return The Point where the mouse was initially clicked.
     */
    public Point getDragPoint() {
        return this.dragPoint;
    }

    /**
     * Sets the point at which the map was clicked for a drag.
     *
     * @param x The mouse x position.
     * @param y The mouse y position.
     */
    public void setDragPoint(int x, int y) {
        this.dragPoint = new Point(x, y);
    }

    /**
     * Is mouse movement differnce above the drag threshold?
     *
     * @param x The new mouse x position.
     * @param y The new mouse y position.
     * @return True if the mouse has been dragged.
     */
    public boolean isDrag(int x, int y) {
        final Point drag = getDragPoint();
        if (drag == null) return false;
        int deltaX = Math.abs(x - drag.x);
        int deltaY = Math.abs(y - drag.y);
        return deltaX >= DRAG_THRESHOLD || deltaY >= DRAG_THRESHOLD;
    }

    /**
     * Checks if there is currently a goto operation on the mapboard.
     *
     * @return True if a goto operation is in progress.
     */
    public boolean isGotoStarted() {
        return this.gotoStarted;
    }

    /**
     * Starts a goto operation.
     */
    public void startGoto() {
        this.gotoStarted = true;
        setCursor(GO_CURSOR);
        mapViewer.changeGotoPath(null);
        refresh();
    }

    /**
     * Stops any ongoing goto operation.
     *
     * @return The old goto path if any.
     */
    public PathNode stopGoto() {
        PathNode ret = (this.gotoStarted) ? mapViewer.getGotoPath() : null;
        this.gotoStarted = false;
        setCursor(null);
        mapViewer.changeGotoPath(null);
        refresh();
        return ret;
    }

    /**
     * Change the goto path for a unit to a new tile.
     *
     * @param unit The {@code Unit} that is travelling.
     * @param tile The new {@code Tile} to go to.
     */       
    public void changeGoto(Unit unit, Tile tile) {
        if (!isGotoStarted()) return;

        // Do nothing if the tile has not changed.
        PathNode oldPath = mapViewer.getGotoPath();
        Tile lastTile = (oldPath == null) ? null
            : oldPath.getLastNode().getTile();
        if (lastTile == tile) return;

        // Do not show a path if it will be invalid, avoiding calling
        // the expensive path finder if possible.
        PathNode newPath = (unit.getTile() == tile
            || !tile.isExplored()
            || !unit.getSimpleMoveType(tile).isLegal()) ? null
            : unit.findPath(tile);
        mapViewer.changeGotoPath(newPath);
    }


    // Internals

    /**
     * Adds a component on this Canvas inside a frame.
     *
     * @param comp The component to add to the canvas.
     * @param toolBox Should be set to true if the resulting frame is
     *     used as a toolbox (that is: it should not be counted as a
     *     frame).
     * @param popupPosition A preferred {@code PopupPosition}.
     * @param resizable Whether this component can be resized.
     * @return The {@code JInternalFrame} that was created and added.
     */
    private JInternalFrame addAsFrame(JComponent comp, boolean toolBox,
                                      PopupPosition popupPosition,
                                      boolean resizable) {
        final int FRAME_EMPTY_SPACE = 60;

        final JInternalFrame f = (toolBox) ? new ToolBoxFrame()
            : new JInternalFrame();
        Container con = f.getContentPane();
        if (con instanceof JComponent) {
            JComponent c = (JComponent)con;
            c.setOpaque(false);
            c.setBorder(null);
        }

        if (comp.getBorder() != null) {
            if (comp.getBorder() instanceof EmptyBorder) {
                f.setBorder(Utility.blankBorder(10, 10, 10, 10));
            } else {
                f.setBorder(comp.getBorder());
                comp.setBorder(Utility.blankBorder(5, 5, 5, 5));
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
        this.add(f, MODAL_LAYER);
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
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addCentered(Component comp, Integer i) {
        comp.setLocation((getWidth() - comp.getWidth()) / 2,
                         (getHeight() - comp.getHeight()) / 2);
        this.add(comp, i);
    }

    /**
     * Adds a component to this Canvas.
     *
     * Make sure the status panel is not present unless the component
     * *is* the status panel.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    private void addToCanvas(Component comp, Integer i) {
        if (statusPanel.isVisible()) {
            if (comp == statusPanel) return;
            if (!(comp instanceof JMenuItem)) removeFromCanvas(statusPanel);
        }

        if (i == null) i = JLayeredPane.DEFAULT_LAYER;
        try {
            super.add(comp, i);
        } catch (Exception e) {
            logger.log(Level.WARNING, "addToCanvas(" + comp + ", " + i
                + ") failed.", e);
        }
    }

    /**
     * Choose a location for a component.
     *
     * @param comp The {@code Component} to place.
     * @param width The component width to use.
     * @param height The component height to use.
     * @param popupPosition An optional {@code PopupPosition} hint.
     * @return A suitable {@code Point} to place the component.
     */
    private Point chooseLocation(Component comp, int width, int height, 
                                 PopupPosition popupPosition) {
        Point p = null;
        if ((comp instanceof FreeColPanel)
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
        if ((p = getClearSpace(x, y, width, height, MAXTRY)) != null
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
     * Try to find some free space on the canvas for a component,
     * starting at x,y.
     *
     * @param x A starting x coordinate.
     * @param y A starting y coordinate.
     * @param w The component width to use.
     * @param h The component height to use.
     * @param tries The number of attempts to find a clear space.
     * @return A {@code Point} to place the component at or null
     *     on failure.
     */
    private Point getClearSpace(final int x, final int y,
                                final int w, final int h, int tries) {
        final Rectangle bounds = this.getBounds();
        if (!bounds.contains(x, y)) return null;

        tries = 3 * tries + 1; // 3 new candidates per level
        List<Point> todo = new ArrayList<>();
        Point p = new Point(x, y);
        todo.add(p);

        List<Component> allComponents
            = transform(this.getComponents(),
                        c -> !(c instanceof GrayLayer) && c.isValid());
        allComponents.addAll(dialogs);

        // Find the position with the least overlap
        int bestScore = Integer.MAX_VALUE;
        Point best = p;
        while (!todo.isEmpty()) {
            p = todo.remove(0);
            Rectangle r = new Rectangle(p.x, p.y, w, h);
            if (!bounds.contains(r)) {
                continue;
            }

            // Find the most overlapping component at this position,
            // as well as the globally least.
            int foundScore = 0;
            Component found = null;
            for (Component c : allComponents) {
                Rectangle rb = c.getBounds();
                if (rb.intersects(r)) {
                    Rectangle rr = rb.intersection(r);
                    int score = (int)Math.round(rr.getWidth() * rr.getHeight());
                    if (foundScore < score) {
                        foundScore = score;
                        found = c;
                    }
                }
            }
            if (found == null) { // Can not improve on no overlap, return now
                return p;
            }
            if (bestScore > foundScore) {
                bestScore = foundScore;
                best = p;
            }
            // Guarantee eventual completion
            if (--tries <= 0) break;

            int n = todo.size(),
                // Some alternative new positions
                //   0: move right/down to avoid the collision
                //   1: move as far as possible right/down
                //   2: wrap back to the far left
                x0 = found.getX() + found.getWidth() + 1,
                y0 = found.getY() + found.getHeight() + 1,
                x1 = bounds.x + bounds.width - w - 1,
                y1 = bounds.y + bounds.height - h - 1,
                x2 = bounds.x,
                y2 = bounds.y;
            boolean x0ok = bounds.contains(x0 + w, y),
                y0ok = bounds.contains(x, y0 + h),
                x1ok = bounds.contains(x1, y),
                y1ok = bounds.contains(x, y1);
            todo.add(n, new Point((x0ok) ? x0 : (x1ok) ? x1 : x2,
                                  (y0ok) ? y0 : (y1ok) ? y1 : y2));
            todo.add(n, new Point(x, (y0ok) ? y0 : (y1ok) ? y1 : y2));
            todo.add(n, new Point((x0ok) ? x0 : (x1ok) ? x1 : x2, y));
        }
        return best;
    }

    /**
     * Gets any currently displayed colony panel for the specified colony.
     *
     * This is distinct from {@link #getExistingFreeColPanel} because
     * there can be multiple ColonyPanels.
     *
     * @param colony The {@code Colony} to check.
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
     * @param c The {@code Component}.
     * @return The given component if this is an internal frame or the
     *     first parent that is an internal frame.  Returns
     *     {@code null} if no internal frame is found.
     */
    private JInternalFrame getInternalFrame(final Component c) {
        Component temp = c;

        while (temp != null && !(temp instanceof JInternalFrame)) {
            temp = temp.getParent();
        }
        return (JInternalFrame) temp;
    }

    /**
     * Make a tile visible, then determine corresponding position to popup
     * a panel.
     *
     * @param tile A {@code Tile} to be made visible.
     * @return A {@code PopupPosition} for a panel to be displayed.
     */
    private PopupPosition setOffsetFocus(Tile tile) {
        if (tile == null) return PopupPosition.CENTERED;
        int where = mapViewer.setOffsetFocus(tile);
        return (where > 0) ? PopupPosition.CENTERED_LEFT
            : (where < 0) ? PopupPosition.CENTERED_RIGHT
            : PopupPosition.CENTERED;
    }

    /**
     * Gets the saved position of a component.
     *
     * @param comp The {@code Component} to use.
     * @return The saved position as a {@code Point}, or null if no
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
        return co.getPanelPosition(comp.getClass().getName());
    }

    /**
     * Get the saved size of a component.
     *
     * @param comp The {@code Component} to use.
     * @return A {@code Dimension} for the component or null if
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
        return co.getPanelSize(comp.getClass().getName());
    }

    /**
     * A component is closing.  Some components need position and size
     * to be saved.
     *
     * @param c The closing {@code Component}.
     * @param frame The enclosing {@code JInternalFrame}.
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
     * Save an {@code int} value to the saved ClientOptions,
     * using the name of the components class plus the given key as
     * and identifier.
     *
     * @param className The class name for the component.
     * @param key The key to save.
     * @param value The value to save.
     */
    private void saveInteger(String className, String key, int value) {
        if (freeColClient == null) return;
        final ClientOptions co = freeColClient.getClientOptions();
        if (co == null) return;
        final OptionGroup etc = co.getOptionGroup(ClientOptions.ETC);
        if (etc == null) return;
        
        // Insist the option is present
        if (!etc.hasOption(className + key, IntegerOption.class)) {
            Specification specification = (freeColClient.getGame() == null)
                ? null : freeColClient.getGame().getSpecification();
            etc.add(new IntegerOption(className + key, specification));
        }
        // Set the value
        etc.setInteger(className + key, value);
    }

    /**
     * Save the position of a component.
     *
     * @param comp The {@code Component} to use.
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
     * @param comp The {@code Component} to use.
     * @param size The {@code Dimension} to save.
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
     * Restart blinking on the map.  Switching it on again needs to check
     * for the presence of other dialogs.
     */
    private void restartBlinking() {
        if (mapViewer.getViewMode() != GUI.ViewMode.MOVE_UNITS) return;
        for (FreeColDialog<?> f : dialogs) {
            if (f.isModal()) return;
        }
        mapViewer.startCursorBlinking();
    }

    /**
     * Stop blinking on the map.
     */
    private void stopBlinking() {
        mapViewer.stopCursorBlinking();
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional {@code Tile} to make visible (not
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
        if (freeColDialog.isModal()) restartBlinking();
        return response;
    }

    /**
     * Displays the given panel, making sure a tile is visible.
     *
     * @param panel The panel to be displayed
     * @param tile A {@code Tile} to make visible (not under the panel!)
     * @param resizable Should the panel be resizable?
     */
    private void showFreeColPanel(FreeColPanel panel, Tile tile,
                                  boolean resizable) {
        showSubPanel(panel, setOffsetFocus(tile), resizable);
    }

    /**
     * Displays a {@code FreeColPanel}.
     *
     * @param panel {@code FreeColPanel}, panel to show
     * @param resizable Should the panel be resizable?
     */
    private void showSubPanel(FreeColPanel panel, boolean resizable) {
        showSubPanel(panel, PopupPosition.CENTERED, resizable);
    }

    /**
     * Displays a {@code FreeColPanel} at a generalized position.
     *
     * @param panel {@code FreeColPanel}, panel to show
     * @param popupPosition {@code PopupPosition} The generalized
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
     * @param comp The {@code Component} to add.
     * @return The component argument.
     */
    @Override
    public Component add(Component comp) {
        this.add(comp, JLayeredPane.DEFAULT_LAYER);
        return comp;
    }

    /**
     * Adds a component to this Canvas.
     *
     * @param comp The {@code Component} to add to this canvas.
     * @param i The layer to add the component to (see JLayeredPane).
     */
    public void add(Component comp, Integer i) {
        addToCanvas(comp, i);
        gui.updateMenuBar();
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
     * Close a panel by class name.
     *
     * @param panel The panel to close.
     */
    public void closePanel(String panel) {
        if (panel.endsWith("Panel")) {
            for (Component c1 : getComponents()) {
                if (c1 instanceof JInternalFrame) {
                    for (Component c2 : ((JInternalFrame)c1).getContentPane()
                             .getComponents()) {
                        if (panel.equals(c2.getClass().getName())) {
                            notifyClose(c2, (JInternalFrame)c1);
                            return;
                        }
                    }
                }
            }
        } else if (panel.endsWith("Dialog")) {
            for (FreeColDialog<?> fcd : new ArrayList<>(dialogs)) {
                if (panel.equals(fcd.getClass().getName())) {
                    dialogs.remove(fcd);
                    fcd.dispose();
                    return;
                }
            }
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
     * Closes the {@code StatusPanel}.
     *
     * @see #showStatusPanel
     */
    public void closeStatusPanel() {
        if (statusPanel.isVisible()) {
            remove(statusPanel);
        }
    }

    /**
     * Tells that a chat message was received.
     *
     * @param message The chat message.
     */
    public void displayChat(GUIMessage message) {
        chatDisplay.addMessage(message);
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Tells that a chat message was received.
     *
     * @param senderName The sender.
     * @param message The chat message.
     * @param privateChat True if this is a private message.
     */
    public void displayStartChat(String senderName, String message,
                                 boolean privateChat) {
        StartGamePanel sgp = getExistingFreeColPanel(StartGamePanel.class);
        if (sgp != null) {
            sgp.displayChat(senderName, message, privateChat);
        }
    }

    /**
     * Add a dialog to the current dialog list.
     *
     * @param fcd The dialog to add.
     */
    private void dialogAdd(FreeColDialog<?> fcd) {
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
     * @param <T> The actual panel type.
     * @param type The type of {@code FreeColPanel} to look for.
     * @return A currently displayed {@code FreeColPanel} of the
     *     requested type, or null if none found.
     */
    private <T extends FreeColPanel> T getExistingFreeColPanel(Class<T> type) {
        for (Component c1 : getComponents()) {
            if (c1 instanceof JInternalFrame) {
                for (Component c2 : ((JInternalFrame)c1).getContentPane()
                         .getComponents()) {
                    try {
                        T ret = type.cast(c2);
                        if (ret != null) {
                            final JInternalFrame jif = (JInternalFrame)c1;
                            SwingUtilities.invokeLater(() -> {
                                    jif.toFront();
                                    jif.repaint();
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
     * Get any panel this {@code Canvas} is displaying.
     *
     * @return A {@code Component} the {@code Canvas} is
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
     * Attach a closing callback to any current error panel.
     *
     * @param callback The {@code Runnable} to attach.
     * @return True if an error panel was present.
     */
    public boolean onClosingErrorPanel(Runnable callback) {
        ErrorPanel ep = getExistingFreeColPanel(ErrorPanel.class);
        if (ep == null) return false;
        ep.addClosingCallback(callback);
        return true;
    }

    /**
     * Checks if this {@code Canvas} displaying another panel.
     * <p>
     * Note that the previous implementation could throw exceptions
     * in some cases, thus the change.
     *
     * @return {@code true} if the {@code Canvas} is displaying an
     *         internal frame.
     */
    private boolean isShowingSubPanel() {
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
     * @param comp The {@code Component} to remove.
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
        for (KeyListener keyListener : keyListeners) {
            removeKeyListener(keyListener);
        }

        MouseListener[] mouseListeners = getMouseListeners();
        for (MouseListener mouseListener : mouseListeners) {
            removeMouseListener(mouseListener);
        }

        MouseMotionListener[] mouseMotionListeners = getMouseMotionListeners();
        for (MouseMotionListener mouseMotionListener : mouseMotionListeners) {
            removeMouseMotionListener(mouseMotionListener);
        }

        for (Component c : getComponents()) {
            removeFromCanvas(c);
        }
    }

    /**
     * Set preferred size to saved size, or to the given
     * {@code Dimension} if no saved size was found. Call this
     * method in the constructor of a FreeColPanel in order to
     * remember its size and position.
     *
     * @param comp The {@code Component} to use.
     * @param d The {@code Dimension} to use as default.
     */
    public void restoreSavedSize(Component comp, Dimension d) {
        final Dimension pref = comp.getPreferredSize();
        final Dimension sugg = (d == null) ? pref : d;
        boolean save = false;

        Dimension size = getSavedSize(comp);
        if (size == null) {
            size = new Dimension(pref);
            save = true;
        }

        // Fix up broken/outdated saved sizes
        if(size.width < sugg.width) {
            size.width = sugg.width;
            save = true;
        }
        if(size.height < sugg.height) {
            size.height = sugg.height;
            save = true;
        }
        if(size.width < pref.width) {
            size.width = pref.width;
            save = true;
        }
        if(size.height < pref.height) {
            size.height = pref.height;
            save = true;
        }

        if(save) {
            saveSize(comp, size);
        }

        if (!pref.equals(size)) {
            comp.setPreferredSize(size);
        }
    }

    /**
     * Closes all panels, changes the background and shows the main menu.
     */
    public void mainTitle() {
        // FIXME: check if the GUI object knows that we're not
        // inGame. (Retrieve value of GUI::inGame.)  If GUI thinks
        // we're still in the game then log an error because at this
        // point the GUI should have been informed.
        removeInGameComponents();
        showMainPanel(null);
        repaint();
    }

    public void setupMouseListeners() {
        addMouseListener(new CanvasMouseListener(freeColClient));
        addMouseMotionListener(new CanvasMouseMotionListener(freeColClient, this));
    }

    /**
     * Updates the sizes of the components on this Canvas.
     */
    private void updateSizes() {
        if (oldSize == null
                || oldSize.width != getWidth()
                || oldSize.height != getHeight()) {
            gui.updateMapControlsInCanvas();
            mapViewer.changeSize(getSize());
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
        chatDisplay.removeOldMessages();
        Dimension size = getSize();

        if (freeColClient.isMapEditor()) {
            if (hasMap()) {
                mapViewer.displayMap(g2d);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, size.width, size.height);
            }

        } else if (freeColClient.isInGame() && hasMap()) {
            mapViewer.displayMap(g2d);

            // Grey out the map if it is not my turn (and a multiplayer game).
            if (!freeColClient.currentPlayerIsMyPlayer()) {
                if (greyLayer == null) {
                    greyLayer = new GrayLayer(freeColClient);
                }
                if (greyLayer.getParent() == null) {
                    add(greyLayer, JLayeredPane.DRAG_LAYER);
                }
                greyLayer.setBounds(0, 0, size.width, size.height);
                greyLayer.setPlayer(freeColClient.getGame().getCurrentPlayer());
            } else {
                if (greyLayer != null && greyLayer.getParent() != null) {
                    removeFromCanvas(greyLayer);
                }
            }

            // paint chat display
            chatDisplay.display(g2d, mapViewer.getImageLibrary(), size);

        } else {
            /* main menu */
            // TODO: Check if its right to sometimes have an unfocused map
            //       ingame and end up here after clicking outside map.
            final Image bgImage = ImageLibrary.getCanvasBackgroundImage();
            if (bgImage != null) {
                // Get the background without scaling, to avoid wasting
                // memory needlessly keeping an unbounded number of rescaled
                // versions of the largest image in FreeCol, forever.
                // Draw background image with scaling.
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(bgImage, 0, 0, size.width, size.height, this);
                String versionStr = "v. " + FreeCol.getVersion();
                Font oldFont = g2d.getFont();
                Color oldColor = g2d.getColor();
                Font newFont = oldFont.deriveFont(Font.BOLD);
                TextLayout layout = new TextLayout(versionStr, newFont,
                    g2d.getFontRenderContext());
                Rectangle2D bounds = layout.getBounds();
                float x = size.width - (float) bounds.getWidth() - 5;
                float y = size.height - (float) bounds.getHeight();
                g2d.setColor(Color.white);
                layout.draw(g2d, x, y);
                g2d.setFont(oldFont);
                g2d.setColor(oldColor);
            } else {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, size.width, size.height);
            }
        }
    }


    // Override Container

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Component comp) {
        removeFromCanvas(comp);
        gui.updateMenuBar();
        if (comp != statusPanel && !isShowingSubPanel()) {
            requestFocus();
        }
    }


    // Special handling for the startGamePanel.

    /**
     * Refresh the player's table (called when a new player is added
     * from PreGameInputHandler.addPlayer).
     */
    void refreshPlayersTable() {
        startGamePanel.refreshPlayersTable();
    }


    // Dialog display

    /**
     * Displays a modal dialog with text and a choice of options.
     *
     * @param <T> The type to be returned from the dialog.
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     * @param obj An object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param cancelKey Key for the text of the optional cancel button.
     * @param choices The {@code List} containing the ChoiceItems to
     *            create buttons for.
     * @return The corresponding member of the values array to the selected
     *     option, or null if no choices available.
     */
    public <T> T showChoiceDialog(Tile tile, StringTemplate tmpl,
                                  ImageIcon icon, String cancelKey,
                                  List<ChoiceItem<T>> choices) {
        if (choices.isEmpty()) return null;
        FreeColChoiceDialog<T> fcd
            = new FreeColChoiceDialog<>(freeColClient, frame, true, tmpl, icon,
                                        cancelKey, choices);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a modal dialog with a text and a ok/cancel option.
     *
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     * @param template A {@code StringTemplate} to explain the choice.
     * @param icon An optional icon to display.
     * @param okKey The text displayed on the "ok"-button.
     * @param cancelKey The text displayed on the "cancel"-button.
     * @return True if the user clicked the "ok"-button.
     */
    public boolean showConfirmDialog(Tile tile, StringTemplate template,
                                     ImageIcon icon,
                                     String okKey, String cancelKey) {
        FreeColConfirmDialog fcd
            = new FreeColConfirmDialog(freeColClient, frame, true, template,
                                       icon, okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays a modal dialog with a text field and a ok/cancel option.
     *
     * @param tile An optional tile to make visible (not under the dialog).
     * @param template A {@code StringTemplate} that explains the
     *     action to the user.
     * @param defaultValue The default value appearing in the text field.
     * @param okKey A key displayed on the "ok"-button.
     * @param cancelKey A key displayed on the optional "cancel"-button.
     * @return The text the user entered, or null if cancelled.
     */
    public String showInputDialog(Tile tile, StringTemplate template,
                                  String defaultValue,
                                  String okKey, String cancelKey) {
        FreeColStringInputDialog fcd
            = new FreeColStringInputDialog(freeColClient, frame, true,
                                           template, defaultValue,
                                           okKey, cancelKey);
        return showFreeColDialog(fcd, tile);
    }

    /**
     * Displays the given dialog, optionally making sure a tile is visible.
     *
     * @param <T> The type to be returned from the dialog.
     * @param freeColDialog The dialog to be displayed
     * @param tile An optional {@code Tile} to make visible (not
     *     under the dialog!)
     */
    private <T> void viewFreeColDialog(final FreeColDialog<T> freeColDialog,
                                       Tile tile) {
        PopupPosition pp = setOffsetFocus(tile);

        // TODO: Remove compatibility code when all non-modal dialogs
        //       have been converted into panels.
        if(!freeColDialog.isModal()) {
            int canvasWidth = getWidth();
            int dialogWidth = freeColDialog.getWidth();
            if(dialogWidth*2 <= canvasWidth) {
                Point location = freeColDialog.getLocation();
                if(pp == PopupPosition.CENTERED_LEFT) {
                    freeColDialog.setLocation(location.x - canvasWidth/4,
                                              location.y);
                } else if(pp == PopupPosition.CENTERED_RIGHT) {
                    freeColDialog.setLocation(location.x + canvasWidth/4,
                                              location.y);
                }
            }
        }

        dialogAdd(freeColDialog);
        if (freeColDialog.isModal()) stopBlinking();
        freeColDialog.requestFocus();
        freeColDialog.setVisible(true);
    }


    // Simple front ends to display each panel or dialog.

    public void removeTradeRoutePanel(TradeRoutePanel panel) {
        remove(panel);
        TradeRouteInputPanel trip
            = getExistingFreeColPanel(TradeRouteInputPanel.class);
        if (trip != null) trip.cancelTradeRoute();
    }
        
    /**
     * Display the AboutPanel.
     */
    public void showAboutPanel() {
        showSubPanel(new AboutPanel(freeColClient), false);
    }

    /**
     * Show the BuildQueuePanel for a given colony.
     *
     * @param colony The {@code Colony} to show the build queue of.
     * @return The {@code BuildQueuePanel}.
     */
    public BuildQueuePanel showBuildQueuePanel(Colony colony) {
        BuildQueuePanel panel = getExistingFreeColPanel(BuildQueuePanel.class);
        if (panel == null || panel.getColony() != colony) {
            panel = new BuildQueuePanel(freeColClient, colony);
            showSubPanel(panel, true);
        }
        return panel;
    }

    /**
     * Display the {@code CaptureGoodsDialog}.
     *
     * @param unit The {@code Unit} capturing goods.
     * @param gl The list of {@code Goods} to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showCaptureGoodsDialog(Unit unit, List<Goods> gl,
                                       DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new CaptureGoodsDialog(freeColClient, frame, unit, gl),
                null, handler));
    }

    /**
     * Displays the {@code ChatPanel}.
     *
     * @see ChatPanel
     */
    public void showChatPanel() {
        // FIXME: does it have state, or can we create a new one?
        if (freeColClient.getSinglePlayer()) return; // chat with who?
        showSubPanel(chatPanel, true);
    }

    /**
     * Displays the {@code ChooseFoundingFatherDialog}.
     *
     * @param ffs The {@code FoundingFather}s to choose from.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showChooseFoundingFatherDialog(List<FoundingFather> ffs,
                                               DialogHandler<FoundingFather> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new ChooseFoundingFatherDialog(freeColClient, frame, ffs),
                null, handler));
    }

    /**
     * Displays a dialog for setting client options.
     *
     * @return The modified {@code OptionGroup}, or null if not modified.
     */
    public OptionGroup showClientOptionsDialog() {
        ClientOptionsDialog dialog = new ClientOptionsDialog(freeColClient, frame);
        OptionGroup group = null;
        clientOptionsDialogShowing = true;
        try {
            group = showFreeColDialog(dialog, null);
        } finally {
            clientOptionsDialogShowing = false;
        }
        return group;
    }

    /**
     * Displays the colony panel of the given {@code Colony}.
     * Defends against duplicates as this can duplicate messages
     * generated by multiple property change listeners registered
     * against the same colony.
     *
     * @param colony The colony whose panel needs to be displayed.
     * @param unit An optional {@code Unit} to select within the panel.
     * @return The {@code ColonyPanel}.
     */
    public ColonyPanel showColonyPanel(Colony colony, Unit unit) {
        if (colony == null) return null;
        ColonyPanel panel = getColonyPanel(colony);
        if (panel == null) {
            try {
                panel = new ColonyPanel(freeColClient, colony);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception in ColonyPanel for "
                    + colony.getId(), e);
                return null;
            }
            showFreeColPanel(panel, colony.getTile(), true);
        } else {
            panel.requestFocus();
        }
        if (unit != null) panel.setSelectedUnit(unit);
        return panel;
    }

    /**
     * Show the colopedia entry for a given node.
     *
     * @param nodeId The node identifier to display.
     */
    public void showColopediaPanel(String nodeId) {
        showSubPanel(new ColopediaPanel(freeColClient, nodeId), true);
    }

    /**
     * Show a {@code ColorChooserPanel}.
     *
     * @param al An {@code ActionListener} to handle panel button
     *     presses.
     * @return The {@code ColorChooserPanel} created.
     */
    public ColorChooserPanel showColorChooserPanel(ActionListener al) {
        ColorChooserPanel ccp = new ColorChooserPanel(freeColClient, al);
        showFreeColPanel(ccp, null, false);
        return ccp;
    }

    /**
     * Show the compact labour report.
     */
    public void showCompactLabourReport() {
        CompactLabourReport details = new CompactLabourReport(freeColClient);
        details.initialize();
        showSubPanel(details, false);

    }

    /**
     * Show the compact labour report for the specified unit data.
     *
     * @param unitData The {@code UnitData} to display.
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
        return showFreeColDialog(new ConfirmDeclarationDialog(freeColClient, frame),
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
     * Display the difficulty dialog for a given group.
     *
     * @param spec The enclosing {@code Specification}.
     * @param group The {@code OptionGroup} containing the difficulty.
     * @param editable If the options should be editable.
     * @return The resulting {@code OptionGroup}.
     */
    public OptionGroup showDifficultyDialog(Specification spec,
                                            OptionGroup group, boolean editable) {
        DifficultyDialog dd = new DifficultyDialog(freeColClient, frame,
                                                   spec, group, editable);
        OptionGroup ret = showFreeColDialog(dd, null);
        if (ret != null) FreeCol.setDifficulty(ret);
        return ret;
    }

    /**
     * Displays the {@code DumpCargoDialog}.
     *
     * @param unit The {@code Unit} that is dumping.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showDumpCargoDialog(Unit unit, DialogHandler<List<Goods>> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new DumpCargoDialog(freeColClient, frame, unit),
                unit.getTile(), handler));
    }

    /**
     * Display the EditOptionDialog.
     *
     * @param op The {@code Option} to edit.
     * @return The response returned by the dialog.
     */
    public boolean showEditOptionDialog(Option op) {
        return (op == null) ? false
            : showFreeColDialog(new EditOptionDialog(freeColClient, frame, op),
                                null);
    }

    /**
     * Display the EditSettlementDialog.
     *
     * @param is The {@code IndianSettlement} to edit.
     */
    public void showEditSettlementDialog(IndianSettlement is) {
        showFreeColDialog(new EditSettlementDialog(freeColClient, frame, is),
                          null);
    }

    /**
     * Shows the panel that allows the user to choose which unit will emigrate
     * from Europe.
     *
     * @param player The {@code Player} whose unit is emigrating.
     * @param fountainOfYouth Is this dialog displayed as a result of a
     *     fountain of youth.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEmigrationDialog(Player player, boolean fountainOfYouth,
                                     DialogHandler<Integer> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new EmigrationDialog(freeColClient, frame, player.getEurope(),
                                     fountainOfYouth),
                null, handler));
    }

    /**
     * Display the EndTurnDialog with given units that could still move.
     *
     * @param units A list of {@code Unit}s that could still move.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showEndTurnDialog(List<Unit> units,
                                  DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new EndTurnDialog(freeColClient, frame, units),
                null, handler));
    }

    /**
     * Displays an error message.
     *
     * @param message The message to display.
     * @param callback Optional routine to run when the error panel is closed.
     */
    public void showErrorMessage(String message, Runnable callback) {
        if (message != null) {
            ErrorPanel errorPanel = new ErrorPanel(freeColClient, message);
            if (callback != null) errorPanel.addClosingCallback(callback);
            showSubPanel(errorPanel, true);
        }
    }

    /**
     * Displays the {@code EuropePanel}.
     *
     * @see EuropePanel
     */
    public void showEuropePanel() {
        if (freeColClient.getGame() == null) return;
        EuropePanel panel = getExistingFreeColPanel(EuropePanel.class);
        if (panel == null) {
            panel = new EuropePanel(freeColClient, (getHeight() > 780));
            panel.addClosingCallback(() -> { removeEuropeanSubpanels(); });
            showSubPanel(panel, true);
        }
    }

    /**
     * Display an event panel.
     *
     * @param header The title.
     * @param image A resource key for the image to display.
     * @param footer Optional footer text.
     */
    public void showEventPanel(String header, String image, String footer) {
        showSubPanel(new EventPanel(freeColClient, header, image, footer),
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
     * Display the first contact dialog (which is really just a
     * non-modal confirm dialog).
     *
     * @param player The {@code Player} making contact.
     * @param other The {@code Player} to contact.
     * @param tile An optional {@code Tile} on offer.
     * @param settlementCount The number of settlements the other
     *     player has (from the server, other.getNumberOfSettlements()
     *     is wrong here!).
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showFirstContactDialog(Player player, Player other,
                                       Tile tile, int settlementCount,
                                       DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new FirstContactDialog(freeColClient, frame, player, other, tile,
                                       settlementCount),
                tile, handler));
    }

    /**
     * Display the GameOptionsDialog.
     *
     * @param editable Should the game options be editable?
     * @return The {@code OptionGroup} selected.
     */
    public OptionGroup showGameOptionsDialog(boolean editable) {
        GameOptionsDialog god = new GameOptionsDialog(freeColClient, frame, editable);
        return showFreeColDialog(god, null);
    }

    /**
     * Displays the high scores panel.
     *
     * @param messageId An optional message to add to the high scores panel.
     * @param scores The list of {@code HighScore}s to display.
     */
    public void showHighScoresPanel(String messageId, List<HighScore> scores) {
        showSubPanel(new ReportHighScoresPanel(freeColClient, messageId, scores),
                     PopupPosition.CENTERED, true);
    }

    /**
     * Displays the panel of the given native settlement.
     *
     * @param is The {@code IndianSettlement} to display.
     */
    public void showIndianSettlementPanel(IndianSettlement is) {
        IndianSettlementPanel panel
            = new IndianSettlementPanel(freeColClient, is);
        showFreeColPanel(panel, is.getTile(), true);
    }

    /**
     * Make image icon from an image.
     * Use only if you know having null is possible!
     *
     * @param image The {@code Image} to create an icon for.
     * @return The {@code ImageIcon}.
     */
    private static ImageIcon createImageIcon(Image image) {
        return (image==null) ? null : new ImageIcon(image);
    }

    /**
     * Make image icon from an object.
     *
     * @param display The FreeColObject to find an icon for.
     * @return The {@code ImageIcon} found.
     */
    private ImageIcon createObjectImageIcon(FreeColObject display) {
        return (display == null) ? null
            : createImageIcon(gui.getImageLibrary().getObjectImage(display, 2f));
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying an icon.
     * @param template The {@code StringTemplate} to display.
     */
    public void showInformationMessage(FreeColObject displayObject,
                                       StringTemplate template) {
        ImageIcon icon = null;
        Tile tile = null;
        if(displayObject != null) {
            icon = createObjectImageIcon(displayObject);
            tile = (displayObject instanceof Location)
                ? ((Location)displayObject).getTile()
                : null;
        }
        showInformationMessage(displayObject, tile, icon, template);
    }

    /**
     * Shows a message with some information and an "OK"-button.
     *
     * @param displayObject Optional object for displaying.
     * @param tile The Tile the object is at.
     * @param icon The icon to display for the object.
     * @param template The {@code StringTemplate} to display.
     */
    public void showInformationMessage(FreeColObject displayObject,
                                       Tile tile, ImageIcon icon,
                                       StringTemplate template) {
        String text = Messages.message(template);
        showFreeColPanel(new InformationPanel(freeColClient, text, 
                                              displayObject, icon),
                         tile, true);
    }

    /**
     * Displays a dialog where the user may choose a file.
     *
     * @param directory The directory containing the files.
     * @param extension An extension to select with.
     * @return The selected {@code File}.
     */
    public File showLoadDialog(File directory, String extension) {
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        File file = null;
        for (;;) {
            file = showFreeColDialog(new LoadDialog(freeColClient, frame,
                                                    directory, filters), null);
            if (file == null || file.isFile()) break;
            showErrorMessage(Messages.message(FreeCol.badFile("error.noSuchFile", file)), null);
        }
        return file;
    }

    /**
     * Displays a dialog for setting options when loading a savegame.  The
     * settings can be retrieved directly from {@link LoadingSavegameDialog}
     * after calling this method.
     *
     * @param pubSer Default value.
     * @param single Default value.
     * @return The {@code LoadingSavegameInfo} if the dialog was accepted,
     *     or null otherwise.
     */
    public LoadingSavegameInfo showLoadingSavegameDialog(boolean pubSer,
                                                         boolean single) {
        LoadingSavegameDialog lsd
            = new LoadingSavegameDialog(freeColClient, frame);
        return (showFreeColDialog(lsd, null)) ? lsd.getInfo() : null;
    }

    /**
     * Show a panel containing the log file.
     */
    public void showLogFilePanel() {
        showSubPanel(new ErrorPanel(freeColClient), true);

    }

    /**
     * Shows the {@code MainPanel}.
     *
     * @param userMsg An option message key to show.
     * @see MainPanel
     */
    public void showMainPanel(String userMsg) {
        closeMenus();
        frame.removeMenuBar();
        mainPanel = new MainPanel(freeColClient);
        addCentered(mainPanel, JLayeredPane.DEFAULT_LAYER);
        if (userMsg != null) gui.showInformationMessage(userMsg);
        mainPanel.requestFocus();
    }

    /**
     * Display the map editor transform panel.
     */
    private void showMapEditorTransformPanel() {
        JInternalFrame f = addAsFrame(new MapEditorTransformPanel(freeColClient),
            true, PopupPosition.CENTERED, false);
        f.setLocation(f.getX(), 50);
        repaint();
    }

    /**
     * Display the map generator options dialog.
     *
     * @param editable Should these options be editable.
     * @return The {@code OptionGroup} as edited.
     */
    public OptionGroup showMapGeneratorOptionsDialog(boolean editable) {
        MapGeneratorOptionsDialog mgod
            = new MapGeneratorOptionsDialog(freeColClient, frame, editable);
        return showFreeColDialog(mgod, null);
    }

    /**
     * Display the map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showMapSizeDialog() {
        return showFreeColDialog(new MapSizeDialog(freeColClient, frame), null);
    }

    /**
     * Displays a number of ModelMessages.
     *
     * @param messages The {@code ModelMessage}s to display.
     */
    public void showModelMessages(List<ModelMessage> messages) {
        if (messages.isEmpty()) return;
        final Game game = freeColClient.getGame();
        int n = messages.size();
        String[] texts = new String[n];
        FreeColObject[] fcos = new FreeColObject[n];
        ImageIcon[] icons = new ImageIcon[n];
        Tile tile = null;
        for (int i = 0; i < n; i++) {
            ModelMessage m = messages.get(i);
            texts[i] = Messages.message(m);
            fcos[i] = game.getMessageSource(m);
            icons[i] = createObjectImageIcon(game.getMessageDisplay(m));
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
     * @param action The {@code MonarchAction} underway.
     * @param template The {@code StringTemplate} describing the
     *     situation.
     * @param monarchKey The resource key for the monarch image.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showMonarchDialog(MonarchAction action,
                                  StringTemplate template, String monarchKey,
                                  DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new MonarchDialog(freeColClient, frame, action, template, monarchKey),
                null, handler));
    }

    /**
     * Display a dialog to set a new name for something.
     *
     * @param template A {@code StringTemplate} for the message
     *     to explain the dialog.
     * @param defaultName The default name.
     * @param unit The {@code Unit} discovering it.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNamingDialog(StringTemplate template, String defaultName,
                                 Unit unit, DialogHandler<String> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new FreeColStringInputDialog(freeColClient, frame, false,
                                             template, defaultName,
                                             "ok", null),
                unit.getTile(), handler));
    }

    /**
     * Display a dialog to handle a native demand to a colony.
     *
     * @param unit The demanding {@code Unit}.
     * @param colony The {@code Colony} being demanded of.
     * @param type The {@code GoodsType} demanded (null for gold).
     * @param amount The amount of goods demanded.
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showNativeDemandDialog(Unit unit, Colony colony,
                                       GoodsType type, int amount,
                                       DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(
                new NativeDemandDialog(freeColClient, frame, unit, colony,
                                       type, amount),
                unit.getTile(), handler));
    }

    /**
     * Displays the {@code NegotiationDialog}.
     *
     * @param our Our {@code FreeColGameObject} that is negotiating.
     * @param other The other {@code FreeColGameObject}.
     * @param agreement The current {@code DiplomaticTrade} agreement.
     * @param comment An optional {@code StringTemplate} containing a
     *     commentary message.
     * @return An updated agreement.
     */
    public DiplomaticTrade showNegotiationDialog(FreeColGameObject our,
                                                 FreeColGameObject other,
                                                 DiplomaticTrade agreement,
                                                 StringTemplate comment) {
        if ((!(our instanceof Unit) && !(our instanceof Colony))
            || (!(other instanceof Unit) && !(other instanceof Colony))
            || (our instanceof Colony && other instanceof Colony)) {
            throw new RuntimeException("Bad DTD args: " + our + ", " + other);
        }
        NegotiationDialog dtd = new NegotiationDialog(freeColClient, frame,
            our, other, agreement, comment);
        return showFreeColDialog(dtd, ((Location)our).getTile());
    }

    /**
     * Display the NewPanel for a given optional specification.
     *
     * @param specification The {@code Specification} to use.
     */
    public void showNewPanel(Specification specification) {
        showSubPanel(new NewPanel(freeColClient, specification), false);
    }

    /**
     * Display the parameters dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Parameters showParametersDialog() {
        return showFreeColDialog(new ParametersDialog(freeColClient, frame),
                                 null);
    }

    /**
     * Display a dialog to confirm a combat.
     *
     * @param attacker The attacker {@code Unit}.
     * @param defender The defender.
     * @param tile A {@code Tile} to make visible.
     * @return True if the combat is to proceed.
     */
    public boolean showPreCombatDialog(Unit attacker,
                                       FreeColGameObject defender,
                                       Tile tile) {
        return showFreeColDialog(new PreCombatDialog(freeColClient, frame,
                                                     attacker, defender),
                                 tile);
    }

    /**
     * Displays the purchase panel.
     */
    public void showPurchasePanel() {
        PurchasePanel panel = getExistingFreeColPanel(PurchasePanel.class);
        if (panel == null) {
            PurchasePanel pp = new PurchasePanel(freeColClient);
            pp.update();
            showFreeColPanel(pp, null, false);
        }
    }

    /**
     * Displays the recruit panel.
     */
    public void showRecruitPanel() {
        RecruitPanel panel = getExistingFreeColPanel(RecruitPanel.class);
        if (panel == null) {
            showFreeColPanel(new RecruitPanel(freeColClient), null, false);
        }
    }

    /**
     * Display the labour detail panel.
     *
     * @param unitType The {@code UnitType} to display.
     * @param data The labour data.
     * @param unitCount A map of unit distribution.
     * @param colonies The list of player {@code Colony}s.
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
     * @param styles The river styles a choice is made from.
     * @return The response returned by the dialog.
     */
    public String showRiverStyleDialog(List<String> styles) {
        return showFreeColDialog(
            new RiverStyleDialog(freeColClient, frame, styles), null);
    }

    /**
     * Displays a dialog where the user may choose a filename.
     *
     * @param directory The directory containing the files in which
     *     the user may overwrite.
     * @param defaultName Default filename for the savegame.
     * @return The selected {@code File}.
     */
    public File showSaveDialog(File directory, String defaultName) {
        String extension = lastPart(defaultName, ".");
        FileFilter[] filters = new FileFilter[] {
            FreeColDataFile.getFileFilter(extension)
        };
        return showFreeColDialog(new SaveDialog(freeColClient, frame,
                                                directory, filters,
                                                defaultName),
                                 null);
    }

    /**
     * Display the scale map size dialog.
     * 
     * @return The response returned by the dialog.
     */
    public Dimension showScaleMapSizeDialog() {
        return showFreeColDialog(new ScaleMapSizeDialog(freeColClient, frame),
                                 null);
    }

    /**
     * Display the select-amount dialog.
     *
     * @param goodsType The {@code GoodsType} to select an amount of.
     * @param available The amount of goods available.
     * @param defaultAmount The amount to select to start with.
     * @param needToPay If true, check the player has sufficient funds.
     * @return The amount selected.
     */
    public int showSelectAmountDialog(GoodsType goodsType, int available,
                                      int defaultAmount, boolean needToPay) {
        FreeColDialog<Integer> fcd
            = new SelectAmountDialog(freeColClient, frame, goodsType, available,
                                     defaultAmount, needToPay);
        Integer result = showFreeColDialog(fcd, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Display a dialog allowing the user to select a destination for
     * a given unit.
     *
     * @param unit The {@code Unit} to select a destination for.
     * @return A destination for the unit, or null.
     */
    public Location showSelectDestinationDialog(Unit unit) {
        return showFreeColDialog(new SelectDestinationDialog(freeColClient, frame,
                unit), unit.getTile());
    }

    /**
     * Display the select-tribute-amount dialog.
     *
     * @param question a {@code stringtemplate} describing the
     *     amount of tribute to demand.
     * @param maximum The maximum amount available.
     * @return The amount selected.
     */
    public int showSelectTributeAmountDialog(StringTemplate question,
                                             int maximum) {
        FreeColDialog<Integer> fcd
            = new SelectTributeAmountDialog(freeColClient, frame, question, maximum);
        Integer result = showFreeColDialog(fcd, null);
        return (result == null) ? -1 : result;
    }

    /**
     * Displays the {@code ServerListPanel}.
     *
     * @param serverList The list containing the servers retrieved from the
     *     metaserver.
     * @see ServerListPanel
     */
    public void showServerListPanel(List<ServerInfo> serverList) {
        closeMenus();

        serverListPanel.initialize(serverList);
        showSubPanel(serverListPanel, true);
    }

    /**
     * Displays the {@code StartGamePanel}.
     *
     * @param game The {@code Game} that is about to start.
     * @param player The {@code Player} using this client.
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
     *
     * @param serverStats A map of server statistics key,value pairs.
     * @param clientStats A map of client statistics key,value pairs.
     */
    public void showStatisticsPanel(Map<String, String> serverStats,
                                    Map<String, String> clientStats) {
        showSubPanel(new StatisticsPanel(freeColClient, serverStats,
                                         clientStats), true);
    }

    /**
     * Shows a status message that cannot be dismissed.  The panel
     * will be removed when another component is added to this
     * {@code Canvas}.  This includes all the
     * {@code showXXX}-methods. In addition,
     * {@link #closeStatusPanel()} also removes this panel.
     *
     * @param message The text message to display on the status panel.
     * @see StatusPanel
     */
    public void showStatusPanel(String message) {
        statusPanel.setStatusMessage(message);
        addCentered(statusPanel, JLayeredPane.POPUP_LAYER);
    }

    /**
     * Display the tile panel for a given tile.
     *
     * @param tile The {@code Tile} to display.
     */
    public void showTilePanel(Tile tile) {
        if (tile == null || !tile.isExplored()) return;
        showSubPanel(new TilePanel(freeColClient, tile), false);
    }

    /**
     * Shows a tile popup.
     *
     * @param tile The {@code Tile} where the popup occurred.
     */
    public void showTilePopup(Tile tile) {
        TilePopup tp = new TilePopup(freeColClient, this, tile);
        if (tp.hasItem()) {
            Point point = mapViewer.calculateTilePosition(tile, true);
            tp.show(this, point.x, point.y);
            tp.repaint();
        } else if (tile.isExplored()) {
            showTilePanel(tile);
        }
    }

    /**
     * Display the trade route input panel for a given trade route.
     *
     * @param newRoute The {@code TradeRoute} to display.
     * @return The {@code TradeRouteInputPanel}.
     */
    public TradeRouteInputPanel showTradeRouteInputPanel(TradeRoute newRoute) {
        TradeRouteInputPanel panel
            = new TradeRouteInputPanel(freeColClient, newRoute);
        showSubPanel(panel, null, true);
        return panel;
    }

    /**
     * Display a panel to select a trade route for a unit.
     *
     * @param unit An optional {@code Unit} to select a trade route for.
     */
    public void showTradeRoutePanel(Unit unit) {
        showFreeColPanel(new TradeRoutePanel(freeColClient, unit),
                         (unit == null) ? null : unit.getTile(), true);
    }

    /**
     * Displays the training panel.
     */
    public void showTrainPanel() {
        TrainPanel panel = getExistingFreeColPanel(TrainPanel.class);
        if (panel == null) {
            TrainPanel tp = new TrainPanel(freeColClient);
            tp.update();
            showFreeColPanel(tp, null, false);
        }
    }

    /**
     * Display the victory dialog.
     *
     * @param handler A {@code DialogHandler} for the dialog response.
     */
    public void showVictoryDialog(DialogHandler<Boolean> handler) {
        SwingUtilities.invokeLater(
            new DialogCallback<>(new VictoryDialog(freeColClient, frame),
                                                   null, handler));
    }

    /**
     * Display the warehouse dialog for a colony.
     *
     * Run out of ColonyPanel, so the tile is already displayed.
     *
     * @param colony The {@code Colony} to display.
     * @return The response returned by the dialog.
     */
    public boolean showWarehouseDialog(Colony colony) {
        return showFreeColDialog(new WarehouseDialog(freeColClient, frame, colony),
                                 null);
    }

    /**
     * Display the production of a unit.
     *
     * @param unit The {@code Unit} to display.
     */
    public void showWorkProductionPanel(Unit unit) {
        showSubPanel(new WorkProductionPanel(freeColClient, unit), true);
    }

    /**
     * Update all panels derived from the EuropePanel.
     */
    public void updateEuropeanSubpanels() {
        RecruitPanel rp
            = getExistingFreeColPanel(RecruitPanel.class);
        if (rp != null) rp.update();
        PurchasePanel pp
            = getExistingFreeColPanel(PurchasePanel.class);
        if (pp != null) pp.update();
        TrainPanel tp
            = getExistingFreeColPanel(TrainPanel.class);
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
        boolean compact;
        try {
            compact = freeColClient.getClientOptions()
                .getInteger(ClientOptions.COLONY_REPORT)
                == ClientOptions.COLONY_REPORT_COMPACT;
        } catch (Exception e) {
            compact = false;
        }
        ReportPanel r = (compact)
            ? getExistingFreeColPanel(ReportCompactColonyPanel.class)
            : getExistingFreeColPanel(ReportClassicColonyPanel.class);
        if (r == null) {
            showSubPanel((compact)
                ? new ReportCompactColonyPanel(freeColClient)
                : new ReportClassicColonyPanel(freeColClient),
                true);
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
     * @param messages The {@code ModelMessage}s to show.
     */
    public void showReportTurnPanel(List<ModelMessage> messages) {
        ReportTurnPanel r = getExistingFreeColPanel(ReportTurnPanel.class);
        if (r == null) {
            showSubPanel(new ReportTurnPanel(freeColClient, messages), true);
        } else {
            r.setMessages(messages);
        }
    }

    /**
     * Shows the given video Component.
     *
     * @param vp The video Component.
     * @param ml A MouseListener for stopping the video.
     * @param kl A KeyListener for stopping the video.
     */
    public void showVideoComponent(final Component vp,
                                   final MouseListener ml,
                                   final KeyListener kl) {
        addMouseListener(ml);
        addKeyListener(kl);
        addCentered(vp, JLayeredPane.PALETTE_LAYER);
    }
}
