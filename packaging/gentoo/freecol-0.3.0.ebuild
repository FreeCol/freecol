# Copyright 1999-2004 Gentoo Foundation
# Distributed under the terms of the GNU General Public License v2

inherit games

DESCRIPTION="An open source clone of the game Colonization"
HOMEPAGE="http://freecol.sf.net"
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
}

src_compile() {
    if [ -z "$JAVA_HOME" ]; then
        einfo
        einfo "\$JAVA_HOME not set!"
        einfo "Please use java-config to configure your JVM and try again."
        einfo
        die "\$JAVA_HOME not set."
    fi

    cd ${WORKDIR}/${PN}
    ant -Dnodata=true || die "compile problem"
}

src_install () {
    cd ${T}
    
    echo "#!/bin/sh" > ${PN}
	echo "cd '${GAMES_DATADIR}/${PN}'" >> ${PN}
	echo "'${JAVA_HOME}'/bin/java -jar FreeCol.jar --freecol-data '${GAMES_DATADIR}/${PN}/data'" >> ${PN}
    
    dogamesbin "${T}/${PN}" || die "dogamesbin failed"
    dodir "${GAMES_DATADIR}/${PN}"
    
    cp "${WORKDIR}/${PN}/FreeCol.jar" "${D}${GAMES_DATADIR}/${PN}"  || die "cp jar file failed"
    cp -r "${WORKDIR}/${PN}/data" "${D}${GAMES_DATADIR}/${PN}"      || die "cp data dir failed"
    
    prepgamesdirs
}
