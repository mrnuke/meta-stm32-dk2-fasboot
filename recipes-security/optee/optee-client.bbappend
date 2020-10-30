#
# Sanity enchancement for optee-client from meta-linaro:
# Add sysvinit service script

inherit update-rc.d

FILESEXTRAPATHS_append := "${THISDIR}/${PN}:"
SRC_URI += "file://init"

INITSCRIPT_NAME = "tee-supplicant"
INITSCRIPT_PARAMS = "defaults 10"

do_install_append() {
	install -d ${D}${sysconfdir}/init.d

	sed -e 's,/usr/sbin,${sbindir},g' \
		-e 's,/var,${localstatedir},g' \
		${WORKDIR}/init > ${D}${sysconfdir}/init.d/tee-supplicant
	chmod 755 ${D}${sysconfdir}/init.d/tee-supplicant
}
