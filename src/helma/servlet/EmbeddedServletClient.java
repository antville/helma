// EmbeddedServletClient.java
// Copyright (c) Hannes Walln�fer,  2002

package helma.servlet;

import javax.servlet.*;
import java.io.*;
import java.util.*;
import helma.framework.*;
import helma.framework.core.Application;
import helma.main.*;
import helma.util.*;

/**
 *  Servlet client that runs a Helma application for the embedded
 *  web server
 */

public final class EmbeddedServletClient extends AbstractServletClient {

    private Application app = null;
    private String appName;

    // The path where this servlet is mounted
    String mountpoint;

    public EmbeddedServletClient () {
	super ();
    }


    public void init (ServletConfig init) throws ServletException {
	super.init (init);
	appName = init.getInitParameter ("application");
	if (appName == null)
	    throw new ServletException ("Application name not set in init parameters");
	mountpoint = init.getInitParameter ("mountpoint");
	if (mountpoint == null)
	    mountpoint = "/"+appName;
    }

    IRemoteApp getApp (String appID) {
	if (app == null)
	    app = Server.getServer().getApplication (appName);
	return app;
    }


    void invalidateApp (String appID) {
	// do nothing
    }

    String getAppID (String path) {
	return appName;
    }

    String getRequestPath (String path) {
	if (path == null)
	    return "";
	// We already get the correct request path
	// from the servlet container.
	return trim (path);
    }

    String trim (String str) {
	char[] val = str.toCharArray ();
	int len = val.length;
	int st = 0;

	while ((st < len) && (val[st] <= ' ' || val[st] == '/'))
	    st++;

	while ((st < len) && (val[len - 1] <= ' ' || val[len - 1] == '/'))
	    len--;

	return ((st > 0) || (len < val.length)) ? new String (val, st, len-st) : str;
    }

}



