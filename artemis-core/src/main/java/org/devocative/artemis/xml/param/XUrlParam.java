package org.devocative.artemis.xml.param;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import com.thoughtworks.xstream.converters.extended.ToAttributedValueConverter;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XStreamAlias("param")
@XStreamConverter(value = ToAttributedValueConverter.class, strings = "valueAsBody")
public class XUrlParam extends XBaseParam {
	private String valueAsBody;
}
