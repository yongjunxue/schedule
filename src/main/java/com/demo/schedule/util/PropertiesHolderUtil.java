package com.demo.schedule.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author xueyongjun
 *
 */
public class PropertiesHolderUtil {
	
	private static PropertiesHolderUtil instance;
	
	private Properties prop=new Properties();
	
	private PropertiesHolderUtil(){
		String path=getClass().getResource("/").toString().substring(6);
		readProperties(path);
	}
	private void readProperties(String path) {
		File dir=new File(path);
		if(dir.exists() ){
			if(dir.isDirectory()){
				String[] names=dir.list();
				for(int i=0;i<names.length;i++){
					String name=names[i];
					String newpath=path+"/"+name;
					readProperties(newpath);
				}
			}else{
				if(path.endsWith(".properties")){
					InputStream in=null;
					try {
						in=new FileInputStream(path);
						prop.load(in);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}finally{
						try {
							if(in != null){
								in.close();
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	public synchronized static PropertiesHolderUtil getInstance(){
		if(instance == null){
			instance=new PropertiesHolderUtil();
		}
		return instance;
	}
	/**
	 * @param key
	 * @return
	 */
	public String getProperty(String key){
		String value=null;
		if(key != null && !key.equals("")){
			value=prop.getProperty(key);
		}
		return value;
	}
}
