#
# Recipe for a linux rootfs with graphical capabilities
#
DESCRIPTION = "HorseSHIfT image with developer and debug tools"

require image-horseshift.bb

IMAGE_FEATURES += "\
	ssh-server-dropbear \
	"

IMAGE_INSTALL_append = "\
	ldd \
	nano \
	optee-examples \
	pv \
	util-linux \
	"
