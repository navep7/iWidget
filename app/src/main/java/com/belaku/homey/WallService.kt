package com.belaku.homey

import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder


class WallService : WallpaperService() {
    override fun onCreateEngine(): Engine {
        return AideEngine()
    }

    private inner class AideEngine : Engine() {
        private var mHolder: SurfaceHolder? = null
        private var mVisible = false
        private val mHandler = Handler()

        private var mCounter = 0
        private val mBackGround = Paint()
        private val mForeGround = Paint()

        private val mDrawTask = Runnable { draw() }

        init {
            mBackGround.color = Color.BLACK
            mBackGround.style = Paint.Style.FILL
            mForeGround.color = Color.RED
            mForeGround.style = Paint.Style.FILL
        }

        override fun onCreate(sh: SurfaceHolder) {
            super.onCreate(sh)
            mHolder = sh
        }

        override fun onVisibilityChanged(v: Boolean) {
            mVisible = v
            if (mVisible) {
                mHandler.post(mDrawTask)
            } else {
                mHandler.removeCallbacks(mDrawTask)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            mHandler.removeCallbacks(mDrawTask)
        }

        private fun draw() {
            if (!mVisible) return
            val canvas = mHolder!!.lockCanvas()
            try {
                mCounter += 5
                mCounter %= 360
                canvas.drawRect(
                    0f,
                    0f,
                    canvas.width.toFloat(),
                    canvas.height.toFloat(),
                    mBackGround
                )
                canvas.drawArc(0f, 0f, 400f, 400f, 0f, mCounter.toFloat(), true, mForeGround)
            } catch (ex: Exception) {
                Log.d("AideWallpaperService", ex.message!!)
            }
            mHolder!!.unlockCanvasAndPost(canvas)
            mHandler.removeCallbacks(mDrawTask)
            mHandler.postDelayed(mDrawTask, 10)
        }
    }
}