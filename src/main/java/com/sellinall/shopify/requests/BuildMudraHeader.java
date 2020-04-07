package com.sellinall.shopify.requests;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.log4j.Logger;

import com.sellinall.config.Config;
import com.sellinall.util.AuthConstant;

public class BuildMudraHeader implements Processor {
	static Logger log = Logger.getLogger(BuildMudraHeader.class.getName());

	public void process(Exchange exchange) throws Exception {
		String inBody = exchange.getIn().getBody(String.class);
		log.debug("inBody:" + inBody);
		exchange.getOut().setHeader("accountNumber", exchange.getProperty("accountNumber", String.class));
		exchange.getOut().setHeader(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
		exchange.getOut().setBody(inBody);
	}
}
