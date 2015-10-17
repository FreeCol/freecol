/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController.BoycottAction;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.HighSeas;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.MarketData;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.TransactionListener;
import net.sf.freecol.common.model.Unit;


/**
 * This is a panel for the Europe display.  It shows the ships in Europe and
 * allows the user to send them back.
 */
public final class EuropePanel extends PortPanel {

    private static final Logger logger = Logger.getLogger(EuropePanel.class.getName());

    /**
     * A panel to hold unit labels that represent units that are going
     * to America or Europe.
     */
    private final class DestinationPanel extends JPanel implements DropTarget {

        private Location destination;


        /**
         * Initialize this DestinationPanel.
         */
        public void initialize(Location destination) {
            this.destination = destination;
            update();
        }

        /**
         * Cleans up this DestinationPanel.
         */
        public void cleanup() {}

        /**
         * Update this DestinationPanel.
         */
        public void update() {
            removeAll();

            HighSeas highSeas = getMyPlayer().getHighSeas();
            if (highSeas != null) {
                for (Unit unit : highSeas.getUnitList()) {
                    boolean belongs;
                    if (destination instanceof Europe) {
                        belongs = unit.getDestination() == destination;
                    } else if (destination instanceof Map) {
                        belongs = unit.getDestination() == destination
                            || (unit.getDestination() != null
                                && unit.getDestination().getTile() != null
                                && unit.getDestination().getTile().getMap()
                                == destination);
                    } else {
                        logger.warning("Bogus DestinationPanel location: "
                            + destination
                            + " for unit: " + unit);
                        belongs = false;
                    }
                    if (belongs) {
                        UnitLabel unitLabel
                            = new UnitLabel(getFreeColClient(), unit);
                        unitLabel.setTransferHandler(defaultTransferHandler);
                        unitLabel.addMouseListener(pressListener);
                        add(unitLabel);
                    }
                }
            }

            // "ship" is a tag, not a key
            Utility.localizeBorder(this, Unit.getDestinationLabel("ship",
                    destination, getMyPlayer()));
            revalidate();
        }


        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Unit unit) {
            return unit.isNaval() && !unit.isDamaged();
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Goods goods) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof UnitLabel)) {
                    logger.warning("Invalid component: " + comp);
                    return null;
                }
                final Unit unit = ((UnitLabel)comp).getUnit();

                if (unit.getTradeRoute() != null) {
                    if (!getGUI().confirmClearTradeRoute(unit)
                        || !igc().assignTradeRoute(unit, null)) return null;
                }

                Location dest = destination;
                if (unit.isInEurope()) {
                    dest = getGUI().showSelectDestinationDialog(unit);
                    if (dest == null) return null; // user aborted
                }
                
                final ClientOptions co = getClientOptions();
                if (!co.getBoolean(ClientOptions.AUTOLOAD_EMIGRANTS)
                    && unit.isInEurope()
                    && !(destination instanceof Europe)
                    && docksPanel.getComponentCount() > 0
                    && unit.hasSpaceLeft()) {
                    StringTemplate locName = destination
                        .getLocationLabelFor(unit.getOwner());
                    if (!getGUI().confirm(null, StringTemplate
                            .template("europePanel.leaveColonists")
                            .addStringTemplate("%newWorld%", locName),
                            unit, "ok", "cancel")) return null;
                }

                comp.getParent().remove(comp);
                igc().moveTo(unit, dest);
                inPortPanel.update();
                docksPanel.update();
                cargoPanel.update();
                if (unit == cargoPanel.getCarrier()) {
                    cargoPanel.setCarrier(null);
                }
            }

            Component c = add(comp);
            revalidate();
            EuropePanel.this.refresh();
            return c;
        }

        /**
         * {@inheritDoc}
         */
        public int suggested(GoodsType type) { return -1; } // N/A
    }

    /**
     * A panel that holds UnitLabels that represent Units that are
     * waiting on the docks in Europe.
     */
    public final class DocksPanel extends UnitPanel implements DropTarget {

        public DocksPanel() {
            super(EuropePanel.this, "Europe - docks", true);

            setLayout(new MigLayout("wrap 6"));
        }


        @Override
        public void addPropertyChangeListeners() {
            europe.addPropertyChangeListener(this);
        }

        @Override
        public void removePropertyChangeListeners() {
            europe.removePropertyChangeListener(this);
        }


        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Unit unit) {
            return !unit.isNaval();
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Goods goods) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public Component add(Component comp, boolean editState) {
            Component c = add(comp);
            update();
            return c;
        }

        /**
         * {@inheritDoc}
         */
        public int suggested(GoodsType type) { return -1; } // N/A


        // Override Container

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(Component comp) {
            update();
        }
    }

    private static final class EuropeButton extends JButton {

        public EuropeButton(String text, int keyEvent, String command,
                            ActionListener listener) {
            setOpaque(true);
            setText(text);
            setActionCommand(command);
            addActionListener(listener);
            InputMap closeInputMap = new ComponentInputMap(this);
            closeInputMap.put(KeyStroke.getKeyStroke(keyEvent, 0, false),
                              "pressed");
            closeInputMap.put(KeyStroke.getKeyStroke(keyEvent, 0, true),
                              "released");
            SwingUtilities.replaceUIInputMap(this,
                                             JComponent.WHEN_IN_FOCUSED_WINDOW,
                                             closeInputMap);
        }
    }

    /**
     * A panel that holds unit labels that represent naval units that
     * are waiting in Europe.
     */
    private final class EuropeInPortPanel extends InPortPanel {

        public EuropeInPortPanel() {
            super(EuropePanel.this, "Europe - port", true);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        protected void addPropertyChangeListeners() {
            europe.addPropertyChangeListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void removePropertyChangeListeners() {
            europe.removePropertyChangeListener(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean accepts(Unit unit) {
            if (!unit.isNaval()) return false;
            switch (unit.getState()) {
            case ACTIVE: case FORTIFIED: case FORTIFYING:
            case SENTRY: case SKIPPED:
                return true;
            }
            return false;
        }
    }

    /**
     * A panel that shows goods available for purchase in Europe.
     */
    private final class MarketPanel extends JPanel implements DropTarget {

        /**
         * Creates this MarketPanel.
         *
         * @param europePanel The panel that holds this CargoPanel.
         */
        public MarketPanel(EuropePanel europePanel) {
            super(new GridLayout(2, 8));
        }


        /**
         * Initialize this MarketPanel.
         */
        public void initialize() {
            removeAll();

            final Market market = getMyPlayer().getMarket();
            ImageLibrary lib = getImageLibrary();
            for (GoodsType goodsType : getSpecification().getStorableGoodsTypeList()) {
                MarketLabel label = new MarketLabel(lib, goodsType, market);
                label.setTransferHandler(defaultTransferHandler);
                label.addMouseListener(pressListener);
                MarketData md = market.getMarketData(goodsType);
                if (md != null) md.addPropertyChangeListener(label);
                add(label);
            }
        }

        /**
         * Cleans up this MarketPanel.
         */
        public void cleanup() {}


        // Interface DropTarget

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Unit unit) {
            return false;
        }

        /**
         * {@inheritDoc}
         */
        public boolean accepts(Goods goods) {
            return true;
        }

        /**
         * {@inheritDoc}
         */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (!(comp instanceof GoodsLabel)) {
                    logger.warning("Invalid component: " + comp);
                    return null;
                }

                Goods goods = ((GoodsLabel)comp).getGoods();
                if (getMyPlayer().canTrade(goods.getType())) {
                    igc().sellGoods(goods);
                } else {
                    BoycottAction act = getGUI()
                        .getBoycottChoice(goods, europe);
                    if (act != null) {
                        switch (act) {
                        case PAY_ARREARS:
                            igc().payArrears(goods.getType());
                            break;
                        case DUMP_CARGO:
                            igc().unloadCargo(goods, true);
                            break;
                        default:
                            logger.warning("showBoycottedGoodsDialog fail: "
                                + act);
                            break;
                        }
                    }
                }
                cargoPanel.revalidate();
                revalidate();
                igc().nextModelMessage();
            }
            EuropePanel.this.refresh();
            return comp;
        }

        /**
         * {@inheritDoc}
         */
        public int suggested(GoodsType type) {
            return -1; // No good choice
        }


        // Override Container

        /**
         * {@inheritDoc}
         */
        @Override
        public void remove(Component comp) {
            // Don't remove market labels.
        }
    }

    /**
     * To log transactions made in Europe
     */
    private final class TransactionLog extends JTextPane
        implements TransactionListener {

        /**
         * Creates a transaction log.
         */
        public TransactionLog() {
            setEditable(false);
        }


        /**
         * Initializes this TransactionLog.
         */
        public void initialize() {
            getMyPlayer().getMarket().addTransactionListener(this);
            setText("");
        }

        /**
         * Cleans up this TransactionLog.
         */
        public void cleanup() {
            getMyPlayer().getMarket().removeTransactionListener(this);
        }

        /**
         * Add text to the transaction log.
         *
         * @param text The text to add.
         */
        private void add(String text) {
            StyledDocument doc = getStyledDocument();
            try {
                if (doc.getLength() > 0) text = "\n\n" + text;
                doc.insertString(doc.getLength(), text, null);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Transaction log update failure", e);
            }
        }

        // Implement TransactionListener

        /**
         * {@inheritDoc}
         */
        @Override
        public void logPurchase(GoodsType goodsType, int amount, int price) {
            int total = amount * price;
            StringTemplate t1 = StringTemplate.template("europePanel.transaction.purchase")
                .addNamed("%goods%", goodsType)
                .addAmount("%amount%", amount)
                .addAmount("%gold%", price);
            StringTemplate t2 = StringTemplate.template("europePanel.transaction.price")
                .addAmount("%gold%", total);
            add(Messages.message(t1) + "\n" + Messages.message(t2));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void logSale(GoodsType goodsType, int amount,
                            int price, int tax) {
            int totalBeforeTax = amount * price;
            int totalTax = totalBeforeTax * tax / 100;
            int totalAfterTax = totalBeforeTax - totalTax;

            StringTemplate t1 = StringTemplate.template("europePanel.transaction.sale")
                .addNamed("%goods%", goodsType)
                .addAmount("%amount%", amount)
                .addAmount("%gold%", price);
            StringTemplate t2 = StringTemplate.template("europePanel.transaction.price")
                .addAmount("%gold%", totalBeforeTax);
            StringTemplate t3 = StringTemplate.template("europePanel.transaction.tax")
                .addAmount("%tax%", tax)
                .addAmount("%gold%", totalTax);
            StringTemplate t4 = StringTemplate.template("europePanel.transaction.net")
                .addAmount("%gold%", totalAfterTax);
            add(Messages.message(t1) + "\n" + Messages.message(t2)
                + "\n" + Messages.message(t3) + "\n" + Messages.message(t4));
        }
    }


    public static enum EuropeAction {
        EXIT,
        RECRUIT,
        PURCHASE,
        TRAIN,
        UNLOAD,
        SAIL
    }

    private DestinationPanel toAmericaPanel;

    private DestinationPanel toEuropePanel;

    private DocksPanel docksPanel;

    private MarketPanel marketPanel;

    private TransactionLog log;

    private JButton exitButton, trainButton, purchaseButton,
                    recruitButton, unloadButton, sailButton;

    private final Europe europe;


    /**
     * The constructor for a EuropePanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param header True when a header should be added.
     */
    public EuropePanel(FreeColClient freeColClient, boolean header) {
        super(freeColClient, new MigLayout("wrap 3, fill",
                                           "[30%:][30%:][15%:]"));

        exitButton = new EuropeButton(Messages.message("close"),
            KeyEvent.VK_ESCAPE, EuropeAction.EXIT.toString(), this);
        trainButton = new EuropeButton(Messages.message("train"),
            KeyEvent.VK_T, EuropeAction.TRAIN.toString(), this);
        purchaseButton = new EuropeButton(Messages.message("purchase"),
            KeyEvent.VK_P, EuropeAction.PURCHASE.toString(), this);
        recruitButton = new EuropeButton(Messages.message("recruit"),
            KeyEvent.VK_R, EuropeAction.RECRUIT.toString(), this);
        unloadButton = new EuropeButton(Messages.message("unload"),
            KeyEvent.VK_U, EuropeAction.UNLOAD.toString(), this);
        sailButton = new EuropeButton(Messages.message("setSail"),
            KeyEvent.VK_S, EuropeAction.SAIL.toString(), this);

        toAmericaPanel = new DestinationPanel();
        toEuropePanel = new DestinationPanel();
        inPortPanel = new EuropeInPortPanel();
        cargoPanel = new CargoPanel(freeColClient, true);
        docksPanel = new DocksPanel();
        marketPanel = new MarketPanel(this);
        log = new TransactionLog();
        europe = freeColClient.getMyPlayer().getEurope();

        SimpleAttributeSet attributes = new SimpleAttributeSet();
        StyleConstants.setAlignment(attributes, StyleConstants.ALIGN_RIGHT);
        //StyleConstants.setForeground(attributes, Color.WHITE);
        StyleConstants.setBold(attributes, true);
        log.setParagraphAttributes(attributes, true);

        defaultTransferHandler
            = new DefaultTransferHandler(freeColClient, this);
        toAmericaPanel.setTransferHandler(defaultTransferHandler);
        toEuropePanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        docksPanel.setTransferHandler(defaultTransferHandler);
        marketPanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(freeColClient, this);
        MouseListener releaseListener = new DropListener();
        toAmericaPanel.addMouseListener(releaseListener);
        toEuropePanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        docksPanel.addMouseListener(releaseListener);
        marketPanel.addMouseListener(releaseListener);

        toAmericaPanel.setLayout(new GridLayout(1, 0));
        toEuropePanel.setLayout(new GridLayout(1, 0));
        inPortPanel.setLayout(new GridLayout(1, 0));
        cargoPanel.setLayout(new GridLayout(1, 0));
        docksPanel.setLayout(new GridLayout(0, 5));

        JScrollPane toAmericaScroll = new JScrollPane(toAmericaPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toAmericaScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane toEuropeScroll = new JScrollPane(toEuropePanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toEuropeScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane inPortScroll = new JScrollPane(inPortPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        inPortScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane cargoScroll = new JScrollPane(cargoPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cargoScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane docksScroll = new JScrollPane(docksPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        docksScroll.getVerticalScrollBar().setUnitIncrement(16);
        JScrollPane marketScroll = new JScrollPane(marketPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane logScroll = new JScrollPane(log,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.getVerticalScrollBar().setUnitIncrement(16);

        toAmericaPanel.setBorder(Utility.localizedBorder("sailingToAmerica"));
        toEuropePanel.setBorder(Utility.localizedBorder("sailingToEurope"));
        docksPanel.setBorder(Utility.localizedBorder("docks"));
        inPortPanel.setBorder(Utility.localizedBorder("inPort"));
        marketPanel.setBorder(Utility.blankBorder(10, 10, 10, 10));
        log.setBorder(Utility.localizedBorder("sales"));

        toAmericaScroll.getViewport().setOpaque(false);
        toAmericaPanel.setOpaque(false);
        toEuropeScroll.getViewport().setOpaque(false);
        toEuropePanel.setOpaque(false);
        inPortScroll.getViewport().setOpaque(false);
        inPortPanel.setOpaque(false);
        cargoScroll.getViewport().setOpaque(false);
        cargoPanel.setOpaque(false);
        docksScroll.getViewport().setOpaque(false);
        docksPanel.setOpaque(false);
        marketScroll.getViewport().setOpaque(false);
        marketPanel.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        log.setOpaque(false);

        initialize(europe);

        if(header) {
            add(Utility.localizedHeader(europe.getNameKey(), false),
                "span, top, center");
        }
        add(toAmericaScroll, "sg, height 15%:, grow");
        add(toEuropeScroll, "sg, height 15%:, grow");
        add(logScroll, "spany 3, grow");
        add(inPortScroll, "sg, height 15%:, grow");
        add(docksScroll, "spany 2, grow");
        add(cargoScroll, "height 10%:, grow");
        add(marketScroll, "span, height 10%:, grow");

        add(recruitButton, "span, split 6");
        add(purchaseButton);
        add(trainButton);
        add(unloadButton);
        add(sailButton);
        add(exitButton, "tag ok");

        setSelectedUnitLabel(null);

        float scale = getImageLibrary().getScaleFactor();
        getGUI().restoreSavedSize(this, 200 + (int)(scale*850), 200 + (int)(scale*525));
    }

    /**
     * Initialize this EuropePanel.
     *
     * @param europe The <code>Europe</code> this panel should display.
     */
    private void initialize(Europe europe) {
        // Initialize the subpanels.
        toAmericaPanel.initialize(europe.getGame().getMap());
        toEuropePanel.initialize(europe);
        // Initialize cargoPanel *before* inPortPanel calls setSelectedUnit().
        cargoPanel.initialize();
        inPortPanel.initialize();
        marketPanel.initialize();
        docksPanel.initialize();
        log.initialize();
    }

    /**
     * Cleans up this EuropePanel.
     */
    public void cleanup() {
        log.cleanup();
        docksPanel.cleanup();
        marketPanel.cleanup();
        inPortPanel.cleanup();
        cargoPanel.cleanup();
        toEuropePanel.cleanup();
        toAmericaPanel.cleanup();
    }

    /**
     * What to do when requesting focus.
     */
    @Override
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
     * Selects a unit that is located somewhere on this panel.
     *
     * @param unitLabel The <code>UnitLabel</code> for the unit that
     *     is being selected.
     */
    @Override
    public void setSelectedUnitLabel(UnitLabel unitLabel) {
        if (selectedUnitLabel != unitLabel) {
            if (selectedUnitLabel != null) {
                selectedUnitLabel.setSelected(false);
            }
            selectedUnitLabel = unitLabel;
            if (unitLabel == null) {
                cargoPanel.setCarrier(null);
            } else {
                cargoPanel.setCarrier(unitLabel.getUnit());
                unitLabel.setSelected(true);
            }
        }
        inPortPanel.revalidate();
        inPortPanel.repaint();
    }

    /**
     * Exits this EuropePanel.
     */
    private void exitAction() {
        cleanup();
        getGUI().removeFromCanvas(this);
        igc().nextModelMessage();
    }

    /**
     * Unload the contents of the currently selected carrier.
     */
    private void unloadAction() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Iterator<Goods> goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = goodsIterator.next();
                if (getMyPlayer().canTrade(goods.getType())) {
                    igc().sellGoods(goods);
                } else {
                    igc().payArrears(goods.getType());
                }
            }
            Iterator<Unit> unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = unitIterator.next();
                igc().leaveShip(newUnit);
            }
            cargoPanel.update();
            docksPanel.update();
        }
        requestFocus();
    }

    /**
     * A unit sets sail for the new world.
     */
    private void sailAction() {
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isNaval()) {
            UnitLabel unitLabel = getSelectedUnitLabel();
            toAmericaPanel.add(unitLabel, true);
        }
        requestFocus();
    }


    // Interface PortPanel

    /**
     * Get the units in Europe.
     *
     * @return A list of units in Europe.
     */
    @Override
    public List<Unit> getUnitList() {
        return europe.getUnitList();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        final String command = ae.getActionCommand();
        EuropeAction act = EuropeAction.valueOf(command);
        switch (act) {
        case EXIT:
            exitAction();
            break;
        case PURCHASE:
            getGUI().showPurchasePanel();
            break;
        case RECRUIT:
            getGUI().showRecruitPanel();
            break;
        case SAIL:
            sailAction();
            break;
        case TRAIN:
            getGUI().showTrainPanel();
            break;
        case UNLOAD:
            unloadAction();
            break;
        default:
            super.actionPerformed(ae);
            break;
        }
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        removeAll();
        toAmericaPanel = null;
        toEuropePanel = null;
        docksPanel = null;
        marketPanel = null;
        log = null;
        exitButton = trainButton = purchaseButton = recruitButton
            = unloadButton = sailButton = null;
    }
}
