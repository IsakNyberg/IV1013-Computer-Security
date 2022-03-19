import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.math.*;
import java.lang.StringBuffer;

public class PasswordCrack{
    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("Usage: java PasswordCrack <dictionary_file> <password_file>");
        }
        String dictionary_path  = args[0];
        String password_path  = args[1];
        List<String> dictionary_file = null;
        List<String> password_file = null;
        try {
            dictionary_file = Files.readAllLines(Paths.get(dictionary_path), StandardCharsets.UTF_8);
            password_file = Files.readAllLines(Paths.get(password_path), StandardCharsets.UTF_8);
        } catch (IOException e){
            System.out.println("One of the files does not exist or cannot be read.");
            System.exit(1);
        }
        String[] hashes = new String[password_file.size()];
        String[] salts = new String[password_file.size()];
        boolean[] found = new boolean[password_file.size()];
        String[] names = new String[password_file.size() * 4];
        for (int i = 0; i < password_file.size(); i++) {  // extract names from file
            String[] arrOfStr = password_file.get(i).split(":", 14);
            found[i] = false;
            names[i * 4] = arrOfStr[0].toLowerCase();
            hashes[i] = arrOfStr[1];
            salts[i] = arrOfStr[1].substring(0, 2);

            String[] name_split = arrOfStr[4].split(" ", 4);
            for (int j = 1; j < 3; j++) {
                if (name_split.length > j){
                    names[i * 4 + j] = name_split[j].toLowerCase();
                } else {
                    names[i * 4 + j] = "1234";
                }
            }
            name_split = arrOfStr[5].split("/", 3);
            names[i * 4 + 3] = name_split[2].toLowerCase();
        }
        String[] temp = new String[4];

        for (int i =0; i< password_file.size() * 4; i++){
            temp[0] = names[i] + names[i];
            temp[1] = names[i] + names[i + 1];
            temp[2] = names[i] + names[i + 2];
            temp[3] = names[i] + names[i + 3];
            names = concatenate(names, temp);
        }

        String[] dictionary = new String[dictionary_file.size()];
        for (int i = 0; i < dictionary_file.size(); i++) {  // extract dictionary from file 
            dictionary[i] = dictionary_file.get(i);
        }
        dictionary = concatenate(dictionary, names);

        /*String[] testing = simple_cases("gloria", 2);
        for (String word : testing){
            System.out.println(word);
        }*/ 

        int total_words = 0;
        long start = System.currentTimeMillis();
        for (int round = 0; round < 11; round++){
            /*System.out.print("Round: ");
            System.out.println(round);*/
            for (int dic_index = 0; dic_index<dictionary.length; dic_index++) {
                String[] scrambled_words = scramble_word(dictionary[dic_index], round);
                total_words += scrambled_words.length;
                for (int person = 0; person < hashes.length; person++){
                    if (found[person]){
                        continue;
                    }
                    for (int word_index = 0; word_index < scrambled_words.length; word_index++){
                        if (jcrypt.crypt(salts[person], scrambled_words[word_index]).equals(hashes[person])) {
                            /*System.out.print(person + 1);
                            System.out.print("\tTime: ");
                            System.out.print((System.currentTimeMillis()-start)/1000);
                            System.out.print("s\t");
                            System.out.print(scrambled_words[word_index]);
                            System.out.print("  (");
                            System.out.print(dictionary[dic_index]);
                            System.out.println(")");*/
                            System.out.println(scrambled_words[word_index]);
                            found[person] = true;
                            break;
                        }
                    }
                }     
            }
            
            /*System.out.print("Words: ");
            System.out.print(total_words);
            System.out.print("\tTime: ");
            System.out.print((System.currentTimeMillis()-start)/1000);
            System.out.print("s\tNot found: ");*/
            boolean finished = true;
            for (int person = 0; person < found.length; person++){
                if (!found[person]){
                    /*System.out.print(person + 1);
                    System.out.print(", ");*/
                    finished = false;
                }
            }
            /*System.out.println();*/
            if (finished){
                break;
            }
        }
        /*System.out.print("Finished: ");
        System.out.println((System.currentTimeMillis()-start)/1000);*/
    }

    private static String[] scramble_word(String word, int round){
        if (round == 0){
            String[] result = new String[1];
            result[0] = word;
            return  result;
        }
        if (round == 1)
            return simple_cases(word, 1);
        if (round == 2) 
            return simple_cases(word, 2);
        if (round == 3)
            return simple_cases(word, 3);
        if (round == 4)
            return append_number(simple_cases(word, 2));
        if (round == 5)
            return prepend_number(simple_cases(word, 2));
        if (round == 6)
            return append_letter(simple_cases(word, 2));
        if (round == 7)
            return prepend_letter(simple_cases(word, 2));
        if (round == 8)
            return append_number(simple_cases(word, 3));
        if (round == 9)
            return prepend_number(simple_cases(word, 3));
        if (round == 10)
            return append_number(prepend_number(simple_cases(word, 2)));
        return append_number(prepend_number(append_number(simple_cases(word, 2))));
    }

    private static String[] simple_cases(String word, int depth){
        String[] result = new String[13];
        // word = word.substring(0, Math.min(8, word.length()));
        if (word.length() < 2){
            word = word + word;
        }
        result[0] = leet_speak(word.toLowerCase());
        result[1] = word.toUpperCase();
        result[2] = word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();  // Capitalizefirst
        result[3] = word.substring(0, 1).toLowerCase() + word.substring(1).toUpperCase();  // lOWERCASE FIRST
        result[4] = word.substring(0, word.length() - 1).toLowerCase() + word.substring(word.length() - 1).toUpperCase();  // capitalizelasT
        result[5] = word.substring(0, word.length() - 1).toUpperCase() + word.substring(word.length() - 1).toLowerCase();  // LOWERCASE LASt
        result[6] = new StringBuilder(word).reverse().toString();  // esrever
        result[7] = word + new StringBuilder(word).reverse().toString();  // reverseesrever

        // AlTeRnAtInG CaSe
        char[] chars1 = word.toLowerCase().toCharArray();
        char[] chars2 = word.toLowerCase().toCharArray();
        for (int i=0; i < word.length(); i++){
            if (i % 2 == 0){
                chars1[i] = Character.toUpperCase(chars1[i]);
                chars2[i] = chars2[i];
            } else {
                chars1[i] = chars1[i];
                chars2[i] = Character.toUpperCase(chars2[i]);
            }
        }
        result[8] = new String(chars1);  // AlTeRnAtInG CaSe
        result[9] = new String(chars2);  // aLtErNaTiNg cAsE
        result[10] = word.substring(1);  // emove first letter
        result[11] = word.substring(0, word.length() - 1); // remove last lette
        result[12] = word + word;  // repeat wordrepeat word
        //result[2] = word.toLowerCase();

        if (depth > 1){
            for (String simple_word : result) {
                result = concatenate(result, simple_cases(simple_word, depth -1));
            }
        }
        return result;
    }

    private static String leet_speak(String string){
        char[] as_array = string.toCharArray();
        for (int i = 0; i< as_array.length; i++){
            switch (as_array[i]) {
                case 'o':
                    as_array[i] = '0';
                    break;
                case 'l':
                    as_array[i] = '1';
                    break;

                case 'e':
                    as_array[i] = '3';
                    break;

                case 'a':
                    as_array[i] = '4';
                    break;

                case 't':
                    as_array[i] = '7';
                    break;
            }
        }
        return new String(as_array);
    }

    private static String[] append_number(String[] strings){
        int new_length = 0;
        for (String string : strings){
            if (string.length() <= 8){
                new_length += 10;
            }
        }

        String[] result = new String[new_length];
        int index = 0;
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].length() > 8){
                continue;
            }
            for (int j = 0; j < 10; j++) {
                result[index] = strings[i] + (char)('0' + j); 
                index += 1;
            }
        }
        return result;
    }

    private static String[] append_letter(String[] strings){
        int new_length = 0;
        for (String string : strings){
            if (string.length() <= 8){
                new_length += 26;
            }
        }

        String[] result = new String[new_length];
        int index = 0;
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].length() > 8){
                continue;
            }
            for (int j = 0; j < 26; j++) {
                result[index] = strings[i] + (char)('a' + j); 
                index += 1;
            }
        }
        return result;
    }

    private static String[] prepend_letter(String[] strings){
        String[] result = new String[strings.length*26];
        int index = 0;
        for (int i = 0; i < strings.length; i++) {
            for (int j = 0; j < 26; j++) {
                result[index] = (char)('a' + j) + strings[i]; 
                index += 1;
            }
        }
        return result;
    }

    private static String[] prepend_number(String[] strings){
        String[] result = new String[strings.length*10];
        int index = 0;
        for (int i = 0; i < strings.length; i++) {
            for (int j = 0; j < 10; j++) {
                result[index] = (char)('0' + j) + strings[i]; 
                index += 1;
            }
        }
        return result;
    }

    // inspired by https://stackoverflow.com/questions/80476/how-can-i-concatenate-two-arrays-in-java
    public static String[] concatenate(String[] a, String[] b) {
        int aLen = a.length;
        int bLen = b.length;

        String[] c = new String[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);

        return c;
    }
}
