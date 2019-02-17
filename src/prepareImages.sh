 #!/bin/bash

 # USAGE: Run this script from the project root folder.

# Location of source images. Will not be overwritten.
imagePath="./res/att-faces-java"

# Output folder path. Stuff here may be overwritten by this script.
outputPath="./res/att-faces-scaled"

mkdir -p $outputPath # Create output directory if it does not exist
cp $imagePath/* $outputPath # Copy all images there

# Crop images by removing top 20 px rows
# Also apply auto-level to increase contrast
mogrify -crop 92x92+0+20 -auto-level $outputPath/*

# Rescale images
mogrify -resize 25x25 $outputPath/*
