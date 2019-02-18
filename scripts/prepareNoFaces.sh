#!/bin/bash

# USAGE: Run this script from the project root folder.

# Location of source images. Will not be overwritten.
sourcePath="./res/source/no-faces"

# Output folder path. Stuff here may be overwritten by this script.
outputPath="./res/generated/no-faces"

mkdir -p $outputPath # Create output directory if it does not exist
rm -r $outputPath/* # Clear output directory

cp $sourcePath/* $outputPath
