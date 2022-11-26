package groovy.lang

class Util {
	static def generate(int n, List<String>... alphaSet) {
		def list = alphaSet.flatten()
		new Random().with {
			(1..n).collect { list[nextInt(list.size())] }.join()
		}
	}
}
