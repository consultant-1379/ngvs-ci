######################
#  gconf workaround  #
######################
# Sanity limit memory to 16Gb to protect from gconfd-2 memleak bug
ulimit -v 16000000
ulimit -a

# Fix 99 char path size limit in gconfd-2
PARENT_FOLDER="$(dirname "${CDT_INSTALL_FOLDER}")"
mkdir -p ${PARENT_FOLDER}
rm -f ${CDT_INSTALL_FOLDER}
ln -s ${WORKSPACE} ${CDT_INSTALL_FOLDER}
ls -dl ${CDT_INSTALL_FOLDER}