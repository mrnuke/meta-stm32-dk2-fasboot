# Cross-compiling packages out-of-bitbake using the SDK

The OpenEmbedded build environment is suitable for creating complete system
images. It is, however, too resource and storage-intensive for developing a
specific program. In these cases, it is more efficient to work in a local
development environment.

The SDK can be used to independently develop and test code intended for the
target board. It contains the compiler, userspace libraries, and environment
setup script.

## Obtaining SDK

	$ bitbake image-horseshift -c populate_sdk

Note that this command is using _image-horseshift_, not _image-horseshift-dev_
as the base image. SDK imaged will appear __tmp-glibc/deploy/sdk/__

	$ ls tmp-glibc/deploy/sdk/*.sh
	tmp-glibc/deploy/sdk/oecore-x86_64-cortexa7t2hf-neon-vfpv4-toolchain-nodistro.0.sh

Copy this to the development machine, and run it as an executable.

	$ ./oecore-x86_64-cortexa7t2hf-neon-vfpv4-toolchain-nodistro.0.sh

It will ask for a destination directory. The exact location does not matter,
and may be changed from the default without consequence.

## Using the SDK

The SDK works by modifying the environment variables to use the desired
compiler and sysroot. Once the environment setup script is sourced, any
makefile based project should compile correctly for the target without any
modifications.

	$ cd <project to work on>
	$ source ./path/to/sdk/environment-setup-cortexa7t2hf...

To verify that the SDK environment is correctly set up, check the CC variable

	$ echo $CC
	arm-oe-linux-gnueabi-gcc -mthumb -mfpu=neon-vfpv4 -mfloat-abi=hard -mcpu=cortex-a7
		--sysroot=<sdk_path>/sysroots/cortexa7t2hf-neon-vfpv4-oe-linux-gnueabi

If the compiler arguments contain a __--sysroot__ argument pointing to the
location of the sdk, then the things are ready. Any make based project should
automatically pick up the correct compiler and libraries when `make` is invoked.

	$ cd <project to work on>
	$ source ./path/to/sdk/environment-setup-cortexa7t2hf...
	$ make <project_target>

## Issues using the SDK

Building under the SDK should be no different than building the program
natively. If the project builds natively, but not under the SDK, then the
problem is likely with how the makefile is written. This is beyond the scope
of this document.
