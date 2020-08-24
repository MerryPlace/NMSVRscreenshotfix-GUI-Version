
package nmsvrscreenshotfix;

import java.io.File;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import static java.lang.Character.isLetterOrDigit;
import java.io.IOException;


/**
 * Program Description: 
 * Iterates through every image in the source folder and if applicable, 
 * based on the users selected settings, converts the files into the result folder.
 * @author Noah Ortega
 */
public class NMSVRScreenshotFix {    
    /**
     * Calls on the logic controller singleton.
     */
    public static void main(String[] args) {
        LogicController.getInstance();
    }
}

/**
 * Controller which handles any methods which handle program logic or state.
 */
class LogicController {
    // State variables 
    private boolean isExecuting = false;
    private boolean canceled = false;
    
    // File handling variables 
    private int totalFiles;
    private int filesConverted;
    private BufferedImage curImage;
    
    // Path variables (changed via main ProgramUI)
    public String sourcePath;
    public String resultPath;
 
    // Behavior Settings (changed via SettingsUI)
    public boolean shouldRename = true;
    public boolean renameNewFile = true;
    public String addTextToFileName = "_fix";
    public boolean addAsPrefix = false;
    
    int MAX_ADD_TEXT_LENGTH = 50;
    
    ProgramUI myUI;
    
    /**
     * Constructor: initiates paths to current directory calls to launch the UI 
     */
    private LogicController() {
        sourcePath = System.getProperty("user.dir");
        resultPath = System.getProperty("user.dir");
        launchUI();
    }
    
    /** 
     * Singleton: ensures there is only one instance of this class.
     */
    private static LogicController sharedController = null;
    public static LogicController getInstance() {
        if(sharedController == null) {
            sharedController = new LogicController();
        }
        return sharedController;
    }
    
