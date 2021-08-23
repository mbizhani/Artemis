package org.devocative.artemis.xml;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@XStreamAlias("scenario")
public class XScenario {
	@XStreamAsAttribute
	private String name;

	@XStreamAsAttribute
	private Boolean enabled;

	private List<XVar> vars;

	@XStreamImplicit
	private List<XBaseRequest> requests;

	// ------------------------------

	public boolean isEnabled() {
		return getEnabled() == null || getEnabled();
	}

	public void updateRequestsIds() {
		int idx = 1;
		for (XBaseRequest rq : requests) {
			if (rq.isWithId() == null) {
				rq.setWithId(rq.getId() != null);
				if (rq.getId() == null) {
					rq.setId(String.format("step #%s", idx++));
				}
			}
		}
	}
}
