package com.temp;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

public class ReadMethods {

	public static void main(String[] args) {

		// File file = new File("L:\\planning\\ADF\\Planning\\Model\\classes\\");
		File file = new File("L:\\planning\\ant-build\\planning\\classes\\");
		try {
			URL url = file.toURI().toURL();
			URL[] urls = new URL[] { url };
			ClassLoader classLoader = new URLClassLoader(urls);
			Class cls = classLoader.loadClass("com.hyperion.planning.governor.HspHealthCheckCriteriaFactory");
			System.out.println(cls.getName());
			Method[] ml = cls.getDeclaredMethods();
			for (Method m : ml) {
				System.out.println(m.getName());
			}
			System.out.println("Done");
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}
