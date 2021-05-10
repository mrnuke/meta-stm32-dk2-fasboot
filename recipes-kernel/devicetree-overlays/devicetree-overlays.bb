SUMMARY = "Devicetree overlays for stm32mp1 boards"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

DEPENDS = "dtc-native"
SRC_URI += "file://dk2-can1-enable.dts"

COMPATIBLE_MACHINE = "(stm32mp)"
KERNEL_BOOTARGS ?= ""

python do_configure() {
    consoles = d.getVar('SERIAL_CONSOLES').split()
    other_args = d.getVar('KERNEL_BOOTARGS')

    (baud, console) = consoles[0].split(';')
    arg = f'console={console},{baud} {other_args}';

    dts = open('bootargs.dts', 'w')

    dts.writelines( [
        '/dts-v1/;\n',
        '/plugin/;\n',
        '/ {\n',
        '	fragment@1 {\n',
        '		target-path = "/chosen";\n',
        '		__overlay__ {\n',
        f'			bootargs = "{arg}";\n',
        '		};\n',
        '	};\n',
        '};\n',
    ])

    dts.close()
}


do_compile() {
	dtc ${S}/bootargs.dts --out ${B}/bootargs.dto

	for dts in ${WORKDIR}/*.dts; do
		dtc $dts --out ${B}/$(basename $dts ".dts").dto
	done
}


do_install() {
	install -d ${D}${nonarch_base_libdir}/firmware/
	install -m 644 ${B}/*.dto ${D}${nonarch_base_libdir}/firmware/
}
FILES_${PN} = "${nonarch_base_libdir}/firmware/"
