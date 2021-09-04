import org.devocative.artemis.Context

/*
  Called by ContextHandler Initialization
*/

def before(Context ctx) {
}

/*
  A sample generator function, call in your XML file as ${_.generate(5, '1'..'9')}
*/

def generate(int n, List<String>... alphaSet) {
	def list = alphaSet.flatten()
	new Random().with {
		(1..n).collect { list[nextInt(list.size())] }.join()
	}
}

// ------------------------------
// For each request, you can create a function
// with the same name as id attribute, and just
// set call="true" for the request

/*
def REQUEST_ID(Context ctx) {
}
*/
