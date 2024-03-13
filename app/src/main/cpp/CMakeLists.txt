cmake_minimum_required(VERSION 3.22.1)

project("jniplayground")

set(jniplaygroundSRCS native-lib.cpp)

message("Cmake CXX flags Release with debug info: ${CMAKE_CXX_FLAGS_RELWITHDEBINFO}")
message("Cmake CXX flags Release: ${CMAKE_CXX_FLAGS_RELEASE}")
message("Cmake CXX flags Debug: ${CMAKE_CXX_FLAGS_DEBUG}")

#[[
if(CMAKE_BUILD_TYPE STREQUAL "Debug")
    set(CMAKE_C_FLAGS_DEBUG "-O0")
    set(CMAKE_CXX_FLAGS_DEBUG "-O0")
else()
    set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O0")
    set(CMAKE_CXX_FLAGS_RELWITHDEBINFO "${CMAKE_CXX_FLAGS_RELWITHDEBINFO} -Ofast")
endif()
]]

add_library(${CMAKE_PROJECT_NAME} SHARED native-lib.cpp)
target_link_libraries(${CMAKE_PROJECT_NAME} android log)