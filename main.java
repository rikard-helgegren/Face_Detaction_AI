public class FaceRecognition {

    public static void main(String[] args) {
        String ImageFolder = "";

        read_images_from_database(folderName);
        preproses_images(images); //only done first time
        save_images(images);      //only done first time, don't override prievious folder
        serch_for_patterns(); 


    }



    public placeholder read_images_from_database(String folderName) {
        
    }


    public placeholder preproses_images(placeholder images) {
        fix_size();
        fix_contrast();
        
    }
    public placeholder fix_size(placeholder images) {
        
    }
    public placeholder fix_contrast(placeholder images) {
        
    }



    public placeholder save_images(placeholder images) {
        
    }


    public placeholder serch_for_patterns(placeholder images) {
        //more methods needed
    }







}