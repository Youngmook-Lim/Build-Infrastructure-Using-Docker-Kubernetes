SONAR_HOST="$1"
SONAR_LOGIN="$2"
SONAR_PASSWORD="$3"
TARGET_COMMIT="$4"
REPOSITORY_NAME="$5"
BUILD_PATH="$6"

cd ${BUILD_PATH}

mvn sonar:sonar \
  -Dsonar.host.url=${SONAR_HOST} \
  -Dsonar.login=${SONAR_LOGIN} \
  -Dsonar.password=${SONAR_PASSWORD} \
  -Dsonar.projectKey=${TARGET_COMMIT} \
  -Dsonar.projectName=${REPOSITORY_NAME} \
  -Dsonar.verbose=true
