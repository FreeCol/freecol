
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.MessageLabel;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;


/**
 * This is a panel for the Europe display. It shows the ships in Europe and
 * allows the user to send them back.
 */
public final class EuropePanel extends JLayeredPane implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(EuropePanel.class.getName());

    private static final int    EXIT = 0,
                                RECRUIT = 1,
                                PURCHASE = 2,
                                TRAIN = 3,
                                UNLOAD = 4;

    private static final int TITLE_FONT_SIZE = 12;
    
    private final Canvas  parent;
    private final FreeColClient freeColClient;
    private InGameController inGameController;

    private final JLabel                    cargoLabel;
    private final JLabel                    toAmericaLabel;
    private final ToAmericaPanel            toAmericaPanel;
    private final ToEuropePanel             toEuropePanel;
    private final InPortPanel               inPortPanel;
    private final DocksPanel                docksPanel;
    private final CargoPanel                cargoPanel;
    private final MarketPanel               marketPanel;
    private final RecruitDialog             recruitDialog;
    private final PurchaseDialog            purchaseDialog;
    private final TrainDialog               trainDialog;
    private final DefaultTransferHandler    defaultTransferHandler;
    private final MouseListener             pressListener;

    private Europe      europe;
    private Game        game;
    private UnitLabel   selectedUnit;

    private JButton exitButton;
    




    /**
     * The constructor for the panel.
     * @param parent The parent of this panel
     * @param freeColClient The main controller object for the client.
     * @param inGameController The controller object to be used when
     *      ingame.
     */
    public EuropePanel(Canvas parent, FreeColClient freeColClient, InGameController inGameController) {
        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = inGameController;

        setFocusCycleRoot(true);
        setSize(parent.getWidth(), parent.getHeight() - parent.getMenuBarHeight());

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
        cargoPanel = new CargoPanel(this);
        marketPanel = new MarketPanel(this);
        recruitDialog = new RecruitDialog();
        purchaseDialog = new PurchaseDialog();
        trainDialog = new TrainDialog();
        
        toAmericaPanel.setBackground(Color.WHITE);
        toEuropePanel.setBackground(Color.WHITE);
        inPortPanel.setBackground(Color.WHITE);
        docksPanel.setBackground(Color.WHITE);
        cargoPanel.setBackground(Color.WHITE);

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
        cargoPanel.addMouseListener(releaseListener);
        marketPanel.addMouseListener(releaseListener);

        toAmericaPanel.setLayout(new GridLayout(0 , 2));
        toEuropePanel.setLayout(new GridLayout(0 , 2));
        inPortPanel.setLayout(new GridLayout(0 , 2));
        docksPanel.setLayout(new GridLayout(0 , 2));
        cargoPanel.setLayout(new GridLayout(1 , 0));

        JScrollPane toAmericaScroll = new JScrollPane(toAmericaPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                    toEuropeScroll = new JScrollPane(toEuropePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                    inPortScroll = new JScrollPane(inPortPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                    docksScroll = new JScrollPane(docksPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                    cargoScroll = new JScrollPane(cargoPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    marketScroll = new JScrollPane(marketPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);                
                
        JLabel  toEuropeLabel = new JLabel(),
                inPortLabel = new JLabel(),
                docksLabel = new JLabel();
        toAmericaLabel = new JLabel();
        cargoLabel = new JLabel(); //"<html><strike>Cargo</strike></html>");
        
        ImageIcon toEuropeImage = new ImageIcon(freeColClient.getGUI().createStringImage(toEuropeLabel, Messages.message("goingToEurope"), Color.WHITE, 200, TITLE_FONT_SIZE));
        toEuropeLabel.setIcon(toEuropeImage);
        ImageIcon inPortImage = new ImageIcon(freeColClient.getGUI().createStringImage(inPortLabel, Messages.message("inPort"), Color.WHITE, 200, TITLE_FONT_SIZE));
        inPortLabel.setIcon(inPortImage);
        ImageIcon docksImage = new ImageIcon(freeColClient.getGUI().createStringImage(docksLabel, Messages.message("docks"), Color.WHITE, 200, TITLE_FONT_SIZE));
        docksLabel.setIcon(docksImage);
        ImageIcon toAmericaImage = new ImageIcon(freeColClient.getGUI().createStringImage(toAmericaLabel, Messages.message("goingToAmerica"), Color.WHITE, 200, TITLE_FONT_SIZE));
        toAmericaLabel.setIcon(toAmericaImage);
        ImageIcon cargoImage = new ImageIcon(freeColClient.getGUI().createStringImage(cargoLabel, Messages.message("cargo"), Color.WHITE, 400, TITLE_FONT_SIZE));
        cargoLabel.setIcon(cargoImage);        
        
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
        recruitButton.setOpaque(true);
        purchaseButton.setOpaque(true);
        trainButton.setOpaque(true);
        exitButton.setOpaque(true);
        unloadButton.setOpaque(true);
        
        final int SPACE = 30;
        final int SPACE_BUTTON = 10;
        
        marketScroll.setSize(640, 114);
        toAmericaScroll.setSize(315, 192);
        toEuropeScroll.setSize(toAmericaScroll.getSize());        
        inPortScroll.setSize(430, 96);
        cargoScroll.setSize(inPortScroll.getSize());
        docksScroll.setSize(marketScroll.getWidth() - inPortScroll.getWidth() - SPACE, cargoScroll.getHeight() + inPortScroll.getHeight() + SPACE);        
        exitButton.setSize(110, 20);
        recruitButton.setSize(100, 20);
        purchaseButton.setSize(100, 20);
        trainButton.setSize(100, 20);
        unloadButton.setSize(100, 20);
        toAmericaLabel.setSize(200, 20);
        toEuropeLabel.setSize(200, 20);
        inPortLabel.setSize(200, 20);
        docksLabel.setSize(200, 20);
        cargoLabel.setSize(410, 20);

        int totalHeight = toEuropeScroll.getHeight() + docksScroll.getHeight() + marketScroll.getHeight() + 4 * SPACE;
        int spaceTop = Math.max(0, (getHeight() - totalHeight) / 2);
        int spaceLeft = Math.max(0, (getWidth() - marketScroll.getWidth()) / 2);
        int spaceRight = spaceLeft;
        int spaceBottom = spaceTop;
        
        toAmericaScroll.setLocation(spaceLeft, spaceTop);
        toAmericaLabel.setLocation(spaceLeft, spaceTop - 20);
        toEuropeScroll.setLocation(getWidth() - spaceRight - toEuropeScroll.getWidth(), spaceTop);
        toEuropeLabel.setLocation(getWidth() - spaceRight - toEuropeScroll.getWidth(), spaceTop - 20);
        inPortScroll.setLocation(spaceLeft, spaceTop + toAmericaScroll.getHeight() + SPACE);
        inPortLabel.setLocation(spaceLeft, inPortScroll.getY() - 20);   
        docksScroll.setLocation(getWidth() - spaceRight - docksScroll.getWidth(), spaceTop + toEuropeScroll.getHeight() + SPACE);
        docksLabel.setLocation(getWidth() - spaceRight - docksScroll.getWidth(), docksScroll.getY() - 20);
        marketScroll.setLocation(spaceLeft, docksScroll.getY() + docksScroll.getHeight() + SPACE);
        cargoScroll.setLocation(spaceLeft, spaceTop + toAmericaScroll.getHeight() + inPortScroll.getHeight() + 2 * SPACE);
        cargoLabel.setLocation(spaceLeft, cargoScroll.getY() - 20);
        
        int spaceTopButtons = spaceTop + (toAmericaScroll.getHeight() - (recruitButton.getHeight() + purchaseButton.getHeight() + trainButton.getHeight() + 2 * SPACE_BUTTON)) / 2;
        recruitButton.setLocation((spaceLeft - recruitButton.getWidth()) / 2, spaceTopButtons);
        purchaseButton.setLocation((spaceLeft - purchaseButton.getWidth()) / 2, spaceTopButtons + recruitButton.getHeight() + SPACE_BUTTON);
        trainButton.setLocation((spaceLeft - trainButton.getWidth()) / 2, purchaseButton.getY() + purchaseButton.getHeight() + SPACE_BUTTON);
        unloadButton.setLocation((spaceLeft - unloadButton.getWidth()) / 2, trainButton.getY() + trainButton.getHeight() + SPACE_BUTTON);
                                
        exitButton.setLocation((marketScroll.getWidth() + marketScroll.getX() + getWidth() - exitButton.getWidth()) / 2, marketScroll.getY() + (marketScroll.getHeight() - exitButton.getHeight()) / 2);                
    
        setLayout(null);

        exitButton.setActionCommand(String.valueOf(EXIT));
        recruitButton.setActionCommand(String.valueOf(RECRUIT));
        purchaseButton.setActionCommand(String.valueOf(PURCHASE));
        trainButton.setActionCommand(String.valueOf(TRAIN));
        unloadButton.setActionCommand(String.valueOf(UNLOAD));

        exitButton.addActionListener(this);
        recruitButton.addActionListener(this);
        purchaseButton.addActionListener(this);
        trainButton.addActionListener(this);
        unloadButton.addActionListener(this);

        add(exitButton);
        add(recruitButton);
        add(purchaseButton);
        add(trainButton);
        add(unloadButton);
        add(toAmericaScroll);
        add(toEuropeScroll);
        add(inPortScroll);
        add(docksScroll);
        add(cargoScroll);
        add(marketScroll);
        add(toAmericaLabel);
        add(toEuropeLabel);
        add(inPortLabel);
        add(docksLabel);
        add(cargoLabel);

        setBorder(null);

        selectedUnit = null;
        
        // See the message of Ulf Onnen for more information about the presence of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});
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
    * @param g The graphics context in which to paint.
    */
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

        Image bgImage = (Image) UIManager.get("EuropeBackgroundImage");
        if (bgImage != null) {
            if (bgImage.getWidth(null) != getWidth() || bgImage.getHeight(null) != getHeight()) {
                bgImage = bgImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                UIManager.put("EuropeBackgroundImage", bgImage);

                /*
                  We have to use a MediaTracker to ensure that the
                  image has been scaled before we paint it.
                */
                MediaTracker mt = new MediaTracker(freeColClient.getCanvas());
                mt.addImage(bgImage, 0, getWidth(), getHeight());

                try {
                    mt.waitForID(0);
                } catch (InterruptedException e) {
                    g.setColor(Color.black);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    return;
                }

            }

            g.drawImage(bgImage, 0, 0, null);
        } else {
            Image tempImage = (Image) UIManager.get("BackgroundImage");

            if (tempImage != null) {
                for (int x=0; x<width; x+=tempImage.getWidth(null)) {
                    for (int y=0; y<height; y+=tempImage.getHeight(null)) {
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
    * Refreshes the components on this panel that need to be refreshed after the user
    * has recruited a new unit.
    */
    public void refreshBuyRecruit() {
        docksPanel.removeAll();

        Iterator unitIterator = europe.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
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
    * Refreshes the components on this panel that need to be refreshed after the user
    * has purchased a new unit.
    *
    * @param type The type of unit that was just purchased. This is needed to know which
    *             component needs to be refreshed.
    */
    public void refreshBuyPurchase(int type) {
        if (type == Unit.ARTILLERY) {
            refreshBuyRecruit();
        } else {
            inPortPanel.removeAll();

            Iterator unitIterator = europe.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();

                if ((unit.getState() == Unit.ACTIVE) && (unit.isNaval())) {
                    UnitLabel unitLabel = new UnitLabel(unit, parent);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);

                    inPortPanel.add(unitLabel);
                }
            }

            // Only one component will be repainted!
            inPortPanel.repaint(0, 0, inPortPanel.getWidth(), inPortPanel.getHeight());
        }
    }


    /**
     * Initialize the data on the window.
     * 
     * @param europe The object of type <code>Europe</code> this panel
     *      should display.
     * @param game The <code>Game</code>-object the
     *      <code>Europe</code>-object is a part of.
     */
    public void initialize(Europe europe, Game game) {
        this.europe = europe;
        this.game = game;

        //
        // Remove the old components from the panels.
        //

        toAmericaPanel.removeAll();
        toEuropePanel.removeAll();
        inPortPanel.removeAll();
        cargoPanel.removeAll();
        marketPanel.removeAll();
        docksPanel.removeAll();

        //
        // Place new components on the panels.
        //

        UnitLabel carrier = null;
        Iterator unitIterator = europe.getUnitIterator();
        while (unitIterator.hasNext()) {
            Unit unit = (Unit) unitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
                docksPanel.add(unitLabel, false);
            } else if (unit.getState() == Unit.ACTIVE) {
                carrier = unitLabel;
                inPortPanel.add(unitLabel);
            } else if (unit.getState() == Unit.TO_EUROPE) {
                toEuropePanel.add(unitLabel, false);
            } else if (unit.getState() == Unit.TO_AMERICA) {
                toAmericaPanel.add(unitLabel, false);
            }
        }

        for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
            MarketLabel marketLabel = new MarketLabel(i, game.getMarket(), parent);
            marketLabel.setTransferHandler(defaultTransferHandler);
            marketLabel.addMouseListener(pressListener);
            ((JPanel)marketPanel).add(marketLabel);
        }

        setSelectedUnitLabel(carrier);

        if (freeColClient.getMyPlayer() != null) {
            String newLandName = freeColClient.getMyPlayer().getNewLandName();
            if (newLandName == null) {
                newLandName = freeColClient.getMyPlayer().getDefaultNewLandName();
            }
            ImageIcon toAmericaImage = new ImageIcon(freeColClient.getGUI().createStringImage(toAmericaLabel, Messages.message("goingTo") + " " + newLandName, Color.WHITE, 200, TITLE_FONT_SIZE));
            toAmericaLabel.setIcon(toAmericaImage);
        }
    }

    /**
    * Reinitializes the panel, but keeps the currently selected unit.
    */
    public void reinitialize() {
        final UnitLabel selectedUnit = this.selectedUnit;
        initialize(europe, game);
        setSelectedUnit(selectedUnit.getUnit());
    }

    
    /**
    * Selects a unit that is located somewhere on this panel.
    * @param unit The unit that is being selected.
    */
    public void setSelectedUnit(Unit unit) {
        Component[] components = inPortPanel.getComponents();
        for (int i=0; i<components.length; i++) {
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
        if (selectedUnit != unitLabel) {
            if (selectedUnit != null) {
                selectedUnit.setSelected(false);
            }
            selectedUnit = unitLabel;
            updateCargoPanel();
            updateCargoLabel();
        }
        cargoPanel.revalidate();
        refresh();
    }


    private void updateCargoPanel () {
        cargoPanel.removeAll();

        if (selectedUnit != null) {
            selectedUnit.setSelected(true);
            Unit selUnit = selectedUnit.getUnit();

            Iterator unitIterator = selUnit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = (Unit) unitIterator.next();

                UnitLabel label = new UnitLabel(unit, parent);
                label.setTransferHandler(defaultTransferHandler);
                label.addMouseListener(pressListener);

                cargoPanel.add(label, false);
            }

            Iterator goodsIterator = selUnit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods g = (Goods) goodsIterator.next();

                GoodsLabel label = new GoodsLabel(g, parent);
                label.setTransferHandler(defaultTransferHandler);
                label.addMouseListener(pressListener);

                cargoPanel.add(label, false);
            }
        }

    }


    /**
    * Updates the label that is placed above the cargo panel. It shows the name
    * of the unit whose cargo is displayed and the amount of space left on that unit.
    */
    private void updateCargoLabel() {
        if (selectedUnit != null) {
            ImageIcon cargoImage = new ImageIcon(freeColClient.getGUI().createStringImage(cargoLabel, Messages.message("cargo") + " (" + selectedUnit.getUnit().getName() + ") " + Messages.message("spaceLeft") + ": " + selectedUnit.getUnit().getSpaceLeft(), Color.WHITE, 200, TITLE_FONT_SIZE));
            cargoLabel.setIcon(cargoImage);  
        } else {
            ImageIcon cargoImage = new ImageIcon(freeColClient.getGUI().createStringImage(cargoLabel, Messages.message("cargo"), Color.WHITE, 200, TITLE_FONT_SIZE));
            cargoLabel.setIcon(cargoImage); 
        }
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
    * @return The currently select unit.
    */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnit;
    }



    private void unload() { 
        Unit unit = getSelectedUnit();
        if (unit != null && unit.isCarrier()) {
            Player player = freeColClient.getMyPlayer();
            Iterator goodsIterator = unit.getGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = (Goods) goodsIterator.next();
                if (freeColClient.getMyPlayer().canTrade(goods)) {
                    inGameController.sellGoods(goods);
                    updateCargoLabel();
                    updateCargoPanel();
                } else {
                    inGameController.payArrears(goods);
                }
                getCargoPanel().revalidate();
                refresh();
            }
            Iterator unitIterator = unit.getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit newUnit = (Unit) unitIterator.next();
                inGameController.leaveShip(newUnit);
                updateCargoLabel();
                updateCargoPanel();
                getCargoPanel().revalidate();
                // update docks panel
                refreshBuyRecruit();
                docksPanel.revalidate();
                refresh();
            }
        }
    }

    /**
     * Analyzes an event and calls the right external methods to take
     * care of the user's request.
     *
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case EXIT:
                    parent.remove(this);
                    freeColClient.getInGameController().nextModelMessage();
                    break;
                case RECRUIT:
                    showRecruitDialog();
                    break;
                case PURCHASE:
                    showPurchaseDialog();
                    break;
                case TRAIN:
                    showTrainDialog();
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
    * A panel that holds UnitsLabels that represent Units that are
    * going to America.
    */
    public final class ToAmericaPanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this ToAmericaPanel.
        * @param europePanel The panel that holds this ToAmericaPanel.
        */
        public ToAmericaPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }




        /**
        * Adds a component to this ToAmericaPanel and makes sure that the unit
        * that the component represents gets modified so that it will sail to
        * America.
        * @param comp The component to add to this ToAmericaPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit is now sailing to America.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.moveToAmerica(unit);
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
    * A panel that holds UnitsLabels that represent Units that are
    * going to Europe.
    */
    public final class ToEuropePanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this ToEuropePanel.
        * @param europePanel The panel that holds this ToEuropePanel.
        */
        public ToEuropePanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }



        /**
        * Adds a component to this ToEuropePanel and makes sure that the unit
        * that the component represents gets modified so that it will sail to
        * Europe.
        * @param comp The component to add to this ToEuropePanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit is now sailing to Europe.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
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
    * A panel that holds UnitsLabels that represent Units that are
    * waiting on the docks in Europe.
    */
    public final class DocksPanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this DocksPanel.
        * @param europePanel The panel that holds this DocksPanel.
        */
        public DocksPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }



        /**
        * Adds a component to this DocksPanel and makes sure that the unit
        * that the component represents gets modified so that it will wait
        * on the docks in Europe.
        *
        * @param comp The component to add to this DocksPanel.
        * @param editState Must be set to 'true' if the state of the component
        *                  that is added (which should be a dropped component
        *                  representing a Unit) should be changed so that the
        *                  underlying unit will wait on the docks in Europe.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof UnitLabel) {
                    comp.getParent().remove(comp);
                    Unit unit = ((UnitLabel)comp).getUnit();
                    inGameController.leaveShip(unit);
                } else {
                    logger.warning("An invalid component got dropped on this DocksPanel.");
                    return null;
                }
            }
            updateCargoLabel();
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }

        public String getUIClassID() {
            return "DocksPanelUI";
        }
    }





    /**
    * A panel that holds units and goods that represent Units and cargo that are
    * on board the currently selected ship.
    */
    public final class CargoPanel extends JPanel {
        private final EuropePanel europePanel;



        /**
        * Creates this CargoPanel.
        * @param europePanel The panel that holds this CargoPanel.
        */
        public CargoPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
        }



        /**
        * Adds a component to this CargoPanel and makes sure that the unit
        * or good that the component represents gets modified so that it is
        * on board the currently selected ship.
        * @param comp The component to add to this CargoPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit or
        * good) should be changed so that the underlying unit or goods are
        * on board the currently selected ship.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (selectedUnit == null) {
                return null;
            }

            if (editState) {
                Container oldParent = comp.getParent();

                if (comp instanceof UnitLabel) {
                    Unit unit = ((UnitLabel)comp).getUnit();
                    if (selectedUnit.getUnit().getSpaceLeft() > 0) {
                        inGameController.boardShip(unit, selectedUnit.getUnit());

                        if (comp.getParent() != null) {
                            comp.getParent().remove(comp);
                        }
                    } else {
                        return null;
                    }
                } else if (comp instanceof GoodsLabel) {
                    Goods g = ((GoodsLabel)comp).getGoods();

                    Unit carrier = getSelectedUnit();
                    int newAmount = g.getAmount();
                    if (carrier.getSpaceLeft() == 0 && carrier.getGoodsContainer().getGoodsCount(g.getType()) % 100 + g.getAmount() > 100) {
                        newAmount = 100 - carrier.getGoodsContainer().getGoodsCount(g.getType()) % 100;
                    } else if (g.getAmount() > 100) {
                        newAmount = 100;
                    }

                    if (newAmount == 0) {
                        return null;
                    }

                    if (g.getAmount() != newAmount) {
                        g.setAmount(g.getAmount() - newAmount);
                        g = new Goods(game, g.getLocation(), g.getType(), newAmount);
                    } else {
                        if (oldParent != null) {
                            oldParent.remove(comp);
                        }
                    }
                    
                    if (!selectedUnit.getUnit().canAdd(g)) {
                        return null;
                    }

                    inGameController.loadCargo(g, selectedUnit.getUnit());

                    // TODO: Make this look prettier :-)
                    UnitLabel t = selectedUnit;
                    selectedUnit = null;
                    setSelectedUnitLabel(t);
                    //reinitialize();

                    return comp;
                } else if (comp instanceof MarketLabel) {
                    MarketLabel label = (MarketLabel) comp;
                    if (freeColClient.getMyPlayer().canTrade(label.getType())) {
                        inGameController.buyGoods(label.getType(),
                                                  label.getAmount(),
                                                  selectedUnit.getUnit());

                        updateCargoLabel();

                        // TODO: Make this look prettier :-)
                        UnitLabel t = selectedUnit;
                        selectedUnit = null;
                        setSelectedUnitLabel(t);

                        europePanel.getMarketPanel().revalidate();
                        revalidate();
                        europePanel.refresh();
                        return comp;
                    }

                    inGameController.payArrears(label.getType());
                    return null;
                } else {
                    logger.warning("An invalid component got dropped on this CargoPanel.");
                    return null;
                }
            }

            updateCargoLabel();
            Component c = add(comp);
            europePanel.refresh();
            return c;
        }

        public boolean isActive() {
            return (getSelectedUnit() != null);
        }


        public String getUIClassID() {
            return "EuropeCargoPanelUI";
        }
    }


    /**
    * A panel that shows goods available for purchase in Europe.
    */
    public final class MarketPanel extends JPanel {
        private final EuropePanel europePanel;


        /**
        * Creates this MarketPanel.
        * @param europePanel The panel that holds this CargoPanel.
        */
        public MarketPanel(EuropePanel europePanel) {
            this.europePanel = europePanel;
            setLayout(new GridLayout(2,8));
        }


        /**
        * If a GoodsLabel is dropped here, sell the goods.
        * @param comp The component to add to this MarketPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing goods)
        * should be sold.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    //comp.getParent().remove(comp);
                    Goods goods = ((GoodsLabel) comp).getGoods();
                    Player player = freeColClient.getMyPlayer();
                    if (freeColClient.getMyPlayer().canTrade(goods)) {
                        inGameController.sellGoods(goods);
                        updateCargoLabel();
                    } else {
                        inGameController.payArrears(goods);
                    }
                    europePanel.getCargoPanel().revalidate();
                    revalidate();
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
    * Returns a pointer to the <code>CargoPanel</code>-object in use.
    * @return The <code>CargoPanel</code>.
    */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
    * Returns a pointer to the <code>MarketPanel</code>-object in use.
    * @return The <code>MarketPanel</code>.
    */
    public final MarketPanel getMarketPanel() {
        return marketPanel;
    }



    /**
    * Displays the <code>RecruitDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    private void showRecruitDialog() {
        recruitDialog.initialize();
        recruitDialog.setLocation(getWidth() / 2 - recruitDialog.getWidth() / 2, getHeight() / 2 - recruitDialog.getHeight() / 2);
        add(recruitDialog, JLayeredPane.PALETTE_LAYER);

        recruitDialog.requestFocus();


        boolean response = recruitDialog.getResponseBoolean();

        remove(recruitDialog);
        revalidate();
        repaint(recruitDialog.getX(), recruitDialog.getY(), recruitDialog.getWidth(), recruitDialog.getHeight());

        if (response) {
            refreshBuyRecruit();
        }

        requestFocus();
    }


    /**
    * The panel that allows a user to recruit people in Europe.
    */
    public final class RecruitDialog extends FreeColDialog implements ActionListener {
        private static final int RECRUIT_CANCEL = 0,
                                 RECRUIT_1 = 1,
                                 RECRUIT_2 = 2,
                                 RECRUIT_3 = 3;

        private final JLabel    price;
        private final JButton   person1,
                                person2,
                                person3;
        private final JButton cancel;

        /**
        * The constructor to use.
        */
        public RecruitDialog() {
            setFocusCycleRoot(true);
            ActionListener actionListener = this;

            JLabel  question = new MessageLabel( "clickAnIndividual" ),
                    question2 = new MessageLabel( "recruitThem" ),
                    priceLabel = new MessageLabel( "theirPrice" );
            cancel = new JButton( Messages.message("cancel") );
            setCancelComponent(cancel);

            price = new JLabel();
            person1 = new JButton();
            person2 = new JButton();
            person3 = new JButton();

            initialize();

            question.setSize(300, 20);
            question2.setSize(300, 20);
            priceLabel.setSize(80, 20);
            price.setSize(60, 20);
            person1.setSize(200, 20);
            person2.setSize(200, 20);
            person3.setSize(200, 20);
            cancel.setSize(80, 20);

            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            priceLabel.setLocation(10, 55);
            price.setLocation(95, 55);
            person1.setLocation(60, 90);
            person2.setLocation(60, 115);
            person3.setLocation(60, 140);
            cancel.setLocation(120, 175);

            setLayout(null);

            person1.setActionCommand(String.valueOf(RECRUIT_1));
            person2.setActionCommand(String.valueOf(RECRUIT_2));
            person3.setActionCommand(String.valueOf(RECRUIT_3));
            cancel.setActionCommand(String.valueOf(RECRUIT_CANCEL));

            person1.addActionListener(actionListener);
            person2.addActionListener(actionListener);
            person3.addActionListener(actionListener);
            cancel.addActionListener(actionListener);

            add(question);
            add(question2);
            add(priceLabel);
            add(price);
            add(person1);
            add(person2);
            add(person3);
            add(cancel);

            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            }
            catch(Exception e) {
            }

            setSize(320, 205);
        }


        public void requestFocus() {
            cancel.requestFocus();
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                price.setText(Integer.toString(freeColClient.getMyPlayer().getRecruitPrice()) + " " + Messages.message("gold"));

                person1.setText(Unit.getName(europe.getRecruitable(1)));
                person2.setText(Unit.getName(europe.getRecruitable(2)));
                person3.setText(Unit.getName(europe.getRecruitable(3)));


                if (freeColClient.getMyPlayer().getRecruitPrice() > freeColClient.getMyPlayer().getGold()) {
                    person1.setEnabled(false);
                    person2.setEnabled(false);
                    person3.setEnabled(false);
                } else {
                    person1.setEnabled(true);
                    person2.setEnabled(true);
                    person3.setEnabled(true);
                }
            }
        }


        /**
        * Analyzes an event and calls the right external methods to take
        * care of the user's request.
        *
        * @param event The incoming action event
        */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            try {
                switch (Integer.valueOf(command).intValue()) {

                case RECRUIT_CANCEL:
                    setResponse(new Boolean(false));
                    break;
                case RECRUIT_1:
                    inGameController.recruitUnitInEurope(1);
                    setResponse(new Boolean(true));
                    break;
                case RECRUIT_2:
                    inGameController.recruitUnitInEurope(2);
                    setResponse(new Boolean(true));
                    break;
                case RECRUIT_3:
                    inGameController.recruitUnitInEurope(3);
                    setResponse(new Boolean(true));
                    break;
                default:
                    logger.warning("Invalid action command");
                    setResponse(new Boolean(false));
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number");
                setResponse(new Boolean(false));
            }
        }
    }



    /**
    * Displays the <code>PurchaseDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    public void showPurchaseDialog() {
        purchaseDialog.initialize();
        purchaseDialog.setLocation(getWidth() / 2 - purchaseDialog.getWidth() / 2, getHeight() / 2 - purchaseDialog.getHeight() / 2);
        add(purchaseDialog, JLayeredPane.PALETTE_LAYER);

        purchaseDialog.requestFocus();


        int response = purchaseDialog.getResponseInt();

        remove(purchaseDialog);
        revalidate();
        repaint(purchaseDialog.getX(), purchaseDialog.getY(), purchaseDialog.getWidth(), purchaseDialog.getHeight());

        if (response > -1) {
            refreshBuyPurchase(response);
        }

        requestFocus();
    }


    /**
    * The panel that allows a user to purchase ships and artillery in Europe.
    */
    public final class PurchaseDialog extends FreeColDialog implements ActionListener {
        private static final int PURCHASE_CANCEL = 0,
                                 PURCHASE_ARTILLERY = 1,
                                 PURCHASE_CARAVEL = 2,
                                 PURCHASE_MERCHANTMAN = 3,
                                 PURCHASE_GALLEON = 4,
                                 PURCHASE_PRIVATEER = 5,
                                 PURCHASE_FRIGATE = 6;

        private JButton   artilleryButton,
                                caravelButton,
                                merchantmanButton,
                                galleonButton,
                                privateerButton,
                                frigateButton;
        private JButton cancel;
        private JLabel artilleryLabel = new JLabel("?");

        /**
        * The constructor to use.
        */
        public PurchaseDialog() {
            setFocusCycleRoot(true);
            ActionListener actionListener = this;

            JLabel  question = new JLabel("Click one of the following items to"),
                    question2 = new JLabel("purchase them."),
                    caravelLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.CARAVEL))),
                    merchantmanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MERCHANTMAN))),
                    galleonLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.GALLEON))),
                    privateerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.PRIVATEER))),
                    frigateLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.FRIGATE)));
            cancel = new JButton("Cancel");
            setCancelComponent(cancel);

            artilleryButton = new JButton(Unit.getName(Unit.ARTILLERY));
            caravelButton = new JButton(Unit.getName(Unit.CARAVEL));
            merchantmanButton = new JButton(Unit.getName(Unit.MERCHANTMAN));
            galleonButton = new JButton(Unit.getName(Unit.GALLEON));
            privateerButton = new JButton(Unit.getName(Unit.PRIVATEER));
            frigateButton = new JButton(Unit.getName(Unit.FRIGATE));

            question.setSize(300, 20);
            question2.setSize(300, 20);
            artilleryLabel.setSize(40, 20);
            caravelLabel.setSize(40, 20);
            merchantmanLabel.setSize(40, 20);
            galleonLabel.setSize(40, 20);
            privateerLabel.setSize(40, 20);
            frigateLabel.setSize(40, 20);
            cancel.setSize(80, 20);
            artilleryButton.setSize(200, 20);
            caravelButton.setSize(200, 20);
            merchantmanButton.setSize(200, 20);
            galleonButton.setSize(200, 20);
            privateerButton.setSize(200, 20);
            frigateButton.setSize(200, 20);

            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            artilleryLabel.setLocation(245, 65);
            caravelLabel.setLocation(245, 90);
            merchantmanLabel.setLocation(245, 115);
            galleonLabel.setLocation(245, 140);
            privateerLabel.setLocation(245, 165);
            frigateLabel.setLocation(245, 190);
            cancel.setLocation(120, 225);
            artilleryButton.setLocation(35, 65);
            caravelButton.setLocation(35, 90);
            merchantmanButton.setLocation(35, 115);
            galleonButton.setLocation(35, 140);
            privateerButton.setLocation(35, 165);
            frigateButton.setLocation(35, 190);

            setLayout(null);

            cancel.setActionCommand(String.valueOf(PURCHASE_CANCEL));
            artilleryButton.setActionCommand(String.valueOf(PURCHASE_ARTILLERY));
            caravelButton.setActionCommand(String.valueOf(PURCHASE_CARAVEL));
            merchantmanButton.setActionCommand(String.valueOf(PURCHASE_MERCHANTMAN));
            galleonButton.setActionCommand(String.valueOf(PURCHASE_GALLEON));
            privateerButton.setActionCommand(String.valueOf(PURCHASE_PRIVATEER));
            frigateButton.setActionCommand(String.valueOf(PURCHASE_FRIGATE));

            cancel.addActionListener(actionListener);
            artilleryButton.addActionListener(actionListener);
            caravelButton.addActionListener(actionListener);
            merchantmanButton.addActionListener(actionListener);
            galleonButton.addActionListener(actionListener);
            privateerButton.addActionListener(actionListener);
            frigateButton.addActionListener(actionListener);

            add(question);
            add(question2);
            add(artilleryLabel);
            add(caravelLabel);
            add(merchantmanLabel);
            add(galleonLabel);
            add(privateerLabel);
            add(frigateLabel);
            add(cancel);
            add(artilleryButton);
            add(caravelButton);
            add(merchantmanButton);
            add(galleonButton);
            add(privateerButton);
            add(frigateButton);

            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            } catch(Exception e) {}

            setSize(320, 255);
        }


        public void requestFocus() {
            cancel.requestFocus();
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                Player gameOwner = freeColClient.getMyPlayer();

                artilleryLabel.setText(Integer.toString(europe.getArtilleryPrice()));

                if (europe.getArtilleryPrice() > gameOwner.getGold()) {
                    artilleryButton.setEnabled(false);
                } else {
                    artilleryButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.CARAVEL) > gameOwner.getGold()) {
                    caravelButton.setEnabled(false);
                } else {
                    caravelButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MERCHANTMAN) > gameOwner.getGold()) {
                    merchantmanButton.setEnabled(false);
                } else {
                    merchantmanButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.GALLEON) > gameOwner.getGold()) {
                    galleonButton.setEnabled(false);
                } else {
                    galleonButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.PRIVATEER) > gameOwner.getGold()) {
                    privateerButton.setEnabled(false);
                } else {
                    privateerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.FRIGATE) > gameOwner.getGold()) {
                    frigateButton.setEnabled(false);
                } else {
                    frigateButton.setEnabled(true);
                }
            }
        }


        /**
        * Analyzes an event and calls the right external methods to take
        * care of the user's request.
        *
        * @param event The incoming action event
        */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            try {
                switch (Integer.valueOf(command).intValue()) {

                case PURCHASE_CANCEL:
                    setResponse(new Integer(-1));
                    break;
                case PURCHASE_ARTILLERY:
                    inGameController.purchaseUnitFromEurope(Unit.ARTILLERY);
                    setResponse(new Integer(Unit.ARTILLERY));
                    break;
                case PURCHASE_CARAVEL:
                    inGameController.purchaseUnitFromEurope(Unit.CARAVEL);
                    setResponse(new Integer(Unit.CARAVEL));
                    break;
                case PURCHASE_MERCHANTMAN:
                    inGameController.purchaseUnitFromEurope(Unit.MERCHANTMAN);
                    setResponse(new Integer(Unit.MERCHANTMAN));
                    break;
                case PURCHASE_GALLEON:
                    inGameController.purchaseUnitFromEurope(Unit.GALLEON);
                    setResponse(new Integer(Unit.GALLEON));
                    break;
                case PURCHASE_PRIVATEER:
                    inGameController.purchaseUnitFromEurope(Unit.PRIVATEER);
                    setResponse(new Integer(Unit.PRIVATEER));
                    break;
                case PURCHASE_FRIGATE:
                    inGameController.purchaseUnitFromEurope(Unit.FRIGATE);
                    setResponse(new Integer(Unit.FRIGATE));
                    break;
                default:
                    logger.warning("Invalid action command");
                    setResponse(new Integer(-1));
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number");
                setResponse(new Integer(-1));
            }
        }
    }



    /**
    * Displays the <code>TrainDialog</code>. Does not return from this
    * method before the panel is closed.
    */
    public void showTrainDialog() {
        trainDialog.initialize();
        trainDialog.setLocation(getWidth() / 2 - trainDialog.getWidth() / 2, getHeight() / 2 - trainDialog.getHeight() / 2);
        add(trainDialog, JLayeredPane.PALETTE_LAYER);

        trainDialog.requestFocus();


        boolean response = trainDialog.getResponseBoolean();

        remove(trainDialog);
        revalidate();
        repaint(trainDialog.getX(), trainDialog.getY(), trainDialog.getWidth(), trainDialog.getHeight());

        if (response) {
            refreshBuyRecruit();
        }

        requestFocus();
    }


    /**
    * The panel that allows a user to train people in Europe.
    */
    public final class TrainDialog extends FreeColDialog implements ActionListener{

        private static final int TRAIN_CANCEL = 0,
                                 TRAIN_EXPERT_ORE_MINER = 1,
                                 TRAIN_EXPERT_LUMBER_JACK = 2,
                                 TRAIN_MASTER_GUNSMITH = 3,
                                 TRAIN_EXPERT_SILVER_MINER = 4,
                                 TRAIN_MASTER_FUR_TRADER = 5,
                                 TRAIN_MASTER_CARPENTER = 6,
                                 TRAIN_EXPERT_FISHERMAN = 7,
                                 TRAIN_MASTER_BLACKSMITH = 8,
                                 TRAIN_EXPERT_FARMER = 9,
                                 TRAIN_MASTER_DISTILLER = 10,
                                 TRAIN_HARDY_PIONEER = 11,
                                 TRAIN_MASTER_TOBACCONIST = 12,
                                 TRAIN_MASTER_WEAVER = 13,
                                 TRAIN_JESUIT_MISSIONARY = 14,
                                 TRAIN_FIREBRAND_PREACHER = 15,
                                 TRAIN_ELDER_STATESMAN = 16,
                                 TRAIN_VETERAN_SOLDIER = 17;

        private JButton   expertOreMinerButton,
                          expertLumberJackButton,
                          masterGunsmithButton,
                          expertSilverMinerButton,
                          masterFurTraderButton,
                          masterCarpenterButton,
                          expertFishermanButton,
                          masterBlacksmithButton,
                          expertFarmerButton,
                          masterDistillerButton,
                          hardyPioneerButton,
                          masterTobacconistButton,
                          masterWeaverButton,
                          jesuitMissionaryButton,
                          firebrandPreacherButton,
                          elderStatesmanButton,
                          veteranSoldierButton;
        private JButton cancel;



        /**
        * The constructor to use.
        */
        public TrainDialog() {
            setFocusCycleRoot(true);

            ActionListener actionListener = this;

            JLabel  question = new JLabel(Messages.message("clickOneOfTheFollowingIndividuals")),
                    question2 = new JLabel(Messages.message("trainThem")),
                    expertOreMinerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_ORE_MINER))),
                    expertLumberJackLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_LUMBER_JACK))),
                    masterGunsmithLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_GUNSMITH))),
                    expertSilverMinerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_SILVER_MINER))),
                    masterFurTraderLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_FUR_TRADER))),
                    masterCarpenterLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_CARPENTER))),
                    expertFishermanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_FISHERMAN))),
                    masterBlacksmithLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_BLACKSMITH))),
                    expertFarmerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.EXPERT_FARMER))),
                    masterDistillerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_DISTILLER))),
                    hardyPioneerLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.HARDY_PIONEER))),
                    masterTobacconistLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_TOBACCONIST))),
                    masterWeaverLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.MASTER_WEAVER))),
                    jesuitMissionaryLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.JESUIT_MISSIONARY))),
                    firebrandPreacherLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.FIREBRAND_PREACHER))),
                    elderStatesmanLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.ELDER_STATESMAN))),
                    veteranSoldierLabel = new JLabel(Integer.toString(Unit.getPrice(Unit.VETERAN_SOLDIER)));
            cancel = new JButton(Messages.message("cancel"));
            setCancelComponent(cancel);

            expertOreMinerButton = new JButton(Unit.getName(Unit.EXPERT_ORE_MINER));
            expertLumberJackButton = new JButton(Unit.getName(Unit.EXPERT_LUMBER_JACK));
            masterGunsmithButton = new JButton(Unit.getName(Unit.MASTER_GUNSMITH));
            expertSilverMinerButton = new JButton(Unit.getName(Unit.EXPERT_SILVER_MINER));
            masterFurTraderButton = new JButton(Unit.getName(Unit.MASTER_FUR_TRADER));
            masterCarpenterButton = new JButton(Unit.getName(Unit.MASTER_CARPENTER));
            expertFishermanButton = new JButton(Unit.getName(Unit.EXPERT_FISHERMAN));
            masterBlacksmithButton = new JButton(Unit.getName(Unit.MASTER_BLACKSMITH));
            expertFarmerButton = new JButton(Unit.getName(Unit.EXPERT_FARMER));
            masterDistillerButton = new JButton(Unit.getName(Unit.MASTER_DISTILLER));
            hardyPioneerButton = new JButton(Unit.getName(Unit.HARDY_PIONEER));
            masterTobacconistButton = new JButton(Unit.getName(Unit.MASTER_TOBACCONIST));
            masterWeaverButton = new JButton(Unit.getName(Unit.MASTER_WEAVER));
            jesuitMissionaryButton = new JButton(Unit.getName(Unit.JESUIT_MISSIONARY));
            firebrandPreacherButton = new JButton(Unit.getName(Unit.FIREBRAND_PREACHER));
            elderStatesmanButton = new JButton(Unit.getName(Unit.ELDER_STATESMAN));
            veteranSoldierButton = new JButton(Unit.getName(Unit.VETERAN_SOLDIER));

            question.setSize(300, 20);
            question2.setSize(300, 20);
            expertOreMinerLabel.setSize(40, 20);
            expertLumberJackLabel.setSize(40, 20);
            masterGunsmithLabel.setSize(40, 20);
            expertSilverMinerLabel.setSize(40, 20);
            masterFurTraderLabel.setSize(40, 20);
            masterCarpenterLabel.setSize(40, 20);
            expertFishermanLabel.setSize(40, 20);
            masterBlacksmithLabel.setSize(40, 20);
            expertFarmerLabel.setSize(40, 20);
            masterDistillerLabel.setSize(40, 20);
            hardyPioneerLabel.setSize(40, 20);
            masterTobacconistLabel.setSize(40, 20);
            masterWeaverLabel.setSize(40, 20);
            jesuitMissionaryLabel.setSize(40, 20);
            firebrandPreacherLabel.setSize(40, 20);
            elderStatesmanLabel.setSize(40, 20);
            veteranSoldierLabel.setSize(40, 20);
            cancel.setSize(80, 20);
            expertOreMinerButton.setSize(200, 20);
            expertLumberJackButton.setSize(200, 20);
            masterGunsmithButton.setSize(200, 20);
            expertSilverMinerButton.setSize(200, 20);
            masterFurTraderButton.setSize(200, 20);
            masterCarpenterButton.setSize(200, 20);
            expertFishermanButton.setSize(200, 20);
            masterBlacksmithButton.setSize(200, 20);
            expertFarmerButton.setSize(200, 20);
            masterDistillerButton.setSize(200, 20);
            hardyPioneerButton.setSize(200, 20);
            masterTobacconistButton.setSize(200, 20);
            masterWeaverButton.setSize(200, 20);
            jesuitMissionaryButton.setSize(200, 20);
            firebrandPreacherButton.setSize(200, 20);
            elderStatesmanButton.setSize(200, 20);
            veteranSoldierButton.setSize(200, 20);

            question.setLocation(10, 10);
            question2.setLocation(10, 30);
            expertOreMinerLabel.setLocation(245, 65);
            expertLumberJackLabel.setLocation(245, 90);
            masterGunsmithLabel.setLocation(245, 115);
            expertSilverMinerLabel.setLocation(245, 140);
            masterFurTraderLabel.setLocation(245, 165);
            masterCarpenterLabel.setLocation(245, 190);
            expertFishermanLabel.setLocation(245, 215);
            masterBlacksmithLabel.setLocation(245, 240);
            expertFarmerLabel.setLocation(245, 265);
            masterDistillerLabel.setLocation(245, 290);
            hardyPioneerLabel.setLocation(245, 315);
            masterTobacconistLabel.setLocation(245, 340);
            masterWeaverLabel.setLocation(245, 365);
            jesuitMissionaryLabel.setLocation(245, 390);
            firebrandPreacherLabel.setLocation(245, 415);
            elderStatesmanLabel.setLocation(245, 440);
            veteranSoldierLabel.setLocation(245, 465);
            cancel.setLocation(120, 500);
            expertOreMinerButton.setLocation(35, 65);
            expertLumberJackButton.setLocation(35, 90);
            masterGunsmithButton.setLocation(35, 115);
            expertSilverMinerButton.setLocation(35, 140);
            masterFurTraderButton.setLocation(35, 165);
            masterCarpenterButton.setLocation(35, 190);
            expertFishermanButton.setLocation(35, 215);
            masterBlacksmithButton.setLocation(35, 240);
            expertFarmerButton.setLocation(35, 265);
            masterDistillerButton.setLocation(35, 290);
            hardyPioneerButton.setLocation(35, 315);
            masterTobacconistButton.setLocation(35, 340);
            masterWeaverButton.setLocation(35, 365);
            jesuitMissionaryButton.setLocation(35, 390);
            firebrandPreacherButton.setLocation(35, 415);
            elderStatesmanButton.setLocation(35, 440);
            veteranSoldierButton.setLocation(35, 465);

            setLayout(null);

            cancel.setActionCommand(String.valueOf(TRAIN_CANCEL));
            expertOreMinerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_ORE_MINER));
            expertLumberJackButton.setActionCommand(String.valueOf(TRAIN_EXPERT_LUMBER_JACK));
            masterGunsmithButton.setActionCommand(String.valueOf(TRAIN_MASTER_GUNSMITH));
            expertSilverMinerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_SILVER_MINER));
            masterFurTraderButton.setActionCommand(String.valueOf(TRAIN_MASTER_FUR_TRADER));
            masterCarpenterButton.setActionCommand(String.valueOf(TRAIN_MASTER_CARPENTER));
            expertFishermanButton.setActionCommand(String.valueOf(TRAIN_EXPERT_FISHERMAN));
            masterBlacksmithButton.setActionCommand(String.valueOf(TRAIN_MASTER_BLACKSMITH));
            expertFarmerButton.setActionCommand(String.valueOf(TRAIN_EXPERT_FARMER));
            masterDistillerButton.setActionCommand(String.valueOf(TRAIN_MASTER_DISTILLER));
            hardyPioneerButton.setActionCommand(String.valueOf(TRAIN_HARDY_PIONEER));
            masterTobacconistButton.setActionCommand(String.valueOf(TRAIN_MASTER_TOBACCONIST));
            masterWeaverButton.setActionCommand(String.valueOf(TRAIN_MASTER_WEAVER));
            jesuitMissionaryButton.setActionCommand(String.valueOf(TRAIN_JESUIT_MISSIONARY));
            firebrandPreacherButton.setActionCommand(String.valueOf(TRAIN_FIREBRAND_PREACHER));
            elderStatesmanButton.setActionCommand(String.valueOf(TRAIN_ELDER_STATESMAN));
            veteranSoldierButton.setActionCommand(String.valueOf(TRAIN_VETERAN_SOLDIER));

            cancel.addActionListener(actionListener);
            expertOreMinerButton.addActionListener(actionListener);
            expertLumberJackButton.addActionListener(actionListener);
            masterGunsmithButton.addActionListener(actionListener);
            expertSilverMinerButton.addActionListener(actionListener);
            masterFurTraderButton.addActionListener(actionListener);
            masterCarpenterButton.addActionListener(actionListener);
            expertFishermanButton.addActionListener(actionListener);
            masterBlacksmithButton.addActionListener(actionListener);
            expertFarmerButton.addActionListener(actionListener);
            masterDistillerButton.addActionListener(actionListener);
            hardyPioneerButton.addActionListener(actionListener);
            masterTobacconistButton.addActionListener(actionListener);
            masterWeaverButton.addActionListener(actionListener);
            jesuitMissionaryButton.addActionListener(actionListener);
            firebrandPreacherButton.addActionListener(actionListener);
            elderStatesmanButton.addActionListener(actionListener);
            veteranSoldierButton.addActionListener(actionListener);

            add(question);
            add(question2);
            add(expertOreMinerLabel);
            add(expertLumberJackLabel);
            add(masterGunsmithLabel);
            add(expertSilverMinerLabel);
            add(masterFurTraderLabel);
            add(masterCarpenterLabel);
            add(expertFishermanLabel);
            add(masterBlacksmithLabel);
            add(expertFarmerLabel);
            add(masterDistillerLabel);
            add(hardyPioneerLabel);
            add(masterTobacconistLabel);
            add(masterWeaverLabel);
            add(jesuitMissionaryLabel);
            add(firebrandPreacherLabel);
            add(elderStatesmanLabel);
            add(veteranSoldierLabel);
            add(cancel);
            add(expertOreMinerButton);
            add(expertLumberJackButton);
            add(masterGunsmithButton);
            add(expertSilverMinerButton);
            add(masterFurTraderButton);
            add(masterCarpenterButton);
            add(expertFishermanButton);
            add(masterBlacksmithButton);
            add(expertFarmerButton);
            add(masterDistillerButton);
            add(hardyPioneerButton);
            add(masterTobacconistButton);
            add(masterWeaverButton);
            add(jesuitMissionaryButton);
            add(firebrandPreacherButton);
            add(elderStatesmanButton);
            add(veteranSoldierButton);

            try {
                BevelBorder border = new BevelBorder(BevelBorder.RAISED);
                setBorder(border);
            } catch(Exception e) {}

            setSize(320, 530);
        }


        public void requestFocus() {
            cancel.requestFocus();
        }


        /**
        * Updates this panel's labels so that the information it displays is up to date.
        */
        public void initialize() {
            if ((game != null) && (freeColClient.getMyPlayer() != null)) {
                Player gameOwner = freeColClient.getMyPlayer();

                if (Unit.getPrice(Unit.EXPERT_ORE_MINER) > gameOwner.getGold()) {
                    expertOreMinerButton.setEnabled(false);
                } else {
                    expertOreMinerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_LUMBER_JACK) > gameOwner.getGold()) {
                    expertLumberJackButton.setEnabled(false);
                } else {
                    expertLumberJackButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_GUNSMITH) > gameOwner.getGold()) {
                    masterGunsmithButton.setEnabled(false);
                } else {
                    masterGunsmithButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_SILVER_MINER) > gameOwner.getGold()) {
                    expertSilverMinerButton.setEnabled(false);
                } else {
                    expertSilverMinerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_FUR_TRADER) > gameOwner.getGold()) {
                    masterFurTraderButton.setEnabled(false);
                } else {
                    masterFurTraderButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_CARPENTER) > gameOwner.getGold()) {
                    masterCarpenterButton.setEnabled(false);
                } else {
                    masterCarpenterButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_FISHERMAN) > gameOwner.getGold()) {
                    expertFishermanButton.setEnabled(false);
                } else {
                    expertFishermanButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_BLACKSMITH) > gameOwner.getGold()) {
                    masterBlacksmithButton.setEnabled(false);
                } else {
                    masterBlacksmithButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.EXPERT_FARMER) > gameOwner.getGold()) {
                    expertFarmerButton.setEnabled(false);
                } else {
                    expertFarmerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_DISTILLER) > gameOwner.getGold()) {
                    masterDistillerButton.setEnabled(false);
                } else {
                    masterDistillerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.HARDY_PIONEER) > gameOwner.getGold()) {
                    hardyPioneerButton.setEnabled(false);
                } else {
                    hardyPioneerButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_TOBACCONIST) > gameOwner.getGold()) {
                    masterTobacconistButton.setEnabled(false);
                } else {
                    masterTobacconistButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.MASTER_WEAVER) > gameOwner.getGold()) {
                    masterWeaverButton.setEnabled(false);
                } else {
                    masterWeaverButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.JESUIT_MISSIONARY) > gameOwner.getGold()) {
                    jesuitMissionaryButton.setEnabled(false);
                } else {
                    jesuitMissionaryButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.FIREBRAND_PREACHER) > gameOwner.getGold()) {
                    firebrandPreacherButton.setEnabled(false);
                } else {
                    firebrandPreacherButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.ELDER_STATESMAN) > gameOwner.getGold()) {
                    elderStatesmanButton.setEnabled(false);
                } else {
                    elderStatesmanButton.setEnabled(true);
                }

                if (Unit.getPrice(Unit.VETERAN_SOLDIER) > gameOwner.getGold()) {
                    veteranSoldierButton.setEnabled(false);
                } else {
                    veteranSoldierButton.setEnabled(true);
                }
            }
        }



        /**
        * Analyzes an event and calls the right external methods to take
        * care of the user's request.
        *
        * @param event The incoming action event
        */
        public void actionPerformed(ActionEvent event) {
            String command = event.getActionCommand();
            try {
                switch (Integer.valueOf(command).intValue()) {
                    case TRAIN_CANCEL:
                        setResponse(new Boolean(false));
                        break;
                    case TRAIN_EXPERT_ORE_MINER:
                        inGameController.trainUnitInEurope(Unit.EXPERT_ORE_MINER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_EXPERT_LUMBER_JACK:
                        inGameController.trainUnitInEurope(Unit.EXPERT_LUMBER_JACK);
                        setResponse(new Boolean(true));                        break;
                    case TRAIN_MASTER_GUNSMITH:
                        inGameController.trainUnitInEurope(Unit.MASTER_GUNSMITH);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_EXPERT_SILVER_MINER:
                        inGameController.trainUnitInEurope(Unit.EXPERT_SILVER_MINER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_FUR_TRADER:
                        inGameController.trainUnitInEurope(Unit.MASTER_FUR_TRADER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_CARPENTER:
                        inGameController.trainUnitInEurope(Unit.MASTER_CARPENTER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_EXPERT_FISHERMAN:
                        inGameController.trainUnitInEurope(Unit.EXPERT_FISHERMAN);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_BLACKSMITH:
                        inGameController.trainUnitInEurope(Unit.MASTER_BLACKSMITH);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_EXPERT_FARMER:
                        inGameController.trainUnitInEurope(Unit.EXPERT_FARMER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_DISTILLER:
                        inGameController.trainUnitInEurope(Unit.MASTER_DISTILLER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_HARDY_PIONEER:
                        inGameController.trainUnitInEurope(Unit.HARDY_PIONEER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_TOBACCONIST:
                        inGameController.trainUnitInEurope(Unit.MASTER_TOBACCONIST);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_MASTER_WEAVER:
                        inGameController.trainUnitInEurope(Unit.MASTER_WEAVER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_JESUIT_MISSIONARY:
                        inGameController.trainUnitInEurope(Unit.JESUIT_MISSIONARY);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_FIREBRAND_PREACHER:
                        inGameController.trainUnitInEurope(Unit.FIREBRAND_PREACHER);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_ELDER_STATESMAN:
                        inGameController.trainUnitInEurope(Unit.ELDER_STATESMAN);
                        setResponse(new Boolean(true));
                        break;
                    case TRAIN_VETERAN_SOLDIER:
                        inGameController.trainUnitInEurope(Unit.VETERAN_SOLDIER);
                        setResponse(new Boolean(true));
                        break;
                    default:
                        logger.warning("Invalid action command");
                        setResponse(new Boolean(false));
                }
            } catch (NumberFormatException e) {
                logger.warning("Invalid action number");
                setResponse(new Boolean(false));
            }
        }
    }

}
