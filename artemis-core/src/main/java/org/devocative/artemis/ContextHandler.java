package org.devocative.artemis;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import groovy.lang.GString;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.text.SimpleTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.HashMap;
import java.util.function.Consumer;

import static org.devocative.artemis.EVarScope.Global;

@Slf4j
public class ContextHandler {
	private static final String ARTEMIS_PROFILE_ENV = "ARTEMIS_PROFILE";
	private static final String ARTEMIS_PROFILE_SYS_PROP = "artemis.profile";
	private static final String ARTEMIS_BASE_URL_ENV = "ARTEMIS_BASE_URL";
	private static final String ARTEMIS_BASE_URL_SYS_PROP = "artemis.base.url";
	private static final String ARTEMIS_DEV_MODE_ENV = "ARTEMIS_DEV_MODE";
	private static final String ARTEMIS_DEV_MODE_SYS_PROP = "artemis.dev.mode";

	private static final String SCRIPT_VAR = "_";

	private static final ThreadLocal<Context> CTX = new ThreadLocal<>();
	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Memory NEW_MEMORY = new Memory();

	private static Script MAIN;
	private static Config CONFIG;
	private static String MEM_FILE;
	private static Memory MEMORY;

	// ------------------------------

	public static void init(Config config) {
		CONFIG = config;

		final GroovyShell shell = new GroovyShell();
		MAIN = shell.parse(new InputStreamReader(loadGroovyFile()));

		MAPPER.setVisibility(MAPPER.getSerializationConfig().getDefaultVisibilityChecker()
			.withFieldVisibility(JsonAutoDetect.Visibility.ANY));

		final SimpleModule grv = new SimpleModule();
		grv.addSerializer(new StdSerializer<GString>(GString.class) {
			@Override
			public void serialize(GString gString, JsonGenerator generator, SerializerProvider provider) throws IOException {
				generator.writeString(gString.toString());
			}
		});
		MAPPER.registerModule(grv);

		if (config.getProfile() == null) {
			config.setProfile(findValue(ARTEMIS_PROFILE_ENV, ARTEMIS_PROFILE_SYS_PROP, "local"));
		}

		if (config.getBaseUrl() == null) {
			config.setBaseUrl(findValue(ARTEMIS_BASE_URL_ENV, ARTEMIS_BASE_URL_SYS_PROP, "http://localhost:8080"));
		}

		if (config.getDevMode() == null) {
			config.setDevMode(Boolean.valueOf(findValue(ARTEMIS_DEV_MODE_ENV, ARTEMIS_DEV_MODE_SYS_PROP, "false")));
		}

		MEM_FILE = String.format(".%s.memory.json", config.getName());
		final File file = new File(MEM_FILE);
		if (config.getDevMode() && file.exists()) {
			try {
				MEMORY = MAPPER.readValue(file, Memory.class);
				final Context ctx = MEMORY.getContext();
				ctx.addVarByScope(SCRIPT_VAR, MAIN, Global);
				CTX.set(ctx);
			} catch (IOException e) {
				throw new RuntimeException(String.format("Invalid '%s' as JSON file, you may need to delete it!", MEM_FILE), e);
			}
		} else {
			MEMORY = new Memory();
		}
	}

	public static synchronized Context get() {
		Context ctx = CTX.get();

		if (ctx == null) {
			CTX.set(ctx = createContext());
		}

		return ctx;
	}

	public static void shutdown() {
		CTX.remove();

		final File file = new File(MEM_FILE);
		if (file.exists()) {
			file.delete();
		}
	}

	public static Object eval(String str) {
		try {
			return ENGINE
				.createTemplate(str)
				.make(new HashMap<>(get().getVars()))
				.toString();
		} catch (ClassNotFoundException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static void invoke(String method) {
		MAIN.invokeMethod(method, new Object[]{get()});
	}

	public static <T> T fromJson(String json, Class<T> cls) throws JsonProcessingException {
		return MAPPER.readValue(json, cls);
	}

	public static Memory getMEMORY() {
		return MEMORY;
	}

	public static void updateMemory(Consumer<Memory> consumer) {
		if (CONFIG.getDevMode()) {
			consumer.accept(NEW_MEMORY);
		}
	}

	public static void memorize() {
		if (CONFIG.getDevMode()) {
			final Context ctx = CTX.get();
			ctx.removeVar(SCRIPT_VAR, Global);

			NEW_MEMORY.setContext(ctx);

			try {
				MAPPER.writeValue(new File(MEM_FILE), NEW_MEMORY);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static InputStream loadXmlFile() {
		if (CONFIG.getBaseDir() == null) {
			return ContextHandler.class.getResourceAsStream(String.format("/%s.xml", CONFIG.getName()));
		} else {
			try {
				return new FileInputStream(String.format("%s/%s.xml", CONFIG.getBaseDir(), CONFIG.getName()));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// ------------------------------

	private static Context createContext() {
		final Context ctx = new Context(CONFIG.getProfile());
		ctx.addVarByScope(SCRIPT_VAR, MAIN, Global);

		ctx.runAtScope(Global, () -> MAIN.invokeMethod("before", new Object[]{ctx}));

		return ctx;
	}

	private static String findValue(String envVar, String sysVar, String def) {
		if (System.getenv(envVar) != null) {
			return System.getenv(envVar);
		} else if (System.getProperty(sysVar) != null) {
			return System.getProperty(sysVar);
		}
		return def;
	}

	private static InputStream loadGroovyFile() {
		if (CONFIG.getBaseDir() == null) {
			return ContextHandler.class.getResourceAsStream(String.format("/%s.groovy", CONFIG.getName()));
		} else {
			final File baseDir = new File(CONFIG.getBaseDir());
			log.info("Artemis Base Dir to Load Files: {}", baseDir.getAbsolutePath());

			try {
				return new FileInputStream(String.format("%s/%s.groovy", CONFIG.getBaseDir(), CONFIG.getName()));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
