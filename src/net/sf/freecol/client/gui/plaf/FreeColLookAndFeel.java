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

package net.sf.freecol.client.gui.plaf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.FAFile;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;

/**
 * Implements the "FreeCol Look and Feel".
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {
    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());
    
    
    private final static Class<FreeCol> resourceLocator = net.sf.freecol.FreeCol.class;
    private File dataDirectory;
    
    private static final Color PRIMARY_1 = new Color(122, 109, 82),
                               BG_COLOR_SELECT = new Color(255, 244, 195),
                               PRIMARY_3 = new Color(203, 182, 136),
                               SECONDARY_1 = new Color(10, 10, 10),
                               DISABLED_COLOR = new Color(166, 144, 95),
                               BG_COLOR = new Color(216, 194, 145);
                               
    private final Dimension windowSize;


    /**
     * Initiates a new "FreeCol Look and Feel".
     *
     * @exception FreeColException If the ui directory could not be found.
     * @param windowSize The size of the application window.
     */
    public FreeColLookAndFeel(Dimension windowSize) throws FreeColException {
        this("", windowSize);
    }
    

   /**
    * Initiates a new "FreeCol Look and Feel".
    *
    * @param dataFolder The home of the FreeCol data files.
    * @param windowSize The size of the application window.
    * @exception FreeColException If the ui directory could not be found.
    */
    public FreeColLookAndFeel(String dataFolder, Dimension windowSize) throws FreeColException {
        super();
        
        this.windowSize = windowSize;
        
        if(dataFolder.equals("")) { // lookup is necessary
            dataDirectory = new File("data");// + System.getProperty("file.separator"));            

            if(!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                dataDirectory = null;                                
            }
        } else {
            dataDirectory = new File(dataFolder);

            if(!dataDirectory.exists() || !dataDirectory.isDirectory()) {
                throw new FreeColException("Data directory not found in: " + dataDirectory.getName());
            }
        }
        
        setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getPrimary1() {
                return new ColorUIResource(PRIMARY_1);
            }

            protected ColorUIResource getPrimary2() {
                return new ColorUIResource(BG_COLOR_SELECT);
            }

            protected ColorUIResource getPrimary3() {
                return new ColorUIResource(PRIMARY_3);
            }

            protected ColorUIResource getSecondary1() {
                return new ColorUIResource(SECONDARY_1);
            }

            protected ColorUIResource getSecondary2() {
                return new ColorUIResource(DISABLED_COLOR);
            }

            protected ColorUIResource getSecondary3() {
                return new ColorUIResource(BG_COLOR);
            }
        });
    }


    
    
   /**
    * Creates the look and feel specific defaults table.
    * @return The defaults table.
    */
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        try {
            u.put("CheckBoxUI", "net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI"));
            u.put("ComboBoxUI", "net.sf.freecol.client.gui.plaf.FreeColComboBoxUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColComboBoxUI"));
            u.put("RadioButtonUI", "net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI"));
            u.put("ButtonUI", "net.sf.freecol.client.gui.plaf.FreeColButtonUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColButtonUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColButtonUI"));
            u.put("TextFieldUI", "net.sf.freecol.client.gui.plaf.FreeColTextFieldUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextFieldUI"));
            u.put("TextAreaUI", "net.sf.freecol.client.gui.plaf.FreeColTextAreaUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTextAreaUI"));
            u.put("PanelUI", "net.sf.freecol.client.gui.plaf.FreeColPanelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPanelUI"));
            u.put("MenuBarUI", "net.sf.freecol.client.gui.plaf.FreeColMenuBarUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuBarUI"));
            u.put("PopupMenuUI", "net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI"));
            u.put("LabelUI", "net.sf.freecol.client.gui.plaf.FreeColLabelUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColLabelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColLabelUI"));
            u.put("MenuItemUI", "net.sf.freecol.client.gui.plaf.FreeColMenuItemUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColMenuItemUI"));
            u.put("ListUI", "net.sf.freecol.client.gui.plaf.FreeColListUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColListUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColListUI"));
            u.put("TableUI", "net.sf.freecol.client.gui.plaf.FreeColTableUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableUI"));
            u.put("TableHeaderUI", "net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI"));
            u.put("ScrollPaneUI", "net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI"));
            u.put("ToolTipUI", "net.sf.freecol.client.gui.plaf.FreeColToolTipUI");
            u.put("net.sf.freecol.client.gui.plaf.FreeColToolTipUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColToolTipUI"));            
            
            //u.put("CargoPanelUI", "net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI");
            //u.put("net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColCargoPanelUI"));

            // Sharing FreeColBrightPanelUI:
            u.put("net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI"));
            u.put("InPortPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");            
            u.put("CargoPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("BuildingsPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("OutsideColonyPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("InPortPanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");
            u.put("WarehousePanelUI", "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI");

            // Sharing FreeColTransparentPanelUI:
            u.put("net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI", Class.forName("net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI"));
            u.put("MarketPanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            u.put("EuropeCargoPanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            u.put("ToAmericaPanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            u.put("ToEuropePanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            u.put("EuropeInPortPanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            u.put("DocksPanelUI", "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI");
            
            // Add the Font Animation File for the signature:          
            InputStream faStream = null;            
            File f = new File(dataDirectory, "fonts" + System.getProperty("file.separator") + "signature.faf");
            if (f.exists() && f.isFile()) {
                try {
                    faStream = new FileInputStream(f.toString());
                } catch (FileNotFoundException e) {} // Ignored.
            } else {
                URL url = resourceLocator.getResource("data/fonts/signature.faf");
                if (url != null) {
                    try {
                        faStream = url.openStream();
                    } catch (IOException e) {} // Ignored.
                }
            }
            try {
                if (faStream != null) {
                    u.put("Declaration.signature.font", new FAFile(faStream));
                }
            } catch (IOException e) {
                logger.warning("Could not load the Font Animation File for the signature.");
            }
            
            // Add image UI resources:
            String [][] resources = {                
                {"BackgroundImage", "bg.png"},
                {"BackgroundImage2", "bg2.png"},
                {"CanvasBackgroundImage", "bg_map1.jpg"},
                {"EuropeBackgroundImage", "bg_europe.jpg"},
                {"TitleImage", "freecol2.png"},
                {"EventImage.firstLanding", "landing.png"},
                {"EventImage.meetingNatives", "meet_natives.png"},
                {"EventImage.meetingEuropeans", "meet_europeans.png"},
                {"EventImage.meetingAztec", "meet_aztec.png"},
                {"EventImage.meetingInca", "meet_inca.png"},
                {"VictoryImage", "victory.png"},
                {"FoundingFather.trade", "trade.png"},
                {"FoundingFather.exploration", "exploration.png"},
                {"FoundingFather.military", "military.png"},
                {"FoundingFather.political", "political.png"},
                {"FoundingFather.religious", "religious.png"},
                {"cursor.go.image", "go.png"},
                {"MiniMap.skin", "minimap-skin.png"},
                {"InfoPanel.skin", "infopanel-skin.png"},
                {"Declaration.image", "doi.png"},
                {"path.foot.image", "path-foot.png"},
                {"path.foot.nextTurn.image", "path-foot-nextturn.png"},
                {"path.foot.illegal.image", "path-foot-illegal.png"},
                {"path.naval.image", "path-naval.png"},
                {"path.naval.nextTurn.image", "path-naval-nextturn.png"},
                {"path.naval.illegal.image", "path-naval-illegal.png"},
                {"path.wagon.image", "path-wagon.png"},
                {"path.wagon.nextTurn.image", "path-wagon-nextturn.png"},
                {"path.wagon.illegal.image", "path-wagon-illegal.png"},
                {"path.horse.image", "path-horse.png"},
                {"path.horse.nextTurn.image", "path-horse-nextturn.png"},
                {"path.horse.illegal.image", "path-horse-illegal.png"},
                {"menuborder.n.image", "menuborder-n.png"},
                {"menuborder.nw.image", "menuborder-nw.png"},
                {"menuborder.ne.image", "menuborder-ne.png"},
                {"menuborder.w.image", "menuborder-w.png"},
                {"menuborder.e.image", "menuborder-e.png"},
                {"menuborder.s.image", "menuborder-s.png"},
                {"menuborder.sw.image", "menuborder-sw.png"},
                {"menuborder.se.image", "menuborder-se.png"},
                {"menuborder.shadow.sw.image", "menuborder-shadow-sw.png"},
                {"menuborder.shadow.s.image", "menuborder-shadow-s.png"},
                {"menuborder.shadow.se.image", "menuborder-shadow-se.png"},
                {"model.nation.french.coatOfArms.image", "french.png"},
                {"model.nation.frenchREF.coatOfArms.image", "french-ref.png"},
                {"model.nation.spanish.coatOfArms.image", "spanish.png"},
                {"model.nation.spanishREF.coatOfArms.image", "spanish-ref.png"},
                {"model.nation.english.coatOfArms.image", "english.png"},
                {"model.nation.englishREF.coatOfArms.image", "english-ref.png"},
                {"model.nation.dutch.coatOfArms.image", "dutch.png"},
                {"model.nation.dutchREF.coatOfArms.image", "dutch-ref.png"},
                {"model.nation.native.coatOfArms.image", "native.png"},
                {"model.nation.inca.coatOfArms.image", "native.png"},
                {"model.nation.aztec.coatOfArms.image", "native.png"},
                {"model.nation.arawak.coatOfArms.image", "native.png"},
                {"model.nation.cherokee.coatOfArms.image", "native.png"},
                {"model.nation.iroquois.coatOfArms.image", "native.png"},
                {"model.nation.sioux.coatOfArms.image", "native.png"},
                {"model.nation.apache.coatOfArms.image", "native.png"},
                {"model.nation.tupi.coatOfArms.image", "native.png"},
                {"map.americas", "map-americas.png"},
                {"Colopedia.openSection.image", "open_section.png"},
                {"Colopedia.closedSection.image", "closed_section.png"},
                {"Colopedia.buildingSection.image", "building_section.png"},
            };

            /*
              Use a media tracker to ensure that the resources are loaded
              before we start the GUI.
            */
            MediaTracker mt = new MediaTracker(new Component() {});
            
            for (int i=0; i<resources.length; i++) {
                Image image = null;                
                File file = new File(dataDirectory, "images" + System.getProperty("file.separator") + "ui" + System.getProperty("file.separator") + resources[i][1]);
                
                if (file.exists() && file.isFile()) {
                    image = Toolkit.getDefaultToolkit().getImage(file.toString());    
                } else {
                    URL url = resourceLocator.getResource("data/images/ui/"+  resources[i][1]);
                    if (url != null) {
                        image = Toolkit.getDefaultToolkit().getImage(url);
                    }
                }

                if (image == null) {
                    logger.warning("Could not find image: " + resources[i][1]);
                } else {
                    mt.addImage(image, 0);
                    u.put(resources[i][0], image);
                }
            }
            
            // Resize background image:
            String[] fullscreenImages = new String[] {
                "CanvasBackgroundImage",
                "EuropeBackgroundImage"
            };
            for (String imageID : fullscreenImages) {
                Image bgImage = (Image) u.get(imageID);
                if (bgImage.getWidth(null) != windowSize.width || bgImage.getHeight(null) != windowSize.height) {
                    bgImage = bgImage.getScaledInstance(windowSize.width, windowSize.height, Image.SCALE_SMOOTH);
                    u.put(imageID + ".scaled", bgImage);
                    mt.addImage(bgImage, 0, windowSize.width, windowSize.height);
                }
            }
                
            try {
                mt.waitForID(0, 30000); // Wait a maximum of 30 seconds for the images to load.
            } catch (InterruptedException e) {
                logger.warning("Interrupted while loading resources!");
            }
            
            // Add font UI resources:
            resources = new String[][] {                
                {"HeaderFont", Messages.message("HeaderFont")},
            };                  

            for (int i=0; i<resources.length; i++) {
                InputStream fontStream = null; 
                
                File file = new File(dataDirectory, "fonts" + System.getProperty("file.separator") + resources[i][1]);
                if (file.exists() && file.isFile()) {
                    try {
                        fontStream = new FileInputStream(file.toString());
                    } catch (FileNotFoundException e) {} // Ignored.
                } else {
                    URL url = resourceLocator.getResource("data/fonts/" + resources[i][1]);
                    if (url != null) {
                        try {
                            fontStream = url.openStream();
                        } catch (IOException e) {} // Ignored.
                    }
                }    

                if (fontStream != null) {  
                    try {
                        u.put(resources[i][0], Font.createFont(Font.TRUETYPE_FONT, fontStream));
                    } catch (FontFormatException e) {
                        logger.warning("Could not load font: " + resources[i][1] + " because it has the wrong format.");
                        u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));
                    } catch (IOException ie) {
                        logger.warning("Could not load font: " + resources[i][1] + " because of an IO problem.");
                        u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));                
                    }                                
                } else {           
                    logger.warning("Could not find font: " + resources[i][1]);
                    u.put(resources[i][0], new Font("SansSerif", Font.PLAIN, 1));
                }
            }          
            
            
            // Add cursors:
            Image im = (Image) u.get("cursor.go.image");
            if (im != null) {
                u.put("cursor.go", Toolkit.getDefaultToolkit().createCustomCursor(im, new Point(im.getWidth(null)/2, im.getHeight(null)/2), "go"));
            } else {
                u.put("cursor.go", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to load look and feel!", e);
            System.exit(-1);
        }

        return u;
    }
    
    
    /**
    * Gets a one line description of this Look and Feel.
    * @return "The default Look and Feel for FreeCol"
    */
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
    * Gets the name of this Look and Feel.
    * @return "FreeCol Look and Feel"
    */
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
