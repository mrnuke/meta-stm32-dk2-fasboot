SUMMARY = "Combined OP-TEE and Linux image for stm32mp"
LICENSE = "GPLv2"

#
# Adding devicetree overlays example:
#
#    KERNEL_DEVICETREE = "board-one.dtb device-two.dtb"
#    DEVICETREE_OVERLAYS[board-one] = "enable-foo"
#    DEVICETREE_OVERLAYS[device-two] = "disable-bar enable-baz"
#
# Where 'enable-foo.dto', 'disable-bar.dto', and 'enable-baz.dto' are already
# installed in ${STAGING_BASELIBDIR}/firmware/
#

DEPENDS = "virtual/kernel optee-os devicetree-overlays"

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


def fitimage_emit_optee(d, its, entry_number, optee_image, compression):
    hash_algo = d.getVar('FIT_HASH_ALG')
    arch = d.getVar('UBOOT_ARCH')
    load_address = d.getVar('OPTEE_LOADADDRESS')
    entry_address = d.getVar('OPTEE_ENTRYPOINT')

    its.writelines([
        f'		optee-{entry_number} {{\n',
        '			description = "OP-TEE secure world firmware";\n',
        f'			data = /incbin/("{optee_image}");\n',
        '			type = "tee";\n',
        f'			arch = "{arch}";\n',
        '			os = "tee";\n',
        f'			compression = "{compression}";\n',
        f'			load = <{load_address}>;\n',
        f'			entry = <{entry_address}>;\n',
        '			hash-1 {\n',
        f'				algo = "{hash_algo}";\n',
        '			};\n',
        '		};\n',
    ])


def fitimage_emit_linux(d, its, entry_number, kernel_image, compression):
    hash_algo = d.getVar('FIT_HASH_ALG')
    arch = d.getVar('UBOOT_ARCH')
    load_address = d.getVar('UBOOT_LOADADDRESS')
    entry_address = d.getVar('UBOOT_ENTRYPOINT')

    its.writelines([
        f'		kernel-{entry_number} {{\n',
        '			description = "Linux kernel";\n',
        f'			data = /incbin/("{kernel_image}");\n',
        '			type = "kernel";\n',
        f'			arch = "{arch}";\n',
        '			os = "linux";\n',
        f'			compression = "{compression}";\n',
        f'			load = <{load_address}>;\n',
        f'			entry = <{entry_address}>;\n',
        '			hash-1 {\n',
        f'				algo = "{hash_algo}";\n',
        '			};\n',
        '		};\n',
    ])


def fitimage_emit_dtb(d, its, entry_number, dtb_bin):
    hash_algo = d.getVar('FIT_HASH_ALG')
    arch = d.getVar('UBOOT_ARCH')

    its.writelines([
        f'		fdt-{entry_number} {{\n',
        '			description = "Flattened Device Tree blob";\n',
        f'			data = /incbin/("{dtb_bin}");\n',
        '			type = "flat_dt";\n',
        f'			arch = "{arch}";\n',
        '			compression = "none";\n',
        '			hash-1 {\n',
        f'				algo = "{hash_algo}";\n',
        '			};\n',
        '		};\n',
    ])


def fitimage_emit_config_secure(d, its, optee_id, devicetrees, kernel_id):
    hash_algo = d.getVar('FIT_HASH_ALG')

    list_of_fdts = ', '.join([f'"fdt-{fdt}"' for fdt in devicetrees])

    its.writelines([
        f'		secure-{devicetrees[0]} {{\n',
        f'			description = "Linux with OP-TEE for {devicetrees[0]}";\n',
        f'			kernel = "optee-{optee_id}";\n',
        f'			fdt = {list_of_fdts};\n',
        f'			loadables = "kernel-{kernel_id}";\n',
        '			hash-1 {\n',
        f'				algo = "{hash_algo}";\n',
        '			};\n',
        '		};\n',
    ])


def fitimage_emit_images_section(d, its):
    optee_bin = os.path.join(d.getVar('STAGING_BASELIBDIR'), 'firmware/tee.bin')
    linux_bin = os.path.join(d.getVar('DEPLOY_DIR_IMAGE'), 'kernel/zImage')
    binaries = [optee_bin, linux_bin]

    overlay_map = d.getVarFlags('DEVICETREE_OVERLAYS') or {}
    overlays = set()

    for dtb in d.getVar('KERNEL_DEVICETREE').split():
        devicetree, _ = os.path.splitext(dtb)
        overlays.update(overlay_map.get(devicetree, '').split())

    its.write('	images {')
    fitimage_emit_optee(d, its, 1, optee_bin, 'none')
    fitimage_emit_linux(d, its, 1, linux_bin, 'none')

    for dtb in d.getVar('KERNEL_DEVICETREE').split():
        dtb_path = os.path.join(d.getVar('DEPLOY_DIR_IMAGE'), 'kernel', dtb)
        fitimage_emit_dtb(d, its, dtb, dtb_path)
        binaries.append(dtb_path)

    for dto in overlays:
        dto_path = os.path.join(d.getVar('STAGING_BASELIBDIR'), 'firmware', dto + '.dto')
        fitimage_emit_dtb(d, its, dto + '.dto', dto_path)
        binaries.append(dto_path)

    its.write('	};\n')

    # Trying to run mkimage with missing binaries will cause a long backtrace
    # Make sure all the binaries exist, or stop right now.
    for image in binaries:
        if not os.path.exists(image):
            bb.fatal(f'Binary image not found: {image}')


def fitimage_emit_configurations_section(d, its):

    its.write('	configurations {')

    default_dtb = d.getVar('KERNEL_DEVICETREE').split()[0]
    its.write(f'		default = "secure-{default_dtb}";\n')

    overlay_map = d.getVarFlags('DEVICETREE_OVERLAYS') or {}

    for dtb in d.getVar('KERNEL_DEVICETREE').split():
        devicetree, _ = os.path.splitext(dtb)
        overlays = overlay_map.get(devicetree, '').split()
        dtb = [dtb] + [dto + '.dto' for dto in overlays]

        fitimage_emit_config_secure(d, its, 1, dtb, 1)

    its.write('	};\n')

def fitimage_write_its(d, its):
    distro = d.getVar('DISTRO_NAME')
    version = d.getVar('PV')
    machine = d.getVar('MACHINE')

    its.writelines( [
        '/dts-v1/\n;',
        '/ {\n',
        f'	description = "U-Boot fitImage for {distro}/{version}/{machine}";\n',
        '	#address-cells = <1>;\n'
    ])

    fitimage_emit_images_section(d, its)
    fitimage_emit_configurations_section(d, its)

    its.write('};\n')


# Both run_mkimage and do_assemble_fitimage must agree that the .its file
# is located in ${B}/fit-image.its
run_mkimage() {
	uboot-mkimage \
		${@'-D "${UBOOT_MKIMAGE_DTCOPTS}"' if len('${UBOOT_MKIMAGE_DTCOPTS}') else ''} \
		-f ${B}/fit-image.its \
		${B}/fitImage
}


python do_assemble_fitimage() {
    if 'fitImage' not in d.getVar('KERNEL_IMAGETYPES'):
        return False

    its_filename = os.path.join(d.getVar('B'), 'fit-image.its')

    with open(its_filename, 'w') as its:
        fitimage_write_its(d, its)

    bb.build.exec_func('run_mkimage', d)
}

# kernel zImage and dtbs are not installed to sysroot. We get those files from
# ${DEPLOY_DIR_IMAGE}. We need the kernel do_deploy() to have executed
do_assemble_fitimage[deptask] = "do_deploy"

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
