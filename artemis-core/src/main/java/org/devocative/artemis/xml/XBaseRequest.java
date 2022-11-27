package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.xml.method.XBody;
import org.devocative.artemis.xml.method.XWhen;
import org.devocative.artemis.xml.param.XFormField;
import org.devocative.artemis.xml.param.XUrlParam;

import java.util.List;

@Getter
@Setter
public abstract class XBaseRequest {
	public static final String BREAK_POINT_ID = "_BREAK_POINT_";

	@XStreamAsAttribute
	private String id;

	@XStreamAsAttribute
	private String url;

	@XStreamAsAttribute
	private Boolean call;

	private XWhen when;

	private List<XVar> vars;

	private List<XHeader> headers;

	private XBody body;

	private List<XUrlParam> urlParams;

	private List<XFormField> form;

	private XAssertRs assertRs;

	// ---------------

	@Getter(AccessLevel.NONE)
	@XStreamOmitField
	private Boolean withId;

	@XStreamOmitField
	private String globalId;

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
