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

SRCREV = "v2020.10"
LIC_FILES_CHKSUM = "file://Licenses/README;md5=5a7450c57ffe5ae63fd732446b988025"

FILESEXTRAPATHS_append := ":${THISDIR}/patches"
FILESEXTRAPATHS_append := ":${THISDIR}/config"

SRC_URI += "file://0001-stm32mp1-Add-support-for-baudrates-higher-than-11520.patch"
SRC_URI += "file://0002-stm32mp1-Add-support-for-falcon-mode-boot.patch"
SRC_URI += "file://0003-mmc-stm32_sdmmc2-Use-mmc_of_parse-to-read-host-capab.patch"
SRC_URI += "file://0004-mmc-mmc_of_parse-Enable-52-MHz-support-with-cap-mmc-.patch"
SRC_URI += "file://0005-fit-Provide-default-symbol-for-board_fit_config_name.patch"
SRC_URI += "file://0006-spl-fit-Prefer-a-malloc-d-buffer-for-loading-images.patch"
SRC_URI += "file://0007-stm32mp1-Increase-SPL-malloc-size.patch"
SRC_URI += "file://1001-Revert-Fix-data-abort-caused-by-mis-aligning-FIT-dat.patch"
SRC_URI += "file://baudrate.cfg"
SRC_URI += "file://boot-delay.cfg"
SRC_URI += "file://falcon-mode.cfg"
SRC_URI += "file://optee.cfg"
SRC_URI += "file://fit-image.cfg"

SPL_BINARY = "spl/u-boot-spl.stm32"
SPL_BINARYNAME = "${@os.path.basename(d.getVar("SPL_BINARY"))}"
SPL_NAME = "${@os.path.splitext(d.getVar("SPL_BINARY"))[0]}"

# Configure for debug elf
UBOOT_DEPLOY_ELF_FILES ?= ""
UBOOT_ELF = "u-boot"
SPL_ELF = "${@d.getVar('SPL_BINARY').split('.')[0]}"
SPL_ELF_NAME  = "${@os.path.basename(d.getVar("SPL_ELF"))}.elf"
OPTEE_TZDRAM_START ?= ""

replace_config_variable() {
	local dotconfig=$1
	local cfgname=$2
	local value=$3

	sed "s/\(${cfgname}\)=.*/\1=${value}/" -i ${dotconfig}
}

replace_config() {
	local cfg=$1
	local value=$2

	for defconfig in ${UBOOT_MACHINE}; do
		replace_config_variable "${B}/${defconfig}/.config" "${cfg}" "${value}"
	done
}

do_configure_append() {
	if [ -n "${OPTEE_TZDRAM_START}" ]; then
		replace_config "CONFIG_OPTEE_TZDRAM_BASE" "${OPTEE_TZDRAM_START}"
	fi
}

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
        for defconfig in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            unset j
            for config_name in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                unset k
                for binary in ${UBOOT_BINARIES}; do
                    k=$(expr $k + 1);
                    if [ $i -ne $j ] || [ $i -ne $k ]; then
                        continue
                    fi

                    replace_config_variable ${B}/${defconfig}/.config "CONFIG_OF_LIST" \""${devicetree}\""
                    oe_runmake -C ${S} O=${B}/${defconfig} DEVICE_TREE=${devicetree}

                    local binarysuffix=$(echo ${binary} | cut -d'.' -f2)
                    install -m 644 ${B}/${defconfig}/${binary} ${B}/${defconfig}/u-boot-${devicetree}-${config_name}.${binarysuffix}
                    install -m 644 ${B}/${defconfig}/${SPL_BINARY} ${B}/${defconfig}/${SPL_NAME}-${devicetree}-${config_name}.stm32

                    if [ ${@d.getVar('UBOOT_DEPLOY_ELF_FILES')} ]; then
                        install -m 755 ${B}/${defconfig}/${UBOOT_ELF} ${B}/${defconfig}/u-boot-${devicetree}-${config_name}.${UBOOT_ELF_SUFFIX}
                        install -m 755 ${B}/${defconfig}/${SPL_ELF} ${B}/${defconfig}/${SPL_ELF_NAME}-${devicetree}-${config_name}
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
        for defconfig in ${UBOOT_MACHINE}; do
            i=$(expr $i + 1);
            unset j
            for config_name in ${UBOOT_CONFIG}; do
                j=$(expr $j + 1);
                unset k
                for binary in ${UBOOT_BINARIES}; do
                    k=$(expr $k + 1);
                    if [ $i -ne $j ] || [ $i -ne $k ]; then
                        continue
                    fi

                    binarysuffix=$(echo ${binary} | cut -d'.' -f2)
                    install -m 644 ${B}/${defconfig}/u-boot-${devicetree}-${config_name}.${binarysuffix} ${DEPLOYDIR}
                    install -m 644 ${B}/${defconfig}/${SPL_NAME}-${devicetree}-${config_name}.stm32 ${DEPLOYDIR}

                    if [ ${@d.getVar('UBOOT_DEPLOY_ELF_FILES')} ]; then
                        install -m 755 ${B}/${defconfig}/u-boot-${devicetree}-${config_name}.${UBOOT_ELF_SUFFIX} ${DEPLOYDIR}
                        install -m 755 ${B}/${defconfig}/${SPL_ELF_NAME}-${devicetree}-${config_name} ${DEPLOYDIR}
                    fi

                done
            done
        done
    done
}

# ELF debug binaries will have relocations. Don't complain about that
INSANE_SKIP_${PN} = "ldflags"
