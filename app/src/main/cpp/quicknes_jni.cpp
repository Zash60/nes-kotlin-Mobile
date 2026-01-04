#include <jni.h>
#include <string>
#include <libretro.h>
#include "Nes_Emu.h"

// Global variables
static Nes_Emu *emu = nullptr;
static retro_video_refresh_t video_cb = nullptr;
static retro_audio_sample_t audio_cb = nullptr;
static retro_audio_sample_batch_t audio_batch_cb = nullptr;
static retro_environment_t environ_cb = nullptr;
static retro_input_poll_t input_poll_cb = nullptr;
static retro_input_state_t input_state_cb = nullptr;
static short *frame_buffer = nullptr;

// Java VM and class references
static JavaVM *jvm = nullptr;
static jclass mainActivityClass = nullptr;
static jmethodID videoCallbackMethod = nullptr;
static jmethodID audioCallbackMethod = nullptr;

// Input state
static int16_t input_state[2][RETRO_DEVICE_ID_JOYPAD_R + 1] = {0};

// Environment callback
bool retro_environment_callback(unsigned cmd, void *data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            // Accept the pixel format set by the core
            return true;
        default:
            return false;
    }
}

// Video callback
void retro_video_callback(const void *data, unsigned width, unsigned height, size_t pitch) {
    if (jvm && mainActivityClass && videoCallbackMethod && frame_buffer) {
        // Copy frame data considering pitch
        for (unsigned y = 0; y < height; ++y) {
            memcpy(&frame_buffer[y * width], &((const short*)data)[y * (pitch / 2)], width * 2);
        }

        JNIEnv *env;
        jvm->AttachCurrentThread(&env, nullptr);

        // Create short array for RGB565 frame data
        jshortArray frameData = env->NewShortArray(width * height);
        env->SetShortArrayRegion(frameData, 0, width * height, (const jshort*)frame_buffer);

        // Call Java method
        env->CallStaticVoidMethod(mainActivityClass, videoCallbackMethod, frameData, (jint)width, (jint)height);

        env->DeleteLocalRef(frameData);
        jvm->DetachCurrentThread();
    }
}

// Audio callback
void retro_audio_callback(int16_t left, int16_t right) {
    if (jvm && mainActivityClass && audioCallbackMethod) {
        JNIEnv *env;
        jvm->AttachCurrentThread(&env, nullptr);

        // Call Java method
        env->CallStaticVoidMethod(mainActivityClass, audioCallbackMethod, (jshort)left, (jshort)right);

        jvm->DetachCurrentThread();
    }
}

// Audio batch callback
size_t retro_audio_batch_callback(const int16_t *data, size_t frames) {
    // For simplicity, call audio callback for each sample
    for (size_t i = 0; i < frames; ++i) {
        retro_audio_callback(data[i * 2], data[i * 2 + 1]);
    }
    return frames;
}

// Input poll callback
void retro_input_poll_callback(void) {
    // No-op
}

// Input state callback
int16_t retro_input_state_callback(unsigned port, unsigned device, unsigned index, unsigned id) {
    if (port < 2 && device == RETRO_DEVICE_JOYPAD && id <= RETRO_DEVICE_ID_JOYPAD_R) {
        return input_state[port][id];
    }
    return 0;
}

extern "C" {

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_init(JNIEnv *env, jclass clazz) {
    env->GetJavaVM(&jvm);
    mainActivityClass = (jclass)env->NewGlobalRef(clazz);

    // Allocate frame buffer
    frame_buffer = new short[256 * 240];

    // Get method IDs
    videoCallbackMethod = env->GetStaticMethodID(clazz, "onVideoFrame", "([SII)V");
    audioCallbackMethod = env->GetStaticMethodID(clazz, "onAudioSample", "(SS)V");

    // Set callbacks
    retro_set_environment(retro_environment_callback);
    retro_set_video_refresh(retro_video_callback);
    retro_set_audio_sample(retro_audio_callback);
    retro_set_audio_sample_batch(retro_audio_batch_callback);
    retro_set_input_poll(retro_input_poll_callback);
    retro_set_input_state(retro_input_state_callback);

    // Call retro_init
    retro_init();
}

JNIEXPORT jboolean JNICALL Java_com_example_neskotlinmobile_MainActivity_loadGame(JNIEnv *env, jclass clazz, jbyteArray romData) {
    jsize romSize = env->GetArrayLength(romData);
    jbyte *romBytes = env->GetByteArrayElements(romData, nullptr);

    struct retro_game_info game_info;
    game_info.path = nullptr;
    game_info.data = romBytes;
    game_info.size = romSize;
    game_info.meta = nullptr;

    jboolean result = retro_load_game(&game_info) ? JNI_TRUE : JNI_FALSE;

    env->ReleaseByteArrayElements(romData, romBytes, JNI_ABORT);
    return result;
}

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_runFrame(JNIEnv *env, jclass clazz) {
    // Call retro_run
    retro_run();
}

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_setInputState(JNIEnv *env, jclass clazz, jint port, jint id, jboolean pressed) {
    if (port < 2 && id <= RETRO_DEVICE_ID_JOYPAD_R) {
        input_state[port][id] = pressed ? 1 : 0;
    }
}

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_reset(JNIEnv *env, jclass clazz) {
    retro_reset();
}

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_unloadGame(JNIEnv *env, jclass clazz) {
    retro_unload_game();
}

JNIEXPORT void JNICALL Java_com_example_neskotlinmobile_MainActivity_deinit(JNIEnv *env, jclass clazz) {
    retro_deinit();
    if (frame_buffer) {
        delete[] frame_buffer;
        frame_buffer = nullptr;
    }
    if (mainActivityClass) {
        env->DeleteGlobalRef(mainActivityClass);
        mainActivityClass = nullptr;
    }
}

}