    /**
     * Standard Java Swing launch, aesthetics, and beginning the UI thread
     */
    private void launchUI() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ProgramUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ProgramUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ProgramUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ProgramUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
               myUI = new ProgramUI();
               myUI.setVisible(true);
            }
        });
    }   
    
    /**
     * Iterates through files in the source directory, validates files before 
     * allowing resizing. Halts execution if necessary.
     * @throws IOException if a file cannot be read from. skips, execution continues
     * @throws NullPointerException if an image is not properly encoded as it's file type.
     *  Gives the user a warning that the file may be corrupted. skips, execution continues
     */
    public void execute() {
        System.out.println("Starting Execution");
        isExecuting = true;
        canceled = false;
        myUI.updateProgressBar(0);
        filesConverted = 0;
        
        //folder of original screenshots
        File sourceFolder = new File(sourcePath);
        
        File[] folderContents = sourceFolder.listFiles();
        totalFiles = folderContents.length;
        File curFile;
        
        //disables UI
        myUI.toggleUI();
        
        for (int fileIndex = 0; fileIndex < totalFiles && !canceled; fileIndex++) {
            curFile = folderContents[fileIndex];
            if (!folderContents[fileIndex].isDirectory() && isImage(curFile.getPath())) {
                System.out.println(">file name: " + curFile.getName());
                try {
                    curImage = ImageIO.read(folderContents[fileIndex]);

                    if (shouldResize(curImage.getWidth(), curImage.getHeight())) {
                        squish(curFile);
                    }
                }
                catch (IOException ex) {
                    System.err.println(">> Caught IOException on '" + curFile.getPath() + "':\n  " + ex.getMessage());
                }
                catch (NullPointerException ex) {
                    System.out.println(">> ImageIO.read returned null (likely corrupt): " + ex);
                    myUI.errorCorruptImage(curFile.getName());
                }
            }
            myUI.updateProgressBar((fileIndex + 1)*100/totalFiles);
        }
        if(canceled) {
            myUI.cancelPopup(filesConverted);
            canceled = false;
        } else {
            myUI.completePopup(filesConverted);
        }
        
        isExecuting = false;
        myUI.toggleUI();
        System.out.println("Finished Execution");
    }
    
    /**
     * Resizes an image to a 1:1 aspect ratio by changing the width
     * @param originalFile Path of the original input image
     * @throws IOException if a file cannot be written. gives user error. cancels execution.
     * 
     * based on code by Nam Ha Minh from article "How to resize images in Java"
     * https://www.codejava.net/java-se/graphics/how-to-resize-images-in-java
     */
    private void squish(File originalFile) throws IOException {
        int newWidth = curImage.getHeight();
        int height = curImage.getHeight();
        // creates output image
        BufferedImage outputImage = new BufferedImage(newWidth, height, curImage.getType());
        
        // scales the input image to the output image
        Graphics2D g2d = outputImage.createGraphics();
        g2d.drawImage(curImage, 0, 0, newWidth, height, null);
        g2d.dispose();
        
        int fileDotIndex = originalFile.getName().lastIndexOf('.');
        String formatName = originalFile.getName().substring(fileDotIndex + 1);
        File newFile = new File(resultPath +"/"+ originalFile.getName());
        if(shouldRename) {
            if (renameNewFile) {
                newFile = modifyFilePath(resultPath,originalFile.getName());
            }
            else {
                originalFile.renameTo(modifyFilePath(sourcePath,originalFile.getName()));
            }
        }

        try {
            // write to output file
            ImageIO.write(outputImage, formatName, newFile);
            filesConverted++;
            System.out.println(">> Converted");
        }
        catch(Exception ex) {
            myUI.errorWriting();
            System.err.print(">> Error writing to result folder " + ex.getClass());
            canceled = true;
        }
    }
    
    /**
     * State variable setter, allows other classes to cancel execution
     */
    public void cancelExecution() {
        canceled = true;
    }
    
    /**
     * Getter for state variable isExecuting
     * @return true if the program is converting files.
     */
    public boolean getIsExecuting() {
        return isExecuting;
    }
    
    /**
     * Generates a string which attempts to explain to the user the consequences 
     * of their behavior setting choices.
     * @return String representing the current user selected settings
     */
    public String getCurrentBehaviorString() {
        String behavior = "";
        if(!shouldRename && (sourcePath.equals(resultPath))) {
            behavior += "• Replacing originals with converted screenshots";
        }
        else {
            behavior += "• Making copies of converted screenshots";
        }
        
        behavior +="\n";
        if(shouldRename) {
            if(addAsPrefix) {
                behavior += "• Adding prefix ";
            }
            else {
                behavior += "• Adding suffix ";
            }
            behavior += "\""+addTextToFileName+"\" to ";
            if(renameNewFile) {
                behavior += "converted image";
            }
            else {
                behavior += "original image";
            }
            behavior += "\n";
            behavior += "• " + getExampleRename();
        }
        return behavior;
    }
    
    /**
     * based on settings, adds a prefix or suffix onto the file name
     * @param oldName the name of the original file before conversion
     * @return the modified file name to be used for the new image
     */
    private String getRename(String oldName) {
        int dotIndex = oldName.lastIndexOf('.');
        
        String name = oldName.substring(0, dotIndex);
        String ext = oldName.substring(dotIndex);
        
        return addAsPrefix ? (addTextToFileName+name+ext) : (name+addTextToFileName+ext);
    }
    
    /**
     * Based on settings, creates an string showing an example renaming
     * Helper function for {@link getCurrentBehaviorString}
     * @return string explaining what the rename will look like
     */
    private String getExampleRename() {
        String exampleName = renameNewFile ? "converted.png" : "original.png";
        return ("Ex: \"" + exampleName + "\" -> \"" + getRename(exampleName) +"\"");
    }  
    
    /**
     * Concatenates parent folder path with the file name 
     * @param parentPath path of the folder holding the file
     * @param fileName the name of the file
     * @return concatenated file path
     */
    private File modifyFilePath(String parentPath, String fileName) {
        return (new File(parentPath + "/" + getRename(fileName)));
    }
    
    /**
     * Determines if a file is a basic image type
     * @param path file path of image
     * @return true if extension is png jpg or jpeg
     */
    private boolean isImage(String path) {
        //extention of file, from file after final period
        int dotIndex = path.lastIndexOf('.');
        String extension = (dotIndex == -1) ? "no extension" : path.substring(dotIndex + 1);
        extension = extension.toLowerCase();
        return (extension.equals("png") || extension.equals("jpg") || extension.equals("jpeg"));
    }
    
    /**
     * Determines if an image should be resized based on criteria:
     * - width must be greater than height
     * @param width image width
     * @param height image height
     * @return true if image matches criteria
     */
    private boolean shouldResize(int width, int height) {
        if (width == height) {
            System.out.println(">> Already 1:1 aspect ratio");
            return false;
        } else if (width < height) {
            System.out.println(">> Height is greater than width");
            return false;
        } else {
            return true;
        }
    }
    
    /**
     * For generating the path of the output image
     * @param inputPath Path of the original input image
     * @param dotIndex the index of the dot in the input path
     * @return the final output path
     */
    public String generateOutputPath(String inputPath, int dotIndex) {
        return inputPath.substring(0, dotIndex) + addTextToFileName + inputPath.substring(dotIndex);
    }
    
    /**
     * Validates text that will be added to file name. Triggers warning popups if false.
     * @param phrase the text addition to be tested
     * @return true if there is no problem with text
     */
    public boolean isValidTextAddition(String phrase) {
        if(phrase.length() == 0) {
            myUI.warningEmptyText();
            return false;
        }
        
        if(phrase.length() > MAX_ADD_TEXT_LENGTH) {
            myUI.warningExceededTextLimit();
            return false;
        }
        
        for (int charIndex = 0; charIndex < phrase.length(); charIndex++) {
            char curChar = phrase.charAt(charIndex);
            if(!isLetterOrDigit(curChar)) {
                if(curChar != '_' && curChar != '-') {
                    myUI.warningInvalidText(curChar);
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Verifies that source and result paths are existing directories
     * @return true if source and result paths are valid
     */
    public boolean hasValidDirectoryPaths() {
        return((new File(sourcePath)).isDirectory() && (new File(resultPath)).isDirectory());
    }
}