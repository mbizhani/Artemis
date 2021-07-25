package org.devocative.artemis.xml.method;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.devocative.artemis.xml.EMethod;
import org.devocative.artemis.xml.XBaseRequest;

@XStreamAlias("put")
public class XPut extends XBaseRequest {
	@Override
	public EMethod getMethod() {
		return EMethod.PUT;
	}
}
