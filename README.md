# TIN175 Face Detection Using Viola-Jones Algorithm

The [ATT Database of Faces](https://www.cl.cam.ac.uk/research/dtg/attarchive/facedatabase.html).

The [Catalano Framework](https://github.com/DiegoCatalano/Catalano-Framework) used for image processing.


## Current Usage
When running files, make sure that your current working directory is the project root. If you are running from the terminal this means you should be at `[placeInYourFileSystem]/code`, not in any subfolder. If you are running from an IDE such as Eclipse or Intellij IDEA you need to make sure the current working directory is correctly set in your current run configuration. If you have not done this, any hardcoded paths will fail to find the right images etc.

1. Run java file in project root to copy and rename images from att folder structure to `att-faces-java`.
2. Run `prepareImages.sh` (bash script) to crop and rescale all images into `att-faces-scaled`.
3. Use cropped and rescaled images somehow.


## TODO

* Get and add to the dataset images that are not faces.
