import java.io.File;

public class FaceRecognition {

    public static void main(String[] args) {
        String imageFolder = "att-faces";

        readImagesFromDatabase(imageFolder);
        //preProcessImages(images); //only done first time
        //saveImages(images);      //only done first time, don't override previous folder
        //searchForPatterns();
    }

    public static void readImagesFromDatabase(String folderName) {
        System.out.println("Reading images...");
        File folder = new File(folderName);
        for (File f : folder.listFiles()) {
            System.out.println(f.getName());
        }
    }

    /*
    public placeholder preProcessImages(placeholder images) {
        fixSize();
        fixContrast();
    }

    public placeholder fixSize(placeholder images) {

    }

    public placeholder fixContrast(placeholder images) {

    }

    public placeholder saveImages(placeholder images) {

    }

    public placeholder searchForPatterns(placeholder images) {
        //more methods needed
    }*/







}
