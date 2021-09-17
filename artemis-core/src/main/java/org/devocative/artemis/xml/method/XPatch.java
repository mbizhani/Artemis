package org.devocative.artemis.xml.method;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.devocative.artemis.xml.EMethod;
import org.devocative.artemis.xml.XBaseRequest;

@XStreamAlias("patch")
public class XPatch extends XBaseRequest {
	@Override
	public EMethod getMethod() {
		return EMethod.PATCH;
	}
}
