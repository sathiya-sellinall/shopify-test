package com.sellinall.shopify.exception;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

/**
 * 
 * @author Raguvaran
 *
 */

public class HandleException implements Processor {
	static Logger log = Logger.getLogger(HandleException.class.getName());

	public void process(Exchange exchange) throws Exception {
		exchange.setProperty("failureReason", "Internal Error");
		if (exchange.getIn().getHeaders().containsKey("pullInventoryException")) {
			log.error("Error occured during pull inventory!" + exchange.getIn().getHeader("pullInventoryException"));
		}
	}

}