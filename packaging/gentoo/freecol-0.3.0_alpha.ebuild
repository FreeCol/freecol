# Copyright 1999-2004 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2
# <put CVS info header here>

inherit games

DESCRIPTION="An open source clone of the game Colonization"
HOMEPAGE="http://freecol.sf.net"
# TODO: update SRC_URI as soon as ebuild is added to Gentoo
SRC_URI="http://freecol.sf.net/download/${PN}-${PV}.tar.gz"

LICENSE="GPL-2"
KEYWORDS="x86"
SLOT="0"
IUSE=""

RDEPEND="|| (
    >=virtual/jdk-1.4
    >=virtual/jre-1.4 )"
DEPEND="${RDEPEND}
    >=dev-java/ant-1.4.1"

src_unpack() {
    unpack ${A}
    cd ${S}
}

src_compile() {
    if [ -z "$JAVA_HOME" ]; then
        einfo
        einfo "\$JAVA_HOME not set!"
        einfo "Please use java-config to configure your JVM and try again."
        einfo
        die "\$JAVA_HOME not set."
    fi

    ant || die "compile problem"
}

src_install () {
    dogamesbin "${T}/${PN}" || die "dogamesbin failed"
    dodir "${GAMES_DATADIR}/${PN}"
    cp FreeCol.jar "${D}${GAMES_DATADIR}/${PN}" || die "cp failed"
    prepgamesdirs
}
