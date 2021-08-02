package org.devocative.artemis.xml.method;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.xml.ERqBodyType;

@Getter
@Setter
@XStreamAlias("body")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = "content")
public class XBody {
	@XStreamAsAttribute
	private ERqBodyType type = ERqBodyType.json;

	private String content;

	// ------------------------------

	public boolean isJson() {
		return getType() == null || ERqBodyType.json == getType();
	}
}
