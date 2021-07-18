package org.devocative.artemis;

import com.thoughtworks.xstream.XStream;
import org.devocative.artemis.xml.XScenario;
import org.devocative.artemis.xml.method.XDelete;
import org.devocative.artemis.xml.method.XGet;
import org.devocative.artemis.xml.method.XPost;
import org.devocative.artemis.xml.method.XPut;

import java.io.FileReader;

public class ArtemisMain {
	public static void main(String[] args) throws Exception {
		final XStream xStream = new XStream();
		XStream.setupDefaultSecurity(xStream);
		xStream.processAnnotations(new Class[]{XScenario.class, XGet.class, XPost.class, XPut.class, XDelete.class});
		xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

		final XScenario scenario = (XScenario) xStream.fromXML(new FileReader("test.xml"));
		System.out.println("scenario.getName() = " + scenario.getName());
	}
}
