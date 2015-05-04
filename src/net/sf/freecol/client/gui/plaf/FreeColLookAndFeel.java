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

package net.sf.freecol.client.gui.plaf;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * Implements the FreeCol look and feel.
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());

    private static final Class uiClasses[] = {
        FreeColButtonUI.class,
        FreeColCheckBoxUI.class,
        FreeColComboBoxUI.class,
        FreeColFileChooserUI.class,
        FreeColLabelUI.class,
        FreeColListUI.class,
        FreeColMenuBarUI.class,
        FreeColMenuItemUI.class,
        FreeColOptionPaneUI.class,
        FreeColPanelUI.class,
        FreeColPopupMenuUI.class,
        FreeColRadioButtonUI.class,
        FreeColScrollPaneUI.class,
        FreeColTableHeaderUI.class,
        FreeColTableUI.class,
        FreeColTextAreaUI.class,
        FreeColTextFieldUI.class,
        FreeColToolTipUI.class,
        FreeColTransparentPanelUI.class
    };


    /**
     * Initiates a new FreeCol look and feel.
     *
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel() throws FreeColException {
        super();

        setCurrentTheme(new DefaultMetalTheme() {
                @Override
                protected ColorUIResource getPrimary1() {
                    return new ColorUIResource(ResourceManager.getColor("color.primary1.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getPrimary2() {
                    return new ColorUIResource(ResourceManager.getColor("color.backgroundSelect.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getPrimary3() {
                    return new ColorUIResource(ResourceManager.getColor("color.primary3.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary1() {
                    return new ColorUIResource(ResourceManager.getColor("color.secondary1.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary2() {
                    return new ColorUIResource(ResourceManager.getColor("color.disabled.LookAndFeel"));
                }

                @Override
                protected ColorUIResource getSecondary3() {
                    return new ColorUIResource(ResourceManager.getColor("color.background.LookAndFeel"));
                }

                @Override
                public ColorUIResource getMenuDisabledForeground() {
                    return new ColorUIResource(ResourceManager.getColor("color.disabledMenu.LookAndFeel"));
                }
            });
    }

    /**
     * Creates the look and feel specific defaults table.
     *
     * @return The defaults table.
     */
    @Override
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

        try {
            int offset = "FreeCol".length();
            for (Class<?> uiClass : uiClasses) {
                String name = uiClass.getName();
                int index = name.lastIndexOf("FreeCol");
                if (index >= 0) {
                    index += offset;
                    String shortName = name.substring(index);
                    u.put(shortName, name);
                    u.put(name, uiClass);
                }
            }

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

            // ColorButton
            u.put("javax.swing.plaf.metal.MetalButtonUI", javax.swing.plaf.metal.MetalButtonUI.class);
            u.put("ColorButtonUI", "javax.swing.plaf.metal.MetalButtonUI");

            // Add cursors:
            String key = "image.icon.cursor.go";
            if (ResourceManager.hasImageResource(key)) {
                Image im = ResourceManager.getImage(key);
                u.put("cursor.go",
                    Toolkit.getDefaultToolkit().createCustomCursor(im,
                        new Point(im.getWidth(null)/2, im.getHeight(null)/2),
                        "go"));
            } else {
                u.put("cursor.go", Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Failed to load look and feel!", e);
            System.exit(1);
        }

        return u;
    }

    /**
     * Installs a FreeColLookAndFeel as the default look and feel.
     *
     * @param fclaf The <code>FreeColLookAndFeel</code> to install.
     * @throws FreeColException if the installation fails.
     */
    public static void install(FreeColLookAndFeel fclaf)
        throws FreeColException {
        try {
            UIManager.setLookAndFeel(fclaf);
            UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        } catch (UnsupportedLookAndFeelException e) {
            throw new FreeColException("Look and feel install failure", e);
        }
    }

    /**
     * Set the default font in all UI elements.
     *
     * @param defaultFont A <code>Font</code> to use by default.
     */
    public static void installFont(Font defaultFont) {
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
    @Override
    public String getDescription() {
        return "The default Look and Feel for FreeCol";
    }


    /**
     * Gets the name of this Look and Feel.
     *
     * @return "FreeCol Look and Feel"
     */
    @Override
    public String getName() {
        return "FreeCol Look and Feel";
    }
}
