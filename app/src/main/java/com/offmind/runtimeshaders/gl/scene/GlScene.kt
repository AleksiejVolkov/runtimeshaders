package com.offmind.runtimeshaders.gl.scene

import com.offmind.runtimeshaders.gl.GlFrameInfo

interface GlScene {
    fun onSurfaceCreated()
    fun onSurfaceChanged(width: Int, height: Int) = Unit
    fun onDrawFrame(frameInfo: GlFrameInfo)
    fun release()
}
