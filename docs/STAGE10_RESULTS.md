# Stage 10 Verification Results

This record separates automated evidence from in-game compatibility. It applies
to the Stage 10 working tree based on commit `e78bcc9` before the final Stage 10
commit.

## Automated Checks

| Check | Environment | Result |
| --- | --- | --- |
| Java compilation and JUnit suite | Linux, Java 21, Gradle 9.6.0 | PASS |
| `compileClientJava` | Linux, Minecraft 1.21.11 mappings | PASS |
| `clean test build` | Linux | PASS |
| English/Russian key parity | JUnit | PASS |
| Legacy/current/future config handling | JUnit | PASS |
| Death path ownership and screenshot failure retention | JUnit | PASS |
| Remapped JAR integrity and metadata | Gradle `verifyReleaseJar` | PASS |
| `git diff --check` | Linux | PASS |

The final commit and artifact checksum are reported with the completed build;
they are not pre-recorded here because both change when this document is added.

## Manual Coverage

The user confirmed the pre-Stage-9 feature set in Minecraft after Stage 8. No
complete, dated Stage 9/10 runtime matrix was recorded in this workspace.

| Platform or server | Status | Notes |
| --- | --- | --- |
| Linux client, Stages 1-8 | PASS | User-confirmed gameplay check |
| Linux client, Toggle Walk/Freelook final lifecycle fixes | NOT RECORDED | Requires final JAR gameplay check |
| Windows client | NOT AVAILABLE | Not tested in this environment |
| macOS client | NOT AVAILABLE | Not tested in this environment |
| Singleplayer final regression | NOT RECORDED | Requires final JAR gameplay check |
| Vanilla-compatible Fabric server | NOT RECORDED | No server result supplied |
| Paper/Spigot | NOT AVAILABLE | No test server supplied |
| Realms | NOT AVAILABLE | No account/environment supplied |

CI validates pure cross-platform logic and the Linux build only. It does not
claim camera-mixin, desktop-command, graphics-driver, or server-policy
compatibility for an untested environment.
