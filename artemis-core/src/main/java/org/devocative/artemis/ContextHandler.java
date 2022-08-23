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
import org.devocative.artemis.cfg.Config;
import org.devocative.artemis.ctx.ContextConfig;
import org.devocative.artemis.ctx.InitContext;
import org.devocative.artemis.log.ALog;

import java.io.*;
import java.util.HashMap;
import java.util.function.Consumer;

import static org.devocative.artemis.EVarScope.Global;

@Slf4j
public class ContextHandler {
	private static final String SCRIPT_VAR = "_";

	private static final ThreadLocal<Context> CTX = new ThreadLocal<>();
	private static final SimpleTemplateEngine ENGINE = new SimpleTemplateEngine();
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final Memory NEW_MEMORY = new Memory();
	private static final ContextConfig CONTEXT_CONFIG = new ContextConfig();

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

		MEM_FILE = String.format(".%s.memory.json", config.getName());
		final File file = new File(MEM_FILE);
		if (config.getDevMode() && file.exists()) {
			try {
				MEMORY = MAPPER.readValue(file, Memory.class);
				final Context ctx = MEMORY.getContext();
				ctx.addVarByScope(SCRIPT_VAR, MAIN, Global);
				CTX.set(ctx);

				NEW_MEMORY.setLastSuccessfulRqId(MEMORY.getLastSuccessfulRqId());
			} catch (IOException e) {
				throw new RuntimeException(String.format("Invalid '%s' as JSON file, you may need to delete it!", MEM_FILE), e);
			}
		} else {
			MEMORY = new Memory();
		}
	}

	public static void createContext() {
		createContext(null);
	}

	public static synchronized void createContext(Context parent) {
		final Context ctx = new Context(parent);

		if (parent == null) {
			ctx.addVarByScope(SCRIPT_VAR, MAIN, Global);

			CONFIG.getVars().forEach(var -> {
				ctx.addVarByScope(var.getName(), var.getValue(), Global);
				ALog.info("%cyan(External Global Var:) name=[{}] value=[{}]", var.getName(), var.getValue());
			});

			final InitContext init = new InitContext(ctx, CONTEXT_CONFIG);
			ctx.runAtScope(Global, () -> MAIN.invokeMethod("before", new Object[]{init}));
		}

		CTX.set(ctx);
	}

	public static Context get() {
		return CTX.get();
	}

	public static ContextConfig getCtxCfg() {
		return CONTEXT_CONFIG;
	}

	public static void shutdown(boolean successfulExec) {
		CTX.remove();

		if (successfulExec) {
			final File file = new File(MEM_FILE);
			if (file.exists()) {
				file.delete();
			}
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

	public static void invoke(String method, Object var2) {
		MAIN.invokeMethod(method, new Object[]{get(), var2});
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
		return loadFile(CONFIG.getXmlName());
	}

	public static InputStream loadFile(String name) {
		if (CONFIG.getBaseDir() == null) {
			return ContextHandler.class.getResourceAsStream("/" + name);
		} else {
			try {
				return new FileInputStream(String.format("%s/%s", CONFIG.getBaseDir(), name));
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// ------------------------------

	private static InputStream loadGroovyFile() {
		return loadFile(CONFIG.getGroovyName());
	}
}
