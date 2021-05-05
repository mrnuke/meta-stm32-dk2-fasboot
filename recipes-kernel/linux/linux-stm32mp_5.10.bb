SUMMARY = "Linux stm32mp kernel"
SECTION = "kernel"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

COMPATIBLE_MACHINE = "(stm32mp)"
KERNEL_UIMAGE_LOADADDRESS ?= "0xC2000040"

inherit kernel
require recipes-kernel/linux/linux-yocto.inc

LINUX_VERSION = "5.10"
LINUX_SUBVERSION = "10"

FILESEXTRAPATHS_prepend := "${THISDIR}/config-${LINUX_VERSION}:"
FILESEXTRAPATHS_prepend := "${THISDIR}/patches-${LINUX_VERSION}:"

SRC_URI = "https://cdn.kernel.org/pub/linux/kernel/v5.x/linux-${LINUX_VERSION}.${LINUX_SUBVERSION}.tar.xz"
SRC_URI[sha256sum] = "60ed866fa951522a5255ea37ec3ac2006d3f3427d4783a13ef478464f37cdb19"

SRC_URI += "file://fragment-01-multiv7_cleanup.cfg"
SRC_URI += "file://fragment-02-multiv7_addons.cfg"
SRC_URI += "file://fragment-04-optee.cfg"
SRC_URI += "file://fragment-05-modules.cfg"
SRC_URI += "file://fragment-06-signature.cfg"
SRC_URI += "file://fragment-07-features-cleanup.cfg"
SRC_URI += "file://fragment-08-modules-builtin.cfg"
SRC_URI += "file://fragment-09-can-bus.cfg"
SRC_URI += "file://fragment-90-big-mess.cfg"
SRC_URI += "file://fragment-91-usb-gadget-workaround.cfg"

PV = "${LINUX_VERSION}.${LINUX_SUBVERSION}"
S = "${WORKDIR}/linux-${LINUX_VERSION}.${LINUX_SUBVERSION}"

KMETA = "kernel-meta"
KBUILD_DEFCONFIG_stm32mp1 ?= "multi_v7_defconfig"
KCONFIG_MODE = "alldefconfig"

KERNEL_EXTRA_ARGS += "LOADADDR=${KERNEL_UIMAGE_LOADADDRESS}"
KERNEL_EXTRA_ARGS += "DTC_FLAGS='-@'"

do_deploy[sstate-outputdirs] = "${DEPLOY_DIR_IMAGE}/kernel"
