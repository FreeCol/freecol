/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ComponentInputMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.resources.ResourceManager;
import cz.autel.dmi.HIGLayout;

/**
 * This is a panel for the Europe display. It shows the ships in Europe and
 * allows the user to send them back.
 */
public final class EuropePanel extends FreeColPanel implements ActionListener, ContainerListener {

    private static Logger logger = Logger.getLogger(EuropePanel.class.getName());

    public static enum EuropeAction { EXIT, RECRUIT, PURCHASE, TRAIN, UNLOAD }

    private final Canvas parent;

    private final FreeColClient freeColClient;

    private InGameController inGameController;

    private final ToAmericaPanel toAmericaPanel;

    private final ToEuropePanel toEuropePanel;

    private final InPortPanel inPortPanel;

    private final DocksPanel docksPanel;

    private final EuropeCargoPanel cargoPanel;

    private final MarketPanel marketPanel;
    
    private final TransactionLog log;

    private final DefaultTransferHandler defaultTransferHandler;

    private final MouseListener pressListener;

    private Europe europe;

    private Game game;

    private UnitLabel selectedUnit;

    private JButton exitButton;


    /**
     * The constructor for the panel.
     * 
     * @param parent The parent of this panel
     * @param freeColClient The main controller object for the client.
     * @param inGameController The controller object to be used when ingame.
     */
    public EuropePanel(Canvas parent, FreeColClient freeColClient, InGameController inGameController) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = inGameController;

        setFocusCycleRoot(true);

        // Use ESCAPE for closing the ColonyPanel:
        exitButton = new JButton(Messages.message("close"));
        InputMap closeInputMap = new ComponentInputMap(exitButton);
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        closeInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(exitButton, JComponent.WHEN_IN_FOCUSED_WINDOW, closeInputMap);

