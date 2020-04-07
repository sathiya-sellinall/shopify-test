package com.sellinall.authmgmt.security;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;

import com.mongodb.DBObject;
import com.sellinall.config.Config;
import com.sellinall.util.AuthConstant;
import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 *  * Allow the system to serve xhr level 2 from all cross domain site  *  * @author
 * Vikraman (LGPLv3)  * @version 0.1  
 */
public class AuthorizationRequestFilter implements ContainerRequestFilter {
	static Logger log = Logger.getLogger(AuthorizationRequestFilter.class.getName());

	// private AuthorizationLifeCycle authLifeCycle;

	public ContainerRequest filter(ContainerRequest arg0) throws WebApplicationException {
		if (arg0.getMethod().equals("OPTIONS")) {
			throw new WebApplicationException(Status.ACCEPTED);
		}
		if (arg0.getPath().contains("notification") && arg0.getMethod().equals("POST")) {
			System.out.println("NO Need To authorize this url because ebay or Paypal");
			log.debug("Method=" + arg0.getMethod());
			return arg0;
		}
		log.debug("method is " + arg0.getMethod());
		try {
			if (arg0.getHeaderValue(AuthConstant.RAGASIYAM_KEY) != null && Config.getConfig().getRagasiyam() != null
					&& checkValidUser(arg0.getHeaderValue(AuthConstant.RAGASIYAM_KEY).split(","),
							Config.getConfig().getRagasiyam().split(","))) {
				InBoundHeaders headers = new InBoundHeaders();
				String accountNumber = arg0.getHeaderValue("accountNumber");
				headers.add("accountNumber", accountNumber);
				// we can pass multiple ragasiyam values using comma separator
				headers.add(AuthConstant.RAGASIYAM_KEY, Config.getConfig().getRagasiyam());
				headers.add("Content-Type", "application/json");
				arg0.setHeaders(headers);
				return arg0;
			}
		} catch (Exception e) {
			log.error(arg0.getAbsolutePath().getHost());
			log.error("some Exception or some one hacking\n" + e);
		}
		// break the code since it reached here
		log.debug("UNAUTHORIZED");
		throw new WebApplicationException(Status.UNAUTHORIZED);
	}

	public boolean checkValidUser(String ragasiyam[], String originalvalue[]) {
		boolean flag = false;
		for (int i = 0; i < ragasiyam.length; i++) {
			for (int j = 0; j < originalvalue.length; j++) {
				if (ragasiyam[i] != null && originalvalue[j] != null && ragasiyam[i].equals(originalvalue[j])) {
					flag = true;
					break;
				}
			}
		}
		return flag;
	}
}
