def myView(viewFunc, viewName, viewRegex) {
	viewFunc(viewName) {
		filterBuildQueue()
		filterExecutors()
		jobs {
			regex(viewRegex)
		}
		columns {
			status()
			weather()
			name()
			lastDuration()
			lastSuccess()
			lastFailure()
			buildButton()
		}
	}
}

myView(this.&listView, 'tianon', 'tianon-.*|apply-dsl')
