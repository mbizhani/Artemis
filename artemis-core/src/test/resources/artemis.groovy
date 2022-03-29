import org.devocative.artemis.Context
import org.devocative.artemis.http.HttpRequestData
import org.junit.jupiter.api.Assertions

// Called by ContextHandler Initialization
def before(Context ctx) {
	Artemis.log("Inside artemis.groovy: profile=${ctx.profile}")

	ctx.addVar("password", generate(9, '0'..'9', 'a'..'z'))

	ctx.config.beforeSend = { HttpRequestData data ->
		switch (data.method) {
			case "POST":
				data.headers["randHead"] = "1${generate(3, '1'..'8')}"
				break
			case "PUT":
				data.headers["randHead"] = "2${generate(3, '1'..'8')}"
				break
			case "GET":
				data.headers["randHead"] = "3${generate(3, '1'..'8')}"
				break
		}
	}
}

def generate(int n, List<String>... alphaSet) {
	def list = alphaSet.flatten()
	new Random().with {
		(1..n).collect { list[nextInt(list.size())] }.join()
	}
}

def registration(Context ctx) {
	ctx.addVar("cell", "09${generate(9, '0'..'9')}", true);
}

def assertRs_registration(Context ctx, Object rsBody) {
	Assertions.assertTrue(ctx.cookies.isEmpty())
}

def assertRs_fetchCode(Context ctx, Object rsBody) {
	Assertions.assertEquals(2, ctx.cookies.size())
	Assertions.assertEquals("11", ctx.cookies.Cookie1)
	Assertions.assertEquals("22", ctx.cookies.Cookie2)
}

def assertRs_verify(Context ctx, Map rsBody) {
	Assertions.assertNotNull(rsBody.token)
	Assertions.assertNotNull(rsBody.userId)
	Assertions.assertNull(rsBody.nullProp)

	Assertions.assertEquals(2, ctx.cookies.size())
	Assertions.assertEquals("111", ctx.cookies.Cookie1)
	Assertions.assertEquals("22", ctx.cookies.Cookie2)
}

def assertRs_login(Context ctx, Object rsBody) {
	Assertions.assertEquals(1, ctx.cookies.size())
	Assertions.assertEquals("22", ctx.cookies.Cookie2)
}