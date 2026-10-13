# Civora performance evidence

Reviewed: 11 October 2026. These measurements describe a resource-constrained Windows development host and local emulators. They are not physical-phone or production SLO measurements.

| Check | Executed result | Scope / limitation |
|---|---|---|
| Backend rush test | 50 orders; five concurrent requests per batch; 40,671 ms including fixture setup | Latest 25-test suite; verifies order correctness under bounded contention, not sustained throughput |
| Repeated checkout | Eight concurrent identical requests return the same order and pickup token | Transaction contention recovery was fixed after a real earlier INTERNAL failure |
| Backend combined suite | 25 pass, zero fail/cancel/skip; 599,343 ms | Auth, Firestore, Functions and Storage emulators; SDK/process initialization contributes materially |
| Android unit suite | 27 pass, zero failure/error | Logic, policy/timezone, notification routing, state, route estimates and ViewModel behavior |
| API 35 device suite | Four pass; 23.448 seconds on retry | Component tests and SDK isolation; first attempt failed at process startup with an ANR |
| Actual cold activity start | 17,879 ms first successful launch; 7,784 ms after instrumentation | Android 15 x86_64 emulator on a busy 16 GB host; unsuitable for judging production startup |
| d7db5a7 debug APK | 29,552,461 bytes | Development APK includes tooling; emulator configuration |
| d7db5a7 optimized APK | 4,057,534 bytes | Unsigned R8/resource-shrunk APK; latest layout/navigation fix included |
| d7db5a7 optimized AAB | 8,806,064 bytes | Unsigned bundle, not Play download size; latest layout/navigation fix included |

Host memory was below 1 GB free during the initial instrumentation ANR. System logs showed memory/IO pressure and slow unrelated Android service startup. The failed attempt is retained; its cause has not been established as exclusively host-related. A warmed retry passed all four tests. Heavy Android optimization, backend test runs and device boot are serialized to reduce contention.

No physical-device frame-time, TalkBack timing, moving GPS accuracy, battery consumption, background survival or sustained cloud-load result is claimed. Before release, measure cold/warm startup, frame percentiles, memory and battery on an actual mid-range supported phone; exercise poor network/process death and foreground trip sharing; record live backend percentile latency/error rates and budgets. Set SLOs with campus operators after those measurements.

Raw local evidence: docs/evidence/campus-admission-backend.log, campus-admission-android-final.log and device-instrumentation-retry.log. Hashes identify the built source in demo/final/artifact-hashes.json.
