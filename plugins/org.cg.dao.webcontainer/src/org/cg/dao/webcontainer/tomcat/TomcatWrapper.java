/**
 * 
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
 * 
 */

package org.cg.dao.webcontainer.tomcat;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Globals;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Catalina;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.dynamichelpers.IExtensionChangeHandler;
import org.eclipse.core.runtime.dynamichelpers.IExtensionTracker;
import org.osgi.framework.Bundle;

public class TomcatWrapper implements IExtensionChangeHandler {

	public static final int DEFAULT_ORDER = 68;
	private static final String EXT_ID = Activator.EXT_ID + ".WebApp";
	private static final String APP_NAME = "webapp";
	private static final String CONTEXT_ROOT = "contextRoot";
	private static final String DOC_ROOT = "docRoot";
	private static final String START_ORDER = "startOrder";
	private static final String PARAMETER = "param";
	private static final String WEBAPP_NAME = "name";
	private static final String APP_VALUE = "value";
	private static final String FILE_NAME = "welcome-file";

	private static Catalina myServer;
	private static Engine myEngine;
	private static Host myHost;
	private static final Log logger = LogFactory.getLog(TomcatWrapper.class);

	// begin - IExtensionChangeHandler
	@Override
	public void addExtension(IExtensionTracker tracker, IExtension extension) {
		logger.info("processing web apps");
		IConfigurationElement[] elements = extension.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			if (element.getName().equals(APP_NAME)) {
				logger.info("starting webapp " + extension.getNamespaceIdentifier());
				try {
					startWebApp(extension, element);
				} catch (Throwable t) {
					logger.error("failed to start webapp " + extension.getNamespaceIdentifier(), t);
				}
			}
		}
	}

	@Override
	public void removeExtension(IExtension extension, Object[] objects) {

	}

	// end - IExtensionChangeHandler

	private IPath resolveWebAppPath(String pluginId, String pathStr) {
		Bundle currBundle = Platform.getBundle(pluginId);
		if (currBundle == null) {
			String msg = MessageFormat.format("failed to get webapp: {0}", new Object[] { pluginId });
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}

		URL webappURL = currBundle.getEntry(pathStr == null ? "/" : pathStr);
		if (webappURL == null) {
			String msg = MessageFormat.format("failed to get path: {1} for webapp: {0}", new Object[] { pluginId,
					pathStr });
			logger.error(msg);
			throw new IllegalArgumentException(msg);
		}
		try {
			return new Path(FileLocator.toFileURL(FileLocator.resolve(webappURL)).getFile());
		} catch (IOException ioe) {
			String msg = MessageFormat.format("failed to resolve path: {1} for webapp: {0}", new Object[] { pluginId,
					pathStr });
			logger.error(msg, ioe);
			throw new IllegalArgumentException(msg, ioe);
		}
	}

	private void prepareAllWebApps() {
		IExtensionPoint registerPoint = Platform.getExtensionRegistry().getExtensionPoint(EXT_ID);
		ArrayList<WebAppWrapper> webapps = new ArrayList<WebAppWrapper>();
		if (registerPoint != null) {
			for (IExtension extension : registerPoint.getExtensions()) {
				for (IConfigurationElement element : extension.getConfigurationElements()) {
					if (element.getName().equals(APP_NAME)) {
						String sorder = element.getAttribute(START_ORDER);
						try {
							Integer.parseInt(sorder);
						} catch (NumberFormatException e) {
							logger.warn(MessageFormat.format("webapp: {0} has invalid startOrder: {1}", new Object[] {
									element.getAttribute(CONTEXT_ROOT), sorder }));
						}
						webapps.add(new WebAppWrapper(extension, element));
					}
				}
			}

		}
		WebAppWrapper[] webApps = webapps.toArray(new WebAppWrapper[webapps.size()]);
		Arrays.sort(webApps);
		for (WebAppWrapper app : webApps) {
			try {
				startWebApp(app.extension, app.element);
			} catch (Exception e) {
				String msg = "fail to start webapp " + app.extension.getLabel();
				logger.error(msg, e);
				throw new IllegalStateException(msg, e);
			}
		}
	}

	private void startWebApp(IExtension extension, IConfigurationElement webappElement) {
		String name = extension.getLabel();
		String ctxRoot = webappElement.getAttribute(CONTEXT_ROOT);
		String docRoot = webappElement.getAttribute(DOC_ROOT);

		String id = extension.getContributor().getName();
		String contextPath = ctxRoot == null ? id : ctxRoot;
		IPath location = resolveWebAppPath(id, docRoot);

		if (!contextPath.startsWith("/"))
			contextPath = "/" + contextPath;

		String msg = MessageFormat.format("starting webapp: {0} from: {1} docroot: {2}", new Object[] { name,
				contextPath, location.toOSString() });
		logger.info(msg);

		Context myctx = new StandardContext();
		myctx.setName(contextPath);
		myctx.setDisplayName(name);
		myctx.setPath(contextPath);
		myctx.setDocBase(location.toOSString());

		ContextConfig config = new ContextConfig();
		myctx.addLifecycleListener(config);

		IConfigurationElement[] welcomeFiles = webappElement.getChildren(FILE_NAME);
		for (IConfigurationElement welcomeFile : welcomeFiles)
			myctx.addWelcomeFile(welcomeFile.getAttribute(WEBAPP_NAME));

		IConfigurationElement[] params = webappElement.getChildren(PARAMETER);
		for (IConfigurationElement param : params)
			myctx.addParameter(param.getAttribute(WEBAPP_NAME), param.getAttribute(APP_VALUE));

		WebAppClassLoader classLoader = new WebAppClassLoader(new DaoClassLoader(id));
		WebappLoader loader = new WebappLoader(classLoader);
		myctx.setLoader(loader);

		myHost.addChild(myctx);

	}

	// LifeCycle
	public void startServer() {
		URL installURL = Activator.getInstance().getBundle().getEntry("/");		
		try {
			String urlPath = FileLocator.toFileURL(FileLocator.resolve(installURL)).getFile();
			System.setProperty(Globals.CATALINA_HOME_PROP, urlPath);
			System.setProperty(Globals.CATALINA_BASE_PROP, urlPath);
		} catch (IOException ex) {
			String msg = "Failed to resolve bundle url " + installURL;
			logger.error(msg, ex);
			throw new IllegalStateException(msg, ex);
		}
		myServer = new Catalina();
		myServer.start();
		myEngine = (Engine) myServer.getServer().findService("Catalina").getContainer();
		myHost = (Host) myEngine.findChild("localhost");
		TomcatURLStreamHandlerFactory.disable();
		prepareAllWebApps();
	}

	public void stopServer() {
		try {
			myServer.getServer().stop();
		} catch (LifecycleException e) {
			throw new IllegalStateException("Failed to stop tomcat", e);
		}
	}

	/**
	 * 
	 *
	 */
	private static class WebAppWrapper implements Comparable<WebAppWrapper> {

		IExtension extension;
		IConfigurationElement element;

		public WebAppWrapper(IExtension extension, IConfigurationElement element) {
			this.element = element;
			this.extension = extension;
		}

		public int compareTo(WebAppWrapper app) {
			int my_order = DEFAULT_ORDER;
			int other_order = DEFAULT_ORDER;
			try {
				my_order = Integer.parseInt(element.getAttribute(START_ORDER));
			} catch (Exception e) {
			}
			try {
				other_order = Integer.parseInt(app.element.getAttribute(START_ORDER));
			} catch (Exception e) {
			}
			return my_order - other_order;
		}
	}

}
