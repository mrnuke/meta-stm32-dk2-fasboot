SUMMARY = "A key generation and signing utility for STM32MP SOCs"
LICENSE = "GPLv3+"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

SRC_URI = "git://github.com/mrnuke/stm32mp-keygen.git"
SRCREV = "1ee6ca5afdf3115cb51149971f0b12431a2a5974"
S = "${WORKDIR}/git"

DEPENDS = "python3-pycryptodomex-native "
BBCLASSEXTEND = "native"

inherit python3native

do_install() {
	install -d ${D}/${bindir}
	install -m 755 ${S}/stm32-sign.py ${D}${bindir}/stm32-sign
	install -m 755 ${S}/ecdsa-sha256.py ${D}${bindir}/ecdsa-sha256
}
FILES_${PN} = "${bindir}/*"
