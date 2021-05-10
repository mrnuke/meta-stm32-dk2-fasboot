SUMMARY = "Init script for super-fast graphics on HorseSHIfT prototype"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

RDEPENDS_${PN} += "glmark2"

SRC_URI = "file://fastinit"

do_install() {
	install -Dm755 ${WORKDIR}/fastinit ${D}/bin/fastinit
}
