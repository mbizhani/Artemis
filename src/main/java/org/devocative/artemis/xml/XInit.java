package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@XStreamAlias("init")
public class XInit {
	@XStreamAsAttribute
	private String call;

	@XStreamImplicit
	private List<XVar> vars;
}
