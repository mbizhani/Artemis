import org.devocative.artemis.ctx.InitContext

/**
  Called at the startup time
*/
def before(InitContext init) {
	/*
	  You can initialize your tests, such as:

	  Artemis.log("...")
	  init.ctx.addVar(<VAR>, <VAR_VALUE>)

	  // An interceptor before sending any request to server
	  init.aspects.createBeforeSend({
		  // `it` is `org.devocative.artemis.ctx.BeforeSendData`

		  it.data.headers["<HEADER_KEY>"] = <HEADER_VALUE>

		  switch (it.data.method) {
			  case "POST":
			  break;
			  case "PUT":
			  break
			  case "GET":
			  break
		  }
	  })

	  init.aspects.createCommonAssertRs({
		  // `it` is `org.devocative.artemis.ctx.CommonAssertRs`
		  it
			  .matches("<REG_EX>", {
				  // it.rqId, it.ctx.vars
			  })
			  .other({
			  })
	  })
	  */
}


/*
- You can call following function in your XML file by ${_.FUNC1(...)} groovy expression

def FUNC1(...) {
	...
}


- By setting `call="true"` in scenario or request, following function is called before:

def <ID_VALUE>(Context ctx) {
	// Here you can call `ctx.addVar()` to add variables to the context, and use them later.
}


- To validate the response body of a request, just set `call="true"` on the <assertRs/> tag,
  and create a function called `assertRs_<ID_VALUE>`. The first parameter is the `Context` object,
  and the second parameter is the response body, which is type of `Map` or `List`.
  Add JUnit dependency, or other assertion libraries, to validate the response body.

  - if the response body is an object
def assertRs_<ID_VALUE>(Context ctx, Map responseBody){
	// For example:
	// Assertions.assertEquals(ctx.vars.cell, responseBody.cell)
	// Assertions.assertNotNull(responseBody.token)
	// Assertions.assertNotNull(responseBody.userId)
}

  - if the response body is an array
def assertRs_<ID_VALUE>(Context ctx, List responseBody){
}

*/
