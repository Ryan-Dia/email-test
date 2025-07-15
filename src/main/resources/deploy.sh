echo "=== 배포 시작 ==="

APP_DIR="email-test"
REPO_URL="https://github.com/Ryan-Dia/email-test.git"
BRANCH="main"

# 기존 디렉토리 삭제 후 클론
if [ -d "$APP_DIR" ]; then
  echo "[INFO] 기존 디렉토리 삭제"
  rm -rf "$APP_DIR"
fi

echo "[INFO] Git 클론 중..."
git clone -b "$BRANCH" "$REPO_URL"

echo "[INFO] 빌드 시작"
./gradlew clean build

echo "[INFO] 기존 애플리케이션 종료"
pkill -f 'email-test'

echo "[INFO] 앱 실행"
nohup java -jar build/libs/email-test-0.0.1-SNAPSHOT.jar > ../ap>
cd ..

echo "[INFO] 배포 완료"
