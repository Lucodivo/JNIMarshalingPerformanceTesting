cmake_minimum_required(VERSION 3.22.1)

project("jniplayground")

# build cpufeatures as a static lib
add_library(cpufeatures STATIC
        ${ANDROID_NDK}/sources/android/cpufeatures/cpu-features.c)

#[[
if(ANDROID_ABI STREQUAL armeabi-v7a)
    set_target_properties(${CMAKE_PROJECT_NAME} PROPERTIES COMPILE_FLAGS -mfpu=neon)
endif()
]]

# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
set(jniplaygroundSRCS native-lib.cpp)
set_property(SOURCE ${jniplaygroundSRCS} APPEND_STRING PROPERTY COMPILE_FLAGS " -mfpu=neon")
add_definitions("-DHAVE_NEON=1")
add_library(${CMAKE_PROJECT_NAME} SHARED native-lib.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android cpufeatures log)