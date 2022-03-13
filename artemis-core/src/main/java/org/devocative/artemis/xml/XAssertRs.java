package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XStreamAlias("assertRs")
public class XAssertRs {
	@XStreamAsAttribute
	private Integer status;

	@XStreamAsAttribute
	private ERsBodyType body;

	@XStreamAsAttribute
	private String properties;

	@XStreamAsAttribute
	private String cookies;

	@XStreamAsAttribute
	private String store;

	@XStreamAsAttribute
	private Boolean call;
}
