// RequestTrans.java
// Copyright (c) Hannes Walln�fer 1998-2000

package helma.framework;

import java.io.*;
import java.util.*;
import helma.objectmodel.*;

/**
 * A Transmitter for a request from the servlet client. Objects of this 
 * class are directly exposed to JavaScript as global property req. 
 */
 
public class RequestTrans implements Externalizable {

    // the uri path of the request
    public String path;
    // the request's session id
    public String session;
    // the map of form and cookie data
    private Map values;
    // the request method - 0 for GET, 1 for POST
    private byte httpMethod = 0;

    // this is used to hold the EcmaScript form data object
    public transient Object data;
    // when was execution started on this request?
    public transient long startTime;

    /**
     *  Create a new Request transmitter with an empty data map.
     */
    public RequestTrans () {
	httpMethod = 0;
	values = new HashMap ();
    }

    /**
    *  Create a new request transmitter with the given data map.
    */
    public RequestTrans (byte method) {
	httpMethod = method;
	values = new HashMap ();
    }

    /**
     *  Set a parameter value in this request transmitter.
     */
    public void set (String name, Object value) {
	values.put (name, value);
    }


    /**
     *  Get a value from the requests map by key.
     */
    public Object get (String name) {
	try {
	    return values.get (name);
	} catch (Exception x) {
	    return null; 
	}
    }

    /**
     *  Get the data map for this request transmitter.
     */
    public Map getRequestData () {
	return values;
    }

    /**
     *  The hash code is computed from the session id if available. This is used to
     *  detect multiple identic requests.
     */
    public int hashCode () {
	return session == null ? super.hashCode () : session.hashCode ();
    }


    /**
     * A request is considered equal to another one if it has the same user, path,
     * and request data. This is used to evaluate multiple simultanous requests only once
     */
    public boolean equals (Object what) {
	try {
	    RequestTrans other = (RequestTrans) what;
	    return (session.equals (other.session) &&
		path.equalsIgnoreCase (other.path) &&
		values.equals (other.getRequestData ()));
	} catch (Exception x) {
	    return false;
	}
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isGet () {
	return httpMethod == 0;
    }

    /**
     *  Return true if this object represents a HTTP GET Request.
     */
    public boolean isPost () {
	return httpMethod == 1;
    }

    /**
     * Custom externalization code for quicker serialization.
     */
    public void readExternal (ObjectInput s) throws ClassNotFoundException, IOException {
	path = s.readUTF ();
	session = s.readUTF ();
	values = (Hashtable) s.readObject ();
	httpMethod = s.readByte ();
    }

    /**
     * Custom externalization code for quicker serialization.
     */
    public void writeExternal (ObjectOutput s) throws IOException {
	s.writeUTF (path);
	s.writeUTF (session);
	s.writeObject (values);
	s.writeByte (httpMethod);
    }
}
