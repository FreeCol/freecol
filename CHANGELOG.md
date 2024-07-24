## FreeCol 1.2.1 (In development) ##

All savegames (without mods) from 0.12.0 and up should continue working with 1.2.1.

### User Interface ###
* Changes the default for most confirm dialogs to OK instead of Cancel.
* Lots of dialogs, that where previously popups outside the game window, have now moved into the main game window.
* The end turn panel can now be resized.
* 10 in now the minimum font size. This mitigates the effect from low reported DPI on macos when a non-native resolution is used.
* Scroll bar speed is now adjusted according to the current font size. This applies when scrolling with the mouse wheel or the scroll bar buttons.
* The resolution of the screen in fullscreen mode can now be configured in the Preferences.
* Bugfix: Attack animations are now played in the correct position.
* Bugfix: Fixes an issue where goods that has been sold is still displayed in the unit info panel.
* Bugfix: Fixes a bug where the escape key closed multiple panels.

### Performance ###
* Unit movements are now faster (area redrawn when moving is reduced).

### Rule Changes ###
* Disabling "Experts have connections" for the classic ruleset.

### AI/computer players ###
* Bugfix: Fixed an issue that prevented diplomacy initiated during the AI players' turns.

### Mod development ###
* Allow mods to add modifiers ("model.modifier.education.teaching.turns") for the education teaching time. Scope checks are applied to the teacher.


## FreeCol 1.2.0 (4th of July, 2024) ##

All savegames (without mods) from 0.12.0 and up should continue working with 1.2.0.

### Graphics ###
* Awesome new building graphics from Misiulo.
* Massive redesign of the colony panel with background images and other styling
* A new styling for the top menu bar with better contrast. This includes changes like showing the top level menus in a button like style.
* New styling for radio and checkbox buttons.
* The face of Peter Minuit now has more realistic coloring. 

### Performance ###
* Improved rendering performance, and especially when scrolling or moving units.
* Allow to run FreeCol with only 512MB memory. Please keep in mind that 2GB is still needed for higher resolution, smoother scroll and high rendering quality.
* Better handling of memory -- for example prioritized automatic clearing of caches when running low on memory.

### Rule Changes ###
* Adding the four extra nations to the classic ruleset. These nations are deactivated by default. Having them as a part of the classic ruleset make it easier to play a game with the classic rules, while still having additional players.
* Deactivates being able to starve out native settlements.
* Bugfix: Fixed a bug that made native settlements be scattered all over the map instead of being grouped by nation (this happened mostly on small maps).
* Bugfix for "Shippable wagons mod": Wagon trains can now be built when the shippable wagons mod has been enabled.

### User Interface ###
* The mapboard scrolling is now much smoother and supports pixel-by-pixel scrolling.
* Allowing scrolling past the borders of a map so that edge tiles can also be placed in focus.
* It's now possible to use 75% display scaling. This can be used when playing on very small screens where fixed size dialogs would otherwise be larger than the screen.
* The warehouse dialog can now be resized.
* Focus is now changed on the mapboard with a single mouse click.
* Unit dragging can now only be started by clicking on the unit (in order to reduce the likelyhood of accidental goto-orders). In addition, the drag can now be started for units that are currently not the active unit.
* The active unit is kept when opening a colony panel.
* The colony panel now works better on small screens.
* The build queue is no longer displayed when clicking on other buildings than the carpenter type of buildings. 
* The colony panel can now be more easily dragged by clicking-and-dragging almost anywhere on the colony panel's open spaces.
* Various fixes and a new visualization of goto-paths.
* Buildings are now randomly distributed in the colony based on the available space, and can be of different sizes.
* The panel showing the signing of the Declaration of Independence is now made larger on bigger screens.
* Panels can now cover the entire mapboard area (this is needed for very small screens in order to show all content).
* The confirm declaration dialog now fits on small screen.
* 6688 translation string updated (in total) for 38 languages.
* Bugfix: Fixes hidden OK-button in the Colopedia on small screens. 
* Bugfix: The height of a colony building no longer changes when a worker is added.
* Bugfix: Lots of small rendering differences between different systems (DPI and OS) have been fixed.
* Bugfix: Caps the automatic font size at no more than 25% larger than the current scaleFactor.
* Bugfix: Hides the display of the "option only"-nation type from the start game panel
* Bugfix: Fixes the tab order on the new game panel.
* Bugfix: An error message is now properly displayed if trying to contact natives with a scout onboard a ship.
* Bugfix: Solved an issue with the Founding Father panel missing the OK button in some languages. 
* Bugfix: REF color is now white for all REF players and Russia is grey (only if the game was started using this version or later). 
* Bugfix: Fixes an issue where the unit info panel displays outdated info. The bug could be reproduced by right-clicking on a tile and then selecting a unit.

