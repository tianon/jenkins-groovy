listView('official-images') {
	filterBuildQueue()
	filterExecutors()
	jobs {
		regex('official-images-.*')
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
