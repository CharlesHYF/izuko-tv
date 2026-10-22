# 小米电视兼容构建的验证入口
# 创建日期：2026-10-22
# 修改日期：2026-10-22
.PHONY: docker-image verify

docker-image:
	docker build --platform linux/amd64 -t izuko-mitv-build:local tools/docker/mitv

verify:
	git diff --check
	bash -n tools/docker/mitv/run.sh tools/docker/mitv/run-inside.sh
	bash test_cases/android/regression/docker-source-sync.sh
	bash tools/docker/mitv/run.sh \
		:app:shared:app-data:testAndroidHostTest --tests '*MiTvSettingsDefaultsTest' --tests '*MiTvMediaSelectionTest' \
		:app:shared:video-player:testAndroidHostTest --tests '*ThrottledSourceLoadControlTest' \
		:app:android:auditMiTvDependencies \
		:app:android:assembleDefaultTvDebug
