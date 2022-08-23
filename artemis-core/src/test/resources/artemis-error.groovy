import groovy.transform.Field
import org.devocative.artemis.ctx.InitContext

import java.util.concurrent.atomic.AtomicInteger

@Field static AtomicInteger counter = new AtomicInteger(1)

def before(InitContext init) {
	Artemis.log("Inside artemis-error.groovy: counter=${counter.get()}")

	def ctx = init.ctx;
	ctx.addVar("randInt", counter.getAndIncrement() % 2 == 0 ?
		Artemis.generate(3, '1'..'9') :
		Artemis.generate(3, 'a'..'z'))

	ctx.addVar("cell", "09${Artemis.generate(9, '0'..'9')}")
}