### Map Editor ###
* Allow the minimap and other map controls to be resized in the map editor.
* The areas for native nations can now be defined in the map editor. These areas mark the allowed locations for native settlements for each nations. Overlapping areas are allowed, since proper distance between native settlements are still checked, and can be used for having a random element to the overlap/size of each nation. In addition, it's still possible to define specific settlements that will be used if "Import settlements" is checked when starting a new game.
* The starting areas for European nations can now be defined in the map editor. A random tile in each nation's starting area is used when starting a new game. The areas may overlap.
* Unit order buttons are now hidden when in the map editor.
* Bugfix: Fixes a bug where map width was used for height, and the other way around, when scaling a map in the map editor.
* Bugfix: A new game is now always loaded before importing or generating a new map. This fixes lots of issues caused by stuff from the old map leaking into the new one.

### AI/computer players ###
* Bugfix: Fixed a bug that prevented the REF computer player (AI) from completing its turn.
* Bugfix: Fixed a bug preventing the REF from surrendering.
* Bugfix: Fixed a bug where the AI sometimes sent ships back and forth to Europe without doing anything.

### Mod development ###
* Adding an empty image resource file (resource:image.empty) to be used for replacing images with empty space.
* Mods can now reference abstract types from the base rule without extending a specific base rule.
* Added "preserve-attributes" that, if true, preserves the attributes of a specification element. This simplifies changing only the subelements. Please note that any "extends" attribute still needs to be repeated since this attributes changes the subelements.
* The game now displays mod initialization errors to the user in the new game and start game dialogs.
* An exception is now thrown if a referenced type is used before it's defined.

### Other bug fixes ###
* Fixed an issue that prevented the Declaration of Independence if the player name had non-ascii characters.
* Allows automated trade routes to unload goods in colonies with more than 100 goods. In addition, unloading food is now always allowed.
* Fixes an issue where moving goods into/out from a colony messes up the production. The most noticeable effect of this bug was colonists starving to death while a new colonist was born.


## FreeCol 1.1.0 (May 7, 2023) ##

All savegames from 0.12.0 and up should continue working with 1.1.0.

### Music ###
* New track: El Dorado
* New track: Free Colonist
* New track: Lost City
* New track: Rainy Day
* New track: Royal Troops
* New track: Wagon Wheels
* Updated the old tracks with new updated versions.

### Graphics ###
* New graphics for hills and mountains.
* The standard panel background image is now both brighter and without obvious tiling artifacts on larger screens.

### Rule changes ###
* Jesuit Missionary can now recommision itself in colonies without a church.
* Added the mod "Hitpoints and Combat" that adds hitpoints and ranged combat to the game.

### Map Generator ###
* Great rivers (ocean like, navigable by ships only) are no longer generated by default, but can still be generated if enabled using the Map Generator Options.
* The number of rivers generated can now be properly controlled using the "Number of rivers" map generator option even when changing map sizes and/or amount of mountains.
* Enabled generation of rivers that are only two tiles long (the previous limit was three).

