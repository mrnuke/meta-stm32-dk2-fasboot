SUMMARY = "Combined OP-TEE and Linux image for stm32mp"
LICENSE = "GPLv2"

DEPENDS = "virtual/kernel optee-os"

UBOOT_LOADADDRESS = "${UBOOT_ENTRYPOINT}"
KERNEL_IMAGETYPES += "fitImage"
OPTEE_TZDRAM_START ?= "0xfe000000"

# OP-TEE loadaddress is 0x1c bytes below the entry point
OPTEE_ENTRYPOINT = "${OPTEE_TZDRAM_START}"
OPTEE_LOADADDRESS ?= "${@hex(int(d.getVar('OPTEE_TZDRAM_START'),0) - 0x1c)}"

inherit kernel-fitimage kernel-arch deploy

python __anonymous () {
    kernel_image_types = d.getVar('KERNEL_IMAGETYPES')

    if 'zImage' not in kernel_image_types:
        bb.fatal('kernel zImage is required, but is not built. Check KERNEL_IMAGETYPES')
}

fitimage_emit_section_optee() {
	local its_filename=$1
	local entry_number=$2
	local optee_image=$3
	local compression=$4

	kernel_csum="${FIT_HASH_ALG}"

	cat << EOF >> ${its_filename}
                optee@${entry_number} {
                        description = "OP-TEE secure world firmware";
                        data = /incbin/("${optee_image}");
                        type = "kernel";
                        arch = "${UBOOT_ARCH}";
                        os = "linux";
                        compression = "${compression}";
                        load = <${OPTEE_LOADADDRESS}>;
                        entry = <${OPTEE_ENTRYPOINT}>;
                        hash@1 {
                                algo = "${kernel_csum}";
                        };
                };
EOF
}

fitimage_emit_section_config_secure() {
	local its_filename=$1
	local optee_id=$2
	local devicetree=$3
	local kernel_id=$4
	local write_default_line=$5
	local default_line=""

	kernel_csum="${FIT_HASH_ALG}"

	if [ "${write_default_line}" = "1" ]; then
		default_line="default = \"secure@${devicetree}\";"
	fi

	cat << EOF >> ${its_filename}
		${default_line}
                secure@${devicetree} {
			description = "Linux with OP-TEE in CrustZone™";
			kernel = "optee@${optee_id}";
			fdt = "fdt@${devicetree}";
			loadables = "kernel@${kernel_id}";
                        hash@1 {
                                algo = "${kernel_csum}";
                        };
                };
EOF
}

fitimage_emit_devicetrees() {
	local its_filename=$1

	for DTB in ${KERNEL_DEVICETREE}; do
		DTB_PATH="${DEPLOY_DIR_IMAGE}/kernel/${DTB}"
		fitimage_emit_section_dtb ${its_filename} ${DTB} ${DTB_PATH}
	done
}

fitimage_emit_images_section() {
	local its_filename=$1
	local kernelcount="1"

	fitimage_emit_section_maint ${its_filename} imagestart

	fitimage_emit_section_optee ${its_filename} "${kernelcount}" "${DEPLOY_DIR_IMAGE}/optee/tee.bin" "none"
	fitimage_emit_section_kernel ${its_filename} "${kernelcount}" "${DEPLOY_DIR_IMAGE}/kernel/zImage" "none"
	fitimage_emit_devicetrees ${its_filename}

	fitimage_emit_section_maint ${its_filename} sectend

}

fitimage_emit_configurations_section() {
	local its_filename=$1
	local kernelcount="1"

	fitimage_emit_section_maint ${its_filename} confstart

	i=1
	for DTB in ${KERNEL_DEVICETREE}; do
		fitimage_emit_section_config_secure ${its_filename} "${kernelcount}" "${DTB}" "1" "`expr ${i} = 1`"
		i=`expr ${i} + 1`
	done

	fitimage_emit_section_maint ${its_filename} sectend
}

fitimage_assemble() {
	local its_filename=$1
	local fit_image=$2
	local ramdisk=$3

	rm -f ${its_filename} arch/${ARCH}/boot/${fit_image}

	fitimage_emit_fit_header ${its_filename}

	fitimage_emit_images_section ${its_filename}
	fitimage_emit_configurations_section ${its_filename}

	fitimage_emit_section_maint ${its_filename} fitend

	uboot-mkimage \
		${@'-D "${UBOOT_MKIMAGE_DTCOPTS}"' if len('${UBOOT_MKIMAGE_DTCOPTS}') else ''} \
		-f ${its_filename} \
		${fit_image}
}

do_install() {
	install -D -m 0644 ${B}/fitImage ${D}/boot/fit-image-unsigned-${KERNEL_FIT_NAME}.img
	ln -snf fit-image-unsigned-${KERNEL_FIT_NAME}.img "${D}/boot/fit-image-unsigned-${KERNEL_FIT_LINK_NAME}"
}

FILES_${PN} = "/boot"

do_deploy() {
	install -m 0644 ${B}/fitImage ${DEPLOYDIR}/fit-image-unsigned-${KERNEL_FIT_NAME}.img
	ln -snf fit-image-unsigned-${KERNEL_FIT_NAME}.img "${DEPLOYDIR}/fit-image-unsigned-${KERNEL_FIT_LINK_NAME}"
}

addtask deploy after do_install
