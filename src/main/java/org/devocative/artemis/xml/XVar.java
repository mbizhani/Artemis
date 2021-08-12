package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XStreamAlias("var")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = "content")
public class XVar implements INameTheValue {
	@XStreamAsAttribute
	private String name;

	@XStreamAsAttribute
	private String value;

	private String content;

	// ------------------------------

	public String getTheValue() {
		return !getContent().isEmpty() ? getContent() : getValue();
	}
}