### User Interface ###
* Units in Europe are no longer included in the list of possible active units.
* Game options and Map generator options are no longer automatically saved and loaded. The default values are initially displayed for all new games.
* Panels/dialogs are now automatically resized and relocated when changing the size of the application window (including when switching between full screen and windowed mode).
* Allows the player to easily view all files while loading savegames/maps. 
* The start game panel can now be resized to smaller than the initial size.
* The chat panel (start game panel in multiplayer) is now placed directly below the list of players making the dialog work better on small screens.
* The displayed occupation string on a pioneer now shows "C" for clear forest instead of "P".

### AI/computer players ###
* Greatly reduces the time needed by the AI (the wait time after ending the turn).
* The AI now gets less benefits on lower difficulty levels to make everything easier.
* Escort unit missions are now immediately aborted if the protected unit gets destroyed.
* More efficient transportation of goods and units (better utilization of the capacity of ships and wagon trains).
* The AI now produces fewer and better colonies.

### Mod development ###
* Allow mods to be loaded when making maps in the map editor. This allows maps to be made with new terrain types, and other resources, added by mods.
* New types of mountains and hills defined by mods are now used when generating maps. The specific type of hill/mountain is determined by the latitude.
* Partial modifications to the specification is now possible again. This fixes a regression introduced in 0.11.x that prevented the "partial" flag from working.
* Mods can now define new order buttons (and have them applied without having to restart the game).
* Bugfix: Mods are no longer loaded twice (thereby causing varius problems in the colony panel and other places when modifications were made).
* Bugfix: Units that are not available to a player can now be captured if a valid "unit-change" exist.

### Bug fixes ###
* Fixed a bug that caused the same maps always being generated when starting a new game.
* Fixes game hangs on end turn in some specific cases.
* Fixed scaling and placement of goods production icons on colony tiles. 
* A unit in a building is no longer drawn at the top of the building (that is, the area for showing goods production) if the production is zero.
* Sound effects are now properly played when attacking.
* Fixed a bug that prevented the map shortcut buttons on the Map Generator dialog from working properly. The bug caused the button to have no effect the first time it was clicked after restarting the application.
* Fixed an issue that sometimes prevented mods from being deactivated in the Preferences.
* The game now fails gracefully if the server cannot be started. This prevents an application freeze when failing to start a new game or failing to load a savegame.
* Revenge mode is now working again.
* Fixes an issue that allowed a malicious client (in a multiplayer game) to attack with a unit that had no moves left.
* The "Train" panel in Europe now has five columns of unit buttons instead of just three.
* Fixes a bug that sometimes stopped trade routes / goto orders from working. For example, this happened if there was a lost city rumor in the search area of a wagon train on a trade route.
* Fixes activation of tile improvement actions (like "Plow") after reconnecting.
* Allows the founding father selection to be postponed without an error message.
* Fixed a bug that sometimes prevented the map from being redrawn properly after a unit move/attack animation.
* Highscores can now be stored and displayed again.


## FreeCol 1.0.0 (January 2, 2023) ##

On this very day, 20 years ago, we made the first public release of FreeCol. Our releases have until now been marked as alpha/beta even though the number of downloads for our game has long been counted in the millions.

We are extremely proud to finally announce FreeCol 1.0.0!

All 0.12.0 and 0.13.0 games should continue to work with 1.0.0.


### User-visible changes since 0.13.0-release ###

#### Graphics ####

* New nation specific colony graphics.
* New forest graphics
* New graphics for deserts.
* New transitions between the base tiles.
* Minor changes to other graphics.


#### Rule changes ####

* Major changes to the tile goods production so that the actual values matches the original game.


#### Mod development ####

* It's now possible to define nation specific unit types that can be recruited and purchased in Europe.
* Added tools for easily making new base tiles from a single tiling texture.
* A bug that prevented certain images from being overridden by mods has been fixed.


#### AI/computer players ####

