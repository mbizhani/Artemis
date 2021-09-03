import org.devocative.artemis.Context

// Called by ContextHandler Initialization
def before(Context ctx) {
	Artemis.log("Inside artemis.groovy: profile=${ctx.profile}")

	ctx.addVar("password", generate(9, '0'..'9', 'a'..'z'))
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