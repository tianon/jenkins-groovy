listView('tianon') {
	filterBuildQueue()
	filterExecutors()
	jobs {
		regex('tianon-.*|apply-dsl')
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
