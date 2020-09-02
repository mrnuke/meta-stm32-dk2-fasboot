SUMMARY = "Linux stm32mp kernel"
SECTION = "kernel"
LICENSE = "GPLv2"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

COMPATIBLE_MACHINE = "(stm32mp)"
KERNEL_UIMAGE_LOADADDRESS ?= "0xC2000040"

inherit kernel
require recipes-kernel/linux/linux-yocto.inc

LINUX_VERSION = "5.8"
LINUX_SUBVERSION = "8"

FILESEXTRAPATHS_prepend := "${THISDIR}/config-${LINUX_VERSION}:"

SRC_URI = "https://cdn.kernel.org/pub/linux/kernel/v5.x/linux-${LINUX_VERSION}.${LINUX_SUBVERSION}.tar.xz"
SRC_URI[sha256sum] = "33f61bb3e99a4b8bcc0fdfc7e7d72071795bccba465184665a9ae7bd7f00a976"

SRC_URI += "file://fragment-01-multiv7_cleanup.cfg"
SRC_URI += "file://fragment-02-multiv7_addons.cfg"
SRC_URI += "file://fragment-03-systemd.cfg"
SRC_URI += "file://fragment-04-optee.cfg"
SRC_URI += "file://fragment-05-modules.cfg"
SRC_URI += "file://fragment-06-signature.cfg"
SRC_URI += "file://fragment-07-features-cleanup.cfg"
SRC_URI += "file://fragment-08-modules-builtin.cfg"
SRC_URI += "file://fragment-90-big-mess.cfg"
SRC_URI += "file://fragment-91-usb-gadget-workaround.cfg"

PV = "${LINUX_VERSION}.${LINUX_SUBVERSION}"
S = "${WORKDIR}/linux-${LINUX_VERSION}.${LINUX_SUBVERSION}"

KMETA = "kernel-meta"
KBUILD_DEFCONFIG_stm32mp1 ?= "multi_v7_defconfig"
KCONFIG_MODE = "alldefconfig"

KERNEL_EXTRA_ARGS += "LOADADDR=${KERNEL_UIMAGE_LOADADDRESS}"
