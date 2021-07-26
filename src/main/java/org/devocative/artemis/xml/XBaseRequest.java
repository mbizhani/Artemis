package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
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

	private List<XParam> params;

	private XAssertRs assertRs;

	// ------------------------------

	public abstract EMethod getMethod();
}
