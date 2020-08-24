SUMMARY = "Init script for super-fast graphics on HorseSHIfT prototype"
LICENSE = "GPLv2+"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/GPL-2.0;md5=801f80980d171dd6425610833a22dbe6"

RDEPENDS_${PN} += "glmark2"

SRC_URI = "file://fastinit"

do_install() {
	install -Dm755 ${WORKDIR}/fastinit ${D}/bin/fastinit
}
