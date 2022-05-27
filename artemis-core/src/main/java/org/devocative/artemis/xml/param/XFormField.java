package org.devocative.artemis.xml.param;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XStreamAlias("field")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = "valueAsBody")
public class XFormField extends XBaseParam {
	private String valueAsBody;
	private boolean file = false;

	@Override
	public String toString() {
		return file ?
			String.format("%s(file)=%s", getName(), getValue()) :
			String.format("%s=%s", getName(), getValue());
	}
}
