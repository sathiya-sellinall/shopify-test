package com.sellinall.shopify;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.log4j.BasicConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.sellinall.config.Config;
import com.sellinall.shopify.services.AccountService;
import com.sellinall.shopify.services.ListingServ;
import com.sellinall.shopify.services.Notification;
import com.sellinall.util.AuthConstant;
import com.sellinall.util.InventorySequence;

/**
 * 
 * This class launches the web application in an embedded Jetty container. This
 * is the entry point to your application. The Java command that is used for
 * launching should fire this main method.
 * 
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String webappDirLocation = "src/main/webapp/";
		Config.context = new ClassPathXmlApplicationContext("Propertycfg.xml");

		// The port that we should run on can be set into an environment
		// variable
		// Look for that variable and default to 8080 if it isn't there.
		String webPort = System.getenv("PORT");
		if (webPort == null || webPort.isEmpty()) {
			webPort = "8081";
		}

		Server server = new Server(Integer.valueOf(webPort));
		WebAppContext root = new WebAppContext();

		root.setContextPath("/");
		root.setDescriptor(webappDirLocation + "/WEB-INF/web.xml");
		root.setResourceBase(webappDirLocation);
		BasicConfigurator.configure();
		// Parent loader priority is a class loader setting that Jetty accepts.
		// By default Jetty will behave like most web containers in that it will
		// allow your application to replace non-server libraries that are part
		// of the
		// container. Setting parent loader priority to true changes this
		// behavior.
		// Read more here:
		// http://wiki.eclipse.org/Jetty/Reference/Jetty_Classloading
		root.setParentLoaderPriority(true);
		server.setHandler(root);
		ApplicationContext appContext = new ClassPathXmlApplicationContext("CamelContext.xml");
		CamelContext camelContext = SpringCamelContext.springCamelContext(appContext, false);
		camelContext.start();
		ProducerTemplate template=camelContext.createProducerTemplate();
		AccountService.setProducerTemplate(template);
		ListingServ.setProducerTemplate(template);
		Notification.setProducerTemplate(template);

		// init db sequence
		Config config = Config.getConfig();
		InventorySequence.init(config.getInventoryCollectionDBName(), config.getInventoryCollectionHostName(),
				config.getInventoryCollectionPort(), config.getDbUserName(), config.getDbPassword());
		config.setRagasiyam(System.getenv(AuthConstant.RAGASIYAM_KEY));

		server.start();
		server.join();
	}

}
