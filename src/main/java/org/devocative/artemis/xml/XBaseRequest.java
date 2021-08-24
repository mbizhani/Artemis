package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.xml.method.XBody;

import java.util.List;

@Getter
@Setter
public abstract class XBaseRequest {

	@XStreamAsAttribute
	private String id;

	@XStreamAsAttribute
	private String url;

	@XStreamAsAttribute
	private String call;

	private List<XVar> vars;

	private List<XHeader> headers;

	private XBody body;

	private List<XParam> urlParams;

	private List<XParam> formParams;

	private XAssertRs assertRs;

	// ---------------

	@Getter(AccessLevel.NONE)
	@XStreamOmitField
	private Boolean withId;

	// ------------------------------

	public abstract EMethod getMethod();

	public Boolean isWithId() {
		return withId;
	}

	@Override
	public String toString() {
		return String.format("Rq(%s): %s - %s", getId(), getMethod(), getUrl());
	}
}
