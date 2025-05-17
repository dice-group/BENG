#!/bin/bash
set -eu
# Export path to Java
export JAVA_HOME="/data/softwares/jdk-11"
export PATH=$JAVA_HOME/bin:$PATH

# Export path to Conda
#export PATH="/data/softwares/miniforge3/bin:$PATH"
source /data/softwares/miniforge3/etc/profile.d/conda.sh

export BENG_HOME="/data/BENG/"

java_ver=$(java --version)
conda_ver=$(conda --version)

#echo "${USER} executed the beng server service. Java version ${java_ver}, Conda version ${conda_ver}" >> $BENG_HOME/server_status.log
cd $BENG_HOME
echo "Server has been restarted on $(date)" >> server_restart_status.log

# Activating conda environment
conda activate ./conda-venv-beng
# Starting BENG server
bash start.sh > beng_server_run.log 2>&1
