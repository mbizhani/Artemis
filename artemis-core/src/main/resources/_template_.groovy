import org.devocative.artemis.Context

/*
  Called at the startup time
*/
def before(Context ctx) {
	/*
	You can call any initialization statements here, such as:

	Artemis.log("...")
	ctx.addVar(<VAR>, <VAR_VALUE>)

	// An interceptor before sending any request to server
	ctx.config.beforeSend = { HttpRequestData rqData ->
		rqData.headers["<HEADER_KEY>"] = <HEADER_VALUE>
		// alter sending request by setting data to 'rqData' properties
	}
	*/
}

/*
  A sample function, callable in XML file as ${_.generate(5, '1'..'9')}
*/
def generate(int n, List<String>... alphaSet) {
	def list = alphaSet.flatten()
	new Random().with {
		(1..n).collect { list[nextInt(list.size())] }.join()
	}
}

// ------------------------------

/*

 // You can set `call="true"` in the XML request tag, and then create a function
 // with the same name as its id. This function is called before sending the request.

def <ID_VALUE>(Context ctx) {
	// Here you can call `ctx.addVar()` to add variables to the context, and use them later.
}


  // To validate the response body of a request, just set `call="true"` on the <assertRs/> tag,
  // and create a function called `assertRs_<ID_VALUE>`. The first parameter is the `Context` object,
  // and the second parameter is the response body, which is type of `Map` or `List`.
  // Add JUnit dependency, or other assertion libraries, to validate the response body.

  // If the response body is an object
def assertRs_<ID_VALUE>(Context ctx, Map responseBody){
	// For example:
	// Assertions.assertEquals(ctx.vars.cell, responseBody.cell)
	// Assertions.assertNotNull(responseBody.token)
	// Assertions.assertNotNull(responseBody.userId)
}

  // If the response body is an array
def assertRs_<ID_VALUE>(Context ctx, List responseBody){
}

*/
