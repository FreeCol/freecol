
package net.sf.freecol.client.gui.panel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.dnd.Autoscroll;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * This is a panel for the Colony display. It shows the units that are working in the
 * colony, the buildings and much more.
 */
public final class ColonyPanel extends JLayeredPane implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static Logger logger = Logger.getLogger(ColonyPanel.class.getName());

    /**
    * The height of the area in which autoscrolling should happen.
    */
    public static final int SCROLL_AREA_HEIGHT = 40;
    
    /**
    * The speed of the scrolling.
    */
    public static final int SCROLL_SPEED = 10;


    private static final int    EXIT = 0,
                                BUY_BUILDING = 1;

    private final Canvas  parent;
    private final FreeColClient freeColClient;
    private InGameController inGameController;

    private final JLabel                    goldLabel;
    private final JLabel                    solLabel;
    private final JLabel                    hammersLabel;
    private final JLabel                    toolsLabel;
    private final JLabel                    colonyNameLabel;    // CHRIS

    private final ProductionPanel           productionPanel;
    private final BuildingBox               buildingBox;
    private final OutsideColonyPanel        outsideColonyPanel;
    private final InPortPanel               inPortPanel;
    private final CargoPanel                cargoPanel;
    private final TitledBorder              cargoBorder;
    private final WarehousePanel            warehousePanel;
    private final TilePanel                 tilePanel;
    private final BuildingsPanel            buildingsPanel;
    private final DefaultTransferHandler    defaultTransferHandler;
    private final MouseListener             pressListener;
    private final MouseListener             releaseListener;

    private Colony      colony;
    private Game        game;
    private UnitLabel   selectedUnit;

    private JButton exitButton = new JButton("Close");
    private JButton buyBuilding = new JButton("Buy building");



    /**
     * The constructor for the panel.
     * @param parent The parent of this panel
     */
    public ColonyPanel(Canvas parent, FreeColClient freeColClient) {
        final int windowHeight = 630;
        final int windowWidth  = 850;

        this.parent = parent;
        this.freeColClient = freeColClient;
        this.inGameController = freeColClient.getInGameController();

        setFocusCycleRoot(true);

        // Use ESCAPE for closing the ColonyPanel:
        InputMap inputMap = new ComponentInputMap(exitButton);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, false), "pressed");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true), "released");
        SwingUtilities.replaceUIInputMap(exitButton, JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);        
        
        productionPanel = new ProductionPanel(this);
        outsideColonyPanel = new OutsideColonyPanel(this);
        inPortPanel = new InPortPanel();
        cargoPanel = new CargoPanel(this);
        warehousePanel = new WarehousePanel(this);
        tilePanel = new TilePanel(this);
        buildingsPanel = new BuildingsPanel(this);

        productionPanel.setBackground(Color.WHITE);
        outsideColonyPanel.setBackground(Color.WHITE);
        inPortPanel.setBackground(Color.WHITE);
        cargoPanel.setBackground(Color.WHITE);
        warehousePanel.setBackground(Color.WHITE);
        buildingsPanel.setBackground(Color.WHITE);

        defaultTransferHandler = new DefaultTransferHandler(parent, this);
        outsideColonyPanel.setTransferHandler(defaultTransferHandler);
        inPortPanel.setTransferHandler(defaultTransferHandler);
        cargoPanel.setTransferHandler(defaultTransferHandler);
        warehousePanel.setTransferHandler(defaultTransferHandler);

        pressListener = new DragListener(this);
        releaseListener = new DropListener();
        outsideColonyPanel.addMouseListener(releaseListener);
        inPortPanel.addMouseListener(releaseListener);
        cargoPanel.addMouseListener(releaseListener);
        warehousePanel.addMouseListener(releaseListener);

        outsideColonyPanel.setLayout(new GridLayout(0 , 2));
        inPortPanel.setLayout(new GridLayout(0 , 2));
        cargoPanel.setLayout(new GridLayout(1 , 0));
        warehousePanel.setLayout(new GridLayout(2 , 8));

        goldLabel = new JLabel("Gold: 0");

        solLabel = new JLabel("SoL: 0%, Tory: 100%");
        hammersLabel = new JLabel("Hammers: 0/0");
        toolsLabel = new JLabel("Tools: 0/0");


        buildingBox = new BuildingBox(this);

        JScrollPane outsideColonyScroll = new JScrollPane(outsideColonyPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    inPortScroll = new JScrollPane(inPortPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
                    cargoScroll = new JScrollPane(cargoPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    warehouseScroll = new JScrollPane(warehousePanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    tilesScroll = new JScrollPane(tilePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                    buildingsScroll = new JScrollPane(buildingsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JLabel  outsideColonyLabel = new JLabel("In front of colony"),
                inPortLabel = new JLabel("In port"),
                tilesLabel = new JLabel("Tiles"),
                buildingsLabel = new JLabel("Buildings");

        // Make the colony label
        colonyNameLabel = new JLabel("", JLabel.CENTER);    // CHRIS
        colonyNameLabel.setFont(new Font(colonyNameLabel.getFont().getName(),Font.BOLD,24));

        buildingsScroll.setAutoscrolls(true);

        buyBuilding.setSize(132, 20);
        exitButton.setSize(80, 20);
        outsideColonyScroll.setSize(204, 275);
        inPortScroll.setSize(250, 120);
        cargoScroll.setSize(365, 120);
        warehouseScroll.setSize(620, 140);
        productionPanel.setSize(400, 35);
        tilesScroll.setSize(400, 225);
        buildingsScroll.setSize(424,225);
        goldLabel.setSize(100, 20);

        solLabel.setSize(180, 20);

        EtchedBorder eBorder = new EtchedBorder();
        tilesScroll.setBorder(new CompoundBorder(new TitledBorder("Tiles"), new BevelBorder(BevelBorder.LOWERED)));
        buildingsScroll.setBorder(new CompoundBorder(new TitledBorder("Buildings"), eBorder));
        warehouseScroll.setBorder(new CompoundBorder(new TitledBorder("Goods"), eBorder));
        cargoScroll.setBorder(new CompoundBorder(cargoBorder = new TitledBorder("Cargo of ..."), eBorder));
        inPortScroll.setBorder(new CompoundBorder(new TitledBorder("Port"), eBorder));
        outsideColonyScroll.setBorder(new CompoundBorder(new TitledBorder("Outside Colony"), eBorder));

        buildingBox.setSize(265, 20);
        hammersLabel.setSize(180, 20);  // was: 150,20  CHRIS
        toolsLabel.setSize(150, 20);
        colonyNameLabel.setSize(windowWidth, 30);
        

        assignLocations(outsideColonyScroll, inPortScroll, cargoScroll, warehouseScroll, tilesScroll, buildingsScroll);

        buyBuilding.setActionCommand(String.valueOf(BUY_BUILDING));
        exitButton.setActionCommand(String.valueOf(EXIT));

        buyBuilding.addActionListener(this);
        exitButton.addActionListener(this);

        setContents(outsideColonyScroll, inPortScroll, cargoScroll, warehouseScroll, tilesScroll, buildingsScroll, outsideColonyLabel, inPortLabel, tilesLabel, buildingsLabel);

        try {
            BevelBorder border = new BevelBorder(BevelBorder.RAISED);
            setBorder(border);
        } catch(Exception e) {}

        setSize(windowWidth, windowHeight); // was: 850,600  CHRIS

        selectedUnit = null;

        // See the message of Ulf Onnen for more information about the presence of this fake mouse listener.
        addMouseListener(new MouseAdapter() {});
    }




    /**
     *
     * @param outsideColonyScroll
     * @param inPortScroll
     * @param cargoScroll
     * @param warehouseScroll
     * @param tilesScroll
     * @param buildingsScroll
     * @param outsideColonyLabel
     * @param inPortLabel
     * @param tilesLabel
     * @param buildingsLabel
     * 
     * @date Oct 3, 2005 1:26:56 AM by chris
     */
    private void setContents(JScrollPane outsideColonyScroll, JScrollPane inPortScroll, JScrollPane cargoScroll, JScrollPane warehouseScroll, JScrollPane tilesScroll, JScrollPane buildingsScroll, JLabel outsideColonyLabel, JLabel inPortLabel, JLabel tilesLabel, JLabel buildingsLabel) {
        setLayout(null);
        add(exitButton);
        add(outsideColonyScroll);
        add(inPortScroll);
        add(cargoScroll);
        add(warehouseScroll);
        add(productionPanel);
        add(tilesScroll);
        add(buildingsScroll);
        add(outsideColonyLabel);
        add(inPortLabel);
        add(solLabel);
        add(goldLabel);
        add(tilesLabel);
        add(buildingsLabel);
        add(buildingBox);
        add(hammersLabel);
        add(toolsLabel);
        add(buyBuilding);
        add(colonyNameLabel);
    }




    /**
     *
     * @param outsideColonyScroll
     * @param inPortScroll
     * @param cargoScroll
     * @param warehouseScroll
     * @param tilesScroll
     * @param buildingsScroll
     * 
     * @date Oct 3, 2005 1:23:22 AM by CHRIS
     */
    private void assignLocations(JScrollPane outsideColonyScroll, JScrollPane inPortScroll, JScrollPane cargoScroll, JScrollPane warehouseScroll, JScrollPane tilesScroll, JScrollPane buildingsScroll) {
        int y = 10;
        colonyNameLabel.setLocation (0,y);   //602);
        
        y+= 30;
        tilesScroll.setLocation     (10, y);         //10);
        buildingsScroll.setLocation (415, y);    //10); // 400,40
        
        y+= 225;
        productionPanel.setLocation (10, y);    //235);
        
        y+= 5;
        buildingBox.setLocation     (417, y);   //240); // 15,305
        hammersLabel.setLocation    (695, y);   //240); // 185,305 (345, 305)

        y+= 20;
        buyBuilding.setLocation     (550, y);   //260); //682
        toolsLabel.setLocation      (695, y);   //260); //682

        y+= 15;
        solLabel.setLocation        (15, y);    //275);
        
        y+= 10;
        outsideColonyScroll.setLocation(635, y);    //285);
        
        y+= 10;
        goldLabel.setLocation       (15, y);    //295);

        y+= 35;
        inPortScroll.setLocation    (10, y);    //330);
        cargoScroll.setLocation     (265, y);    //330);
        
        y+= 120;
        warehouseScroll.setLocation (10, y);    //450);

        y+= 120;
        exitButton.setLocation      (760, y);   //570);

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
     * Initialize the data on the window.
     */
    public void initialize(Colony colony, Game game) {
        initialize(colony, game, null);
    }


    /**
     * Initialize the data on the window.
     */
    public void initialize(final Colony colony, Game game, Unit preSelectedUnit) {
        this.colony = colony;
        this.game = game;

        //
        // Remove the old components from the panels.
        //

        cargoPanel.removeAll();
        warehousePanel.removeAll();
        outsideColonyPanel.removeAll();
        inPortPanel.removeAll();
        tilePanel.removeAll();


        //
        // Units outside the colony:
        //

        Tile tile = colony.getTile();

        UnitLabel lastCarrier = null;
        UnitLabel preSelectedUnitLabel = null;
        selectedUnit = null;

        Iterator tileUnitIterator = tile.getUnitIterator();
        while (tileUnitIterator.hasNext()) {
            Unit unit = (Unit) tileUnitIterator.next();

            UnitLabel unitLabel = new UnitLabel(unit, parent);
            unitLabel.setTransferHandler(defaultTransferHandler);
            unitLabel.addMouseListener(pressListener);

            //if (((unit.getState() == Unit.ACTIVE) || (unit.getState() == Unit.SENTRY)) && (!unit.isNaval())) {
            if (!unit.isCarrier()) {
                outsideColonyPanel.add(unitLabel, false);
            } else {
                inPortPanel.add(unitLabel);
                lastCarrier = unitLabel;
                if (unit == preSelectedUnit) {
                    preSelectedUnitLabel = unitLabel;
                }

            }
        }

        if (preSelectedUnitLabel == null) {
            setSelectedUnitLabel(lastCarrier);
        } else {
            setSelectedUnitLabel(preSelectedUnitLabel);
        }

        //
        // Warehouse panel:
        //

        warehousePanel.initialize();


        //
        // Units in buildings:
        //

        buildingsPanel.initialize();

        //
        // TilePanel:
        //

        tilePanel.initialize();

        goldLabel.setText("Gold: " + freeColClient.getMyPlayer().getGold());

        buildingBox.initialize();

        updateSoLLabel();
        updateProgressLabel();
        this.colonyNameLabel.setText( colony.getName() );
    }


    public void reinitialize() {
        if (selectedUnit != null) {
            initialize(colony, game, selectedUnit.getUnit());
        } else {
            initialize(colony, game, null);
        }
    }


    /**
    * Updates the label that is placed above the cargo panel. It shows the name
    * of the unit whose cargo is displayed and the amount of space left on that unit.
    */
    private void updateCargoLabel() {
        if (selectedUnit != null) {
            cargoPanel.getParent().setEnabled(true);
            cargoBorder.setTitle("Cargo of " + selectedUnit.getUnit().getName() + " (" + selectedUnit.getUnit().getSpaceLeft() + " left)");
        } else {
            cargoPanel.getParent().setEnabled(false);
            cargoBorder.setTitle("Cargo of ...");
        }
    }

    /**
    * Updates the SoL membership label.
    */
    private void updateSoLLabel() {
        solLabel.setText("SoL: " + colony.getSoL() + "% (" + ((colony.getUnitCount() * colony.getSoL()) / 100) +
                         "), Tory: " + colony.getTory() + "% (" +
                         (colony.getUnitCount() - ((colony.getUnitCount() * colony.getSoL()) / 100)) + ")");
    }


    public void updateBuildingBox() {
        buildingBox.initialize();
    }

    
    public void updateWarehouse() {
        warehousePanel.initialize();
    }


    /**
    * Updates the building progress label.
    */
    private void updateProgressLabel() {
        if (colony.getCurrentlyBuilding() == Building.NONE) {
            hammersLabel.setText("");
            toolsLabel.setText("");
        } else {
            final Building carpenter = colony.getBuildingForProducing( Goods.HAMMERS );
            final int hammers = carpenter.getProductionNextTurn();
            final String hammerDelta = (hammers == 0)?"":(hammers>0)?"+"+ hammers:String.valueOf(hammers);
            final String hammerDisplay = "Hammers: "+ colony.getHammers() + hammerDelta;
            if (colony.getCurrentlyBuilding() < Colony.BUILDING_UNIT_ADDITION) {
                hammersLabel.setText(hammerDisplay + "/" + colony.getBuilding(colony.getCurrentlyBuilding()).getNextHammers());
                toolsLabel.setText("Tools: " + colony.getGoodsCount(Goods.TOOLS) + "/" + colony.getBuilding(colony.getCurrentlyBuilding()).getNextTools());
            } else {
                hammersLabel.setText(hammerDisplay + "/" + Unit.getNextHammers(colony.getCurrentlyBuilding() - Colony.BUILDING_UNIT_ADDITION));
                toolsLabel.setText("Tools: " + colony.getGoodsCount(Goods.TOOLS) + "/" + Unit.getNextTools(colony.getCurrentlyBuilding() - Colony.BUILDING_UNIT_ADDITION));
            }
            buyBuilding.setEnabled(colony.getCurrentlyBuilding() >= 0 && colony.getPriceForBuilding() <= freeColClient.getMyPlayer().getGold());
        }
    }


    /**
    * Returns the currently select unit.
    * @return The currently select unit.
    */
    public Unit getSelectedUnit() {
        return selectedUnit.getUnit();
    }


    /**
    * Returns the currently select unit.
    * @return The currently select unit.
    */
    public UnitLabel getSelectedUnitLabel() {
        return selectedUnit;
    }


    /**
     * Analyzes an event and calls the right external methods to take
     * care of the user's request.
     * @param event The incoming action event
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        try {
            switch (Integer.valueOf(command).intValue()) {
                case BUY_BUILDING:
                    freeColClient.getInGameController().payForBuilding(colony);
                    //updateProgressLabel();
                    reinitialize();                    
                    freeColClient.getCanvas().updateGoldLabel();
                    requestFocus();
                    break;
                case EXIT:
                    closeColonyPanel();
                    break;
                default:
                    logger.warning("Invalid action");
            }
        } catch (NumberFormatException e) {
            logger.warning("Invalid action number");
        }
    }


    /**
    * Closes the <code>ColonyPanel</code>.
    */
    public void closeColonyPanel() {
        parent.remove(this);
        freeColClient.getInGameController().nextModelMessage();
        Unit activeUnit = parent.getGUI().getActiveUnit();
        if (activeUnit == null || activeUnit.getTile() == null
                || activeUnit.getMovesLeft() <= 0
                || (!(activeUnit.getLocation() instanceof Tile) 
                && !(activeUnit.getLocation() instanceof Unit))) {
            parent.getGUI().setActiveUnit(null);
            freeColClient.getInGameController().nextActiveUnit();
        }
    }

    
    /**
    * Paints this component.
    * @param g The graphics context in which to paint.
    */
    public void paintComponent(Graphics g) {
        int width = getWidth();
        int height = getHeight();

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
            cargoPanel.removeAll();
            selectedUnit = unitLabel;

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

            updateCargoLabel();
        }
        cargoPanel.revalidate();
        refresh();
    }


    /**
    * Returns a pointer to the <code>cargoPanel</code>-object in use.
    */
    public final CargoPanel getCargoPanel() {
        return cargoPanel;
    }

    /**
    * Returns a pointer to the <code>warehousePanel</code>-object in use.
    */
    public final WarehousePanel getWarehousePanel() {
        return warehousePanel;
    }

    /**
    * Returns a pointer to the <code>tilePanel</code>-object in use.
    */
    public final TilePanel getTilePanel() {
        return tilePanel;
    }

    /**
    * Returns a pointer to the <code>FreeColClient</code> which uses this panel.
    */
    public final FreeColClient getClient() {
        return freeColClient;
    }

    /**
    * Returns a pointer to the <code>Colony</code>-pointer in use.
    */
    public final Colony getColony() {
        return colony;
    }


    /**
     * Toggles the export settings of the custom house.
     *
     * @param goods The goods for which to toggle the settings.
     */
    public void toggleExports(Goods goods) {
        inGameController.toggleExports(colony, goods);
    }
    
    /**
    * This panel is a list of the colony's buildings.
    */
    public final class BuildingsPanel extends JPanel {
        private final ColonyPanel colonyPanel;


        /**
        * Creates this BuildingsPanel.
        * @param colonyPanel The panel that holds this BuildingsPanel.
        */
        public BuildingsPanel(ColonyPanel colonyPanel) {
            super(new GridLayout(0, 1));
            this.colonyPanel = colonyPanel;
        }


        public String getUIClassID() {
            return "BuildingsPanelUI";
        }


        /**
        * Initializes the <code>BuildingsPanel</code> by loading/displaying the buildings of the colony.
        */
        public void initialize() {
            removeAll();

            ASingleBuildingPanel aSingleBuildingPanel;

            Iterator buildingIterator = colony.getBuildingIterator();
            while (buildingIterator.hasNext()) {
                Building building = (Building) buildingIterator.next();
                if (building.isBuilt()) {
                    aSingleBuildingPanel = new ASingleBuildingPanel(building);
                    aSingleBuildingPanel.addMouseListener(releaseListener);
                    aSingleBuildingPanel.setTransferHandler(defaultTransferHandler);
                    aSingleBuildingPanel.setOpaque(false);
                    add(aSingleBuildingPanel);
                }
            }
        }



        /**
        * This panel is a single line (one building) in the <code>BuildingsPanel</code>.
        */
        public final class ASingleBuildingPanel extends JPanel implements Autoscroll {
            Building building;
            JPanel productionInBuildingPanel = new JPanel();


            /**
            * Creates this ASingleBuildingPanel.
            * @param building The building to display information from.
            */
            public ASingleBuildingPanel(Building building) {
                this.building = building;

                removeAll();
                setBackground(Color.WHITE);

                setLayout(new GridLayout(1, 3));

                JPanel colonistsInBuildingPanel = new JPanel();
                colonistsInBuildingPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                colonistsInBuildingPanel.setBackground(Color.WHITE);

                if (building.getMaxUnits() == 0) {
                    add(new JLabel("(" + building.getName() + ")"));
                } else {
                    add(new JLabel(building.getName()));
                }

                Iterator unitIterator = building.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    UnitLabel unitLabel = new UnitLabel(unit, parent, true);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);
                    colonistsInBuildingPanel.add(unitLabel);
                }

                colonistsInBuildingPanel.setOpaque(false);
                add(colonistsInBuildingPanel);

                updateProductionInBuildingPanel();
                add(productionInBuildingPanel);

                setPreferredSize(new Dimension(getWidth(), (parent.getGUI().getImageLibrary().getUnitImageHeight(0) / 3) * 2 + 5));
            }


            public void autoscroll(Point p) {
                JViewport vp = (JViewport) colonyPanel.buildingsPanel.getParent();
                if (getLocation().y + p.y - vp.getViewPosition().y < SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math.max(vp.getViewPosition().y - SCROLL_SPEED, 0)));
                } else if (getLocation().y + p.y - vp.getViewPosition().y >= vp.getHeight() - SCROLL_AREA_HEIGHT) {
                    vp.setViewPosition(new Point(vp.getViewPosition().x, Math.min(vp.getViewPosition().y + SCROLL_SPEED, colonyPanel.buildingsPanel.getHeight() - vp.getHeight())));
                }
            }


            public Insets getAutoscrollInsets() {
                Rectangle r = getBounds();
                return new Insets(r.x, r.y, r.width, r.height);
            }


            public void updateProductionInBuildingPanel() {
                productionInBuildingPanel.removeAll();
                productionInBuildingPanel.setOpaque(false);
                productionInBuildingPanel.setLayout(new FlowLayout());

                int production = building.getProductionNextTurn();
                if (production > 0) {
                    ImageIcon goodsIcon = parent.getImageProvider().getGoodsImageIcon(building.getGoodsOutputType());
                    int width = goodsIcon.getIconWidth() * 3;
                    BufferedImage productionImage = parent.getGUI().createProductionImage(goodsIcon, production, width, goodsIcon.getIconHeight());
                    JLabel productionLabel = new JLabel(new ImageIcon(productionImage));
                    productionInBuildingPanel.add(productionLabel);

                    if (production != building.getMaximumProduction()) {
                        //JLabel tl = new JLabel("(" + (building.getMaximumProduction() - production) + ")");
                        //tl.setForeground(Color.RED);
                        JLabel tl = new JLabel(Integer.toString(production - building.getMaximumProduction()));
                        productionInBuildingPanel.add(tl);
                    }
                }

            }


            /**
            * Adds a component to this ASingleBuildingPanel and makes sure that the unit
            * that the component represents gets modified so that it will be located
            * in the colony.
            * @param comp The component to add to this ColonistsPanel.
            * @param editState Must be set to 'true' if the state of the component
            * that is added (which should be a dropped component representing a Unit)
            * should be changed so that the underlying unit will be located in the colony.
            * @return The component argument.
            */
            public Component add(Component comp, boolean editState) {
                Component c;
                Container oldParent = comp.getParent();

                if (editState) {
                    if (comp instanceof UnitLabel) {
                        Unit unit = ((UnitLabel) comp).getUnit();

                        if (building.canAdd(unit)) {
                            oldParent.remove(comp);
                            inGameController.work(unit, building);
                            updateBuildingBox();
                            updateWarehouse();
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this BuildingsPanel.");
                        return null;
                    }
                }

                ((UnitLabel) comp).setSmall(true);
                c = ((JPanel) getComponent(1)).add(comp);
                refresh();
                colonyPanel.updateSoLLabel();

                updateProductionInBuildingPanel();
                if (oldParent.getParent() instanceof ASingleBuildingPanel) {
                    ((ASingleBuildingPanel) oldParent.getParent()).updateProductionInBuildingPanel();
                }

                return c;
            }
        }
    }


    /**
    * This panel holds the information of the current food, liberty bell and cross production.
    */
    public final class ProductionPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        public ProductionPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }

        public void paintComponent(Graphics g) {
            int need = colony.getFoodConsumption();
            int surplus = colony.getFoodProduction() - need;
            final int horses = colony.getHorseProduction();
            final int bells = colony.getProductionOf(Goods.BELLS);
            final int crosses = colony.getProductionOf(Goods.CROSSES);

            ImageIcon goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.FOOD);
            BufferedImage productionImage;
            int nextX = 0;
            int add;

            // Food production:
            if (need != 0) {
                nextX = (need > 12) ? goodsIcon.getIconWidth() : Math.min(need, 4) * goodsIcon.getIconWidth();
                if (horses == 0) {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, need, nextX, getHeight(), 12);
                } else {
                    need += horses;
                    surplus -= horses;
                    nextX = goodsIcon.getIconWidth();
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, need, nextX, getHeight(), 1);
                }
                g.drawImage(productionImage, 0, 0, null);
                nextX += goodsIcon.getIconWidth()/4;
            }

            // Food surplus:
            if (surplus != 0) {
                if (surplus > 6 || surplus < 0 || surplus == 1) {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, surplus, goodsIcon.getIconWidth(), getHeight(), 6, true);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, surplus, goodsIcon.getIconWidth() * 2, getHeight(), 6, true);
                    add = goodsIcon.getIconWidth() * 3;
                }
                
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth()/4 + add;
            } else {
                // Show it even if zero surplus
                productionImage = parent.getGUI().createProductionImage(goodsIcon, surplus, goodsIcon.getIconWidth(), getHeight(), -1, 1, -1, true);
                add = goodsIcon.getIconWidth();
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth()/4 + add;
            }
            
            // Horses:
            if (horses != 0) {
                goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.HORSES);
                if (horses > 2 || horses == 1) {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, horses, goodsIcon.getIconWidth(), getHeight(), 2);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, horses, goodsIcon.getIconWidth() * 2, getHeight(), 2);
                    add = goodsIcon.getIconWidth()*2;
                }
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth()/4 + add;
            }

            // Liberty bells:
            if (bells != 0) {
                goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.BELLS);
                if (bells > 6 || bells == 1) {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, bells, goodsIcon.getIconWidth(), getHeight(), 6);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, bells, goodsIcon.getIconWidth() * 2, getHeight(), 6);
                    add = goodsIcon.getIconWidth()*2;
                }
                g.drawImage(productionImage, nextX, 0, null);
                nextX += productionImage.getWidth()/4 + add;
            }
            
            // Crosses:
            if (crosses != 0) {
                goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.CROSSES);
                if (crosses > 6 || crosses == 1) {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, crosses, goodsIcon.getIconWidth(), getHeight(), 6);
                    add = goodsIcon.getIconWidth();
                } else {
                    productionImage = parent.getGUI().createProductionImage(goodsIcon, crosses, goodsIcon.getIconWidth() * 2, getHeight(), 6);
                    add = goodsIcon.getIconWidth()*2;
                }
                g.drawImage(productionImage, nextX, 0, null);
            }
        }
    }


    /**
    * A panel that holds UnitsLabels that represent Units that are
    * standing in front of a colony.
    */
    public final class OutsideColonyPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this OutsideColonyPanel.
        * @param colonyPanel The panel that holds this OutsideColonyPanel.
        */
        public OutsideColonyPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }
        
        

        public String getUIClassID() {
            return "OutsideColonyPanelUI";
        }

        
        /**
        * Adds a component to this OutsideColonyPanel and makes sure that the unit
        * that the component represents gets modified so that it will be located
        * in the colony.
        * @param comp The component to add to this ColonistsPanel.
        * @param editState Must be set to 'true' if the state of the component
        * that is added (which should be a dropped component representing a Unit)
        * should be changed so that the underlying unit will be located in the colony.
        * @return The component argument.
        */
        public Component add(Component comp, boolean editState) {
            Container oldParent = comp.getParent();
            if (editState) {
                if (comp instanceof UnitLabel) {
                    UnitLabel unitLabel = ((UnitLabel) comp);
                    Unit unit = unitLabel.getUnit();

                    if (!(unit.getLocation() instanceof Unit)) {
                        inGameController.putOutsideColony(unit);
                    }

                    if (unit.getTile().getColony() == null) {
                        closeColonyPanel();
                        return null;
                    } else if (!(unit.getLocation() instanceof Tile) && !(unit.getLocation() instanceof Unit)) {
                        return null;
                    }
                    
                    oldParent.remove(comp);
                    updateBuildingBox();
                } else {
                    logger.warning("An invalid component got dropped on this ColonistsPanel.");
                    return null;
                }
            }

            ((UnitLabel) comp).setSmall(false);
            updateCargoLabel();
            Component c = add(comp);
            refresh();
            colonyPanel.updateSoLLabel();
            if (oldParent != null && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent()).updateProductionInBuildingPanel();
            }
            return c;
        }
    }



    /**
    * A panel that holds UnitsLabels that represent naval Units that are
    * waiting in the port of the colony.
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
            return "InPortPanelUI";
        }
    }



    /**
    * A panel that holds goods that represent cargo that is inside the
    * Colony.
    */
    public final class WarehousePanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this CargoPanel.
        * @param colonyPanel The panel that holds this CargoPanel.
        */
        public WarehousePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }
        
        
        public String getUIClassID() {
            return "WarehousePanelUI";
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
            if (editState) {
                if (comp instanceof GoodsLabel) {
                    comp.getParent().remove(comp);
                    //Goods g = ((GoodsLabel)comp).getGoods();
                    ((GoodsLabel) comp).setSmall(false);
                    //inGameController.unloadCargo(g, selectedUnit.getUnit());
                    //colonyPanel.getWarehousePanel().revalidate();
                    //colonyPanel.getCargoPanel().revalidate();
                    //updateCargoLabel();
                    //buildingsPanel.initialize();
                    //initialize();
                    reinitialize();
                    return comp;
                } else {
                    logger.warning("An invalid component got dropped on this WarehousePanel.");
                    return null;
                }
            }

            Component c = add(comp);

            refresh();
            return c;
        }

        public void remove(Component comp) {
            if (comp instanceof GoodsLabel) {
                //Goods g = ((GoodsLabel)comp).getGoods();

                super.remove(comp);
                
                colonyPanel.getWarehousePanel().revalidate();
                colonyPanel.getCargoPanel().revalidate();
            }
        }


        public void initialize() {
            warehousePanel.removeAll();
            Iterator goodsIterator = colony.getGoodsContainer().getFullGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods goods = (Goods) goodsIterator.next();

                GoodsLabel goodsLabel = new GoodsLabel(goods, parent);
                goodsLabel.setTransferHandler(defaultTransferHandler);
                goodsLabel.addMouseListener(pressListener);

                warehousePanel.add(goodsLabel, false);
            }

            warehousePanel.revalidate();
        }
    }

    /**
    * A panel that holds units and goods that represent Units and cargo that are
    * on board the currently selected ship.
    */
    public final class CargoPanel extends JPanel {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this CargoPanel.
        * @param colonyPanel The panel that holds this CargoPanel.
        */
        public CargoPanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
        }


        public String getUIClassID() {
            return "CargoPanelUI";
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
            Container oldParent = comp.getParent();
            if (selectedUnit == null) {
                return null;
            }

            if (editState) {
                if (comp instanceof UnitLabel) {
                    if (oldParent != null) {
                        oldParent.remove(comp);
                    }
                    Unit unit = ((UnitLabel)comp).getUnit();
                    if (!unit.isCarrier()) {// No, you cannot load ships onto other ships.
                        if(!selectedUnit.getUnit().canAdd(unit)) {
                            if (oldParent != null) {
                                oldParent.add(comp);
                            }
                            return null;
                        }

                        ((UnitLabel) comp).setSmall(false);
                        if (inGameController.boardShip(unit, selectedUnit.getUnit())) {
                            if (unit.getTile().getSettlement() == null) {
                                closeColonyPanel();
                            }

                            updateBuildingBox();
                            colonyPanel.updateSoLLabel();
                        } else {
                            if (oldParent != null) {
                                oldParent.add(comp);
                            }
                            return null;
                        }
                    } else {
                        if (oldParent != null) {
                            oldParent.add(comp);
                        }

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
                        if (oldParent != null) {
                            oldParent.add(comp);
                        }
                        return null;
                    }

                    ((GoodsLabel) comp).setSmall(false);
                    inGameController.loadCargo(g, selectedUnit.getUnit());
                    colonyPanel.getWarehousePanel().revalidate();

                    reinitialize();

                    return comp;
                } else {
                    logger.warning("An invalid component got dropped on this CargoPanel.");
                    return null;
                }
            }

            updateCargoLabel();
            Component c = add(comp);

            refresh();
            if (oldParent != null && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent()).updateProductionInBuildingPanel();
            }
            return c;
        }


        public boolean isActive() {
            return (getSelectedUnit() != null);
        }


        public void remove(Component comp) {
            if (comp instanceof UnitLabel) {
                Unit unit = ((UnitLabel)comp).getUnit();
                inGameController.leaveShip(unit);

                super.remove(comp);
            } else if (comp instanceof GoodsLabel) {
                Goods g = ((GoodsLabel)comp).getGoods();
                inGameController.unloadCargo(g);
                super.remove(comp);
                colonyPanel.getWarehousePanel().revalidate();
                colonyPanel.getCargoPanel().revalidate();
                
                // TODO: Make this look prettier :-)
                UnitLabel t = selectedUnit;
                selectedUnit = null;
                setSelectedUnitLabel(t);
            }
        }
    }


    /**
    * A panel that displays the tiles in the immediate area around the colony.
    */
    public final class TilePanel extends JLayeredPane {
        private final ColonyPanel colonyPanel;

        /**
        * Creates this TilePanel.
        * @param colonyPanel The panel that holds this TilePanel.
        */
        public TilePanel(ColonyPanel colonyPanel) {
            this.colonyPanel = colonyPanel;
            setBackground(Color.BLACK);
            //setOpaque(false);
            setLayout(null);
        }


        public void initialize() {
            int layer = 2;

            for (int x=0; x<3; x++) {
                for (int y=0; y<3; y++) {
                    ASingleTilePanel p = new ASingleTilePanel(colony.getColonyTile(x, y), x, y);
                    add(p, new Integer(layer));
                    layer++;
                }
            }
        }


        public void paintComponent(Graphics g) {
            GUI gui = parent.getGUI();
            ImageLibrary lib = parent.getGUI().getImageLibrary();

            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());

            for (int x=0; x<3; x++) {
                for (int y=0; y<3; y++) {
                    gui.displayColonyTile((Graphics2D) g, game.getMap(), colony.getTile(x, y), ((2-x)+y)*lib.getTerrainImageWidth(1)/2, (x+y)*lib.getTerrainImageHeight(1)/2, colony);
                }
            }
        }

        /**
         * Panel for visualizing a <code>ColonyTile</code>.
         */
        public final class ASingleTilePanel extends JPanel {

            private ColonyTile colonyTile;
            private int x;
            private int y;

            public ASingleTilePanel(ColonyTile colonyTile, int x, int y) {
                this.colonyTile = colonyTile;
                this.x = x;
                this.y = y;

                setOpaque(false);

                if (colonyTile.getUnit() != null) {
                    Unit unit = colonyTile.getUnit();

                    UnitLabel unitLabel = new UnitLabel(unit, parent);
                    unitLabel.setTransferHandler(defaultTransferHandler);
                    unitLabel.addMouseListener(pressListener);

                    add(unitLabel);
                }

                ImageLibrary lib = ((Canvas)parent).getGUI().getImageLibrary();

                if (colonyTile.isColonyCenterTile()) {
                    setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                    int width = lib.getTerrainImageWidth(1)*2/3;

                    ImageIcon goodsIcon = parent.getImageProvider().getGoodsImageIcon(Goods.FOOD);
                    BufferedImage productionImage = parent.getGUI().createProductionImage(goodsIcon, colonyTile.getTile().potential(Goods.FOOD), width, goodsIcon.getIconHeight());
                    JLabel sl = new JLabel(new ImageIcon(productionImage), JLabel.CENTER);
                    sl.setSize(lib.getTerrainImageWidth(1), goodsIcon.getIconHeight());
                    add(sl);

                    if (colonyTile.getTile().potential(colonyTile.getTile().secondaryGoods()) != 0) {
                        goodsIcon = parent.getImageProvider().getGoodsImageIcon(colonyTile.getTile().secondaryGoods());
                        productionImage = parent.getGUI().createProductionImage(goodsIcon, colonyTile.getTile().potential(colonyTile.getTile().secondaryGoods()), width, goodsIcon.getIconHeight());
                        sl = new JLabel(new ImageIcon(productionImage), JLabel.CENTER);
                        sl.setSize(lib.getTerrainImageWidth(1), goodsIcon.getIconHeight());
                        add(sl);
                    }
                }

                setTransferHandler(defaultTransferHandler);
                addMouseListener(releaseListener);

                // Size and position:
                setSize(lib.getTerrainImageWidth(1), lib.getTerrainImageHeight(1));
                setLocation(((2-x)+y)*lib.getTerrainImageWidth(1)/2, (x+y)*lib.getTerrainImageHeight(1)/2);
            }


            public void paintComponent(Graphics g) {

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
                Container oldParent = comp.getParent();
                if (editState) {
                    if (comp instanceof UnitLabel) {
                        Unit unit = ((UnitLabel)comp).getUnit();
                        if (colonyTile.getWorkTile().getOwner() != null && colonyTile.getWorkTile().getOwner() != colony) {
                            if (colonyTile.getWorkTile().getOwner().getOwner().isEuropean()) {
                                parent.errorMessage("tileTakenEuro");
                            } else {  // its an indian setttlement
                                parent.errorMessage("tileTakenInd");
                            }
                            return null;
                        }
                        if (colonyTile.getWorkTile().getNationOwner() != Player.NO_NATION
                                && colonyTile.getWorkTile().getNationOwner() != unit.getOwner().getNation()
                                && !Player.isEuropean(colonyTile.getWorkTile().getNationOwner())
                                && !unit.getOwner().hasFather(FoundingFather.PETER_MINUIT)) {
                            int nation = colonyTile.getWorkTile().getNationOwner();
                            int price = game.getPlayer(colonyTile.getWorkTile().getNationOwner()).getLandPrice(colonyTile.getWorkTile());
                            ChoiceItem[] choices = {
                                new ChoiceItem(Messages.message("indianLand.pay").replaceAll("%amount%", Integer.toString(price)), 1),
                                new ChoiceItem(Messages.message("indianLand.take"), 2)
                            };
                            ChoiceItem ci = (ChoiceItem) parent.showChoiceDialog(
                                Messages.message("indianLand.text").replaceAll("%player%", Player.getNationAsString(nation)),
                                Messages.message("indianLand.cancel"), choices
                            );
                            if (ci == null) {
                                return null;
                            } else if (ci.getChoice() == 1) {
                                if (price > freeColClient.getMyPlayer().getGold()) {
                                    parent.errorMessage("notEnoughGold");
                                    return null;
                                } else {
                                    inGameController.buyLand(colonyTile.getWorkTile());
                                }
                            }
                        }

                        if (colonyTile.canAdd(unit)) {
                            oldParent.remove(comp);

                            inGameController.work(unit, colonyTile);

                            updateBuildingBox();
                            updateWarehouse();

                            ((UnitLabel) comp).setSmall(false);
                            colonyPanel.updateSoLLabel();
                        } else {
                            return null;
                        }
                    } else {
                        logger.warning("An invalid component got dropped on this CargoPanel.");
                        return null;
                    }
                }

                updateCargoLabel();
                Component c = add(comp);
                refresh();
                if (oldParent != null && oldParent.getParent() instanceof BuildingsPanel.ASingleBuildingPanel) {
                    ((BuildingsPanel.ASingleBuildingPanel) oldParent.getParent()).updateProductionInBuildingPanel();
                }
                return c;
            }

            public void remove(Component comp) {
                if (comp instanceof UnitLabel) {
                    ((UnitLabel) comp).setSmall(false);
                }
                super.remove(comp);
            }


            /**
            * Checks if this <code>JComponent</code> contains the given coordinate.
            */
            public boolean contains(int px, int py) {
                /**
                * We are assuming the tile size is 128x64.                
                *
                * How this nasty piece of code works:
                *
                * We have a rectangle of 128x64. Inside of it is a diamond (rectangle on its side)
                * whose corners are in the middle of the rectangle edges.
                *
                * We have to figure out if the (x,y) coords are within this diamond.
                *
                * I do this by using the y axis as a reference point. If you look at this diamond, it is
                * widest when y=32, and smallest when y=0 && y=63.
                *
                * So we'return basically saying, when y=32, active x is 100% of 128.
                * When y=10 then active x = 31.25% of 128.
                * 31.25% of 128pixels is 40 pixels, situated in the middle of 128.
                * The middle 40 pixels of 128 is 63-20 and 63+20
                *
                * Tada. A way of detecting if the x,y is withing the diamond. This algorithm should work
                * no matter how tall or short the rectangle (and therefore the diamond within) is.
                */

                int activePixels;

                // Check if the value is in the rectangle at all.
                if (!super.contains(px,py)) {
                    return false;
                }

                if (py>=32) {
                    py = 32 - (py-31);
                }

                // Determine active amount of pixels
                activePixels = (py * 128) / 64;  // 64 --> /32 /2
                // Now determine if x is in the diamond.
                return ( (px >= 63 - activePixels) && (px <= 63 + activePixels) );
            }
        }

    }

    /**
    * A combo box that contains a list of all the buildings that can be built in this colony.
    */
    public final class BuildingBox extends JComboBox {

        private final ColonyPanel colonyPanel;
        private final BuildingBoxListener buildingBoxListener;

        /**
        * Creates a new BuildingBox for this Colony.
        */
        public BuildingBox(ColonyPanel colonyPanel) {
            super();
            this.colonyPanel = colonyPanel;

            buildingBoxListener = new BuildingBoxListener(this, colonyPanel);
            super.addActionListener(buildingBoxListener);
        }

        /**
        * Sets up the BuildingBox such that it contains the appropriate buildings.
        */
        public void initialize() {
            super.removeActionListener(buildingBoxListener);
            removeAllItems();
            BuildingBoxItem nothingItem = new BuildingBoxItem("Nothing", -1);
            this.addItem(nothingItem);
            BuildingBoxItem toSelect = nothingItem;
            for (int i = 0; i < Building.NUMBER_OF_TYPES; i++) {
                if (colonyPanel.getColony().getBuilding(i).getNextName() != null &&
                        colonyPanel.getColony().getUnitCount() >= colonyPanel.getColony().getBuilding(i).getNextPop()) {
                    String theText = new String(
                        colony.getBuilding(i).getNextName() +
                        " (" +
                        Integer.toString(colonyPanel.getColony().getBuilding(i).getNextHammers()) +
                        " hammers");

                    if (colonyPanel.getColony().getBuilding(i).getNextTools() > 0) {
                        theText += ", " + Integer.toString(colony.getBuilding(i).getNextTools()) + " tools";
                    }

                    theText += ")";

                    BuildingBoxItem nextItem = new BuildingBoxItem(theText, i);
                    this.addItem(nextItem);
                    if (i == colonyPanel.getColony().getCurrentlyBuilding()) {
                        toSelect = nextItem;
                    }
                }
            }

            Iterator buildableUnitIterator = colonyPanel.getColony().getBuildableUnitIterator();
            while (buildableUnitIterator.hasNext()) {
                int unitID = (int) ((Integer) buildableUnitIterator.next()).intValue();

                String theText = new String(Unit.getName(unitID) +
                    " (" + Unit.getNextHammers(unitID) + " hammers");

                if (Unit.getNextTools(unitID) > 0) {
                    theText += ", " + Integer.toString(Unit.getNextTools(unitID)) + " tools";
                }

                theText += ")";

                int i = unitID + Colony.BUILDING_UNIT_ADDITION;
                BuildingBoxItem uItem = new BuildingBoxItem(theText, i);
                addItem(uItem);
                if (i == colonyPanel.getColony().getCurrentlyBuilding()) {
                    toSelect = uItem;
                }
            }

            this.setSelectedItem(toSelect);
            super.addActionListener(buildingBoxListener);
            colonyPanel.updateProgressLabel();
        }

        /**
        * Represents a type of building, and the text of the next building in that type.
        */
        public final class BuildingBoxItem {
            private final String text;
            private final int type;


            /**
            * Sets up the text and the type.
            */
            public BuildingBoxItem(String text, int type) {
                this.text = text;
                this.type = type;
            }

            /**
            * Gets the text associated with this item.
            * @return The text associated with this item.
            */
            public String getText() {
                return text;
            }

            /**
            * Gets the building type associated with that item.
            * @return The building type associated with this item.
            */
            public int getType() {
                return type;
            }

            public String toString() {
                return getText();
            }
        }

        /**
        * The ActionListener for the BuildingBox.
        */
        public final class BuildingBoxListener implements ActionListener {

            private ColonyPanel colonyPanel;
            private BuildingBox buildingBox;

            /**
            * Sets up this BuildingBoxListener's buildingBox and colonyPanel.
            */
            public BuildingBoxListener(BuildingBox buildingBox, ColonyPanel colonyPanel) {
                super();
                this.buildingBox = buildingBox;
                this.colonyPanel = colonyPanel;
            }

            /**
            * Sets the ColonyPanel's Colony's type of building.
            */
            public void actionPerformed(ActionEvent e) {
                colonyPanel.getClient().getInGameController().setCurrentlyBuilding(colonyPanel.getColony(), ((BuildingBoxItem)buildingBox.getSelectedItem()).getType());
                colonyPanel.updateProgressLabel();
            }
        }
    }
}
