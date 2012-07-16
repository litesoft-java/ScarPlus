#!/bin/bash

###############################################
# iOSCreate.sh
#
# Usage: iOSCreate.sh projectName
#
# Creates an iOS directory containing a xcode iOS project
# for a generic phoneGap application with projectName as
# the name of the project. If iOS directory already exists
# it is zero'd out and a fresh xcode project is created
#
# A web app is already assumed to exist. This script
# looks for the file 'build.yaml' and the directory 'src'
# as a check to help make sure the current directory is
# a web app
# 
##############################################

if [ $# -ne 1 ]; then
	echo "
    Usage: $(basename $0) projectName

	projectName is the name of the project to be created
	"
    exit 1
fi

PROJECT_NAME=$1

# Check env variables
: ${JAVA_HOME:?"JAVA_HOME needs to be set"}
: ${SCAR_HOME:?"SCAR_HOME needs to be set"}

# Check to see if you are in a directory for a web application
if [ ! -f build.yaml -o ! -d src ]; then
	echo "'build.yaml' file and/or src directory do not exist. You are probably not in a project dir. Exiting"
	exit 3
fi

if [ -d iOS ]; then
    read -p "iOS directory already exists. Do you want to remove it and recreate it (y/n)? "
    if [ "$REPLY" != "y" ]; then
        echo "Exiting"
        exit 
    fi
    /bin/rm -rf iOS
fi

mkdir iOS

$SCAR_HOME/createPhoneGap_iOSproject.sh $PROJECT_NAME ./iOS

echo "
Done!
"

exit 0
