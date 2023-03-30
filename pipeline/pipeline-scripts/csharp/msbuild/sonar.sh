SONARQUBE_URL=$1
SONARQUBE_LOGIN=$2
SONARQUBE_PASSWORD=$3
SONARQUBE_PROJECT_KEY=$4
SONARQUBE_SOURCES_PATH=$5
DOCKER_WORKSPACE_PATH=$6
BIND_MOUNT=$7

docker run --rm -v ${BIND_MOUNT}:/src -w /src mcr.microsoft.com/dotnet/sdk:5.0 sh -c "
ls -la /src;
dotnet tool install --global dotnet-sonarscanner;
export PATH=\"$PATH:/root/.dotnet/tools\";
dotnet sonarscanner begin /k:\"${SONARQUBE_PROJECT_KEY}\" /d:sonar.host.url=\"${SONARQUBE_URL}\" /d:sonar.login=\"${SONARQUBE_LOGIN}\" /d:sonar.password=\"${SONARQUBE_PASSWORD}\";
dotnet restore \"/src\";
dotnet build \"/src\";
dotnet sonarscanner end /d:sonar.login=\"${SONARQUBE_LOGIN}\" /d:sonar.password=\"${SONARQUBE_PASSWORD}\";
"
