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
5. Try running a main file to create a run configuration. It will fail to find any files to run on. Now edit your run configuration and set the __working directory__ to point to the project root directory (`code`).

If you're not using an IDE you will have to manually make sure that libraries are on your path when compiling/running. In addition, do NOT mark the `res` folder as resources root in your IDE since it is likely to cause indexing on all resource files. This takes a long time when much data is present.

## Current Usage
When running files, make sure that your current working directory is the project root. If you are running from the terminal this means you should be at `[placeInYourFileSystem]/code`, not in any subfolder. If you are running from an IDE such as Eclipse or Intellij IDEA you need to make sure the current working directory is correctly set in your current run configuration. If you have not done this, any hardcoded paths will fail to find the right images etc.

At the top of `hal2019.training.TrainClassifiers` is a boolean variable `loadFromFile` that allows you to load a trained network from file rather than training a new one from scratch. There is also a boolean for training a cascade classifier or a regular strong classifier.

When training terminates, the classifier is automatically saved in `saves/save.cascade` or `saves/save.strong`. In addition, cascade classifiers are saved as `saves/autosave.cascade` after each finished layer. Be careful since existing saves with these names will be overwritten, rename or move any saves you want to keep.

## Startpoints for reading the code
The main program code is located in `/src/`. Some additional scripts for pre-processing of images and scraping, using [ImageMagick](http://www.imagemagick.org/) and [Scrapy](https://scrapy.org/) respectively, can be found in `/scripts/`.

Classifier training is initialized from `hal2019.training.TrainClassifiers`.

Data loading and data set preparation is done in `hal2019.Data`. There are several variables that can be tweaked to adjust how much data is in each set. Note that feature pre-calculation can be done on any subset of the data and will result in speed-up on that data. If features are not pre-calculated, they will be calculated every time when needed.

Image detection on real images is handled my `hal2019.graphics.Detector`. The path for the image to train on is set in that class.

One thing to note is that many methods throw generic exceptions. This is done deliberately to crash the program if an error occurs rather than to continue training with faulty values. As such, exceptions are also used to enforce pre-conditions in some stages of the algorithms. Ideally, custom exception sub-classes should be created, but this was not done initially.

## Future improvements
* Make single classifier training faster. Some leads:
   * Optimize feature calculation. Rectangle sum can be calculated in 4 array accesses (currently 4). Two-rectangle features can be calculated in 6 (currently 8).  Three-rectangle in 8 (not implemented).  Four-rectangle in 9 (not implemented).
