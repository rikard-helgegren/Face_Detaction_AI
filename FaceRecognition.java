public class FaceRecognition {

    public static void main(String[] args) {
        String ImageFolder = "";

        readImagesFromDatabase(folderName);
        preProcessImages(images); //only done first time
        saveImages(images);      //only done first time, don't override prievious folder
        searchForPatterns();
    }

    /*
    public placeholder readImagesFromDatabase(String folderName) {

    }

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
