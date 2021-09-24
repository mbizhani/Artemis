package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;

@XStreamAlias("break-point")
public class XBreakPoint extends XBaseRequest {

	public XBreakPoint() {
		setId(BREAK_POINT_ID);
	}

	@Override
	public EMethod getMethod() {
		throw new RuntimeException("No Method for XBreakPoint!");
	}
}
