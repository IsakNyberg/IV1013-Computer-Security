import java.io.IOException;

import java.util.Arrays;

import java.nio.file.Paths;
import java.nio.file.Files;

import java.security.MessageDigest;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class Hiddec {

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.out.println("Usage: java Hiddec --key=KEY --input=INPUT_FILE --output=OUTPUT_FILE");
            System.out.println("OR");
            System.out.println("Usage: java Hiddec --key=KEY --ctr=CTR --input=INPUT_FILE --output=OUTPUT_FILE");
            System.exit(1);
        }

        byte[] key = null;
        String in_file = null;
        String out_file = null;
        byte[] ctr = null;
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
            System.out.println("Error reading argument (No match): ");
            System.out.println(argument);
            System.exit(1);
        }
        if (key == null || in_file == null || out_file == null){
            System.out.println("Missing one or more required arguments.");
            System.out.println("required arguments: --key, --input, and --output");
            System.exit(1);
        }

        byte[] input_bytes = read_file(in_file);
        byte[] key_hash = md5_hash(key);
        Cipher encryptor = cipher_setup(key, ctr);
        byte[] message = null;
        if (ctr == null){
            message = ecb_decrypt(encryptor, input_bytes, key_hash);
        } else {
            message = ctr_decrypt(encryptor, input_bytes, key_hash, key, ctr);
        }
        
        write_file(out_file, message);

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
            System.exit(1);
        }
        return input_bytes;
    }

    private static void write_file(String path, byte[] data){
        try {
            Files.write(Paths.get(path), data);
        } catch (IOException e){
            System.out.println("File could not be written to.");
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
                cipher.init(Cipher.DECRYPT_MODE, cipher_key, iv_param_spec);
            } else {
                cipher = Cipher.getInstance("AES/ECB/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, cipher_key);
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

    private static byte[] ecb_decrypt(Cipher encryptor, byte[] input_bytes, byte[] key_hash){
        byte[] decrypted_data = encryptor.update(input_bytes);

        int start_index = find_hash(decrypted_data, key_hash, 0);
        int end_index = find_hash(decrypted_data, key_hash, start_index + key_hash.length);
        if (start_index == -1 || end_index == -1){
            System.out.println("Could not find hash in data.");
            System.out.println("Maybe key is wrong?");
            System.exit(1);
        }

        byte[] decrypted_data_hash = Arrays.copyOfRange(decrypted_data, end_index + key_hash.length, end_index + 2*key_hash.length);
        decrypted_data = Arrays.copyOfRange(decrypted_data, start_index + key_hash.length, end_index);
        byte[] expected_data_hash = md5_hash(decrypted_data);

        for (int i = 0; i < decrypted_data_hash.length; i++){
            if (expected_data_hash[i] != decrypted_data_hash[i]){
                System.out.println("Data does not match checksum.");
                System.out.println("Data or checksum is corrupt.");
                System.exit(1);
            }
        }
        return decrypted_data;
    }

    private static int find_hash(byte[] data, byte[] key_hash, int start){
        for (int i = start; i < data.length; i+=16){  // TOTO check blkc length 
            int j = 0;
            while (i+j < data.length && data[j+i] == key_hash[j]){
                j += 1;
                if (j == key_hash.length){
                    return i;
                }
            }
        }
        return -1;
    }

    private static byte[] ctr_decrypt(Cipher encryptor, byte[] input_bytes, byte[] key_hash, byte[] key, byte[] ctr){
        int offset = 0;
        int start_index = -1;
        int end_index = -1;
        byte[] decrypted_data = null;
        while (start_index == -1 || end_index == -1){
            // reset cipher class so that counter doesn't change
            SecretKeySpec cipher_key = new SecretKeySpec(key, "AES");
            IvParameterSpec iv_param_spec = new IvParameterSpec(ctr);
            try {
                encryptor.init(Cipher.DECRYPT_MODE, cipher_key, iv_param_spec);
            } catch (Exception e){
                System.out.println("Error while decrypting");
                System.exit(1);
            }
            // generate new decrypted_data data for the new counter
            decrypted_data = Arrays.copyOfRange(input_bytes, offset, input_bytes.length);
            decrypted_data = encryptor.update(decrypted_data);
            
            start_index = find_hash(decrypted_data, key_hash, 0);
            end_index = find_hash(decrypted_data, key_hash, start_index + key_hash.length);
            offset += 1;
            if (offset == input_bytes.length){
                System.out.println("Could not find hash in data.");
                System.out.println("Maybe key or counter is wrong?");
                System.exit(1);
            }
        }

        byte[] decrypted_data_hash = Arrays.copyOfRange(decrypted_data, end_index + key_hash.length, end_index + 2*key_hash.length);
        decrypted_data = Arrays.copyOfRange(decrypted_data, start_index + key_hash.length, end_index);
        byte[] expected_data_hash = md5_hash(decrypted_data);

        for (int i = 0; i < decrypted_data_hash.length; i++){
            if (expected_data_hash[i] != decrypted_data_hash[i]){
                System.out.println("Data does not match checksum.");
                System.out.println("Data or checksum is corrupt.");
                System.exit(1);
            }
        }
        return decrypted_data;
    }
}