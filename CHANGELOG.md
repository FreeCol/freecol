## FreeCol 0.12.0 (Unreleased) ##

### Developer issues ###

* **Java 8 is required to build FreeCol.**
* Resource keys for same images in different resolution can now be added to support higher resolution images when zooming or using --gui-scale option.
* All client-server interaction now *must* be asynchronous, as askExpecting() has been removed.
* DOM is gone, and there was much rejoicing.

### Common problems ###

* Java 7 is no longer receiving active support, therefore the previous version of FreeCol switched to using Java 8 which is the currently supported release.  This hopefully fixed the problems that many Mac users have been experiencing.  If FreeCol 0.12.0 fails to run, check first if your installed Java libraries are up to date.

#### Mac OS ####
Running FreeCol in Full-screen mode on OSX is known to be problematic and may not work.

### User-visible changes since 0.11.6-release ###

#### New Features ####

* A number of higher resolution images for units, goods and more have been added to the game.
* A unit may now be ordered to go to an unexplored tile, as long as there is an adjacent explored tile that the unit can reach (without requring transport).
* Display of European prices in several dialogs [IR#43](https://sourceforge.net/p/freecol/improvement-requests/43), from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers,
* Move all goods with a hot key [IR#199](https://sourceforge.net/p/freecol/improvement-requests/199/) from Brian Kim, Louise Zhang, Seongmin Park and Michael Jeffers.
* Easier river editing in map editor through separate options for adding/removing and changing the river style.

#### Improvements ####

* Added tool tip to explain the last sale price annotation on the native settlement panel
* Added "About FreeCol" button to the opening menu screen [IR#15](https://sourceforge.net/p/freecol/improvement-requests/15/)
* Added option to disable / enable region naming dialog. [IR#222](https://sourceforge.net/p/freecol/improvement-requests/222)
* Images of native converts who become dragoons or scouts retain ethnicity, [BR#3086](https://sourceforge.net/p/freecol/bugs/3086).
* Game options added to adjust trade bonus for native alarm. [BR#3092](https://sourceforge.net/p/freecol/bugs/3092/)

#### Bug Fixes ####

* Fixed scout with goto-orders looping at lost city rumours, and broken messages following exploration of "strange mounds", [BR#2932](https://sourceforge.net/p/freecol/bugs/2932)
* "slowed by" message no longer reveals enemy privateer nation, [BR#2933](https://sourceforge.net/p/freecol/bugs/2933)
* The most recently arrived carrier is selected by default when the Europe panel is displayed, [BR#2941](https://sourceforge.net/p/freecol/bugs/2941)
* Various path finding fixes with middle-click gotos, [BR#2943](https://sourceforge.net/p/freecol/bugs/2943), [BR#2944](https://sourceforge.net/p/freecol/bugs/2944), [BR#2980](https://sourceforge.net/p/freecol/bugs/2980).
* Abandoning an improvement no longer corrupts the work of other pioneers also working on the same improvement, [BR#2946](https://sourceforge.net/p/freecol/bugs/2946).
* Trying to overfill a unit with goods in Europe now just does nothing (like other such cases) and does not provoke the "server can not do that" message [IR#214 but really a bug](https://sourceforge.net/p/freecol/improvement-requests/214/)
* Custom options were broken, again, [BR#2948](https://sourceforge.net/p/freecol/bugs/2948).
* Fixed bad "trade refused" message, [BR#2950](https://sourceforge.net/p/freecol/bugs/2950).
* deSoto and scouts could desynchronize the map, [BR#2953](https://sourceforge.net/p/freecol/bugs/2953).
* Bad turn number when sailing from Europe, [BR#2955](https://sourceforge.net/p/freecol/bugs/2955).
* Suppress bogus payment listing when dumping in Europe, [BR#2956](https://sourceforge.net/p/freecol/bugs/2956).
* Suppress bogus colony abandonment warning when spying, [BR#2957](https://sourceforge.net/p/freecol/bugs/2957).
* Drop carried goods when a brave is captured, [BR#2958](https://sourceforge.net/p/freecol/bugs/2958).
* Fix colony tile ownership inconsistency,  [BR#2963](https://sourceforge.net/p/freecol/bugs/2963).
* Clickable link problems, [BR#2965](https://sourceforge.net/p/freecol/bugs/2965).
* Correctly update after feature change (such as goods party), [BR#2966](https://sourceforge.net/p/freecol/bugs/2966).
* Problems using units that have exhausted their moves, [BR#2968](https://sourceforge.net/p/freecol/bugs/2968).
* Slight clarification of unattended production modifiers, [BR#2973](https://sourceforge.net/p/freecol/bugs/2973).
* Stop accumulating colonies in classic colony report, [BR#2974](https://sourceforge.net/p/freecol/bugs/2974).
* Many Col1-compatibility checks and bugfixes with tile and building production with help from Lone_Wolf, [BR#2978](https://sourceforge.net/p/freecol/bugs/2978), [BR#2979](https://sourceforge.net/p/freecol/bugs/2979), [BR#2981](https://sourceforge.net/p/freecol/bugs/2981).
* New versions of the national anthems and addition of the missing ones, from CalebRW.
* Problems with selecting from multiple wagons in the build queue [BR#2014](https://sourceforge.net/p/freecol/bugs/2014), fixed by Lars Willemsens.
* Problems with auto-save cleanup [BR#1307](https://sourceforge.net/p/freecol/bugs/1307), fixed by Lars Willemsens.
* Various fixes to the map editor including smaller dialog for changing river styles [BR#2926](https://sourceforge.net/p/freecol/bugs/2926).
* Layout of Warehouse Panel got improved to better fit inside the minimum supported resolution [BR#3033](https://sourceforge.net/p/freecol/bugs/3033), contributed by Jonas Stevnsvig.
* Seasoned Scouts no longer disappear at LSR's according to Col1 and no longer increase negative results. [BR#3093](https://sourceforge.net/p/freecol/bugs/3093/)
* Delivering gifts to Natives has a stronger effect on reducing native alarm. [BR#3092](https://sourceforge.net/p/freecol/bugs/3092/)


## FreeCol 0.11.6 (2015##

The FreeCol team are glad to announce the release of FreeCol 0.11.6.  This is a bug fix and incremental improvement release from 0.11.5.  All 0.10.x and 0.11.x (x != 4) games should continue to work with 0.11.6.

We hope you enjoy FreeCol 0.11.6.  Onward to 1.0.

The FreeCol Team


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


<!--
Bug:  2883? 2886 2887 2891 2893 2899 2904 2905
Dup: 2907 2914 2923
Media: 2888
Fixed:  2879 2880 2881 2882 2884 2889 2890 2894 2896 2898 2903 2906 2908 2909 2910 2915 2917 2920 2922
Invalid: 2897 2913 2916
NeedInfo  2895 2897 2901 2921
Rejected: 2900 2918 2919
->IR:   2892 2902
UI:      2911 2912

[2879 .. 2923, (2885 in 0.11.5)]

Fixed:    2580 2873
OutOfDate: 2047 2828 2830 2850 2854
-->

#### Other improvements ####

- When characters are not supported by the "ShadowedBlack" font we provide (mostly used for panel headers) the game automatically falls back to using the normal game font, to prevent rectangle glyphs from being shown for non-latin characters.
- Map display is improved; the optional region borders and black map-grid are consistently shown everywhere now.

<!-- ecbc3b7(0.11.4) .. -->

### Developer issues ###

- **Java 8 is now required to build FreeCol.**
- Developer documentation is now compiled and added as a pdf to the distributions.
- Integer, Select and Range options now validate their input.  Watch out for logged warnings of bad input.
- splash.jpg moved into ./build, and is built into FreeCol.jar, added --no-splash option.
- The game now tolerates modding in higher resolution tile images, see git.a85590de for more detailed explanation.