        // train button
        JButton trainButton = new JButton(Messages.message("train"));
        InputMap trainInputMap = new ComponentInputMap(trainButton);
        trainInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0, false), "pressed");
        trainInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_T, 0, true), "released");
        SwingUtilities.replaceUIInputMap(trainButton, JComponent.WHEN_IN_FOCUSED_WINDOW, trainInputMap);

        // purchase button
        JButton purchaseButton = new JButton(Messages.message("purchase"));
        InputMap purchaseInputMap = new ComponentInputMap(purchaseButton);
        purchaseInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0, false), "pressed");
        purchaseInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0, true), "released");
        SwingUtilities.replaceUIInputMap(purchaseButton, JComponent.WHEN_IN_FOCUSED_WINDOW, purchaseInputMap);

        // recruit button
        JButton recruitButton = new JButton(Messages.message("recruit"));
        InputMap recruitInputMap = new ComponentInputMap(recruitButton);
        recruitInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, false), "pressed");
        recruitInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0, true), "released");
        SwingUtilities.replaceUIInputMap(recruitButton, JComponent.WHEN_IN_FOCUSED_WINDOW, recruitInputMap);

        // unload button
        JButton unloadButton = new JButton(Messages.message("unload"));
        InputMap unloadInputMap = new ComponentInputMap(unloadButton);
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, false), "pressed");
        unloadInputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_U, 0, true), "released");
        SwingUtilities.replaceUIInputMap(unloadButton, JComponent.WHEN_IN_FOCUSED_WINDOW, unloadInputMap);

        toAmericaPanel = new ToAmericaPanel(this);
        toEuropePanel = new ToEuropePanel(this);
        inPortPanel = new InPortPanel();
        docksPanel = new DocksPanel(this);
        cargoPanel = new EuropeCargoPanel(parent);
        cargoPanel.setParentPanel(this);
        marketPanel = new MarketPanel(this);
        
        log = new TransactionLog();
        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_RIGHT);
        StyleConstants.setForeground(attributes, Color.WHITE);
        StyleConstants.setBold(attributes, true);
        log.setParagraphAttributes(attributes, true);

        toAmericaPanel.setBackground(Color.WHITE);
        toEuropePanel.setBackground(Color.WHITE);
        inPortPanel.setBackground(Color.WHITE);
        docksPanel.setBackground(Color.WHITE);
        cargoPanel.setBackground(Color.WHITE);

        exitButton.setForeground(Color.WHITE);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        toAmericaPanel.setTransferHandler(defaultTransferHandler);
        toEuropePanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        docksPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        marketPanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(this);
        MouseListener releaseListener = new DropListener();
        toAmericaPanel.addMouseListener(releaseListener);
        toEuropePanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        docksPanel.addMouseListener(releaseListener);
        marketPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        cargoPanel.addContainerListener(this);

        toAmericaPanel.setLayout(new GridLayout(0, 2));
        toEuropePanel.setLayout(new GridLayout(0, 2));
        inPortPanel.setLayout(new GridLayout(0, 2));
        docksPanel.setLayout(new GridLayout(0, 2));
        cargoPanel.setLayout(new GridLayout(1, 0));

        JScrollPane toAmericaScroll = new JScrollPane(toAmericaPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane toEuropeScroll = new JScrollPane(toEuropePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane inPortScroll = new JScrollPane(inPortPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane docksScroll = new JScrollPane(docksPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollPane cargoScroll = new JScrollPane(cargoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane marketScroll = new JScrollPane(marketPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane logScroll = new JScrollPane(log, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        toAmericaPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("goingToAmerica")));
        toEuropePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("goingToEurope")));
        docksPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("docks")));
        inPortPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), Messages
                .message("inPort")));
        logScroll.setBorder(BorderFactory.createEmptyBorder());

        marketScroll.getViewport().setOpaque(false);
        marketPanel.setOpaque(false);
        cargoScroll.getViewport().setOpaque(false);
        cargoPanel.setOpaque(false);
        toAmericaScroll.getViewport().setOpaque(false);
        toAmericaPanel.setOpaque(false);
        toEuropeScroll.getViewport().setOpaque(false);
        toEuropePanel.setOpaque(false);
        docksScroll.getViewport().setOpaque(false);
        docksPanel.setOpaque(false);
        inPortScroll.getViewport().setOpaque(false);
        inPortPanel.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        log.setOpaque(false);
        recruitButton.setOpaque(false);
        purchaseButton.setOpaque(false);
        trainButton.setOpaque(false);
        exitButton.setOpaque(false);
        unloadButton.setOpaque(false);

        int[] widths = { 0, 315, margin, 103, margin, 198, margin, 0, 0 };
        int[] heights = { 0, // top margin
                120, margin, // log
                // sailing to America, sailing to Europe
                39, margin, // recruit button
                39, margin, // buy button
                39, margin, // train button
                39, margin, // unload button
                116, margin, // in port
                116, margin, // cargo
                75, 39, // market
                0 // bottom margin
        };

        HIGLayout layout = new HIGLayout(widths, heights);
        layout.setRowWeight(1, 1);
        layout.setRowWeight(heights.length, 1);
        layout.setColumnWeight(1, 1);
        layout.setColumnWeight(widths.length, 1);
        setLayout(layout);

        int row = 2;
        add(logScroll, higConst.rcwh(row, 2, 6, 1));
        row += 2;
        add(toAmericaScroll, higConst.rcwh(row, 2, 1, 7));
        add(toEuropeScroll, higConst.rcwh(row, 4, 3, 7));
        add(recruitButton, higConst.rc(row, 8));
        row += 2;
        add(purchaseButton, higConst.rc(row, 8));
        row += 2;
        add(trainButton, higConst.rc(row, 8));
        row += 2;
        add(unloadButton, higConst.rc(row, 8));
        row += 2;
        add(inPortScroll, higConst.rcwh(row, 2, 3, 1));
        add(docksScroll, higConst.rcwh(row, 6, 1, 3));
        row += 2;
        add(cargoScroll, higConst.rcwh(row, 2, 3, 1));
        row += 2;
        add(marketScroll, higConst.rcwh(row, 2, 5, 2));
        row += 1;
        add(exitButton, higConst.rc(row, 8));

        exitButton.setActionCommand(EuropeAction.EXIT.toString());
        recruitButton.setActionCommand(EuropeAction.RECRUIT.toString());
        purchaseButton.setActionCommand(EuropeAction.PURCHASE.toString());
        trainButton.setActionCommand(EuropeAction.TRAIN.toString());
        unloadButton.setActionCommand(EuropeAction.UNLOAD.toString());
        
        enterPressesWhenFocused(exitButton);
        enterPressesWhenFocused(recruitButton);
        enterPressesWhenFocused(purchaseButton);
        enterPressesWhenFocused(trainButton);
        enterPressesWhenFocused(unloadButton);

        exitButton.addActionListener(this);
        recruitButton.addActionListener(this);
        purchaseButton.addActionListener(this);
        trainButton.addActionListener(this);
        unloadButton.addActionListener(this);
        setBorder(null);

        selectedUnit = null;

        // See the message of Ulf Onnen for more information about the presence
        // of this fake mouse listener.
        addMouseListener(new MouseAdapter() {
        });

        setSize(parent.getWidth(), parent.getHeight() - parent.getMenuBarHeight());

    }

    public void requestFocus() {
        exitButton.requestFocus();
    }

    /**
     * Refreshes this panel.
     */
    public void refresh() {
        repaint(0, 0, getWidth(), getHeight());
    }

    /**
     * Paints this component.
     * 
     * @param g The graphics context in which to paint.
     */
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        Image bgImage = ResourceManager.getImage("EuropeBackgroundImage.scaled");
        if (bgImage == null) {
            bgImage = ResourceManager.getImage("EuropeBackgroundImage");
        }
        if (bgImage != null) {
            if (bgImage.getWidth(null) != parent.getWidth() || bgImage.getHeight(null) != parent.getHeight()) {
                final Image fullSizeBgImage = ResourceManager.getImage("EuropeBackgroundImage");
                bgImage = fullSizeBgImage.getScaledInstance(parent.getWidth(), parent.getHeight(), Image.SCALE_SMOOTH);
                //UIManager.put("EuropeBackgroundImage.scaled", bgImage);

                /*
                 * We have to use a MediaTracker to ensure that the image has
                 * been scaled before we paint it.
                 */
                MediaTracker mt = new MediaTracker(freeColClient.getCanvas());
                mt.addImage(bgImage, 0, parent.getWidth(), parent.getHeight());

                try {
                    mt.waitForID(0);
                } catch (InterruptedException e) {
                    g.setColor(Color.black);
                    g.fillRect(0, 0, parent.getWidth(), parent.getHeight());
                    return;
                }

            }

            g.drawImage(bgImage, 0, 0, null);
        } else {
            Image tempImage = ResourceManager.getImage("BackgroundImage");

            if (tempImage != null) {
                for (int x = 0; x < width; x += tempImage.getWidth(null)) {
                    for (int y = 0; y < height; y += tempImage.getHeight(null)) {
                        g.drawImage(tempImage, x, y, null);
                    }
                }
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, width, height);
            }
        }
    }

    /**
     * Refreshes the components on this panel that need to be refreshed after
     * the user has recruited a new unit.
     */
    public void refreshDocks() {
        docksPanel.removeAll();

        Iterator<Unit> unitIterator = europe.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = unitIterator.next();

            if (((unit.getState() == UnitState.ACTIVE) ||
                 (unit.getState() == UnitState.SENTRY)) && (!unit.isNaval())) {
                UnitLabel unitLabel = new UnitLabel(unit, parent);
                unitLabel.setTransferHandler(defaultTransferHandler);
                unitLabel.addMouseListener(pressListener);

                docksPanel.add(unitLabel, false);
            }
        }

        // Only one component will be repainted!
        docksPanel.repaint(0, 0, docksPanel.getWidth(), docksPanel.getHeight());
    }

    /**
     * Refreshes the components on this panel that need to be refreshed after
     * the user has purchased a new unit.
      */
    public void refreshInPort() {
        inPortPanel.removeAll();

        List<Unit> units = europe.getUnitList();
        for (Unit unit : units) {
            if ((unit.getState() == UnitState.ACTIVE) && (unit.isNaval())) {
                UnitLabel unitLabel = new UnitLabel(unit, parent);
                unitLabel.setTransferHandler(defaultTransferHandler);
                unitLabel.addMouseListener(pressListener);
                unitLabel.addContainerListener(this);
                inPortPanel.add(unitLabel);
            }
        }

        // Only one component will be repainted!
        inPortPanel.repaint(0, 0, inPortPanel.getWidth(), inPortPanel.getHeight());
        if (!units.isEmpty()) {
            setSelectedUnit(units.get(units.size() - 1));
        }
    }

    public void componentAdded(ContainerEvent event) {
        refreshDocks();
    }

    public void componentRemoved(ContainerEvent event) {
        if (event.getComponent() instanceof CargoPanel) {
            refreshDocks();
        }
    }

    /**
     * Initialize the data on the window.
     * 
     * @param europe The object of type <code>Europe</code> this panel should
     *            display.
     * @param game The <code>Game</code>-object the <code>Europe</code>-object
     *            is a part of.
     */
    public void initialize(Europe europe, Game game) {
        this.europe = europe;
        this.game = game;

        freeColClient.getMyPlayer().getMarket().addTransactionListener(log);
        
        //
        // Remove the old components from the panels.
        //

        toAmericaPanel.removeAll();
        toEuropePanel.removeAll();
        inPortPanel.removeAll();
        cargoPanel.removeAll();
        marketPanel.removeAll();
        docksPanel.removeAll();
        log.setText("");

        //
        // Place new components on the panels.
        //

        UnitLabel lastCarrier = null;
        for (Unit unit : europe.getUnitList()) {
            UnitLabel unitLabel = new UnitLabel(unit, parent);
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            if (!unit.isNaval()) {
                // If it's not a naval unit, it belongs on the docks.
                docksPanel.add(unitLabel, false);
            } else {
                // Naval units can either be in the port, going to europe or
                // going to america.
                switch (unit.getState()) {
                case ACTIVE:
                    lastCarrier = unitLabel;
                    inPortPanel.add(unitLabel);
                    break;
                case TO_EUROPE:
                    toEuropePanel.add(unitLabel, false);
                    break;
                case TO_AMERICA:
                    toAmericaPanel.add(unitLabel, false);
                    break;
                default:
                    // This should normally not happen, but until the problem is solved, i comment it out.
                    // throw new RuntimeException("Naval unit in Europe is in an invalid state.");
                    // TODO: Find the cause of units not arriving active in Europe.
                }
            }
        }
        // We set the last carrier in the list active.
        setSelectedUnitLabel(lastCarrier);

        if (lastCarrier != null) {
            cargoPanel.setCarrier(lastCarrier.getUnit());
        }

        Player player = freeColClient.getMyPlayer();
        List<GoodsType> goodsTypes = FreeCol.getSpecification().getGoodsTypeList();
        for (GoodsType goodsType : goodsTypes) {
            if (goodsType.isStorable()) {
                MarketLabel marketLabel = new MarketLabel(goodsType, player.getMarket(), parent);
                marketLabel.setTransferHandler(defaultTransferHandler);
                marketLabel.addMouseListener(pressListener);
                marketPanel.add(marketLabel);
            }
        }

        String newLandName = player.getNewLandName();
        ((TitledBorder) toAmericaPanel.getBorder()).setTitle(Messages.message("sailingTo", 
                "%location%", newLandName));
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     * 
     * @param unit The unit that is being selected.
     */
    public void setSelectedUnit(Unit unit) {
        Component[] components = inPortPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i] instanceof UnitLabel && ((UnitLabel) components[i]).getUnit() == unit) {
                setSelectedUnitLabel((UnitLabel) components[i]);
                break;
            }
        }
    }

    /**
     * Selects a unit that is located somewhere on this panel.
     * 
     * @param unitLabel The unit that is being selected.
     */
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnit == unitLabel) {
            // No need to change anything
            return;
        }
        if (selectedUnit != null) {
            selectedUnit.setSelected(false);
        }
        selectedUnit = unitLabel;
        if (unitLabel == null) {
            cargoPanel.setCarrier(null);
        } else {
            cargoPanel.setCarrier(unitLabel.getUnit());
            unitLabel.setSelected(true);
        }

        updateCargoPanel();

        cargoPanel.revalidate();
        refresh();
    }

    private void updateCargoPanel() {
        cargoPanel.initialize();
    }

    /**
     * Returns the currently select unit.
     * 
     * @return The currently select unit.
     */
    public Unit getSelectedUnit() {
        if (selectedUnit == null) {
            return null;
        } else {
            return selectedUnit.getUnit();
        }
    }

    /**
     * Returns the currently select unit.
     * 
     * @return The currently select unit.
     */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnit;
    }

    private void unload() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Player player = freeColClient.getMyPlayer();
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                if (player.canTrade(goods)) {
                    inGameController.sellGoods(goods);
                    updateCargoPanel();
                } else {
                    inGameController.payArrears(goods);
                }
                getCargoPanel().revalidate();
                refresh();
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                inGameController.leaveShip(newUnit);
                updateCargoPanel();
                getCargoPanel().revalidate();
                // update docks panel
                refreshDocks();
                docksPanel.revalidate();
                refresh();
            }
        }
    }

    /**
     * Analyzes an event and calls the right external methods to take care of
     * the user's request.
     * 
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            // Get Command
            EuropeAction europeAction = Enum.valueOf(EuropeAction.class, command);
            // Close any open Europe Dialog, and show new one if required
            //int response = 
            parent.showEuropeDialog(europeAction);
            // Process Command
            switch (europeAction) {
            case EXIT:
                freeColClient.getMyPlayer().getMarket().removeTransactionListener(log);
                parent.remove(this);
                freeColClient.getInGameController().nextModelMessage();
                break;
            case RECRUIT:
            case PURCHASE:
            case TRAIN:
                refreshDocks();
                refreshInPort();
                revalidate();
                break;
            case UNLOAD:
                unload();
                break;
            default:
                logger.warning("Invalid action command");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }

    /**
     * Asks for pay arrears of a type of goods, if those goods are boycotted
     * 
     * @param goodsType The type of goods for paying arrears
     */
    public void payArrears(GoodsType goodsType) {
        if (freeColClient.getMyPlayer().getArrears(goodsType) > 0) {
            inGameController.payArrears(goodsType);
            getMarketPanel().revalidate();
            refresh();
        }
    }

    public void loadedGoods(Goods goods) {
        marketPanel.revalidate();
        revalidate();
        refresh();
    }

    public void loadedUnit(Unit unit) {
        refreshDocks();
    }

    public final class EuropeCargoPanel extends CargoPanel {
        public EuropeCargoPanel(Canvas parent) {
            super(parent, true);
        }

        @Override
        public String getUIClassID() {
            return "EuropeCargoPanelUI";
        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are going to
     * America.
     */
    public final class ToAmericaPanel extends JPanel {
        private final EuropePanel europePanel;


        /**
         * Creates this ToAmericaPanel.
         * 
         * @param europePanel The panel that holds this ToAmericaPanel.
         */
        public ToAmericaPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }

        /**
         * Adds a component to this ToAmericaPanel and makes sure that the unit
         * that the component represents gets modified so that it will sail to
         * America.
         * 
         * @param comp The component to add to this ToAmericaPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit) should be changed so that the
         *            underlying unit is now sailing to America.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    final Unit unit = ((UnitLabel) comp).getUnit();
                    final ClientOptions co = freeColClient.getClientOptions();
                    boolean autoload = co.getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS);
                    if (!autoload
                            && docksPanel.getUnitCount() > 0
                            && unit.getSpaceLeft() > 0) {
                        boolean leaveColonists = parent.showConfirmDialog(
                                "europe.leaveColonists",
                                "yes",
                                "no",
                                "%newWorld%", unit.getOwner().getNewLandName());
                        if (!leaveColonists) {
                            // Remain in Europe.
                            return null;
                        }
                    }
                    comp.getParent().remove(comp);

                    inGameController.moveToAmerica(unit);
                    docksPanel.removeAll();
                    for (Unit u : europe.getUnitList()) {
                        UnitLabel unitLabel = new UnitLabel(u, parent);
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);

                        if (!u.isNaval()) {
                            // If it's not a naval unit, it belongs on the docks.
                            docksPanel.add(unitLabel, false);
                        }
                    }
                    docksPanel.revalidate();
                } else {
                    logger.warning("An invalid component got dropped on this ToAmericaPanel.");
                    return null;
                }
            }
            setSelectedUnitLabel(null);
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }

        public String getUIClassID() {
            return "ToAmericaPanelUI";
        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are going to
     * Europe.
     */
    public final class ToEuropePanel extends JPanel {
        private final EuropePanel europePanel;


        /**
         * Creates this ToEuropePanel.
         * 
         * @param europePanel The panel that holds this ToEuropePanel.
         */
        public ToEuropePanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }

        /**
         * Adds a component to this ToEuropePanel and makes sure that the unit
         * that the component represents gets modified so that it will sail to
         * Europe.
         * 
         * @param comp The component to add to this ToEuropePanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit) should be changed so that the
         *            underlying unit is now sailing to Europe.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel) comp).getUnit();
                    inGameController.moveToEurope(unit);
                } else {
                    logger.warning("An invalid component got dropped on this ToEuropePanel.");
                    return null;
                }
            }
            setSelectedUnitLabel(null);
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }

        public String getUIClassID() {
            return "ToEuropePanelUI";
        }
    }

    /**
     * A panel that holds UnitsLabels that represent naval Units that are
     * waiting in Europe.
     */
    public final class InPortPanel extends JPanel {

        /**
         * Adds a component to this InPortPanel.
         * 
         * @param comp The component to add to this InPortPanel.
         * @return The component argument.
         */
        public Component add(Component comp) {
            return super.add(comp);
        }

        public String getUIClassID() {
            return "EuropeInPortPanelUI";
        }
    }

    /**
     * A panel that holds UnitsLabels that represent Units that are waiting on
     * the docks in Europe.
     */
    public final class DocksPanel extends JPanel {
        private final EuropePanel europePanel;


        /**
         * Creates this DocksPanel.
         * 
         * @param europePanel The panel that holds this DocksPanel.
         */
        public DocksPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }

        /**
         * Adds a component to this DocksPanel and makes sure that the unit that
         * the component represents gets modified so that it will wait on the
         * docks in Europe.
         * 
         * @param comp The component to add to this DocksPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing a Unit) should be changed so that the
         *            underlying unit will wait on the docks in Europe.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    /* Unnecessary, leaveShip() is called in CargoPanel.remove()
                    Unit unit = ((UnitLabel) comp).getUnit();
                    inGameController.leaveShip(unit);
                    */
                } else {
                    logger.warning("An invalid component got dropped on this DocksPanel.");
                    return null;
                }
            }
            Component c = add(comp);
            revalidate();
            europePanel.refresh();
            return c;
        }
        
        public int getUnitCount() {
            int number = 0;
            for (Unit u : europe.getUnitList()) {
                if (!u.isNaval()) {
                    number++;
                }
            }
            return number;
        }

        public String getUIClassID() {
            return "DocksPanelUI";
        }
    }



    /**
     * A panel that shows goods available for purchase in Europe.
     */
    public final class MarketPanel extends JPanel {
        private final EuropePanel europePanel;


        /**
         * Creates this MarketPanel.
         * 
         * @param europePanel The panel that holds this CargoPanel.
         */
        public MarketPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
            setLayout(new GridLayout(2, 8));
        }

        /**
         * If a GoodsLabel is dropped here, sell the goods.
         * 
         * @param comp The component to add to this MarketPanel.
         * @param editState Must be set to 'true' if the state of the component
         *            that is added (which should be a dropped component
         *            representing goods) should be sold.
         * @return The component argument.
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    // comp.getParent().remove(comp);
                    Goods goods = ((GoodsLabel) comp).getGoods();
                    Player player = freeColClient.getMyPlayer();
                    if (player.canTrade(goods)) {
                        inGameController.sellGoods(goods);
                    } else if (parent.showBoycottedGoodsDialog(goods, europe)) {
                        inGameController.payArrears(goods);
                    } else {
                        inGameController.unloadCargo(goods);
                    }
                    europePanel.getCargoPanel().revalidate();
                    revalidate();
                    inGameController.nextModelMessage();
                    europePanel.refresh();

                    // TODO: Make this look prettier :-)
                    UnitLabel t = selectedUnit;
                    selectedUnit = null;
                    setSelectedUnitLabel(t);

                    return comp;
                }

                logger.warning("An invalid component got dropped on this MarketPanel.");
                return null;
            }
            europePanel.refresh();
            return comp;
        }

        public void remove(Component comp) {
            // Don't remove the marketLabel.
        }

        public String getUIClassID() {
            return "MarketPanelUI";
        }
    }
    
    /**
     * To log transactions made in Europe
     */
    public class TransactionLog extends JTextPane implements TransactionListener {
        public TransactionLog() {
            setEditable(false);
        }
        
        private void add(String text) {
            StyledDocument doc = getStyledDocument();
            try {
                if (doc.getLength() > 0) {
                    text = "\n\n" + text;
                }
                doc.insertString(doc.getLength(), text, null);
            } catch(Exception e) {
                logger.warning("Failed to update transaction log: " + e.toString());
            }
        }
        
        public void logPurchase(GoodsType goodsType, int amount, int price) {
            int total = amount * price;
            String text = Messages.message("transaction.purchase",
                    "%goods%", goodsType.getName(),
                    "%amount%", String.valueOf(amount),
                    "%gold%", String.valueOf(price))
                + "\n" + Messages.message("transaction.price",
                    "%gold%", String.valueOf(total));
            add(text);
        }

        public void logSale(GoodsType goodsType, int amount, int price, int tax) {
            int totalBeforeTax = amount * price;
            int totalTax = totalBeforeTax * tax / 100;
            int totalAfterTax = totalBeforeTax - totalTax;
            
            String text = Messages.message("transaction.sale",
                    "%goods%", goodsType.getName(),
                    "%amount%", String.valueOf(amount),
                    "%gold%", String.valueOf(price))
                + "\n" + Messages.message("transaction.price",
                    "%gold%", String.valueOf(totalBeforeTax))
                + "\n" + Messages.message("transaction.tax",
                    "%tax%", String.valueOf(tax),
                    "%gold%", String.valueOf(totalTax))
                + "\n" + Messages.message("transaction.net",
                    "%gold%", String.valueOf(totalAfterTax));
            add(text);
        }
    }


    /**
     * Returns a pointer to the <code>CargoPanel</code>-object in use.
     * 
     * @return The <code>CargoPanel</code>.
     */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
     * Returns a pointer to the <code>MarketPanel</code>-object in use.
     * 
     * @return The <code>MarketPanel</code>.
     */
    public final MarketPanel getMarketPanel() {
        return marketPanel;
    }
}
