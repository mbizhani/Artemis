package org.devocative.artemis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thoughtworks.xstream.XStream;
import org.devocative.artemis.cfg.Config;
import org.devocative.artemis.http.*;
import org.devocative.artemis.xml.*;
import org.devocative.artemis.xml.method.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.devocative.artemis.EVarScope.*;
import static org.devocative.artemis.Memory.EStep.*;
import static org.devocative.artemis.Util.asMap;

public class ArtemisExecutor {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    private static final String THIS = "_this";
    private static final String PREV = "_prev";
    private static final String LOOP_VAR = "_loop";

    private final Config config;
    private final HttpFactory httpFactory;

    // ------------------------------

    public static void run() {
        run(new Config());
    }

    // Main
    public static void run(Config config) {
        config.init();

        ALog.init(config.getXmlName(), config.getConsoleLog() != null ?
                config.getConsoleLog() :
                config.getDevMode() || config.getParallel() == 1);

        new ArtemisExecutor(config).execute();
    }

    // ------------------------------

    private ArtemisExecutor(Config config) {
        this.config = config;

        ContextHandler.init(config);
        this.httpFactory = new HttpFactory(config.getBaseUrl(), config.getProxy());
    }

    // ------------------------------

    private void execute() {
        final XArtemis artemis = createXArtemis();

        final List<XScenario> scenarios = artemis.getScenarios()
                .stream()
                .filter(scenario -> scenario.isEnabled() &&
                        (config.getOnlyScenarios().isEmpty() || config.getOnlyScenarios().contains(scenario.getName())))
                .collect(Collectors.toList());

        final Runnable runnable = () ->
                run(scenarios, artemis.getVars(), config.getLoop());

        final Result result = Parallel.execute(config.getXmlName(), config.getParallel(), runnable);

        StatisticsContext.print();

        if (result.hasError()) {
            throw new TestFailedException(result.getErrors())
                    .setDegree(result.getDegree())
                    .setNoOfErrors(result.getNoOfErrors());
        }
    }

