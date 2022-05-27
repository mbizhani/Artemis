package org.devocative.artemis.xml.param;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.xml.INameTheValue;

@Getter
@Setter
public abstract class XBaseParam implements INameTheValue {
	@XStreamAsAttribute
	private String name;

	@XStreamAlias("value")
	@XStreamAsAttribute
	private String valueAsAttr;

	// ------------------------------

	public abstract String getValueAsBody();

	// ---------------

	@Override
	public String getValue() {
		return !getValueAsBody().isEmpty() ? getValueAsBody() : getValueAsAttr();
	}

	@Override
	public String toString() {
		return String.format("Param: '%s' = '%s'", getName(), getValue());
	}
}
