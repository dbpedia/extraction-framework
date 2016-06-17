package org.dbpedia.extraction.live.administrative;

import java.io.*;
import java.util.Scanner;

/**
 * Created by Andre on 11/09/2015.
 */
public class AdminAuthentication {
    private static String password = null;

    public static boolean authenticate(String input){
        if(password == null) readFile();

        if(input.equals(password))
            return true;
        return false;
    }

    private static void readFile(){
        String path = ClassLoader.getSystemResource("adminPassword.txt").getPath();
        try {
            password = new Scanner(new File(path)).nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
