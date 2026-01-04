cmake_minimum_required(VERSION 3.10.2)

project("neskotlinmobile")

# Set the path to QuickNES
set(QUICKNES_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/QuickNES)

# Include directories
include_directories(
    ${QUICKNES_DIR}
    ${QUICKNES_DIR}/nes_emu
    ${QUICKNES_DIR}/libretro
    ${QUICKNES_DIR}/libretro/libretro-common/include
)

# Source files
set(SOURCES
    ${QUICKNES_DIR}/libretro/libretro.cpp
    ${QUICKNES_DIR}/nes_emu/abstract_file.cpp
    ${QUICKNES_DIR}/nes_emu/apu_state.cpp
    ${QUICKNES_DIR}/nes_emu/Blip_Buffer.cpp
    ${QUICKNES_DIR}/nes_emu/Effects_Buffer.cpp
    ${QUICKNES_DIR}/nes_emu/Multi_Buffer.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Apu.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Buffer.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Cart.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Core.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Cpu.cpp
    ${QUICKNES_DIR}/nes_emu/nes_data.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Effects_Buffer.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Emu.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_File.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Fme7_Apu.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Mapper.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Vrc7.cpp
    ${QUICKNES_DIR}/nes_emu/emu2413.cpp
    ${QUICKNES_DIR}/nes_emu/emu2413_state.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Namco_Apu.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Oscs.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Ppu.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Ppu_Impl.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Ppu_Rendering.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_State.cpp
    ${QUICKNES_DIR}/nes_emu/nes_util.cpp
    ${QUICKNES_DIR}/nes_emu/Nes_Vrc6_Apu.cpp
    ${QUICKNES_DIR}/nes_emu/Data_Reader.cpp
    ${QUICKNES_DIR}/nes_emu/nes_ntsc.cpp
    # Add JNI wrapper
    quicknes_jni.cpp
)

# Add definitions
add_definitions(-DANDROID -D__LIBRETRO__ -Wall -Wno-multichar -Wno-unused-variable -Wno-sign-compare -DNO_UNALIGNED_ACCESS)

# Build shared library
add_library(quicknes SHARED ${SOURCES})

# Link libraries if needed
target_link_libraries(quicknes log)