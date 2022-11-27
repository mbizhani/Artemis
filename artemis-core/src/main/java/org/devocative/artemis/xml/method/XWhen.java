package org.devocative.artemis.xml.method;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XStreamAlias("when")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = "content")
public class XWhen {
	private String content;

	@XStreamAsAttribute
	private String message;
}