* The AI now chooses better locations when constructing colonies, and also avoid placing the colonies too close to one another.
* The AI colony development and expansion have been improved.
* The AI can now construct docks.
* More efficient transportation of goods.
* The AI can now buy units from Europe without cheating.
* The computer players will now use more advanced tactics when defending and attacking.
* The AI no longer defends its colonies with scouts.
* Adding a new escort mission for protecting units. This mission is used for dragoons that are escorting artillery when attacking.
* National advantages will now change how the AI utilizes its military units. For example, the conquest advantage (spanish) will make the player focus on destroying the natives.
* The AI will no longer equip specialists (like Elder Statemen) with muskets.
* AI ships should no longer get stuck outside native settlements.


#### Bugfixes ####

* Several bugs that might cause the game to crash have been fixed (for example when all settlements of a native player was destroyed).
* Fixed several issues with goto-orders that made the game hang after end of turn.
* Fixed several issues that forced a server reconnect.
* Fixed leaking file descriptors that crashed the game on Windows.
* Reduces the amount of jumping around on the map while animating the moves of enemy units.
* Accelerator keys can now be changed, saved and loaded in the preferences.
* It's now possible to add the same goods multiple times to the same stop in the trade route panel.
* Fixes the scaling of several different panels (preferences).
* Scrolling on the map should now be smoother.
* Units should no longer have their graphics clipped on the map.
* Smoother playback of audio.
* The colony panel is now properly updated when clearing the speciality of a worker.
* Recursive autosave directories should no longer be created.
* Several text template fixes.
* Lots of other minor bugfixes.


