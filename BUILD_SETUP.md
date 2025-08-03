# Build Setup and Troubleshooting

## Issues Fixed

### 1. Java Version Compatibility
- **Problem**: Android Gradle Plugin 8.7.3 requires Java 17, but system was using Java 11
- **Solution**: Updated `gradle.properties` to explicitly use Java 17:
  ```properties
  org.gradle.java.home=/Users/alexvolkov/Library/Java/JavaVirtualMachines/jbr-17.0.11/Contents/Home
  ```

### 2. Gradle Version Consistency
- **Problem**: IntelliJ IDEA was detecting different Gradle versions (8.2 vs 8.9)
- **Solution**: 
  - Regenerated Gradle wrapper with `gradle-8.9-all.zip` distribution
  - Added IntelliJ IDEA specific `.idea/gradle.xml` configuration
  - Disabled configuration cache temporarily for IDE compatibility

### 3. Java Compilation Target
- **Problem**: Project was using Java 1.8 which might cause IDE compatibility issues
- **Solution**: Updated to Java 17 for consistency:
  ```kotlin
  compileOptions {
      sourceCompatibility = JavaVersion.VERSION_17
      targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
      jvmTarget = "17"
  }
  ```

## IntelliJ IDEA Setup Instructions

### Step 1: Open Project
1. Open IntelliJ IDEA
2. File → Open → Select the project root directory
3. Wait for Gradle sync to complete

### Step 2: Gradle Settings
1. Go to File → Settings → Build, Execution, Deployment → Build Tools → Gradle
2. Ensure the following settings:
   - **Use Gradle from**: 'gradle-wrapper.properties' file
   - **Gradle JVM**: Project SDK (jbr-17)
   - **Build and run using**: Gradle
   - **Run tests using**: Gradle

### Step 3: Project SDK
1. Go to File → Project Structure → Project
2. Set **Project SDK** to: jbr-17 (JetBrains Runtime 17.0.11)
3. Set **Project language level** to: 17

### Step 4: Verify Build
1. Open Gradle tool window (View → Tool Windows → Gradle)
2. Run: Tasks → build → clean
3. Run: Tasks → build → build
4. Run: Tasks → android → assembleDebug

## Command Line Verification

To verify everything works from command line:

```bash
# Clean build
./gradlew clean

# Full build
./gradlew build

# Create debug APK
./gradlew assembleDebug
```

## Project Structure

- **Custom Shader Generation**: The project uses custom Gradle tasks to generate shader dependency mappings
- **Generated Files**: Located in `app/build/generated/src/main/java/com/offmind/runtimeshaders/generated/`
- **Build Output**: APK files are in `app/build/outputs/apk/debug/`

## Current Status

✅ **All Fixed Issues:**
- Java 17 compatibility
- Gradle 8.9 consistency
- IntelliJ IDEA configuration
- Build and assembly working
- APK generation successful

The project is now ready for development in IntelliJ IDEA!
