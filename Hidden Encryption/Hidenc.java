import java.io.IOException;

import java.util.Arrays;
import java.util.Random;

import java.nio.file.Paths;
import java.nio.file.Files;

import java.security.MessageDigest;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class Hidenc {

    public static void main(String[] args) {
        if (args.length < 4 || args.length > 7) {
            System.out.println("Usage: java Hiddec --key=KEY --input=INPUT_FILE --output=OUTPUT_FILE --ctr=CTR --offset=NUM --template=TEMPLATE_FILE --size=SIZE");
            System.out.println("required arguments: --key, --input, --output, and (--size XOR --template)");
            System.exit(1);
        }

        byte[] key = null;      // required
        String in_file = null;  // required
        String out_file = null; // required
        byte[] ctr = null;      // determines ECB or CTR
        int offset = -1;        // randomize if not entered
        String template_file = null; // EITHER THIS ONE
        int size = -1;          // OR THIS ONE NOT BOTH

        for (String argument : args){
            String[] arg_split = argument.split("=");
            if (arg_split.length != 2) {
                System.out.println("Error reading argument (Wrong length): ");
                System.out.println(argument);
                System.exit(1);
            }
            if (arg_split[0].equals("--key")){
                key = hexStringToByteArray(arg_split[1]);
                continue;
            }
            if (arg_split[0].equals("--input")){
                in_file = arg_split[1];
                continue;
            }
            if (arg_split[0].equals("--output")){
                out_file = arg_split[1];
                continue;
            }
            if (arg_split[0].equals("--ctr")){
                ctr = hexStringToByteArray(arg_split[1]);
                continue;
            }
            if (arg_split[0].equals("--offset")){
                offset = Integer.parseInt(arg_split[1]);
                continue;
            }
            if (arg_split[0].equals("--template")){
                template_file = arg_split[1];
                continue;
            }
            if (arg_split[0].equals("--size")){
                size = Integer.parseInt(arg_split[1]);
                continue;
            }
            System.out.println("Error reading argument (No match): ");
            System.out.println(argument);
            System.exit(1);
        }

        if (key == null || in_file == null || out_file == null || (size < 0 && template_file == null)){
            System.out.println("Missing one or more required arguments.");
            System.out.println("required arguments: --key, --input, --output, and (--size XOR --template)");
            System.exit(1);
        }
        if (size != -1 && template_file != null){
            System.out.println("Input either size OR template, not both");
            System.exit(1);
        }
        if (offset < -1) {
            System.out.println("Offset must be positive.");
            System.exit(1);
        }

        byte[] input_bytes = read_file(in_file);
        byte[] key_hash = md5_hash(key);
        Cipher encryptor = cipher_setup(key, ctr);
        byte[] blob = encrypt(encryptor, input_bytes, key_hash);
        byte[] template = null;
        if (size != -1){
            Random random = new Random();
            template = new byte[size];
            random.nextBytes(template);
        } else {
            template = read_file(template_file);
        }


        if (offset == -1){
            if (blob.length/16 < template.length/16){
                Random random = new Random();
                offset = random.nextInt(template.length/16 - blob.length/16) * 16;  // TOTO check blkc length
            } else if (blob.length/16 == template.length/16){
                offset = 0;
            } else {
                System.out.println("Cannot fit blob in given size or template.");
                System.out.print("Minimum size for blob: ");
                System.out.println(blob.length);
                System.exit(1);
            }
        } else {
            if (template.length < blob.length + offset){
                System.out.println("Cannot fit blob in given size or template and offset.");
                System.out.print("Minimum size for blob: ");
                System.out.println(blob.length + offset);
                System.exit(1);
            }
        }
        for (int i = 0; i < blob.length; i++){
            template[offset + i] = blob[i];
        }

        write_file(out_file, template);

    }
    // taken from https://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        try {
            for (int i = 0; i < len; i += 2) {
                data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
            }
        } catch (Exception e) {
            System.out.println("Invalid hex key: ");
            System.out.println(s);
            System.exit(1);
        }
        return data;
    }

    private static byte[] read_file(String path){
        byte[] input_bytes = null;
        try {
            input_bytes = Files.readAllBytes(Paths.get(path));
        } catch (IOException e){
            System.out.println("One of the files does not exist or cannot be read.");
            System.out.print("file: ");
            System.out.println(path);
            System.exit(1);
        }
        if (input_bytes.length == 0) {
            System.out.println("File is empty");
            System.out.print("file: ");
            System.out.println(path);
            System.exit(1);
        }
        if (input_bytes.length % 16 != 0) {
            System.out.println("File size must be multiple of 16 byte");
            System.out.print("file: ");
            System.out.print(path);
            System.out.print("\tlength: ");
            System.out.println(input_bytes.length);
            System.exit(1);
        }
        return input_bytes;
    }

    private static void write_file(String path, byte[] data){
        try {
            Files.write(Paths.get(path), data);
        } catch (IOException e){
            System.out.println("File could not be written to.");
            System.out.print("file: ");
            System.out.println(path);
            System.exit(1);
        }
    }


    private static Cipher cipher_setup(byte[] key, byte[] counter){
        SecretKeySpec cipher_key = new SecretKeySpec(key, "AES");
        Cipher cipher = null;
        try {
            if (counter != null) {
                cipher = Cipher.getInstance("AES/CTR/NoPadding");
                IvParameterSpec iv_param_spec = new IvParameterSpec(counter);
                cipher.init(Cipher.ENCRYPT_MODE, cipher_key, iv_param_spec);
            } else {
                cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.ENCRYPT_MODE, cipher_key);
            }
        } catch (Exception e) {
            System.out.println("Something went wrong with setting up decryptor:");
            System.out.println(e);
            System.exit(1);
        }
        return cipher;
    }

    private static byte[] md5_hash(byte[] data){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
            digest.update(data);
        } catch (Exception e){
            System.out.println("Hashing error.");
            System.out.println(e);
            System.exit(1);
        }
        return digest.digest();
    }

    private static byte[] encrypt(Cipher encryptor, byte[] input_bytes, byte[] key_hash){
        byte[] encrypted_data = input_bytes;
        byte[] data_hash = md5_hash(input_bytes);
        encrypted_data = concatenate(key_hash, encrypted_data);
        encrypted_data = concatenate(encrypted_data, key_hash);
        encrypted_data = concatenate(encrypted_data, data_hash);
        try {
            encrypted_data = encryptor.doFinal(encrypted_data);
        } catch (IllegalBlockSizeException e){
            System.out.println("Input bytes are not of correct size. (Must be multiple of 16)");
            System.exit(1);
        } catch (BadPaddingException e) {
            System.out.println("Bad padding when encrypting.");
            System.exit(1);
        }  
        return encrypted_data;
    }

    private static int find_hash(byte[] data, byte[] key_hash, int start){
        if (start == data.length){
            return -1;
        }
        int i = 0;
        while (start+i < data.length && data[start+i] == key_hash[i]){
            i += 1;
            if (i == key_hash.length){
                return start;
            }
        }
        return find_hash(data, key_hash, start+1);
    }

    // inspired by https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    public static byte[] concatenate(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;

        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}