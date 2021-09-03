package org.devocative.artemis.xml.method;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;
import org.devocative.artemis.xml.EMethod;
import org.devocative.artemis.xml.XBaseRequest;

@Getter
@Setter
@XStreamAlias("post")
public class XPost extends XBaseRequest {
	@Override
	public EMethod getMethod() {
		return EMethod.POST;
	}
}
