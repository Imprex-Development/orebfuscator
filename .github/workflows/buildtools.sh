build () {
	echo Building $1 ...
    mkdir $1
    cd $1

    curl -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
    java -jar BuildTools.jar --rev $1

    cd ..
}

checkVersion () {
	echo Checking version $1

	if [ ! -d ~/.m2/repository/org/spigotmc/spigot/$1-R0.1-SNAPSHOT ]; then
		build $1
	fi
}

checkVersion 1.9.4
checkVersion 1.10.2
checkVersion 1.11.2
checkVersion 1.12.2
checkVersion 1.13
checkVersion 1.13.2
checkVersion 1.14.4
checkVersion 1.15.1
checkVersion 1.16.1
checkVersion 1.16.2
checkVersion 1.16.4