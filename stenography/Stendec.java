import java.io.IOException;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.io.File;
import java.io.FileWriter;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Stendec {
    static int channels = 4;

    public static void main(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.out.println("Usage: java Stendec --image=IMAGE_FILE --output=OUTPUT_FILE");
            System.exit(1);
        }

        String image_file = null;  // required
        String out_file = null; // required

        for (String argument : args){
            String[] arg_split = argument.split("=");
            if (arg_split.length != 2) {
                System.out.println("Error reading argument: ");
                System.out.println(argument);
                System.exit(1);
            }
            if (arg_split[0].equals("--image")){
                image_file = arg_split[1];
                if (!image_file.contains(".png")){
                    System.out.println("Warning: image does not appear to be a .png file, continuing anyways.");
                }
                continue;
            }
            if (arg_split[0].equals("--output")){
                out_file = arg_split[1];
                continue;
            }
            System.out.println("Error reading argument (No match): ");
            System.out.println(argument);
            System.exit(1);
        }

        if (image_file == null || out_file == null){
            System.out.println("Missing one or more required arguments.");
            System.out.println("Usage: java Stendec --image=INPUT_FILE --output=OUTPUT_FILE");
            System.exit(1);
        }

        byte[] image_bytes = import_image(image_file);
        byte[] unstenographed_bytes = unstenographise(image_bytes);
        write_file(out_file, unstenographed_bytes);

        System.out.print("Successfully found ");
        System.out.print(unstenographed_bytes.length);
        System.out.print(" bytes in file ");
        System.out.println(image_file);
    }

    private static byte[] read_file(String path){
        byte[] input_bytes = null;
        try {
            input_bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e){
            System.out.println("One of the files does not exist or cannot be read.");
            System.out.print("file: ");
            System.out.println(path);
            System.out.println(e);
            System.exit(1);
        }
        if (input_bytes.length == 0) {
            System.out.println("File is empty");
            System.out.print("file: ");
            System.out.println(path);
            System.exit(1);
        }
        return input_bytes;
    }

    // inspired by tutorialspoint.com/how-to-get-pixels-rgb-values-of-an-image-using-java-opencv-library
    private static byte[] import_image(String path){
        File file = new File(path);
        byte[] output_bytes = null;
        try {
            BufferedImage img = ImageIO.read(file);
        
            output_bytes = new byte[img.getHeight() * img.getWidth() * channels];
            int counter = 0;
            for (int y = 0; y < img.getHeight(); y++) {
                for (int x = 0; x < img.getWidth(); x++) {
                    for (int i = 0; i < channels; i++){
                        int argb = img.getRGB(x,y);
                        output_bytes[counter] = (byte)((argb >> (i * 8)) & 0xff);
                        counter += 1;
                    }
                }
            }
        } catch (IOException e){
            System.out.println("Image file cannot be read.");
            System.out.print("file: ");
            System.out.println(path);
            System.out.println(e);
            System.exit(1);
        }
        
        return output_bytes;
    }

    private static byte[] unstenographise(byte[] image_bytes){
        int signature = 0;  // first bit of the 32 first bytes is the length of the message
        for (int i = 0; i < 32; i++){
            signature |= (image_bytes[i] & 1) << i;
        }
        if (signature != 0b01101001011100110110000101101011){
            System.out.println("Signature not found in file, are you sure there is data in here?");
            System.exit(1);
        }
        int depth = 0;  // first bit of the 64 next bytes is the length of the message
        for (int i = 32; i < 35; i++){
            depth |= (image_bytes[i] & 1) << (i-32);
        }
        depth += 1;
        int length = 0;  // first bit of the 64 next bytes is the length of the message
        for (int i = 35; i < 64; i++){
            length |= (image_bytes[i] & 1) << (i-35);
        }
        int mask = (1 << depth) - 1;
        image_bytes = Arrays.copyOfRange(image_bytes, 64, length*(8/depth)+64);  // extract data
        byte[] fitted_data = new byte[image_bytes.length];
        byte[] data_bytes = new byte[length];
        int data_index = 0;


        for (int i = 0; i < image_bytes.length; i++){
            fitted_data[i] |= image_bytes[i] & mask;
        }


        int image_index = 0;
        while (image_index < fitted_data.length){
            for (int bit = 0; bit < 8/depth; bit++){
                data_bytes[data_index] |= fitted_data[image_index] << bit*depth;
                image_index += 1;
            }
            data_index += 1;
        }


        /*
        while (counter < image_bytes.length){
            for (int b = 0; b < 8; b++){
                data_bytes[counter/8] += (image_bytes[counter] & 1) << b;
                counter += 1;
            }
        }*/

        return data_bytes; 
    }

    private static void write_file(String path, byte[] data){
        try {
            Files.write(Paths.get(path), data);
        } catch (IOException e){
            System.out.println("File could not be written to.");
            System.out.print("file: ");
            System.out.println(path);
            System.out.println(e);
            System.exit(1);
        }
    }
}
