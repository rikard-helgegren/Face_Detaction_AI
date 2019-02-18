#!/bin/bash

# USAGE: Run this script from the project root folder.

# Location of source images. Will not be overwritten.
sourcePath="./res/source/att-faces"

# Output folder path. Stuff here may be overwritten by this script.
outputPath="./res/generated/att-faces-scaled"

mkdir -p $outputPath # Create output directory if it does not exist
rm -r $outputPath/* # Clear output directory

number=0 # Variable for naming files at destination

# For each .pgm file in the source folder
for file in $(find . -path $sourcePath/\*.pgm); do
   cp $file $outputPath/$number.pgm # Copy it to the destination with a new name
   ((number++))
done

# Crop images by removing top 20 px rows
# Apply auto-level to increase contrast
# Convert them to png
mogrify -crop 92x92+0+20 -auto-level -format png $outputPath/*
rm $outputPath/*.pgm # Remove all pgm files (mogrify saved as png)

# Rescale images
mogrify -resize 25x25 $outputPath/*
