#
# u-boot recipe for HorseSHIfT prototype
#
# UBOOT_DEVICETREES and UBOOT_CONFIG should be defined by the machine conf
#
# For example:
#     UBOOT_DEVICETREES = "stm32mp157c-ev1"
#     UBOOT_CONFIG += "nonsec"
#     UBOOT_CONFIG[nonsec] = "stm32mp15_basic_defconfig,,u-boot.img"
#

SRCREV = "v2020.10-rc4"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=5a7450c57ffe5ae63fd732446b988025"

FILESEXTRAPATHS_append := ":${THISDIR}/patches"
FILESEXTRAPATHS_append := ":${THISDIR}/config"

SRC_URI += "file://0001-stm32mp1-Add-support-for-baudrates-higher-than-11520.patch"
SRC_URI += "file://0002-stm32mp1-Add-support-for-falcon-mode-boot.patch"
SRC_URI += "file://0003-mmc-stm32_sdmmc2-Use-mmc_of_parse-to-read-host-capab.patch"
SRC_URI += "file://baudrate.cfg"
SRC_URI += "file://boot-delay.cfg"
SRC_URI += "file://falcon-mode.cfg"

SPL_BINARY = "spl/u-boot-spl.stm32"
SPL_BINARYNAME = "${@os.path.basename(d.getVar("SPL_BINARY"))}"
SPL_NAME = "${@os.path.splitext(d.getVar("SPL_BINARY"))[0]}"

# Configure for debug elf
UBOOT_DEPLOY_ELF_FILES ?= ""
UBOOT_ELF = "u-boot"
SPL_ELF = "${@d.getVar('SPL_BINARY').split('.')[0]}"
SPL_ELF_NAME  = "${@os.path.basename(d.getVar("SPL_ELF"))}.elf"

# -----------------------------------------------------------------------------
# We want to pass 'DEVICE_TREE=' option to u-boot, which the core recipe does
# not support. Thus we have to override it
#
do_compile_append() {
    if [ -z "${UBOOT_DEVICETREES}" ]; then
        bbfatal "No u-boot devicetree. Set UBOOT_DEVICETREES appropriately"
    fi
    if [ -z "${UBOOT_CONFIG}" ]; then
        bbfatal "No valid u-boot configuration. Set UBOOT_CONFIG appropriately"
    fi

    for devicetree in ${UBOOT_DEVICETREES}; do
        unset i
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            unset j
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                unset k
                for binary in ${UBOOT_BINARIES}; do
                    k=$(expr $k + 1);
                    if [ $i -ne $j ] || [ $i -ne $k ]; then
                        continue
                    fi

                    oe_runmake -C ${S} O=${B}/${config} DEVICE_TREE=${devicetree}

                    local binarysuffix=$(echo ${binary} | cut -d'.' -f2)
                    install -m 644 ${B}/${config}/${binary} ${B}/${config}/u-boot-${devicetree}-${type}.${binarysuffix}
                    install -m 644 ${B}/${config}/${SPL_BINARY} ${B}/${config}/${SPL_NAME}-${devicetree}-${type}.stm32

                    if [ ${@d.getVar('UBOOT_DEPLOY_ELF_FILES')} ]; then
                        install -m 755 ${B}/${config}/${UBOOT_ELF} ${B}/${config}/u-boot-${devicetree}-${type}.${UBOOT_ELF_SUFFIX}
                        install -m 755 ${B}/${config}/${SPL_ELF} ${B}/${config}/${SPL_ELF_NAME}-${devicetree}-${type}
                    fi

                done
            done
        done
    done
}

# -----------------------------------------------------------------------------
# We want to have more logical names in the deploy/ directory, and not so many
# damn symlinks. To do that, we need to forego the core recipe.
#
do_deploy[sstate-outputdirs] = "${DEPLOY_DIR_IMAGE}/bootloader"
do_deploy() {

    install -d ${DEPLOYDIR}

    for devicetree in ${UBOOT_DEVICETREES}; do
        unset i
        for config in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            unset j
            for type in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                unset k
                for binary in ${UBOOT_BINARIES}; do
                    k=$(expr $k + 1);
                    if [ $i -ne $j ] || [ $i -ne $k ]; then
                        continue
                    fi

                    binarysuffix=$(echo ${binary} | cut -d'.' -f2)
                    install -m 644 ${B}/${config}/u-boot-${devicetree}-${type}.${binarysuffix} ${DEPLOYDIR}
                    install -m 644 ${B}/${config}/${SPL_NAME}-${devicetree}-${type}.stm32 ${DEPLOYDIR}

                    if [ ${@d.getVar('UBOOT_DEPLOY_ELF_FILES')} ]; then
                        install -m 755 ${B}/${config}/u-boot-${devicetree}-${type}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}
                        install -m 755 ${B}/${config}/${SPL_ELF_NAME}-${devicetree}-${type} ${DEPLOYDIR}
                    fi

                done
            done
        done
    done
}

# ELF debug binaries will have relocations. Don't complain about that
INSANE_SKIP_${PN} = "ldflags"
