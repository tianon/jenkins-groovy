freeStyleJob('apply-dsl') {
	description("""<a href="https://github.com/tianon/jenkins-groovy/tree/master/dsl" target="_blank">https://github.com/tianon/jenkins-groovy/tree/master/dsl</a>""")
	logRotator { numToKeep(5) }
	concurrentBuild(false)
	label('master') // running this job directly on the master is an order of magnitude faster than running it on a node (probably due to the fact that the DSL scripts themselves run on the master regardless)
	scm {
		git {
			remote { url('https://github.com/tianon/jenkins-groovy.git') }
			branches('*/master')
			extensions {
				cleanAfterCheckout()
			}
		}
	}
	triggers {
		scm('H/30 * * * *')
	}
	wrappers { colorizeOutput() }
	steps {
		dsl {
			external('dsl/**/*.groovy')
			removeAction('DELETE')
			removeViewAction('DELETE')
			sandbox(true)
		}
	}
}
