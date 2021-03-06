package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@XStreamAlias("artemis")
public class XArtemis {
	private List<XVar> vars;

	@XStreamImplicit
	private List<XScenario> scenarios;
}
