#
# Recipe for a linux rootfs with graphical capabilities
#
DESCRIPTION = "HorseSHIfT image with developer and debug tools"

require image-horseshift.bb

IMAGE_FEATURES += "\
	ssh-server-dropbear \
	tools-debug \
	"

IMAGE_INSTALL:append = "\
	can-utils \
	iproute2 \
	ldd \
	nano \
	optee-examples \
	pv \
	util-linux \
	"
