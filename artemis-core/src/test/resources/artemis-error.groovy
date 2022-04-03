import groovy.transform.Field
import org.devocative.artemis.Context

import java.util.concurrent.atomic.AtomicInteger

@Field static AtomicInteger counter = new AtomicInteger(1)

def before(Context ctx) {
	Artemis.log("Inside artemis-error.groovy: counter=${counter.get()}")

	ctx.addVar("randInt", counter.getAndIncrement() % 2 == 0 ?
			Artemis.generate(3, '1'..'9') :
			Artemis.generate(3, 'a'..'z'))

	ctx.addVar("cell", "09${Artemis.generate(9, '0'..'9')}")
}