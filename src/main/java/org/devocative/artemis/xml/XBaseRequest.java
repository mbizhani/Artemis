package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.devocative.artemis.xml.method.XBody;

import java.util.List;

import static org.devocative.artemis.xml.EMethod.POST;
import static org.devocative.artemis.xml.EMethod.PUT;

@Getter
@Setter
@ToString(of = "url")
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

	private List<XParam> params;

	private XAssertRs assertRs;

	// ---------------

	@Getter(AccessLevel.NONE)
	@XStreamOmitField
	private Boolean withId;

	// ------------------------------

	public abstract EMethod getMethod();

	public boolean shouldHaveBody() {
		return getMethod() == POST || getMethod() == PUT;
	}

	public Boolean isWithId() {
		return withId;
	}
}
