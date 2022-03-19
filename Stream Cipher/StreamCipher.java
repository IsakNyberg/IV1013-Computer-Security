import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Random;
import java.io.*;

public class StreamCipher {
    public static void main(String[] args) {
        if (args.length != 3){
            System.out.println("Usage: StreamCipher <key> <infile> <outfile>");
            System.exit(1);
        }
        String key = "";
        MyRandom prng = null;
        try{
            key = args[0];
            prng = new MyRandom(key);
        } catch (NumberFormatException e){
            System.out.println("Key must be a valid long");
            System.exit(1);
        }

        try {
            byte[] source_file = Files.readAllBytes(Paths.get(args[1]));
            byte[] dest_file = encrypt(prng, source_file); 
            Files.write(Paths.get(args[2]), dest_file);
        } catch (IOException e){
            System.out.println("One of the files does not exist or cannot be read.");
            System.exit(1);
        }

        System.exit(0);
    }

    private static byte[] encrypt(MyRandom prng, byte[] message){
        byte[] cipher = new byte[message.length];
        for (int i=0; i<message.length; i++){
            cipher[i] = (byte) (message[i] ^ prng.next(8));
        }
        return cipher;
    }

}