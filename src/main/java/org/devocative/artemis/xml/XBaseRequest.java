package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class XBaseRequest {
	@XStreamAsAttribute
	private String name;

	@XStreamAsAttribute
	private String url;

	private List<XHeader> headers;

	private List<XParam> params;

	private XAssertRs assertRs;
}
