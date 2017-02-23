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

package org.cg.dao.logging;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;


public class Activator extends Plugin {

	private final Log logger = LogFactory.getLog(getClass());
	public static Activator myActivator;

	public Activator() {
		myActivator = this;
	}

	public void start(BundleContext context) throws Exception {
		super.start(context);
		try {
			new DOMConfigurator().doConfigure(getBundle().getEntry("log4j.xml").openStream(), Logger.getRootLogger().getLoggerRepository());
		} catch (IOException ex) {
			logger.fatal("Error start logger", ex);
			throw ex;
		}
	}

	public void stop(BundleContext context) throws Exception {
		myActivator = null;
		super.stop(context);
	}

}