    private void run(final List<XScenario> scenarios, final List<XVar> globalVars, final int loopMax) {
        ALog.info("*---------*---------*");
        ALog.info("|   A R T E M I S   |");
        ALog.info(config.getDevMode() ?
                "*-------D E V-------*" :
                "*---------*---------*");

        final Context ctx = ContextHandler.get();
        config.getVars().forEach(v -> ctx.addVarByScope(v.getName(), v.getValue(), Global));

        long start = System.currentTimeMillis();
        int iteration = 0;
        boolean successfulExec = true;

        try {
            for (; iteration < loopMax; iteration++) {
                start = System.currentTimeMillis();

                globalVars.forEach(var -> {
                    final String value = var.getValue();
                    ctx.addVarByScope(var.getName(), value, EVarScope.Global);
                    ALog.info("{}Global Var:{} name=[{}] value=[{}]", ANSI_CYAN, ANSI_RESET, var.getName(), value);
                });

                for (XScenario scenario : scenarios) {
                    ctx.addVarByScope(LOOP_VAR, iteration, Global);

                    ContextHandler.updateMemory(m -> m.setScenarioName(scenario.getName()));

                    ALog.info("{}=============== [{}] ==============={}", ANSI_PURPLE, scenario.getName(), ANSI_RESET);
                    scenario.getVars()
                            .forEach(v -> ctx.addVarByScope(v.getName(), v.getValue(), Scenario));

                    scenario.updateRequestsIds();

                    for (XBaseRequest rq : scenario.getRequests()) {
                        ALog.info("{}--------------- [{}] ---------------{}", ANSI_BLUE, rq.getId(), ANSI_RESET);

                        ContextHandler.updateMemory(m -> m.setRqId(rq.getId()));

                        if (!XBaseRequest.BREAK_POINT_ID.equals(rq.getId())) {
                            initRq(rq);
                            sendRq(rq);

                            ContextHandler.updateMemory(Memory::clear);
                            ctx.clearVars(EVarScope.Request);
                        } else if (config.getDevMode()) {
                            throw new TestFailedException("Reached Break Point!");
                        } else {
                            ALog.warn("Passing <break-point/> in Main Mode!");
                        }
                    }

                    ctx.clearVars(Scenario);
                    ContextHandler.updateMemory(Memory::clearAll);
                }

                final long duration = System.currentTimeMillis() - start;
                if (loopMax == 1) {
                    ALog.info("{}***** [STATISTICS] *****{}", ANSI_CYAN, ANSI_RESET);
                    StatisticsContext.printThis();
                    ALog.info("{}***** [PASSED SUCCESSFULLY in {} ms] *****{}", ANSI_GREEN
                            , duration, ANSI_RESET);
                } else {
                    ALog.info("{}***** [PASSED SUCCESSFULLY in {} ms, loopIdx={}] *****{}", ANSI_GREEN,
                            duration, iteration, ANSI_RESET);
                }

                StatisticsContext.execFinished(iteration, duration, "");
            }
        } catch (RuntimeException e) {
            successfulExec = false;
            ALog.error(e.getMessage());
            ContextHandler.memorize();
            StatisticsContext.execFinished(iteration, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        } finally {
            ContextHandler.shutdown(successfulExec);
            httpFactory.shutdown();
        }
    }

    private void initRq(XBaseRequest rq) {
        final Context ctx = ContextHandler.get();

        if (ctx.containsVar(THIS, Scenario)) {
            ctx.addVarByScope(PREV, ctx.removeVar(THIS, Scenario), Scenario);
        }

        ContextHandler.updateMemory(m -> m.addStep(RqVars));

        int addVars = 0;
        for (XVar var : rq.getVars()) {
            ctx.addVarByScope(var.getName(), var.getValue(), Request);
            addVars++;
        }
        if (addVars > 0) {
            ALog.info("[{}] var(s) added to context", addVars);
        }

        ContextHandler.updateMemory(m -> m.addStep(RqCall));
        if (rq.getCall() != null && rq.getCall()) {
            if (!rq.isWithId()) {
                throw new TestFailedException(rq.getId(), "No id for Request to Call");
            }
            try {
                ctx.runAtScope(Request, () -> ContextHandler.invoke(rq.getId()));
                ALog.info("{}Call Method{} - '{}(Context)'", ANSI_CYAN, ANSI_RESET, rq.getId());
            } catch (RuntimeException e) {
                ALog.error("ERROR: RQ({}) - calling method: '{}(Context)'", rq.getId(), rq.getId());
                throw e;
            }
        }
    }

    private void sendRq(XBaseRequest rq) {
        ContextHandler.updateMemory(m -> m.addStep(RqSend));

        final Context ctx = ContextHandler.get();

        final Map<String, Object> rqAndRs = new HashMap<>();
        ctx.addVarByScope(THIS, rqAndRs, Scenario);
        if (rq.isWithId()) {
            ctx.addVarByScope(rq.getId(), rqAndRs, Scenario);
        }

        final HttpRequestData data = new HttpRequestData(rq.getId(), rq.getUrl(), rq.getMethod().name());
        data.setHeaders(asMap(rq.getHeaders()));

        final XBody body = rq.getBody();
        if (body != null) {
            data.setBody(body.getContent().trim());
        }

        if (rq.getForm() != null) {
            data.setFormFields(rq.getForm().stream()
                    .map(x -> new FormField(x.getName(), x.getValue(), x.isFile()))
                    .collect(Collectors.toList()));
        }

        if (ctx.getConfig().getBeforeSend() != null) {
            ctx.getConfig().getBeforeSend().accept(data);
        }

        final HttpRequest httpRq = httpFactory.create(rq);
        httpRq.setHeaders(data.getHeaders());
        if (data.getBody() != null) {
            httpRq.setBody(data.getBody().toString());
        }
        httpRq.setFormParams(data.getFormFields());
        httpRq.send(rs -> processRs(rs, rq, rqAndRs));
    }

    private void processRs(HttpResponse rs, XBaseRequest rq, Map<String, Object> rqAndRs) {

        if (rq.getAssertRs() == null) {
            ALog.warn("RQ({}) - No <assertRs/>!", rq.getId());
            rq.setAssertRs(new XAssertRs());
        }

        final XAssertRs assertRs = rq.getAssertRs();
        if (assertRs.getBody() == null) {
            assertRs.setBody(ERsBodyType.json);
        }
        assertCode(rq, rs);

        if (assertRs.getProperties() != null && assertRs.getBody() != ERsBodyType.json) {
            throw new TestFailedException(rq.getId(),
                    "Invalid <assertRs/> Definition: properties defined for non-json body");
        }

        final String rsBodyAsStr = rs.getBody();
        switch (assertRs.getBody()) {
            case json:
                final Object obj = json(rq.getId(), rsBodyAsStr);
                rqAndRs.put("rs", obj);
                if (obj instanceof Map) {
                    ALog.info("{}RS Properties ={} {}", ANSI_CYAN, ANSI_RESET, ((Map<?, ?>) obj).keySet());
                }
                assertProperties(rq, obj);
                if (assertRs.getStore() != null) {
                    if (rq.isWithId()) {
                        storeProperties(rq.getId(), assertRs.getStore(), obj);
                    } else {
                        throw new TestFailedException(rq.getId(), "Id Not Found to Store: %s", assertRs.getStore());
                    }
                }

                if (assertRs.getCall() != null && assertRs.getCall()) {
                    if (rq.isWithId()) {
                        assertCall(rq, obj);
                    } else {
                        throw new TestFailedException(rq.getId(), "Id Not Found to Call Assert");
                    }
                }
                break;
            case text:
                if (rsBodyAsStr.trim().isEmpty()) {
                    throw new TestFailedException(rq.getId(), "Invalid Rs Body: expecting text, got empty");
                }
                rqAndRs.put("rs", rsBodyAsStr);
                break;
            case empty:
                if (!rsBodyAsStr.trim().isEmpty()) {
                    throw new TestFailedException(rq.getId(), "Invalid Rs Body: expecting empty, got text");
                }
                break;
        }

        assertCookies(rs, rq, assertRs);
    }

    // ---------------

    private XArtemis createXArtemis() {
        final XStream xStream = new XStream();
        xStream.processAnnotations(new Class[]{XArtemis.class, XGet.class,
                XPost.class, XPut.class, XPatch.class, XDelete.class, XBreakPoint.class});
        xStream.allowTypesByWildcard(new String[]{"org.devocative.artemis.xml.**"});

        final XArtemis artemis = (XArtemis) xStream.fromXML(ContextHandler.loadXmlFile());

        if (config.getDevMode()) {
            if (config.getLoop() != null && config.getLoop() > 1) {
                ALog.warn("DEV MODE: 'loop' set to 1");
            }

            if (config.getParallel() != null && config.getParallel() > 1) {
                ALog.warn("DEV MODE: 'parallel' set to 1");
            }

            config
                    .setLoop(1)
                    .setParallel(1);

            final Memory memory = ContextHandler.getMEMORY();
            if (memory.isEmpty()) {
                ALog.info("DEV MODE - Empty Memory");
            } else {
                ALog.info("DEV MODE - Memory: {} -> {}, {}", memory.getScenarioName(), memory.getRqId(), memory.getSteps());
            }

            if (memory.getScenarioName() != null) {
                artemis.setVars(Collections.emptyList());

                while (!memory.getScenarioName().equals(artemis.getScenarios().get(0).getName())) {
                    final XScenario removed = artemis.getScenarios().remove(0);
                    ALog.info("DEV MODE - Removed Scenario: {}", removed.getName());
                }

                final XScenario scenario = artemis.getScenarios().get(0);
                scenario.updateRequestsIds();
                ALog.info("DEV MODE - Removed Scenario Vars: {}", scenario.getName());
                scenario.setVars(Collections.emptyList());

                final List<Memory.EStep> steps = memory.getSteps();

                if (steps.contains(RqVars)) {
                    final Context ctx = memory.getContext();
                    if (ctx.containsVar(PREV, Scenario)) {
                        ctx.addVarByScope(THIS, ctx.removeVar(PREV, Scenario), Scenario);
                    }

                    while (!memory.getRqId().equals(scenario.getRequests().get(0).getId())) {
                        final XBaseRequest removed = scenario.getRequests().remove(0);
                        ALog.info("DEV MODE - Removed Rq: {} ({})", removed.getId(), scenario.getName());
                    }

                    final XBaseRequest rq = scenario.getRequests().get(0);
                    if (steps.contains(RqCall)) {
                        ALog.info("DEV MODE - Removed Rq Vars: {} ({})", rq.getId(), scenario.getName());
                        rq.setVars(Collections.emptyList());

                        if (steps.contains(RqSend)) {
                            ALog.info("DEV MODE - Removed Rq Call: {} ({})", rq.getId(), scenario.getName());
                            rq.setCall(null);
                        }
                    }
                } else if (
                        steps.isEmpty() &&
                                XBaseRequest.BREAK_POINT_ID.equals(memory.getRqId()) &&
                                memory.getLastSuccessfulRqId() != null) {

                    while (!scenario.getRequests().isEmpty()) {
                        final XBaseRequest removed = scenario.getRequests().remove(0);
                        ALog.info("DEV MODE - Removed Rq: {} ({})", removed.getId(), scenario.getName());

                        if (memory.getLastSuccessfulRqId().equals(removed.getId())) {
                            break;
                        }
                    }
                }
            }
        }

        return Proxy.create(artemis);
    }

    private void assertProperties(XBaseRequest rq, Object rsAsObj) {
        final XAssertRs assertRs = rq.getAssertRs();

        if (assertRs.getProperties() != null) {
            final String[] properties = assertRs.getProperties().split(",");

            if (rsAsObj instanceof Map) {
                final Map<?, ?> rsAsMap = (Map<?, ?>) rsAsObj;
                for (String property : properties) {
                    final String prop = property.trim();
                    if (!rsAsMap.containsKey(prop)) {
                        throw new TestFailedException(rq.getId(), "Invalid Property in RS Object: [%s]", prop);
                    }
                }
            } else {
                throw new TestFailedException(rq.getId(), "Invalid RS Type for Asserting Properties");
            }
        }
    }

    private void assertCookies(HttpResponse rs, XBaseRequest rq, XAssertRs assertRs) {
        if (!rs.getCookies().isEmpty() && assertRs.getCookies() != null) {
            final String[] cookieNames = assertRs.getCookies().split(",");
            for (String cookieName : cookieNames) {
                if (!rs.getCookies().containsKey(cookieName.trim())) {
                    throw new TestFailedException(rq.getId(), "Cookie Not Found: %s", cookieName);
                }
            }
        }
    }

    private void storeProperties(String id, String properties, Object rsAsObj) {
        if (rsAsObj instanceof Map) {
            final Map<?, ?> rsAsMap = (Map<?, ?>) rsAsObj;
            final Map<String, Object> rs = new HashMap<>();
            final String[] props = properties.split(",");

            for (String prop : props) {
                final String[] parts = prop.trim().split("[.]");
                rs.put(parts[0], findValue(parts, 0, rsAsMap));
            }

            Map<String, Object> store = new HashMap<>();
            store.put("rs", rs);
            ContextHandler.get().addVarByScope(id, store, Global);
        }
    }

    private void assertCall(XBaseRequest rq, Object obj) {
        final String methodName = String.format("assertRs_%s", rq.getId());
        if (obj instanceof Map) {
            ALog.info("{}AssertRs Call:{} {}(Context, Map)", ANSI_CYAN, ANSI_RESET, methodName);
            ContextHandler.get().runAtScope(Assert, () ->
                    ContextHandler.invoke(methodName, Immutable.create((Map) obj)));
        } else if (obj instanceof List) {
            ALog.info("{}AssertRs Call:{} {}(Context, List)", ANSI_CYAN, ANSI_RESET, methodName);
            ContextHandler.get().runAtScope(Assert, () ->
                    ContextHandler.invoke(methodName, Immutable.create((List<?>) obj)));
        } else {
            throw new TestFailedException(rq.getId(), "Unsupported Response Body Type");
        }
    }

    private Object findValue(String[] parts, int idx, Map<?, ?> rsAsMap) {
        final Object obj = rsAsMap.get(parts[idx]);

        if (obj == null) {
            throw new RuntimeException(String.format("Prop Not Found: %s (%s)",
                    parts[idx],
                    String.join(".", parts)));
        }

        if (idx == parts.length - 1) {
            return obj;
        } else {
            final Map<String, Object> result = new HashMap<>();
            if (!(obj instanceof Map)) {
                throw new RuntimeException(String.format("Invalid Prop as Map: %s (%s)",
                        parts[idx], String.join(".", parts)));
            }
            result.put(parts[idx], findValue(parts, idx + 1, (Map<?, ?>) obj));
            return result;
        }
    }

    private void assertCode(XBaseRequest rq, HttpResponse rs) {
        final XAssertRs assertRs = rq.getAssertRs();
        if (assertRs.getStatus() != null && !assertRs.getStatus().equals(rs.getCode())) {
            throw new TestFailedException(rq.getId(), "Invalid RS Code: expected %s, got %s",
                    assertRs.getStatus(), rs.getCode());
        }
    }

    private Object json(String id, String content) {
        try {
            return ContextHandler.fromJson(content, Object.class);
        } catch (JsonProcessingException e) {
            throw new TestFailedException(id, "Invalid JSON Format:\n%s", content);
        }
    }
}