#### Other changes ####
* The escape key can now be used to close most panels.
* There are now separate volume sliders for music and sound effects.
* Added a mod where basic buildings (level 1) needs to be constructed (except Town Hall and Carpenter's house).
* Added a mod for having 19th century nations.
* Treasure trains are now displayed in the cargo report panel.
* A malfunctional AI should no longer stop the game.
* Better handling of severe errors like out-of-memory and stack-overflow.
* Translation updates (a total of 5191 translation strings updated in 53 different languages).



## FreeCol 0.13.0 (July 9, 2022) ##

The FreeCol team are pleased to announce the release of FreeCol 0.13.0. All 0.11.x (x != 4) and 0.12.0 games should continue to work with 0.13.0, but not vice versa.

We hope you enjoy FreeCol 0.13.0. Onward to 1.0.

The FreeCol Team


### User-visible changes since 0.12.0-release ###

* In-game music by Alexander Zhelanov.
* New skins for the minimap and the unit info panel.
* New animated rivers.
* Major river tiles now gets the minor river production bonus twice.
* The rendering performance has been improved.
* Rendering quality can now be modified using a new option in the Preferences.
* More high resolution versions of existing images have been added.
* Better support for screens with low resolution.
* Adding two new mods for changing the skins for the minimap and the unit info panel.
* Multiplayer savegame loading has been fixed.
* Over 30 bugs fixed.


## FreeCol 0.12.0 (May 1, 2022) ##

The FreeCol team are pleased to announce the release of FreeCol 0.12.0.  Its been a while.  All 0.11.x (x != 4) games should continue to work with 0.12.0, but not vice versa.

We hope you enjoy FreeCol 0.12.0. Onward to 1.0.

The FreeCol Team


### Java Platform ###

FreeCol requires Java 11 at minimum.  Java 11 is a "Long Term Support" release, but later releases are also known to work.

#### Mac OS ####

Running FreeCol in Full-screen mode on OSX is known to be problematic and may not work well.  As far we can tell this is due to a problem with the Java there, which we are unable to fix.

#### Older Mods ####

Old saved games that use old versions of the mods may lose functionality with the new release.  Most of the packaged mods should continue to work (there are known problems with "convertUpgrade").  New games using the updated mods will work.



### User-visible changes since 0.11.6-release ###

#### New features ####

* Major graphics rework
    * New tile and building images from Misiulo
    * Support for high-DPI screens, customizable font size and high resolution versions of existing graphics
    * Support for animations, smooth map scrolling and other improvements to the graphics engine
* Many new maps, from Euzimar, piotrus, Mazim, organized by Blake
* A huge expansion of the name lists (regions, ships, colonies) from Marcin
* A unit may now be ordered to go to an unexplored tile, as long as there is an adjacent explored tile that the unit can reach (without requring transport)
* Display of European prices in several dialogs [IR#43](https://sourceforge.net/p/freecol/improvement-requests/43), from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers
* Move all goods with a hot key [IR#199](https://sourceforge.net/p/freecol/improvement-requests/199/) from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers
* Easier river editing in map editor through separate options for adding/removing and changing the river style
* There are now separate options controlling movement speed of your units, other friendly nation units, and other hostile nation units (formerly just your units and other nation units)
* Over 150 bugs fixed


#### Other improvements ####

* Added tool tip to explain the last sale price annotation on the native settlement panel
* Added "About FreeCol" button to the opening menu screen [IR#15](https://sourceforge.net/p/freecol/improvement-requests/15/)
* Added option to disable / enable region naming dialog. [IR#222](https://sourceforge.net/p/freecol/improvement-requests/222)
* Game options added to adjust trade bonus for native alarm. [BR#3092](https://sourceforge.net/p/freecol/bugs/3092/)


### Developer issues ###

* **Java 11 is required to build FreeCol.**
* Resource keys for same images in different resolution can now be added to support higher resolution images when zooming or using --gui-scale option.
* DOM is gone, and there was much rejoicing.


## FreeCol 0.11.6 (October 16, 2015) ##

### Common problems ###

Java 7 is no longer receiving active support, therefore this version of FreeCol switches to using Java 8 which is the currently supported release.  This will hopefully fix the problems that many Mac users have been experiencing.  If FreeCol 0.11.6 fails to run, check first if your installed Java libraries are up to date.

There has been significant effort trying to track down cases where the lower right corner panel (InfoPanel) disappears for unknown reasons (see [BR#2907](https://sourceforge.net/p/freecol/bugs/2907) and elsewhere).  If this is still occurring we are particularly interested in *reproducible* test cases with 0.11.6.


### Bugs ###

43 new bug reports have been made since 0.11.5 was released.  19 were fixed, 4 need more information, 3 were duplicates, 3 were invalid, 3 were rejected, 2 were quite minor and reclassified as improvement requests, leaving 9 definite new bugs.

At least 7 older bug reports were also closed, 2 were fixed and 5 went out of date.

There are now 50 open bug reports, many received partial fixes.
The majority of cases are still UI issues, but the proportion of reports that are blocked needing further information is rising.  Please remember, if you report a bug, check back to see if developers that are working on it have clarifying questions.  On the other hand, there has been useful progress on several "What Would Col1 Do?" issues, and more is anticipated in the next release thanks to a highly detailed play through of Col1 contributed by Mark Jackson.
Additionally, there are some open bug reports which require more media (images or sounds). Any help on these would be highly appreciated.

### User-visible changes since 0.11.5-release ###

#### New features ####

- The autosave directory now follows the save directory if it changes.
- New warning that distinguishes approaching colony food exhaustion from actual ongoing starvation.
- Lots of tweaks to the compact colony report.
- Better tool tips on the trade report.
- Added a link to the FreeCol manual on the About panel.
- All cargo (more than 100 units) can be dragged with the control key, contributed by Fenyo.
- Native settlements have roads as in Col1 (confirmation from Fenyo).
- Number of goods on a carrier is shown in InfoPanel at lower right, full stacks have bigger icons instead [IR#157](https://sourceforge.net/p/freecol/improvement-requests/157/), [PF#44](https://sourceforge.net/p/freecol/pending-features-for-freecol/44/).
- Active unit cargo should be selected by default when entering a colony [IR#22](https://sourceforge.net/p/freecol/improvement-requests/22/).
- Terrain bonus does not apply to settlement defence [PF#73](https://sourceforge.net/p/freecol/pending-features-for-freecol/73).
- Coronado only reveals minimal colonies in classic mode [PF#83](https://sourceforge.net/p/freecol/pending-features-for-freecol/83).

#### Bugs fixed ####

- Sentries now obey the client option and do not cause doubled moves [BR#2882](https://sourceforge.net/p/freecol/bugs/2882)
- TilePopup is now correctly shown near the tile when using keyboard or menu.
- Use of country name rather than an adjectival form in several places [BR#2888](https://sourceforge.net/p/freecol/bugs/2888).
- Abandoning a colony when units have it as a destination no longer hangs the game [BR#2889](https://sourceforge.net/p/freecol/bugs/2889).
- The window frame now correctly appears on second monitor in correct size when starting the game in windowed mode, see [BR#2803](https://sourceforge.net/p/freecol/bugs/2803).
- FreeColDialogs are correctly positioned now, also avoiding them being wrongly shown on primary monitor when the main window frame is on secondary monitor, see [BR#2803](https://sourceforge.net/p/freecol/bugs/2803).
- The classic colony report works again [BR#2898](https://sourceforge.net/p/freecol/bugs/2898).
- Cities of gold are named properly [BR#2903](https://sourceforge.net/p/freecol/bugs/2903).
- Colonies can be founded on native land [BR#2906](https://sourceforge.net/p/freecol/bugs/2906).
- FreeColDialog instances do not contain redundant shifted backgrounds anymore, contributed by Bernat, see [BR#2911](https://sourceforge.net/p/freecol/bugs/2911).
- Fixed TileInfoPanel to show information about the terrain cursor being on unexplored tiles.
- Fixed map display to consistently draw everything on partial updates, see [BR#2580](https://sourceforge.net/p/freecol/bugs/2580).
- --server option now only controls starting a stand alone server, --server-port controls the port independently.

#### Other improvements ####

- When characters are not supported by the "ShadowedBlack" font we provide (mostly used for panel headers) the game automatically falls back to using the normal game font, to prevent rectangle glyphs from being shown for non-latin characters.
- Map display is improved; the optional region borders and black map-grid are consistently shown everywhere now.

### Developer issues ###

- **Java 8 is now required to build FreeCol.**
- Developer documentation is now compiled and added as a pdf to the distributions.
- Integer, Select and Range options now validate their input.  Watch out for logged warnings of bad input.
- splash.jpg moved into ./build, and is built into FreeCol.jar, added --no-splash option.
- The game now tolerates modding in higher resolution tile images, see git.a85590de for more detailed explanation.

## FreeCol 0.11.5 (August 3, 2015) ##

### Common problems ###

There was a major reorganization of the translation subsystem.  This may have caused an increase in the number of untranslated texts appearing.  This should go away in time as the translators resynchronize.  Feel free to help at [translatewiki](https://translatewiki.net/wiki/Translating:FreeCol).

Autosaving may have been disabled in old savegames, as a bug([BR#2825](https://sourceforge.net/p/freecol/bugs/2825/)) with the autosave interval could not be fixed in a completely backwards compatible way.  Manually set the autosave interval if you have problems.

### Bugs ###

For the 0.11.5 release we only fixed the map generation bug which incompletely generated regions, which could hang the game for various reasons but most quickly when the display tile regions option was activated.

There are now less than fifty open bug reports.  The majority of cases are now UI issues, followed by reports that are blocked in the `What would Col1 do?' state.

<!---
Bug:
Dup:
Fixed:    2885
Invalid:
NeedInfo:
->IR:
OutOfDate:

[>2876]

Fixed:
->IR:
WontFix:
-->


### Changes since 0.11.4-release ###

#### Bugs fixed ####

- Map region generation has been fixed, see  [BR#2885](https://sourceforge.net/p/freecol/bugs/2885/).

### Changes since 0.11.3-release ###

#### New features ####

- Major improvements to map scaling, suitable for higher resolution displays [BR#2726](https://sourceforge.net/p/freecol/bugs/2726/).
- Overall scaling of most text, icons and many panels in the GUI is a new feature to help users with high resolution displays. This can now be set at the command line with the "--gui-scale *percentage*" option. See also  [BR#2726](https://sourceforge.net/p/freecol/bugs/2726/).
- The monarch now sometimes provides troops and gold when declaring war [PF#4](https://sourceforge.net/p/freecol/pending-features-for-freecol/4/).
- Native nations may support the REF or the player following a declaration of independence [PF#5](https://sourceforge.net/p/freecol/pending-features-for-freecol/5/).
- Added attack animations for artillery, damaged artillery, privateer and frigate [BR#2722](https://sourceforge.net/p/freecol/bugs/2722/).
- Added Col1-compatibility game option to auto-disembark units in colonies, [PF#54](https://sourceforge.net/p/freecol/pending-features-for-freecol/54/).
- Multi-season year mods from Fenyo.


#### Bugs fixed ####

- Bryce mod no longer duplicates requirements [BR#2816](https://sourceforge.net/p/freecol/bugs/2816/)
- Unblocked trade routes [BR#2819](https://sourceforge.net/p/freecol/bugs/2819/)
- Windowed mode can not be changed while panels are visible, mitigating but not really fixing [BR#2820](https://sourceforge.net/p/freecol/bugs/2820/).
- Temporary goods warning message disable works again [BR#2822](https://sourceforge.net/p/freecol/bugs/2822/).
- Military report REF units should aggregate correctly [BR#2823](https://sourceforge.net/p/freecol/bugs/2823/).
- Autosaves can be disabled again [BR#2825](https://sourceforge.net/p/freecol/bugs/2825/).
- Colonies are named before claiming the tile they are on (reverting an old behaviour),
but rejected suggested colony names are now recycled.
- Several cases of missing i18n fixed.
- Some panels, for example, Europe Panel have been made internally and externally resizable to prevent problems from wrongly cut off parts of the panel and its buttons [BR#2786](https://sourceforge.net/p/freecol/bugs/2786/).
- The WorkProductionPanel does not anymore cut off the top part of a forest or similar in the tile image it is showing, similar glitches have been fixed in other places.
- Fixed missing window frame icon after switching between windowed and fullscreen mode.
- Fixed colors for minimap in markovoss mod.
- Fixed partially wrong dialog background texture in Classic FreeCol UI mod.
- Fixed rare crash when loading a savegame with the map centered over a colony [BR#2875](https://sourceforge.net/p/freecol/bugs/2875/).
- Fixed font loading for languages needing localized glyph sets [BR#2877](https://sourceforge.net/p/freecol/bugs/2877/).


#### Other improvements ####

- Better preloading of resources and other code improvements to game startup.
- Code for resource management reorganized. This results in major savings of RAM [BR#2843](https://sourceforge.net/p/freecol/bugs/2823/) and is also including less, but more correct, thread synchronization.
- The size of the game in windowed mode now correctly adapts to the space used for task bar and window decorations.
- Several minor improvements in image quality (for example, dynamically drawing labels, building images, fixed icons, better scaling code).
- Some AI efficiency improvements.
- Improved where line breaks are added to text in InformationPanel.


#### Developer issues ###

- Updated project file and developer documentation for using NetBeans.
- Code for the major gui classes reorganized with less circular dependencies and not as large classes.
- Some code cleanup, like adding @override, reformatting and improving comments, contributed by calebrw.
- Most types of resource keys renamed to bring them into a consistent scheme.
- Large message renaming for consistency and ease of checking.
- Nation options now have a distinct tag that does not rely on capitalization.
- Other serialization naming consistency fixes.
- Added --headless command line option.
