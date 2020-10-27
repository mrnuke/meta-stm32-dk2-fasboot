# meta-horseshift

## Building

**meta-horseshift** is an OpenEmbedded layer for building STM32MP linux images
that boot really fast. It depends on the following layers:

 * openembedded-core
 * meta-openembedded

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
	meta-horseshift       <path-to>/meta-horseshift  6


### Build the images

	$ bitbake image-horseshift u-boot


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
NOTE: replace __-ev1__ with __-dk2__ if using the smaller DK2 board.

	$ cp bootloader/u-boot-spl-stm32mp157c-ev1-nonsec.stm32 /dev/mmcblk0p1
	$ cp bootloader/u-boot-stm32mp157c-ev1-nonsec.img /dev/mmcblk0p3
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

	> spl export fdt $loadaddr#secure@stm32mp157c-ev1.dtb
	> mmc write $fdtargsaddr 0x9800 0x8000

Reboot the board, and do not hold any buttons. The kernel should now boot.
