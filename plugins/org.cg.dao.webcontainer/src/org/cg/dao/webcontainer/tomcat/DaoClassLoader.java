/**
 * Copyright CodeGerm. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cg.dao.webcontainer.tomcat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class DaoClassLoader extends URLClassLoader {
	private static final Log logger = LogFactory.getLog(TomcatWrapper.class);
	
	private String myId;
	private Bundle myBundle;

	public DaoClassLoader(String pid) {
		super(new URL[0]);
		myId = pid;
		myBundle = Platform.getBundle(pid);
	}


	private Set<URL> getAppClasspath(String pluginId) {
		Set<URL> urls = new HashSet<URL>();
		
		Set<String> pidSet = new HashSet<String>();
		getTransitiveRequiredBundles(pluginId, pidSet);
		
		for (String pid: pidSet) {
			try {
				Bundle budle = Platform.getBundle(pid);
				if (budle != null) {
					String headers = (String) budle.getHeaders().get(Constants.BUNDLE_CLASSPATH);
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, headers);
					if (elements != null) {
						for (ManifestElement pm: elements) {
							URL url = budle.getEntry(pm.getValue());
							if (url != null)
								try {
									urls.add(FileLocator.toFileURL(url));
								} catch (IOException ioe) {
									logger.warn("fail to resolve " + url, ioe);
								}
						}
					}
				}
			} catch (BundleException e) {
				logger.warn("fail to get bundle " + pid, e);
			}
		}
		return urls;
	}

	private String[] getImmediateDepencies(String pluginId) {
		try {
			Bundle bundle = Platform.getBundle(pluginId);
			if (bundle != null) {
				String header = (String) bundle.getHeaders().get(Constants.REQUIRE_BUNDLE);
				ManifestElement[] requires = ManifestElement.parseHeader(Constants.REQUIRE_BUNDLE, header);
				if (requires != null) {
					String[] reqs = new String[requires.length];
					for (int i = 0; i < requires.length; i++) {
						reqs[i] = requires[i].getValue();
					}
					return reqs;
				}
			}
		} catch (BundleException e) {
			logger.warn("failed to get bundle: "+ pluginId, e);
		}
		return new String[0];
	}
		

	private void getTransitiveRequiredBundles(String pluginId, Set<String> pluginIds) {
		if (pluginIds.contains(pluginId)) {
			return;
		}

		for (String immediatePreReq : getImmediateDepencies(pluginId)) 
			getTransitiveRequiredBundles(immediatePreReq, pluginIds);
		
		pluginIds.add(pluginId);
	}

	@Override
	public URL findResource(String name) {
		URL url = myBundle.getResource(name);
		if (url != null)
			return url;
		return super.findResource(name);
	}

	@Override
	public Enumeration<URL> findResources(String name) throws IOException {
		Enumeration<URL> urls = myBundle.getResources(name);
		if (urls != null && urls.hasMoreElements())
			return urls;
		return super.findResources(name);
	}

	public URL getResource(String resName) {
		return myBundle.getResource(resName);
	}

	public Enumeration<URL> getResources(String name) throws IOException {
		return myBundle.getResources(name);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public URL[] getURLs() {
		Set curURLSet = getAppClasspath(myId);
		return (URL[]) curURLSet.toArray(new URL[curURLSet.size()]);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Class loadClass(String className) throws ClassNotFoundException {
		return myBundle.loadClass(className);
	}

	@Override
	public String toString() {
		return getClass().getName() + ": " + myBundle.getSymbolicName() + " location : " + myBundle.getLocation();
	}
}
