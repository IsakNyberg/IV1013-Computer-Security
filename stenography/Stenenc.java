import java.io.IOException;
import java.io.ByteArrayOutputStream;

import java.util.Arrays;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.io.File;
import java.io.FileWriter;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Stenenc {
    static int channels = 4;

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("Usage: java Stenenc --data=DATA_FILE --image=IMAGE_FILE --output=OUTPUT_FILE (--depth=1-8)");
            System.exit(1);
        }

        String image_file = null;  // required
        String out_file = null; // required
        String data_file = null; // required
        int depth = 1;

        for (String argument : args){
            String[] arg_split = argument.split("=");
            if (arg_split.length != 2) {
                System.out.println("Error reading argument: ");
                System.out.println(argument);
                System.exit(1);
            }
            if (arg_split[0].equals("--data")){
                data_file = arg_split[1];
                continue;
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
                if (!out_file.contains(".png")){
                    System.out.println("Warning: output does not have the .png extension, continuing anyways.");
                }
                continue;
            }
            if (arg_split[0].equals("--depth")){
                depth = Integer.parseInt(arg_split[1]);
                if (depth < 1 || depth > 8){
                    System.out.println("Depth must be in range 1 to 8.");
                    System.exit(1);
                }
                continue;
            }
            System.out.println("Error reading argument (No match): ");
            System.out.println(argument);
            System.exit(1);
        }

        if (image_file == null || out_file == null || data_file == null){
            System.out.println("Missing one or more required arguments.");
            System.out.println("Usage: java Stenenc --data=DATA_FILE --image=INPUT_FILE --output=OUTPUT_FILE");
            System.exit(1);
        }

        byte[] image_bytes = import_image(image_file);
        byte[] data_bytes = read_file(data_file);
        int[] dimensions = get_dimentions(image_file);

        if (image_bytes.length * depth < data_bytes.length * 8 + 64){
            System.out.println("The image is too small to fit the data.");
            System.out.print("Bytes that can be stored in ");
            System.out.print(image_file);
            System.out.print(" with a depth of ");
            System.out.print(depth);
            System.out.print(" is ");
            System.out.println(image_bytes.length/8 * depth - 64);
            System.out.print("Data size in bytes:  ");
            System.out.println(data_bytes.length);
            System.out.print("Difference in bytes: ");
            System.out.println(data_bytes.length - image_bytes.length/8);
            if (((data_bytes.length * 8)+64) / image_bytes.length < 8){
                System.out.println("The file might be able to fit depth is increase (max 8)");
                System.out.println("Higher depth means more distortion.");
                System.out.print("Use: --depth=");
                System.out.println(((data_bytes.length * 8)+64) / image_bytes.length + 1);
            }
            System.exit(1);
        }
        if (data_bytes.length % depth != 0){
            System.out.println("Warning: data length should be a multiple of the depth.");
            System.out.print("Continuing anyways, final message will be padded with ");
            System.out.print(data_bytes.length % depth);
            System.out.println(" random bytes.");
        }


        byte[] stenographed_bytes = stenographise(image_bytes, data_bytes, depth);
        byte[] output_bytes = array_to_image(stenographed_bytes, dimensions[0], dimensions[1]);
        //byte[] output_bytes  = array_to_image(image_bytes, dimensions[0], dimensions[1]);
        write_file(out_file, output_bytes);

        System.out.print("Successfully hid ");
        System.out.print(data_bytes.length);
        System.out.print(" bytes from file ");
        System.out.print(data_file);
        System.out.print(" in file ");
        System.out.print(out_file);
        System.out.print(" using depth ");
        System.out.println(depth);
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

    private static int[] get_dimentions(String path){
        File file = new File(path);
        int[] dimensions = new int[2];
        try {
            BufferedImage img = ImageIO.read(file);
            dimensions[0] = img.getWidth();
            dimensions[1] = img.getHeight();
        } catch (IOException e){
            System.out.println("Image file cannot be read.");
            System.out.print("file: ");
            System.out.println(path);
            System.out.println(e);
            System.exit(1);
        }
        
        return dimensions; // wish java had tuple like python
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
                        output_bytes[counter] = (byte)((argb >> (i * 8) ) & 0xff);
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

    private static byte[] stenographise(byte[] template, byte[] data, int depth){
        // first 4 is is to indicate that the file contains data
        int signature = 0b01101001011100110110000101101011;
        for (int i = 0; i < 32; i++){
            template[i] &= ~1;
            template[i] |= (signature >> i) & 1;
        }
        // the next 3 bit is the depth of the encoding
        for (int i = 32; i < 35; i++){
            template[i] &= ~1;
            template[i] |= ((depth-1) >> (i-32)) & 1;
        }
        // the next 4 byte - 3 bit is the length of the data
        int length = data.length;
        for (int i = 35; i < 64; i++){
            template[i] &= ~1;
            template[i] |= (length >> (i-35)) & 1;
        }

        int data_index = 0;
        int image_index = 0;
        int mask = (1 << depth) - 1;
        byte[] fitted_data = new byte[8 * data.length/depth];
        while (data_index < data.length){
            for (int b = 0; b < 8/depth; b++){
                fitted_data[image_index] |= (data[data_index] >> b*depth) & mask;
                image_index += 1;
            }
            data_index += 1;
        }

        for (int i = 0; i < fitted_data.length; i++){
            template[i + 64] &= ~((1 << depth)-1);
            template[i + 64] |= fitted_data[i];
        }

        /*
        for (int i = 0; i < data.length; i++){
            for (int b = 0; b < 8; b++){
                template[8*i + 64 + b] &= ~1;
                template[8*i + 64 + b] |= (data[i] >> b) & 1;
            }
        }
        */
        return template;
    }

    private static byte[] array_to_image(byte[] image_array, int width, int height){
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB); 
        int counter = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = 0;
                for (int i = 0; i < channels; i++){
                    argb |= (image_array[counter] & 0xff) << (i * 8);
                    counter += 1;
                }
                img.setRGB(x, y, argb);
            }
        }

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", stream);
        } catch (IOException e){
            System.out.println("Image file could not be created.");
            System.out.println(e);
            System.exit(1);
        }
        return stream.toByteArray();
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
