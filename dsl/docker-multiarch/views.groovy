def arches = [
	'arm64',
	'armel',
	'armhf',
	'ppc64le',
	's390x',
]

def images = [
	'buildpack',
	'busybox',
	'debian',
	'gcc',
	'node',
	'php',
	'python',
	'redis',
	'ruby',
	'ubuntu',
	'wordpress',
]

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

nestedView('docker-multiarch') {
	description('BECAUSE IBM')
	filterBuildQueue()
	filterExecutors()
	columns {
		status()
		weather()
	}
	views {
		nestedView('arches') {
			filterBuildQueue()
			filterExecutors()
			columns {
				status()
				weather()
			}
			views {
				myView(delegate.&listView, '-all', 'docker-.*')
				for (arch in arches) {
					myView(delegate.&listView, arch, 'docker-' + arch + '-.*')
				}
			}
		}
		nestedView('images') {
			filterBuildQueue()
			filterExecutors()
			columns {
				status()
				weather()
			}
			views {
				myView(delegate.&listView, '-all', 'docker-.*')
				for (image in images) {
					myView(delegate.&listView, image, 'docker-.*-' + image)
				}
			}
		}
	}
}
