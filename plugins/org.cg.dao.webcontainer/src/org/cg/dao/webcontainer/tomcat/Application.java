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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.equinox.app.IApplicationContext;

public class Application implements org.eclipse.equinox.app.IApplication {

	private final Log logger = LogFactory.getLog(getClass());
	private TomcatWrapper myTomcat = new TomcatWrapper();
	
	public Object start(IApplicationContext context) throws Exception {
		try {
			myTomcat.startServer();
		} catch (Exception e) {
			logger.fatal("Failed to start webcontainer", e);
			throw e;
		}
		return null;
	}

	public void stop() {
		try {
			myTomcat.stopServer();
		} catch (Exception e) {
			logger.fatal("Failed to stop webcontainer", e);
			throw new IllegalStateException(e);
		}

	}

}
