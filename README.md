# meta-horseshift

## Building

**meta-horseshift** is an OpenEmbedded layer for building STM32MP linux images
that boot really fast. It depends on the following layers:

 * openembedded-core
 * meta-openembedded
 * meta-linaro/meta-optee

### Layer Setup

	$ bitbake-layers add-layer /path/to/meta-horseshift
	$ bitbake-layers add-layer /path/to/meta-openembedded/meta-oe

The layer setup should look like the following:

	$ bitbake-layers show-layers
	NOTE: Starting bitbake server...
	layer                 path                                      priority
	==========================================================================
	meta                  <path-to>/openembedded-core/meta  5
	meta-oe               <path-to>/meta-openembedded/meta-oe  6
	meta-python           <path-to>/meta-openembedded/meta-python  7
	meta-optee            <path-to>/meta-linaro/meta-optee  8
	meta-horseshift       <path-to>/meta-horseshift  9

### Enabling image signing

Bootloader images can be signed automatically by the build; however this must
be configured separately from the rest of the build. The key must be an ECDSA
private key. The signing key for development boards can be found in
_meta-horseshift/keys/_ . The signing key for production releases is not
included in this repository.

  1. Copy the key to <build_dir/conf>
  2. Edit conf/local.conf to enable key signing:

	STM32_SIGNING_KEY = "${TOPDIR}/conf/signing-key.pem"
	STM32_SIGNING_ENABLE = "1"

### Build the images

	$ bitbake image-horseshift u-boot

### Normal vs -dev images

The __image-horseshift__ image provides all the packages and functionality
required for running a secure boot flow, and 3D graphics. However, for poking
around and debugging __image-horseshift-dev__ provides a better selection of
tools. These include the GDB debugger, SSH server, DHCP and zeroconf networking
support, and various command-line utilities. __image-horseshift-dev__ is
designed to make life easier in the lab, but  __image-horseshift__ should be
usable in the field.


## Installing to SD card

### Partitioning setup

The SD card must be partitioned as described in the table below. Start sectors
indicated in bold are hardcoded and the partition __must__ start there.

| Path           | Start sector      | Size (sectors)   | Name      |
|----------------|-------------------|------------------|-----------|
| /dev/mmcblk0p1 |            **34** |              512 | "fsbl1"   |
| /dev/mmcblk0p3 |          **1058** |             4096 | "ssbl"    |
| /dev/mmcblk0p4 |          **5154** |           131072 | "kernel"  |
| /dev/mmcblk0p5 |        **136226** |            32768 | "fdtargs" |
| /dev/mmcblk0p7 |           1705858 |          1439837 | "rootfs"  |

### Manual installation

After the build, images are found in __tmp-glibc/deploy/images/stm32mp1/__
NOTE: replace __-dk2__ with __-ev1__ if using the larger EV1 board.

	$ cp bootloader/u-boot-spl-stm32mp157c-dk2-nonsec.stm32 /dev/mmcblk0p1
	$ cp bootloader/u-boot-stm32mp157c-dk2-nonsec.img /dev/mmcblk0p3
	$ cp fit-image-unsigned-stm32mp1 /dev/mmcblk0p4
	$ cp image-horseshift-stm32mp1.ext4 /dev/mmcblk0p7

#### One-time u-boot configuration -- falcon mode

This step requires using the serial console to send u-boot commands.

 * Boot the board by holding by holding the __User PA13__ or __USER2__ button.
 * Keep holding the button until "Hit any key to stop autoboot" message appears.
 * At that point, hit any key and wait for the terminal prompt.

Finally, enter the following command to configure falcon mode:

	> load mmc 0:7 $loadaddr boot/fit-image-unsigned-stm32mp1

	> setenv bootargs console=ttySTM0,2000000 root=/dev/mmcblk0p7 init=/bin/fastinit
	> setenv bootm_boot_mode sec

	> spl export fdt $loadaddr#secure@stm32mp157c-dk2.dtb
	> mmc write $fdtargsaddr 0x9800 0x8000

Reboot the board, and do not hold any buttons. The kernel should now boot.
