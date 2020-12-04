SUMMARY = "OP-TEE Trusted OS"
DESCRIPTION = "OPTEE OS"
LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://${S}/LICENSE;md5=c1f21c4f72f372ef38a5a4aee55ec173"

SRCREV = "3.14.0"
SRC_URI = "git://github.com/OP-TEE/optee_os.git"

inherit deploy python3native

DEPENDS = "python3-pycryptodomex-native python3-pyelftools-native"
S = "${WORKDIR}/git"
OPTEE_PLATFORM ?= "${MACHINE}"
OPTEE_TZDRAM_START ?= ""

EXTRA_OEMAKE = "PLATFORM=${OPTEE_PLATFORM} \
		CFG_WITH_PAGER=n \
		CFG_NS_ENTRY_ADDR=${KERNEL_UIMAGE_LOADADDRESS} \
		CROSS_COMPILE=${HOST_PREFIX} \
		CFG_TEE_CORE_DEBUG=y \
		CFG_TEE_CORE_LOG_LEVEL=2 \
		${TZDRAM_FLAGS} \
        "

# Workaround for "libgcc.a not found" error issue
TARGET_CFLAGS += "--sysroot=${STAGING_DIR_HOST}"
EXTRA_OEMAKE += "LDFLAGS="

python __anonymous() {
    # Avoid ugly backtraces and make sure TZDRAM start is in a valid format
    tzdram_start = d.getVar('OPTEE_TZDRAM_START')
    if tzdram_start:
        try:
            tzdram_address = int(tzdram_start, 0)
        except ValueError:
            bb.fatal('OPTEE_TZDRAM_START=' + tzdram_start + ' is not valid')

        # Use tzdram_start to guess if we're on a 512 MB board or 1GB board
        dram_size = 512 << 20
        if tzdram_address > 0xc0000000 + dram_size:
            dram_size = 1024 << 20

        d.setVar('TZDRAM_FLAGS', 'CFG_TZDRAM_START=' + hex(tzdram_address) +
                                ' CFG_DRAM_SIZE=' + hex(dram_size))
}

do_install() {
    install -d ${D}${nonarch_base_libdir}/firmware/
    install -m 644 ${B}/out/arm-plat-${OPTEE_PLATFORM}/core/*.bin ${D}${nonarch_base_libdir}/firmware/

    install -d ${D}/usr/include/optee/export-user_ta/

    for f in  ${B}/out/arm-plat-${OPTEE_PLATFORM}/export-ta_arm32/* ; do
        cp -aR  $f ${D}/usr/include/optee/export-user_ta/
    done
}

do_deploy() {
	install -d ${DEPLOYDIR}/optee
	for f in ${D}${nonarch_base_libdir}/firmware/*; do
		install -m 644 $f ${DEPLOYDIR}/optee/
	done
}

addtask deploy before do_build after do_install

FILES_${PN} = "${nonarch_base_libdir}/firmware/"
FILES_${PN}-dev = "/usr/include/optee"
INSANE_SKIP_${PN}-dev = "staticdev"
