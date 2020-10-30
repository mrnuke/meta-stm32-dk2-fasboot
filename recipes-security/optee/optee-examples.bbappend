# Workaround for "libgcc.a not found" error issue
TARGET_CFLAGS += "--sysroot=${STAGING_DIR_HOST}"
EXTRA_OEMAKE += "LDFLAGS="

DEPENDS += "python3-pycryptodomex-native"
