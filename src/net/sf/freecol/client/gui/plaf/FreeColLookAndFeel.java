/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.Font;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.UIDefaults;
import javax.swing.UIDefaults.LazyValue;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.FreeColException;


/**
 * Implements the FreeCol look and feel.
 */
public class FreeColLookAndFeel extends MetalLookAndFeel {

    private static final Logger logger = Logger.getLogger(FreeColLookAndFeel.class.getName());

    private static class FreeColMetalTheme extends DefaultMetalTheme {

        private static ColorUIResource getColor(String resourceName) {
            return new ColorUIResource(ImageLibrary.getColor(resourceName));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getPrimary1() {
            return getColor("color.primary1.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getPrimary2() {
            return getColor("color.backgroundSelect.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getPrimary3() {
            return getColor("color.primary3.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getSecondary1() {
            return getColor("color.secondary1.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getSecondary2() {
            return getColor("color.disabled.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected ColorUIResource getSecondary3() {
            return getColor("color.background.LookAndFeel");
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ColorUIResource getMenuDisabledForeground() {
            return getColor("color.disabledMenu.LookAndFeel");
        }
    };
        
    private static final String brightPanelUI
        = "net.sf.freecol.client.gui.plaf.FreeColBrightPanelUI";
    private static final String transparentPanelUI
        = "net.sf.freecol.client.gui.plaf.FreeColTransparentPanelUI";

    private static final Class uiClasses[] = {
        FreeColButtonUI.class,
        FreeColCheckBoxMenuItemUI.class,
        FreeColCheckBoxUI.class,
        FreeColComboBoxUI.class,
        FreeColFileChooserUI.class,
        FreeColLabelUI.class,
        FreeColListUI.class,
        FreeColMenuBarUI.class,
        FreeColMenuItemUI.class,
        FreeColMenuUI.class,
        FreeColOptionPaneUI.class,
        FreeColPanelUI.class,
        FreeColPopupMenuUI.class,
        FreeColRadioButtonMenuItemUI.class,
        FreeColRadioButtonUI.class,
        FreeColScrollPaneUI.class,
        FreeColTableHeaderUI.class,
        FreeColTableUI.class,
        FreeColTextAreaUI.class,
        FreeColTextFieldUI.class,
        FreeColFormattedTextFieldUI.class,
        FreeColToolTipUI.class,
        FreeColTransparentPanelUI.class,
        FreeColSpinnerUI.class
    };


    /**
     * Initiates a new FreeCol look and feel.
     *
     * @exception FreeColException If the ui directory could not be found.
     */
    public FreeColLookAndFeel() throws FreeColException {
        super();

        setCurrentTheme(new FreeColMetalTheme());
    }

    /**
     * Creates the look and feel specific defaults table.
     *
     * @return The defaults table.
     */
    @Override
    public UIDefaults getDefaults() {
        UIDefaults u = super.getDefaults();

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
        try {
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
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Could not load " + brightPanelUI, cnfe);
        }

        // FIXME: These do not appear to be in use in 201901
        // Sharing FreeColTransparentPanelUI:
        try {
            u.put(transparentPanelUI, Class.forName(transparentPanelUI));
            u.put("MarketPanelUI", transparentPanelUI);
            u.put("EuropeCargoPanelUI", transparentPanelUI);
            u.put("ToAmericaPanelUI", transparentPanelUI);
            u.put("ToEuropePanelUI", transparentPanelUI);
            u.put("EuropeInPortPanelUI", transparentPanelUI);
            u.put("DocksPanelUI", transparentPanelUI);
        } catch (ClassNotFoundException cnfe) {
            logger.log(Level.WARNING, "Could not load " + transparentPanelUI, cnfe);
        }

        // ColorButton
        u.put("javax.swing.plaf.metal.MetalButtonUI",
            javax.swing.plaf.metal.MetalButtonUI.class);
        u.put("ColorButtonUI", "javax.swing.plaf.metal.MetalButtonUI");

        // Add cursors:
        u.put("cursor.go", ImageLibrary.getCursor());

        u.put("CheckBox.icon", (LazyValue) t -> FreeColCheckBoxUI.createCheckBoxIcon());
        u.put("CheckBoxMenuItem.checkIcon", (LazyValue) t -> FreeColCheckBoxUI.createCheckBoxIcon());
        
        u.put("RadioButton.icon", (LazyValue) t -> FreeColRadioButtonUI.createRadioButtonIcon());
        u.put("RadioButtonMenuItem.checkIcon", (LazyValue) t -> FreeColRadioButtonUI.createRadioButtonIcon());
        
        // TODO: We might want to allow overriding font colors for the menu:
        //u.put("Menu.foreground", java.awt.Color.WHITE);
        
        return u;
    }

    /**
     * Installs a FreeColLookAndFeel as the default look and feel.
     *
     * @param fclaf The {@code FreeColLookAndFeel} to install.
     * @throws FreeColException if the installation fails.
     */
    public static void install(FreeColLookAndFeel fclaf)
        throws FreeColException {
        try {
            UIManager.setLookAndFeel(fclaf);
            UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        } catch (UnsupportedLookAndFeelException e) {
            throw new FreeColException("Look and feel install failure: " + fclaf, e);
        }
    }

    /**
     * Set the default font in all UI elements.
     *
     * @param defaultFont A {@code Font} to use by default.
     */
    public static void installFont(Font defaultFont) {
        final UIDefaults u = UIManager.getLookAndFeelDefaults();
        final Set<Object> keySet = new HashSet<>(u.keySet());
        for (Object key : keySet) {
            if (u.get(key) instanceof javax.swing.plaf.FontUIResource) {
                u.put(key, defaultFont);
            } else if (u.get(key) instanceof Font) {
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
