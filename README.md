# TIN175 Face Detection Using Viola-Jones Algorithm

The [ATT Database of Faces](https://www.cl.cam.ac.uk/research/dtg/attarchive/facedatabase.html).

The [Catalano Framework](https://github.com/DiegoCatalano/Catalano-Framework) is used only for its FastBitmap class. The library is distributed in this repo for convenience. It is licensed under LGPL and you can find it's copyright notice and full license text in `CATALANO-COPYRIGHT.txt` and `CATALANO-LICENSE.txt` respectively.

## Setup
First, place all datasets inte the `res` folder. Path to each dataset is set in code in the `Data` class but it can be beneficial to have the following folder layout:
```
res/
├── faces/
│   ├── my-dataset/
│   │   ├── face.jpg
│   │   └── face2.png
│   └── another-dataset/
└── non-faces/
    ├── my-dataset/
    │   ├── no-face.jpg
    │   └── car.png
    └── another-dataset/
```

Second, set up your local development environment. One way to setup your local development environment for this project is the following.

1. Create an IDE project (Intellij IDEA, Eclipse, etc.) in the __parent__ folder of project root, that is, outside of source control (git).
2. Add the repository folder (by default called `code`) to your project if it was not visible automatically.
3. Add libraries in `code/libs` to your project libraries. (In Intellij: File -> Project structure -> libraries -> plus symbol -> select all jars and the zip file containing javadoc)
4. Mark `code/src` as the code source folder. (In Intellij, right click -> mark as.. -> source folder)
5. Mark `code/res` as resources folder, `code/test` as test source, `code/test-res` as test resources.
6. Try running a main file to create a run configuration. It will fail to find any files to run on. Now edit your run configuration and set the __working directory__ to point to the project root directory (`code`).

If you're not using an IDE you will have to manually make sure that libraries are on your path when compiling/running.

## Current Usage
When running files, make sure that your current working directory is the project root. If you are running from the terminal this means you should be at `[placeInYourFileSystem]/code`, not in any subfolder. If you are running from an IDE such as Eclipse or Intellij IDEA you need to make sure the current working directory is correctly set in your current run configuration. If you have not done this, any hardcoded paths will fail to find the right images etc.

At the top of `hal2019.training.TrainClassifiers.java` is a boolean variable `loadFromFile` that allows you to load a trained network from file rather than training a new one from scratch.

## Things that are already done
* Get some face images.
* Get some no-face images.
* Load images into java application.
* Create integral images in java.
* Create a function to calculate rectangle sum from integral image. See `hal2019.HalIntegralImage.getRectangleSum(x1, x2, y1, y2)`.
* Implement function for calculating the 4 features.
   * For horizontal rectangles.
   * For vertical rectangles.
* Implement core Adaboost algorithm.
* Finish Adaboost algorithm by iterating multiple times to create many weak hal2019.training.classifiers. Currently we only create a single weak classifier.
* Implement final cascade classifier using all the simple hal2019.training.classifiers. (But not finalized.)
* Implement a way to save and load trained hal2019.training.classifiers so that we can use them without first training them every time.
* Implement function for calculating the 4 features.
   * For three rectangles (the nose feature). Dummy function in `hal2019.training.Feature.calcThreeRectFeature()`.
   * For four rectangles (the checkerboard feature). Dummy function in `hal2019.training.Feature.calcFourRectFeature()`.
* Calculate __parity__ value in Adaboost single-classifier training.
* Multithreaded step 2 in adaboost.
* Test and improve AdaBoost and cascade classifier.
* Report performance for 36 feature strong classifier.
* Hand check S+T+ part of adaboost.
* Move calculating featurevalues so that it is done once.

## Things that are TODO
* Make single classifier training faster. Some leads:
   * Optimize feature calculation. hal2019.graphics.Rectangle sum can be calculated in 4 array accesses (currently 4). Two-rectangle features can be calculated in 6 (currently 8).  Three-rectangle in 8 (not implemented).  Four-rectangle in 9 (not implemented).
* Clean up and comment code.
