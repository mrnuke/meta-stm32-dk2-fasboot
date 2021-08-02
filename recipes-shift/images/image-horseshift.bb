#
# Recipe for a linux rootfs with graphical capabilities
#
SUMMARY = "HorseSHIfT image"

inherit core-image

IMAGE_LINGUAS = "en-us"
IMAGE_FSTYPES = "ext4 squashfs"

CORE_IMAGE_EXTRA_INSTALL += " \
	boost-serialization \
	fastinit \
	optee-client \
	"

IMAGE_INSTALL_append = "kernel-image kernel-devicetree kernel-modules"
IMAGE_INSTALL_append += "fit-image"
