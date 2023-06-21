package groovy.lang

class Helper {
	private final String message

	Helper(String message) {
		this.message = message
	}

	void printMessage() {
		println("----> Message: ${this.message}")
	}
}