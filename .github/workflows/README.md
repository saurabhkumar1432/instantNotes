# GitHub Actions Workflows

This directory contains the CI/CD workflows for the Voice Notes AI Android app.

## Workflows

### 1. CI Workflow (`ci.yml`)
Runs on every push to `main` and on all pull requests.

**Jobs:**
- Validates custom checks (no hardcoded UI strings, proper Composable usage)
- Builds debug APK
- Runs unit tests
- Runs lint checks
- Uploads build reports on failure

**Caching:** Uses Gradle cache to speed up builds

### 2. Release APK Workflow (`release-apk.yml`)
Automatically builds and releases an APK when code is pushed to `main` branch.

**Jobs:**
- Builds release APK
- Generates version-tagged release
- Creates GitHub Release with APK artifact
- Auto-increments version based on commit count

**Version Format:** `v{version_name}-{commit_count}-{short_sha}`
Example: `v1.0-42-a1b2c3d`

## APK Signing Setup (Optional)

To properly sign your release APKs, follow these steps:

### Step 1: Generate a Keystore
```bash
keytool -genkey -v -keystore my-release-key.keystore \
  -alias my-key-alias -keyalg RSA -keysize 2048 -validity 10000
```

### Step 2: Encode Keystore to Base64
```bash
base64 my-release-key.keystore > keystore.b64
```

### Step 3: Add GitHub Secrets
Go to your repository Settings → Secrets and variables → Actions, and add:

1. `KEYSTORE_FILE` - Contents of `keystore.b64` file
2. `KEYSTORE_PASSWORD` - Keystore password
3. `KEY_ALIAS` - Key alias (e.g., `my-key-alias`)
4. `KEY_PASSWORD` - Key password

### Step 4: Update release-apk.yml

Add these steps before "Sign APK":

```yaml
- name: Decode Keystore
  env:
    ENCODED_KEYSTORE: ${{ secrets.KEYSTORE_FILE }}
  run: |
    echo $ENCODED_KEYSTORE | base64 -di > keystore.jks

- name: Sign APK
  run: |
    jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 \
      -keystore keystore.jks \
      -storepass ${{ secrets.KEYSTORE_PASSWORD }} \
      -keypass ${{ secrets.KEY_PASSWORD }} \
      app/build/outputs/apk/release/app-release-unsigned.apk \
      ${{ secrets.KEY_ALIAS }}
```

## Troubleshooting

### Build fails with "Plugin not found"
- Ensure you're using a valid Android Gradle Plugin version
- Check that repositories in `settings.gradle.kts` are correct

### APK not uploaded
- Check the workflow logs for any errors in the "Upload APK artifact" step
- Ensure the APK path is correct

### Release not created
- Verify that `GITHUB_TOKEN` has permission to create releases
- Check that the tag doesn't already exist

## Customization

### Changing Release Trigger
To trigger releases only on tags instead of every push:

```yaml
on:
  push:
    tags:
      - 'v*'
```

### Adding Build Variants
To build multiple variants (debug, release, etc.):

```yaml
- name: Build all variants
  run: ./gradlew assembleDebug assembleRelease
```

## Performance Tips

1. **Gradle Caching**: Already configured - saves ~2-5 minutes per build
2. **Parallel Builds**: Add `org.gradle.parallel=true` to `gradle.properties`
3. **Build Scan**: Add `--scan` flag to gradle commands for detailed insights
4. **Incremental Builds**: Keep `org.gradle.caching=true` in `gradle.properties`

## Questions?

For issues with the CI/CD pipeline, check:
1. Workflow run logs in the Actions tab
2. Build reports artifacts (uploaded on failure)
3. Gradle build scans (if enabled)
