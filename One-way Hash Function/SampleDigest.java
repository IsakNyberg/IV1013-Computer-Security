//package com.iv1013.cryptohash;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Arrays;
/*
IV1013 security
Security is fun
Yes, indeed
Secure IV1013
No way
*/
public class SampleDigest {
    public static void main(String[] args) {
        if (args.length != 2){
            System.out.println("Usage: SampleDigest <match bytes> <infile>");
            System.exit(1);
        }
        String digestAlgorithm = "SHA-256";
        int match_length = 0;
        byte[] target_bytes = {0};

        try{
            match_length = Integer.parseInt(args[0]);
        } catch (NumberFormatException e){
            System.out.println("match bytes must be an int in range (0,32)");
            System.exit(1);
        }
        if (match_length < 0 || match_length > 32){
            System.out.println("match bytes must be an int in range (0,32)");
            System.exit(1);
        }
        try {
            target_bytes = Files.readAllBytes(Paths.get(args[1]));
        } catch (IOException e){
            System.out.println("The files does not exist or cannot be read.");
            System.exit(1);
        }
    
        // computation starts here        
        try {
            MessageDigest md = MessageDigest.getInstance(digestAlgorithm);
            md.update(target_bytes);
            byte[] target_digest = md.digest();
            printDigest(new String(target_bytes, StandardCharsets.UTF_8), digestAlgorithm, target_digest);

            int count = 0;
            byte[] trial = {0};
            byte[] trial_digest = {0};
            int matching_bytes = 0;
            System.out.println("Searching for match of " + match_length + " bytes...");
            while (matching_bytes != match_length){
                trial = Arrays.copyOf(trial_digest, trial_digest.length);
                /*
                // This is to only allow "nice" characters in the message
                for (int i = 0; i<trial.length; i++){
                    trial[i] &= 0x7f;
                    trial[i] = (byte)((trial[i] % 93) + 33);
                }*/
                md.update(trial);
                trial_digest = md.digest();

                // we only care about 24 bits aka the first 6 byte
                matching_bytes = 0;
                for (int i = 0; i < match_length; i++){
                    if (target_digest[i] == trial_digest[i]){
                        matching_bytes += 1;
                    }
                }
                count += 1;
                //System.out.println(matching_bytes);
            }
            
            System.out.println("Match Found!");
            printDigest(new String(trial), digestAlgorithm, trial_digest);
            System.out.print("Trials needed: ");
            System.out.println(count);



        } catch (NoSuchAlgorithmException e) {
            System.out.println("Algorithm \"" + digestAlgorithm  + "\" is not available");
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Exception "+e);
            System.exit(1);
        }
    }

    //printDigest(inputText1, md.getAlgorithm(), digest);
    public static void printDigest(String inputText, String algorithm, byte[] digest) {
        System.out.println("Digest for the message \"" + inputText +"\", using " + algorithm + " is:");
        for (int i=0; i<digest.length; i++)
            System.out.format("%02x", digest[i]&0xff);
        System.out.println();
    }
}
