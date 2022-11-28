import org.devocative.artemis.Context
import org.devocative.artemis.ctx.InitContext
import org.junit.jupiter.api.Assertions

// Called by ContextHandler Initialization
def before(InitContext init) {
	init.ctx.addVar("password", Util.generate(9, '0'..'9', 'a'..'z'))

	init.aspects.createBeforeSend({
		switch (it.data.method) {
			case "POST":
				it.data.headers["randHead"] = "1${Util.generate(3, '1'..'8')}"
				break
			case "PUT":
				it.data.headers["randHead"] = "2${Util.generate(3, '1'..'8')}"
				break
			case "GET":
				it.data.headers["randHead"] = "3${Util.generate(3, '1'..'8')}"
				break
		}
	})

	init.aspects.createCommonAssertRs({
		it
			.matches("step", {
				Artemis.log("CommonAssertRs for no-id: ${it.rqId}, this=${it.ctx.vars._this}")
			})
			.matches("verify", {
				Assertions.assertTrue(it.rqId.contains("verify"))

				if (it.rqId == 'verify_by$sms') {
					Assertions.assertEquals("sms", it.rqIdVar("by"))
				}
			})
			.other({
				Artemis.log("CommonAssertRs for id: ${it.rqId}, this=${it.ctx.vars._this}")
			})
	})
}

def registration(Context ctx) {
	ctx.addVar("cell", "09${Util.generate(9, '0'..'9')}", true);
}

def assertRs_registration(Context ctx, Object rsBody) {
	Assertions.assertTrue(ctx.cookies.isEmpty())
}

def assertRs_fetchCode(Context ctx, Object rsBody) {
	Assertions.assertEquals(2, ctx.cookies.size())
	Assertions.assertEquals("11", ctx.cookies.Cookie1)
	Assertions.assertEquals("22", ctx.cookies.Cookie2)
}

def assertRs_verify_by$sms(Context ctx, Map rsBody) {
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