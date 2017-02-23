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

package org.cg.dao.products;

import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class Activator implements BundleActivator {

	private final Log logger = LogFactory.getLog(getClass());
	private static final String EXTENSION_ID = "org.cg.dao.startup";
	private static final String AUTO_START = "auto-start";
	private static final String LEVEL = "level";

	public Activator() {
		// TODO Auto-generated constructor stub
	}

	private class AutoStartBundle implements Comparable {
		public String id;
		public String name;
		public int level;

		public int compareTo(Object obj) {
			if (obj instanceof AutoStartBundle) {
				AutoStartBundle entry = (AutoStartBundle) obj;
				return level - entry.level;
			}
			throw new IllegalArgumentException();
		}
	}

	private void startBundle(Bundle bundle) {
		boolean isFragment = bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
		if (!isFragment && bundle.getState() == Bundle.RESOLVED )
			try {
				bundle.start();
			} catch (BundleException e) {
				logger.error("Fail to start bundle: " + bundle.getSymbolicName());
			}
	}

	private void init() {
		IExtensionPoint extp = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_ID);
		TreeSet<AutoStartBundle> appSet = new TreeSet<AutoStartBundle>();
		if (extp != null) {
			IExtension[] exts = extp.getExtensions();
			for (IExtension ext : exts) {
				IConfigurationElement[] elements = ext.getConfigurationElements();
				for (IConfigurationElement element : elements) {
					if (element.getName().equals(AUTO_START)) {

						AutoStartBundle ab = new AutoStartBundle();
						try {
							ab.id = element.getContributor().getName();
							ab.name = ext.getLabel();
							String level = element.getAttribute(LEVEL);
							try {
								ab.level = Integer.parseInt(level);
							} catch (NumberFormatException e) {
								ab.level = Integer.MAX_VALUE;
							}
							appSet.add(ab);
						} catch (Throwable e) {
							logger.error("Fail to resolve auto start extension . " + e);
						}
					}
					break;
				}
			}
		}

		for (AutoStartBundle abundle : appSet) {
			Bundle bundle = Platform.getBundle(abundle.id);
			if (bundle == null)
				logger.error("Fail to locate bundle: " + abundle.id);
			logger.info("starting bundle: " + abundle.name + ", id: " + abundle.id);
			try {
				startBundle(bundle);
			} catch (Throwable ex) {
				logger.error("fail to start bundle " + abundle.id, ex);
			}
		}
	}


	public void start(BundleContext context) throws Exception {
		init();
	}

	public void stop(BundleContext context) throws Exception {

	}

}
