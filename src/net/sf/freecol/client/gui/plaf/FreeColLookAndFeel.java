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

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Implements the FreeCol look and feel.
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());
    
    private final static Class<FreeCol> resourceLocator = net.sf.freecol.FreeCol.class;

    private File dataDirectory;

    
    /**
     * Initiates a new FreeCol look and feel.
     *
     * @param dataDirectory The home of the FreeCol data files.
     * @param windowSize The size of the application window.
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel(File dataDirectory, Dimension windowSize)
        throws FreeColException {
        super();

        setCurrentTheme(new DefaultMetalTheme() {
            protected ColorUIResource getPrimary1() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.primary1.color"));
            }

            protected ColorUIResource getPrimary2() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.backgroundSelect.color"));
            }

            protected ColorUIResource getPrimary3() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.primary3.color"));
            }

            protected ColorUIResource getSecondary1() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.secondary1.color"));
            }

            protected ColorUIResource getSecondary2() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.disabled.color"));
            }

            protected ColorUIResource getSecondary3() {
                return new ColorUIResource(ResourceManager.getColor("lookAndFeel.background.color"));
            }
        });

        if (dataDirectory.isDirectory()) {
            this.dataDirectory = dataDirectory;
        } else {
            throw new FreeColException("Data directory is not a directory.");
        }
    }

    /**
     * Creates the look and feel specific defaults table.
     *
     * @return The defaults table.
     */
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        try {
            String checkBoxUI = "net.sf.freecol.client.gui.plaf.FreeColCheckBoxUI";
            u.put("CheckBoxUI", checkBoxUI);
            u.put(checkBoxUI, Class.forName(checkBoxUI));

            String comboBoxUI = "net.sf.freecol.client.gui.plaf.FreeColComboBoxUI";
            u.put("ComboBoxUI", comboBoxUI);
            u.put(comboBoxUI, Class.forName(comboBoxUI));

            String radioButtonUI = "net.sf.freecol.client.gui.plaf.FreeColRadioButtonUI";
            u.put("RadioButtonUI", radioButtonUI);
            u.put(radioButtonUI, Class.forName(radioButtonUI));

            String buttonUI = "net.sf.freecol.client.gui.plaf.FreeColButtonUI";
            u.put("ButtonUI", buttonUI);
            u.put(buttonUI, Class.forName(buttonUI));

            String textFieldUI = "net.sf.freecol.client.gui.plaf.FreeColTextFieldUI";
            u.put("TextFieldUI", textFieldUI);
            u.put(textFieldUI, Class.forName(textFieldUI));

            String textAreaUI = "net.sf.freecol.client.gui.plaf.FreeColTextAreaUI";
            u.put("TextAreaUI", textAreaUI);
            u.put(textAreaUI, Class.forName(textAreaUI));

            String panelUI = "net.sf.freecol.client.gui.plaf.FreeColPanelUI";
            u.put("PanelUI", panelUI);
            u.put(panelUI, Class.forName(panelUI));

            String menuBarUI = "net.sf.freecol.client.gui.plaf.FreeColMenuBarUI";
            u.put("MenuBarUI", menuBarUI);
            u.put(menuBarUI, Class.forName(menuBarUI));

            String popupMenuUI = "net.sf.freecol.client.gui.plaf.FreeColPopupMenuUI";
            u.put("PopupMenuUI", popupMenuUI);
            u.put(popupMenuUI, Class.forName(popupMenuUI));

            String labelUI = "net.sf.freecol.client.gui.plaf.FreeColLabelUI";
            u.put("LabelUI", labelUI);
            u.put(labelUI, Class.forName(labelUI));

            String menuItemUI = "net.sf.freecol.client.gui.plaf.FreeColMenuItemUI";
            u.put("MenuItemUI", menuItemUI);
            u.put(menuItemUI, Class.forName(menuItemUI));

            String listUI = "net.sf.freecol.client.gui.plaf.FreeColListUI";
            u.put("ListUI", listUI);
            u.put(listUI, Class.forName(listUI));

            String tableUI = "net.sf.freecol.client.gui.plaf.FreeColTableUI";
            u.put("TableUI", tableUI);
            u.put(tableUI, Class.forName(tableUI));

            String tableHeaderUI = "net.sf.freecol.client.gui.plaf.FreeColTableHeaderUI";
            u.put("TableHeaderUI", tableHeaderUI);
            u.put(tableHeaderUI, Class.forName(tableHeaderUI));

            String scrollPanelUI = "net.sf.freecol.client.gui.plaf.FreeColScrollPaneUI";
            u.put("ScrollPaneUI", scrollPanelUI);
            u.put(scrollPanelUI, Class.forName(scrollPanelUI));

            String toolTipUI = "net.sf.freecol.client.gui.plaf.FreeColToolTipUI";
            u.put("ToolTipUI", toolTipUI);
            u.put(toolTipUI, Class.forName(toolTipUI));

            // Sharing FreeColBrightPanelUI:
            String brightPanelUI = "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI";
            u.put(brightPanelUI, Class.forName(brightPanelUI));
            u.put("InPortPanelUI", brightPanelUI);
            u.put("CargoPanelUI", brightPanelUI);
            u.put("BuildingsPanelUI", brightPanelUI);
            u.put("OutsideColonyPanelUI", brightPanelUI);
            u.put("WarehousePanelUI", brightPanelUI);
            u.put("ConstructionPanelUI", brightPanelUI);
            u.put("PopulationPanelUI", brightPanelUI);
            u.put("WarehouseGoodsPanelUI", brightPanelUI);
            u.put("ReportPanelUI", brightPanelUI);
            u.put("ColopediaPanelUI", brightPanelUI);
            u.put("TilePanelUI", brightPanelUI);
            u.put("OptionGroupUI", brightPanelUI);

            // Sharing FreeColTransparentPanelUI:
            String transparentPanelUI = "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI";
            u.put(transparentPanelUI, Class.forName(transparentPanelUI));
            u.put("MarketPanelUI", transparentPanelUI);
            u.put("EuropeCargoPanelUI", transparentPanelUI);
            u.put("ToAmericaPanelUI", transparentPanelUI);
            u.put("ToEuropePanelUI", transparentPanelUI);
            u.put("EuropeInPortPanelUI", transparentPanelUI);
            u.put("DocksPanelUI", transparentPanelUI);


            // Add image UI resources:
            String [][] resources = {                
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
            /*String[] fullscreenImages = new String[] {
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
            
            resources = new String[][] {                
                //{"HeaderFont", Messages.message("HeaderFont")},
                //{"NormalFont", Messages.message("NormalFont")},
                //{"BoldFont", Messages.message("BoldFont")}
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
            */
            
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
     * Installs a FreeColLookAndFeel as the default look and feel.
     *
     * @param fclaf The <code>FreeColLookAndFeel</code> to install.
     * @param defaultFont A <code>Font</code> to use by default.
     * @throws FreeColException if the installation fails.
     */
    public static void install(FreeColLookAndFeel fclaf, Font defaultFont)
        throws FreeColException {
        try {
            UIManager.setLookAndFeel(fclaf);
        } catch (UnsupportedLookAndFeelException e) {
            throw new FreeColException(e.getMessage());
        }

        // Set the default font in all UI elements.
        UIDefaults u = UIManager.getDefaults();
        java.util.Enumeration<Object> keys = u.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (u.get(key) instanceof javax.swing.plaf.FontUIResource) {
                u.put(key, defaultFont);
            }
        }
    }
    
    /**
     * Gets a one line description of this Look and Feel.
     *
     * @return "The default Look and Feel for FreeCol"
     */
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
     * Gets the name of this Look and Feel.
     *
     * @return "FreeCol Look and Feel"
     */
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
