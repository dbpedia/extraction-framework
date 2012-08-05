
package org.dbpedia.extraction.live.util;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.zip.GZIPOutputStream;

/**
 * @author Jens Lehmann
 * 
 */
public class Files {
	public static boolean debug = false;

	public static <K, V> void writeMap(String fileName, Map<K, V> map)
		throws FileNotFoundException
	{
		File file = new File(fileName);
		XMLEncoder out = new XMLEncoder(new FileOutputStream(file));
		out.writeObject(map);
		out.close();
	}
	
	@SuppressWarnings("unchecked")
	public static Map readMap(String fileName)
		throws FileNotFoundException
	{
		File file = new File(fileName);		
		XMLDecoder in = new XMLDecoder(new FileInputStream(file));
		
		return (Map)in.readObject();
	}
	

	/**
	 * Reads in a file.
	 * 
	 * @param file
	 *            The file to read.
	 * @return Content of the file.
	 */
	public static String readFile(File file) throws FileNotFoundException, IOException {
			
		BufferedReader br = new BufferedReader(new FileReader(file));
		StringBuffer content = new StringBuffer();
		try{
		String line;
		
		while ((line = br.readLine()) != null) {
			content.append(line);
			content.append(System.getProperty("line.separator"));
		}
		}finally{br.close();}
		return content.toString();
		
	}
	
	/**
	 * Reads in a file as Array
	 * 
	 * @param file
	 *            The file to read.
	 * @return StringArray with lines
	 */
	public static String[] readFileAsArray(File file) throws FileNotFoundException, IOException {
		String content = readFile(file);
		StringTokenizer st = new StringTokenizer(content, System.getProperty("line.separator"));
		List<String> l = new ArrayList<String>();
		while (st.hasMoreTokens()) {
			l.add(st.nextToken());
			
		}
		
		return l.toArray(new String[l.size()]);
		
	}
	
	/**
	 * writes a serializable Object to a File.
	 * @param obj
	 * @param file
	 */
	public static void writeObjectToFile(Object obj, File file){
		
		ObjectOutputStream oos = null;
		try{
			FileOutputStream fos = new FileOutputStream(file);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			oos = new ObjectOutputStream(bos);
			
			oos.writeObject(obj);
			oos.flush();
			oos.close();
			}catch (Exception e) {
				 e.printStackTrace();
			}finally{
				try{
					oos.close();
				}catch (Exception e) {
					 e.printStackTrace();
				}
			}
	}
	
	public static Object readObjectfromFile( File file){
		ObjectInputStream ois = null;
		try{
			FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis);
			ois = new ObjectInputStream(bis);
			return ois.readObject();
		}catch (Exception e) {
			 e.printStackTrace();
		}finally{
			try {
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return null;
	}
		

	/**
	 * Creates a new file with the given content or replaces the content of a
	 * file.
	 * 
	 * @param file
	 *            The file to use.
	 * @param content
	 *            Content of the file.
	 */
	public static synchronized void  createFile(File file, String content) {
		
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(content.getBytes());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		} catch (IOException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		}
	}
	/**
	 * Creates a new file with the given content or replaces the content of a
	 * file.
	 * 
	 * @param file
	 *            The file to use.
	 * @param content
	 *            Content of the file.
	 */
	public static synchronized void  createGzippedFile(File file, String content) {
		
		try {
			FileOutputStream fos = new FileOutputStream(file);
			GZIPOutputStream gos = new GZIPOutputStream(fos);
			
			gos.write(content.getBytes());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		} catch (IOException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		}
	}

	/**
	 * Appends content to a file.
	 * 
	 * @param file
	 *            The file to create.
	 * @param content
	 *            Content of the file.
	 */
	public static void appendFile(File file, String content) {
		try {
			FileOutputStream fos = new FileOutputStream(file, true);
			fos.write(content.getBytes());
			fos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		} catch (IOException e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		}
	}

	public static void clearFile(File file) {
		try{
		createFile(file, "");
		}catch (Exception e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		}
	}
	
	public static void deleteFile(File file) {
		
		try{
			file.delete();
		}catch (Exception e) {
			e.printStackTrace();
			if(debug){System.exit(0);}
		}
	}
	
	public static void mkdir(String dir){
		//System.out.println("Checking: + " + (new File(dir)).getAbsolutePath());
		
		if (!new File(dir).exists()) {
			try{
			new File(dir).mkdir();
			}catch (Exception e) {
				e.printStackTrace();
				if(debug){System.exit(0);}
				// this should not be a show stopper
			}		
		}
	}
	
	/**
	 * deletes all Files in the dir, does not delete the dir itself
	 * no warning is issued, use with care, cannot undelete files
	 *
	 * @param dir without a separator e.g. tmp/dirtodelete
	 */
	public static void deleteDir(String dir) {
		
			File f = new File(dir);
			
			if(debug){
				System.out.println(dir);
				System.exit(0);
			}
			
		    String[] files = f.list();
		   
		    for (int i = 0; i < files.length; i++) {
		    	
		    	Files.deleteFile(new File(dir+File.separator+files[i]));
		    }     
	}
	
	/**
	 * copies all files in dir to "tmp/"+System.currentTimeMillis()
	 * @param dir the dir to be backupped
	 */
	public static void backupDirectory(String dir){
		File f = new File(dir);
		String backupDir = "tmp/"+System.currentTimeMillis();
		mkdir("tmp");
		mkdir(backupDir);
		
		if(debug){
			System.out.println(dir);
			System.exit(0);
		}
		
	    String[] files = f.list();
	   try{
	    for (int i = 0; i < files.length; i++) {
	    	File target = new File(dir+File.separator+files[i]);
	    	if(!target.isDirectory()){
	    		String s = readFile(target);
	    		createFile(new File(backupDir+File.separator+files[i]), s);
	    	}
	    }   
	   }catch (Exception e) {
		e.printStackTrace();
	}
	}
	
	

}
