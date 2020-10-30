#
# Recipe for a linux rootfs with graphical capabilities
#
SUMMARY = "HorseSHIfT image"

inherit core-image

IMAGE_LINGUAS = "en-us"
IMAGE_FSTYPES = "ext4 squashfs"
IMAGE_FEATURES += "\
    ssh-server-dropbear \
    "

CORE_IMAGE_EXTRA_INSTALL += " \
	fastinit \
	optee-client \
	optee-examples \
	pv \
	nano \
	ldd \
	util-linux \
	"

IMAGE_INSTALL_append = "kernel-image kernel-devicetree kernel-modules"
IMAGE_INSTALL_append += "fit-image"
